# -*- coding: utf-8 -*-
"""
Zorza Launcher

Uruchom:  uruchom.bat   (albo:  python launcher.py)

Backend w Pythonie (profile, pobieranie gry, mody, konta, Discord), a wygląd to strona
HTML otwierana w Microsoft Edge w trybie aplikacji (bez pasków przeglądarki).
"""

import base64
import ctypes
import html
import json
import os
import re
import shutil
import sqlite3
import struct
import subprocess
import sys
import threading
import time
import traceback
import urllib.parse
import uuid
import webbrowser
import zlib
from collections import deque
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

# ---------------------------------------------------------------------------
#  USTAWIENIA – tu możesz zmieniać
# ---------------------------------------------------------------------------

NAZWA_LAUNCHERA = "Zorza"          # nazwa wyświetlana w launcherze
NAZWA_DISCORD = "Zorza Client"     # tak ma się nazywać aplikacja w Discord Developer Portal
DOMYSLNA_WERSJA = "1.21.4"         # wersja pierwszego profilu
NAJSTARSZA_WERSJA = "1.20.1"       # starszych wersji launcher nie pokazuje
PORT = 47615                       # NIE ZMIENIAJ – jest wpisany w Azure
GITHUB_URL = "https://github.com/Lizqk054319/zorza"

# Logowanie Microsoft – NIE ZMIENIAJ (zatwierdzane przez Mojang dla tego ID)
AZURE_CLIENT_ID = "6553e03b-b905-4f30-a877-535ed1c3a975"
REDIRECT_URI = f"http://localhost:{PORT}/callback"

# Discord Rich Presence – wklej tu "Application ID" z discord.com/developers (instrukcja w INSTRUKCJA.txt)
DISCORD_APP_ID = ""

# ---------------------------------------------------------------------------

try:
    import requests
    import minecraft_launcher_lib as mll
except ImportError:
    print("Brakuje bibliotek. Uruchom launcher przez plik uruchom.bat")
    print("albo wpisz:  python -m pip install -r requirements.txt")
    input("Naciśnij Enter, aby zamknąć...")
    sys.exit(1)

APP_DIR = Path(__file__).resolve().parent
UI_DIR = APP_DIR / "ui"
APPDATA = Path(os.environ.get("APPDATA", Path.home()))
HOME = Path.home()
DATA_DIR = APPDATA / "ZorzaLauncher"
MC_DIR = DATA_DIR / "minecraft"          # wspólne pliki gry (wersje, biblioteki, Java)
INST_DIR = DATA_DIR / "instances"        # osobny folder gry dla każdego profilu
TRASH_DIR = DATA_DIR / "usuniete"        # usunięte profile trafiają tu, nie znikają od razu
SKIN_DIR = DATA_DIR / "skiny"
ACCOUNTS_FILE = DATA_DIR / "accounts.json"
SETTINGS_FILE = DATA_DIR / "settings.json"
PROFILES_FILE = DATA_DIR / "profiles.json"
BG_EXTS = (".jpg", ".jpeg", ".png", ".webp", ".gif")
for d in (DATA_DIR, MC_DIR, INST_DIR, SKIN_DIR):
    d.mkdir(parents=True, exist_ok=True)

KINDS = {  # rodzaj zawartości -> (folder w profilu, typ projektu na Modrinth)
    "mods": ("mods", "mod"),
    "resourcepacks": ("resourcepacks", "resourcepack"),
    "shaders": ("shaderpacks", "shader"),
}
UA = {"User-Agent": f"ZorzaLauncher/0.2 ({GITHUB_URL})"}
MODRINTH = "https://api.modrinth.com/v2"

lock = threading.RLock()
log_lines = deque(maxlen=3000)
log_counter = 0
task = {"running": False, "status": "", "progress": 0, "max": 0, "error": "", "done": ""}
game = {"proc": None, "profile": None, "where": "", "start": 0}
last_ping = time.time()
pending_logins = {}   # state -> code_verifier
login_message = {"text": "", "ok": True}
version_cache = {"list": None}


def log(msg):
    global log_counter
    with lock:
        for line in str(msg).splitlines() or [""]:
            log_lines.append((log_counter, line))
            log_counter += 1
    try:
        print(msg)
    except Exception:
        pass


def load_json(path, default):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        return default


def save_json(path, data):
    tmp = path.with_suffix(".tmp")
    tmp.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")
    tmp.replace(path)


settings = load_json(SETTINGS_FILE, {})
settings.setdefault("jvm_args", "")
settings.setdefault("remember_accounts", True)
settings.setdefault("active_account", None)
settings.setdefault("active_profile", None)
settings.setdefault("discord", True)
settings.pop("owner_token", None)

# Konta: zapisywane tylko na tym komputerze (nick, UUID i token odświeżania).
# Hasło i e-mail nigdy nie trafiają do launchera. Na Windowsie plik jest szyfrowany
# systemowo (DPAPI) – da się go odczytać tylko na tym koncie Windows, na tym komputerze.

class _Blob(ctypes.Structure):
    _fields_ = [("cbData", ctypes.c_uint32), ("pbData", ctypes.POINTER(ctypes.c_char))]


def _dpapi(data, encrypt):
    crypt32, kernel32 = ctypes.windll.crypt32, ctypes.windll.kernel32
    buf = ctypes.create_string_buffer(data, len(data))
    src, out = _Blob(len(data), ctypes.cast(buf, ctypes.POINTER(ctypes.c_char))), _Blob()
    fn = crypt32.CryptProtectData if encrypt else crypt32.CryptUnprotectData
    if not fn(ctypes.byref(src), None, None, None, None, 0x1, ctypes.byref(out)):
        raise OSError("DPAPI nie zadziałało")
    try:
        return ctypes.string_at(out.pbData, out.cbData)
    finally:
        kernel32.LocalFree(out.pbData)


def load_accounts():
    raw = load_json(ACCOUNTS_FILE, [])
    if isinstance(raw, list):          # stary, niezaszyfrowany zapis – zaszyfruje się przy zapisie
        return raw
    try:
        if raw.get("enc") == "dpapi":
            return json.loads(_dpapi(base64.b64decode(raw["data"]), False))
    except Exception as e:
        log(f"Nie udało się odczytać zapisanych kont ({e}) – zaloguj się ponownie.")
    return []


accounts = load_accounts() if settings["remember_accounts"] else []


def save_settings():
    save_json(SETTINGS_FILE, settings)


def save_accounts():
    if settings["remember_accounts"]:
        data = json.dumps(accounts).encode()
        if os.name == "nt":
            save_json(ACCOUNTS_FILE, {"enc": "dpapi", "data": base64.b64encode(_dpapi(data, True)).decode()})
        else:
            save_json(ACCOUNTS_FILE, accounts)
    elif ACCOUNTS_FILE.exists():
        ACCOUNTS_FILE.unlink()


# ---------------------------------------------------------------------------
#  Wersje gry
# ---------------------------------------------------------------------------

def get_versions():
    """Wersje release od najnowszej do NAJSTARSZA_WERSJA włącznie."""
    if version_cache["list"] is None:
        try:
            vl = [v["id"] for v in mll.utils.get_version_list() if v["type"] == "release"]
            if NAJSTARSZA_WERSJA in vl:
                vl = vl[:vl.index(NAJSTARSZA_WERSJA) + 1]
            version_cache["list"] = vl
        except Exception as e:
            log(f"Nie udało się pobrać listy wersji: {e}")
            return [DOMYSLNA_WERSJA]
    return version_cache["list"]


def group_key(v):
    parts = v.split(".")
    return ".".join(parts[:2]) if parts[0] == "1" else parts[0]


def installed_ids():
    try:
        return {v["id"] for v in mll.utils.get_installed_versions(str(MC_DIR))}
    except Exception:
        return set()


# ---------------------------------------------------------------------------
#  Profile
# ---------------------------------------------------------------------------

profiles = load_json(PROFILES_FILE, [])


def save_profiles():
    save_json(PROFILES_FILE, profiles)


def new_profile_id(name):
    base = re.sub(r"[^a-z0-9]+", "-", name.lower()).strip("-")[:24] or "profil"
    pid, n = base, 2
    while any(p["id"] == pid for p in profiles) or (INST_DIR / pid).exists():
        pid, n = f"{base}-{n}", n + 1
    return pid


def make_profile(name, version, loader="fabric", ram_mb=4096):
    name = (name or "").strip()[:40] or f"Minecraft {version}"
    p = {"id": new_profile_id(name), "name": name, "version": version,
         "loader": "vanilla" if loader == "vanilla" else "fabric",
         "ram_mb": max(1024, min(32768, int(ram_mb))), "created": int(time.time())}
    profiles.append(p)
    profile_dir(p)
    settings["active_profile"] = p["id"]
    save_profiles()
    save_settings()
    log(f"Utworzono profil {p['name']} ({p['version']})")
    return p


def migrate():
    """Pierwsze uruchomienie / stara wersja launchera bez profili."""
    if profiles:
        return
    old = [d for d in INST_DIR.iterdir() if d.is_dir()] if INST_DIR.exists() else []
    for d in old:   # stare foldery nazwane wersją, np. instances/1.21.4
        profiles.append({"id": d.name, "name": f"Minecraft {d.name}", "version": d.name,
                         "loader": "fabric", "ram_mb": int(settings.get("ram_mb", 4096)),
                         "created": int(time.time())})
    if not profiles:
        profiles.append({"id": "glowny", "name": "Główny", "version": DOMYSLNA_WERSJA,
                         "loader": "fabric", "ram_mb": int(settings.get("ram_mb", 4096)),
                         "created": int(time.time())})
    save_profiles()


migrate()


def get_profile(pid=None):
    pid = pid or settings["active_profile"]
    p = next((p for p in profiles if p["id"] == pid), None)
    if not p:
        p = profiles[0]
        settings["active_profile"] = p["id"]
    return p


def profile_dir(p):
    d = INST_DIR / p["id"]
    for folder, _ in KINDS.values():
        (d / folder).mkdir(parents=True, exist_ok=True)
    return d


def meta_path(p):
    return profile_dir(p) / ".zorza.json"


def safe_child(folder, name):
    p = (folder / name).resolve()
    if p.parent != folder.resolve() or not name or name in (".", ".."):
        raise ValueError("Nieprawidłowa nazwa pliku")
    return p


# --- kody profili (udostępnianie) ------------------------------------------

def export_code(p):
    meta = load_json(meta_path(p), {}).get("modrinth", {})
    d = profile_dir(p)
    items = {"mods": [], "resourcepacks": [], "shaders": []}
    for pid, m in meta.items():
        kind = next((k for k, (f, _) in KINDS.items() if f == m["folder"]), None)
        if kind and (d / m["folder"] / m["file"]).exists():
            items[kind].append(pid)
    data = {"n": p["name"], "v": p["version"], "l": p["loader"], "r": p["ram_mb"],
            "m": items["mods"], "rp": items["resourcepacks"], "sh": items["shaders"]}
    raw = zlib.compress(json.dumps(data, separators=(",", ":")).encode(), 9)
    return "ZORZA-" + base64.urlsafe_b64encode(raw).decode().rstrip("=")


def decode_code(code):
    code = re.sub(r"\s+", "", code or "")
    if not code.upper().startswith("ZORZA-"):
        raise ValueError("To nie jest kod profilu Zorzy (powinien zaczynać się od ZORZA-).")
    body = code[6:]
    try:
        data = json.loads(zlib.decompress(base64.urlsafe_b64decode(body + "=" * (-len(body) % 4))))
        assert isinstance(data.get("v"), str)
    except Exception:
        raise ValueError("Kod jest uszkodzony – skopiuj go jeszcze raz w całości.")
    return data


def do_import_code(data):
    p = make_profile(data.get("n"), data["v"], data.get("l", "fabric"), data.get("r", 4096))
    todo = [(x, "mods") for x in data.get("m", [])] + \
           [(x, "resourcepacks") for x in data.get("rp", [])] + \
           [(x, "shaders") for x in data.get("sh", [])]
    failed = []
    for i, (pid, kind) in enumerate(todo):
        task.update(max=len(todo), progress=i)
        try:
            modrinth_install(pid, kind, p)
        except Exception as e:
            failed.append(pid)
            log(f"Nie udało się zainstalować {pid}: {e}")
    task["done"] = f"Dodano profil {p['name']}" + (f" ({len(failed)} elementów pominięto)" if failed else "")


# ---------------------------------------------------------------------------
#  Import z innych launcherów
# ---------------------------------------------------------------------------

def parse_version_id(vid):
    """'fabric-loader-0.16.9-1.21.4' -> ('1.21.4','fabric');  '1.21.4' -> ('1.21.4','vanilla')"""
    vid = vid or ""
    m = re.match(r"fabric-loader-[^-]+-(.+)$", vid)
    if m:
        return m.group(1), "fabric"
    m = re.match(r"(?:quilt-loader-[^-]+-|neoforge-|forge-)?(\d+\.\d+(?:\.\d+)?)", vid)
    if m:
        return m.group(1), "fabric" if vid.startswith(("quilt", "neoforge", "forge")) else "vanilla"
    return "", "vanilla"


def dir_counts(d):
    out = {}
    for key, folder in (("mods", "mods"), ("resourcepacks", "resourcepacks"),
                        ("shaders", "shaderpacks"), ("saves", "saves")):
        p = d / folder
        try:
            out[key] = sum(1 for _ in p.iterdir()) if p.is_dir() else 0
        except OSError:
            out[key] = 0
    return out


def ram_from_args(args):
    m = re.search(r"-Xmx(\d+)([mMgG])", args or "")
    if not m:
        return 4096
    n = int(m.group(1))
    return n * 1024 if m.group(2).lower() == "g" else n


def scan_official():
    root = APPDATA / ".minecraft"
    lp = load_json(root / "launcher_profiles.json", None)
    if lp is None:
        return None
    latest = None
    out = []
    for key, pr in (lp.get("profiles") or {}).items():
        vid = pr.get("lastVersionId", "")
        if vid in ("latest-release", "latest-snapshot"):
            if vid == "latest-snapshot":
                continue
            latest = latest or (get_versions() or [DOMYSLNA_WERSJA])[0]
            ver, loader = latest, "vanilla"
        else:
            ver, loader = parse_version_id(vid)
        gd = Path(pr["gameDir"]) if pr.get("gameDir") else root
        name = pr.get("name") or ("Najnowsza wersja" if pr.get("type") == "latest-release" else vid)
        out.append({"key": key, "name": name, "version": ver, "loader": loader,
                    "ram_mb": ram_from_args(pr.get("javaArgs")), "path": str(gd),
                    "counts": dir_counts(gd), "options": (gd / "options.txt").exists()})
    return {"id": "official", "name": "Oficjalny launcher Minecraft", "path": str(root), "profiles": out}


def scan_modrinth():
    for base in (APPDATA / "ModrinthApp", APPDATA / "com.modrinth.theseus"):
        pdir = base / "profiles"
        if not pdir.is_dir():
            continue
        info = {}
        db = base / "app.db"
        if db.exists():
            try:
                con = sqlite3.connect(f"file:{db}?mode=ro", uri=True)
                for row in con.execute("SELECT path, name, game_version, mod_loader FROM profiles"):
                    info[row[0]] = {"name": row[1], "version": row[2], "loader": row[3]}
                con.close()
            except Exception as e:
                log(f"Modrinth App: nie udało się odczytać bazy ({e})")
        out = []
        for d in sorted(pdir.iterdir()):
            if not d.is_dir():
                continue
            i = info.get(d.name)
            if not i:   # starsze wersje Modrinth App: profile.json w folderze
                pj = load_json(d / "profile.json", {})
                meta = pj.get("metadata", {})
                i = {"name": meta.get("name", d.name), "version": meta.get("game_version", ""),
                     "loader": meta.get("loader", "")}
            out.append({"key": d.name, "name": i["name"] or d.name, "version": i["version"] or "",
                        "loader": "vanilla" if (i["loader"] or "vanilla") == "vanilla" else "fabric",
                        "ram_mb": 4096, "path": str(d), "counts": dir_counts(d),
                        "options": (d / "options.txt").exists()})
        return {"id": "modrinth", "name": "Modrinth App", "path": str(base), "profiles": out}
    return None


def scan_prism():
    for base in (APPDATA / "PrismLauncher", APPDATA / "MultiMC"):
        idir = base / "instances"
        if not idir.is_dir():
            continue
        out = []
        for d in sorted(idir.iterdir()):
            cfg = d / "instance.cfg"
            if not cfg.exists():
                continue
            conf = {}
            for line in cfg.read_text(encoding="utf-8", errors="replace").splitlines():
                if "=" in line:
                    k, v = line.split("=", 1)
                    conf[k.strip()] = v.strip()
            pack = load_json(d / "mmc-pack.json", {})
            ver, loader = "", "vanilla"
            for c in pack.get("components", []):
                if c.get("uid") == "net.minecraft":
                    ver = c.get("version", "")
                elif c.get("uid") in ("net.fabricmc.fabric-loader", "org.quiltmc.quilt-loader"):
                    loader = "fabric"
            gd = d / ".minecraft" if (d / ".minecraft").is_dir() else d / "minecraft"
            ram = int(conf["MaxMemAlloc"]) if conf.get("MaxMemAlloc", "").isdigit() else 4096
            out.append({"key": d.name, "name": conf.get("name", d.name), "version": ver,
                        "loader": loader, "ram_mb": ram, "path": str(gd), "counts": dir_counts(gd),
                        "options": (gd / "options.txt").exists()})
        return {"id": "prism", "name": "Prism Launcher" if "Prism" in base.name else "MultiMC",
                "path": str(base), "profiles": out}
    return None


def scan_lunar():
    """Lunar Client: ustawienia gry trzyma w .minecraft, mody Fabric w .lunarclient/profiles."""
    lroot = HOME / ".lunarclient"
    if not lroot.is_dir():
        return None
    mc = APPDATA / ".minecraft"
    out = []
    pdir = lroot / "profiles"
    if pdir.is_dir():
        for mods in pdir.glob("**/mods"):
            rel = mods.relative_to(pdir).parts
            if len(rel) > 5:
                continue
            ver = next((x for x in reversed(rel) if re.fullmatch(r"\d+\.\d+(\.\d+)?", x)), "")
            if not ver:
                m = re.search(r"(\d+\.\d+(?:\.\d+)?)", "/".join(rel))
                ver = m.group(1) if m else ""
            try:
                n = sum(1 for f in mods.iterdir() if f.suffix == ".jar")
            except OSError:
                n = 0
            if n:
                out.append({"key": str(mods), "name": f"Lunar Client {ver}".strip(), "version": ver,
                            "loader": "fabric", "ram_mb": 4096, "path": str(mc),
                            "mods_path": str(mods), "counts": {**dir_counts(mc), "mods": n},
                            "options": (mc / "options.txt").exists()})
    if not out:
        out.append({"key": "lunar", "name": "Lunar Client (ustawienia)", "version": "",
                    "loader": "fabric", "ram_mb": 4096, "path": str(mc),
                    "counts": {**dir_counts(mc), "mods": 0}, "options": (mc / "options.txt").exists()})
    return {"id": "lunar", "name": "Lunar Client", "path": str(lroot), "profiles": out}


def scan_launchers():
    found = []
    for fn in (scan_official, scan_modrinth, scan_prism, scan_lunar):
        try:
            r = fn()
            if r:
                found.append(r)
        except Exception as e:
            log(f"Błąd skanowania ({fn.__name__}): {e}")
    return found


def copy_tree(src, dst):
    if not src.exists():
        return 0
    n = 0
    if src.is_file():
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dst)
        return 1
    for f in src.rglob("*"):
        if f.is_file():
            t = dst / f.relative_to(src)
            t.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(f, t)
            n += 1
            if n % 25 == 0:
                task["progress"] = n
    return n


def do_import_launcher(d):
    launcher = next((l for l in scan_launchers() if l["id"] == d["launcher"]), None)
    if not launcher:
        raise RuntimeError("Nie znaleziono tego launchera.")
    src = next((p for p in launcher["profiles"] if p["key"] == d["key"]), None)
    if not src:
        raise RuntimeError("Nie znaleziono tego profilu.")
    p = make_profile(d.get("name") or src["name"], d["version"], d.get("loader", src["loader"]),
                     d.get("ram_mb", src["ram_mb"]))
    dst = profile_dir(p)
    gd = Path(src["path"])
    c = d.get("copy", {})
    task.update(status=f"Kopiowanie z: {launcher['name']}...", max=0)
    if c.get("mods"):
        copy_tree(Path(src.get("mods_path") or gd / "mods"), dst / "mods")
    if c.get("settings"):
        for f in ("options.txt", "optionsof.txt", "optionsshaders.txt"):
            copy_tree(gd / f, dst / f)
        copy_tree(gd / "config", dst / "config")
    if c.get("packs"):
        copy_tree(gd / "resourcepacks", dst / "resourcepacks")
        copy_tree(gd / "shaderpacks", dst / "shaderpacks")
    if c.get("servers"):
        copy_tree(gd / "servers.dat", dst / "servers.dat")
    if c.get("saves"):
        copy_tree(gd / "saves", dst / "saves")
    task["done"] = f"Zaimportowano profil {p['name']}"
    log(f"Import z {launcher['name']} ({src['name']}) zakończony")


# ---------------------------------------------------------------------------
#  Instalacja i uruchamianie
# ---------------------------------------------------------------------------

def make_callback():
    return {
        "setStatus": lambda s: (task.update(status=str(s)), log(f"[instalacja] {s}")),
        "setProgress": lambda p: task.update(progress=int(p)),
        "setMax": lambda m: task.update(max=int(m), progress=0),
    }


def java_for(version):
    try:
        info = mll.runtime.get_version_runtime_information(version, str(MC_DIR))
        if info:
            return mll.runtime.get_executable_path(info["name"], str(MC_DIR))
    except Exception:
        pass
    return None


def install_profile(p, force=False):
    """Instaluje wersję profilu (z Fabricem, jeśli wybrany). Zwraca id wersji do uruchomienia."""
    version = p["version"]
    cb = make_callback()
    if force or version not in installed_ids():
        task.update(status=f"Pobieranie Minecraft {version}...")
        mll.install.install_minecraft_version(version, str(MC_DIR), callback=cb)
    if p["loader"] != "fabric":
        return version
    try:
        fabric_ok = mll.fabric.is_minecraft_version_supported(version)
    except Exception:
        fabric_ok = False
    if not fabric_ok:
        log(f"Fabric nie obsługuje {version} – uruchamiam bez modów.")
        return version
    loader = mll.fabric.get_latest_loader_version()
    fabric_id = f"fabric-loader-{loader}-{version}"
    if force or fabric_id not in installed_ids():
        task.update(status="Instalowanie Fabric...")
        mll.fabric.install_fabric(version, str(MC_DIR), loader_version=loader,
                                  callback=cb, java=java_for(version))
    return fabric_id


def refresh_account(acc):
    """Odświeża token gry (ważny ok. 24 h) na podstawie tokenu odświeżania."""
    data = mll.microsoft_account.complete_refresh(
        AZURE_CLIENT_ID, None, REDIRECT_URI, acc["refresh_token"])
    if "error" in data:
        raise RuntimeError("Sesja konta wygasła – usuń konto i zaloguj się ponownie.")
    acc["refresh_token"] = data["refresh_token"]
    acc["name"] = data["name"]
    save_accounts()
    return data["access_token"]


def run_task(fn, *args):
    if task["running"]:
        return False
    task.update(running=True, status="Przygotowanie...", progress=0, max=0, error="", done="")

    def worker():
        try:
            fn(*args)
        except Exception as e:
            task["error"] = friendly_error(e)
            log("BŁĄD: " + "".join(traceback.format_exception(e)))
        finally:
            task.update(running=False, status="")
    threading.Thread(target=worker, daemon=True).start()
    return True


def friendly_error(e):
    s = str(e)
    if "Permission to use the Minecraft API" in s or "AzureAppNotPermitted" in type(e).__name__:
        return "Mojang jeszcze nie zatwierdził launchera – logowanie zadziała po ich zgodzie."
    if "AccountNotOwnMinecraft" in type(e).__name__ or "doesn't own" in s:
        return "To konto Microsoft nie ma kupionego Minecraft: Java Edition."
    return s


RE_SERVER = re.compile(r"Connecting to ([^\s,]+),\s*(\d+)")


def watch_log_line(line):
    m = RE_SERVER.search(line)
    if m:
        host, port = m.group(1), m.group(2)
        game["where"] = host if port == "25565" else f"{host}:{port}"
    elif "Starting integrated minecraft server" in line:
        game["where"] = "Świat jednoosobowy"
    elif "Stopping server" in line or "Stopping worker threads" in line:
        game["where"] = ""
    else:
        return
    discord.update()


def do_play(pid):
    p = get_profile(pid)
    acc = next((a for a in accounts if a["uuid"] == settings["active_account"]), None)
    if not acc:
        raise RuntimeError("Najpierw dodaj konto Microsoft.")
    launch_id = install_profile(p)
    task.update(status="Logowanie...", max=0)
    token = refresh_account(acc)
    ram = int(p["ram_mb"])
    jvm = [f"-Xmx{ram}M", f"-Xms{min(ram, 1024)}M"] + settings["jvm_args"].split()
    options = {
        "username": acc["name"],
        "uuid": acc["uuid"],
        "token": token,
        "gameDirectory": str(profile_dir(p)),
        "jvmArguments": jvm,
        "launcherName": NAZWA_LAUNCHERA,
        "launcherVersion": "0.2",
    }
    cmd = mll.command.get_minecraft_command(launch_id, str(MC_DIR), options)
    task.update(status="Uruchamianie gry...")
    log(f"Uruchamiam {launch_id} ({p['name']}) jako {acc['name']}")
    flags = subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0
    proc = subprocess.Popen(cmd, cwd=str(profile_dir(p)), stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, text=True, encoding="utf-8",
                            errors="replace", creationflags=flags)
    game.update(proc=proc, profile=p["id"], where="", start=int(time.time()))
    discord.update()

    def reader(pr):
        for line in pr.stdout:
            line = line.rstrip()
            log(line)
            watch_log_line(line)
        log(f"Gra zamknięta (kod {pr.wait()}).")
        game.update(proc=None, where="")
        discord.update()
    threading.Thread(target=reader, args=(proc,), daemon=True).start()


def game_running():
    return game["proc"] is not None and game["proc"].poll() is None


def do_repair(pid):
    p = get_profile(pid)
    install_profile(p, force=True)
    task["done"] = f"Gra w profilu {p['name']} jest gotowa."


# ---------------------------------------------------------------------------
#  Mody, paczki zasobów, shadery
# ---------------------------------------------------------------------------

def list_content(p, kind):
    folder = profile_dir(p) / KINDS[kind][0]
    meta = load_json(meta_path(p), {}).get("modrinth", {})
    by_file = {m["file"]: {"title": m.get("title"), "icon": m.get("icon")} for m in meta.values()}
    items = []
    for f in sorted(folder.iterdir(), key=lambda x: x.name.lower()):
        if f.name.startswith("."):
            continue
        enabled = not f.name.endswith(".disabled")
        base = f.name[:-9] if not enabled else f.name
        info = by_file.get(base, {})
        items.append({"file": f.name, "name": info.get("title") or base, "icon": info.get("icon"),
                      "enabled": enabled, "size": f.stat().st_size if f.is_file() else 0})
    return items


def counts(p):
    out = {}
    for kind, (folder, _) in KINDS.items():
        d = profile_dir(p) / folder
        out[kind] = sum(1 for f in d.iterdir() if not f.name.startswith("."))
    return out


def modrinth_search(q, kind, cat, sort, p, offset):
    facets = [[f"project_type:{KINDS[kind][1]}"], [f"versions:{p['version']}"]]
    if kind == "mods":
        facets.append(["categories:fabric"])
    if cat:
        facets.append([f"categories:{cat}"])
    params = {"query": q, "facets": json.dumps(facets), "index": sort or "relevance",
              "limit": 20, "offset": offset}
    r = requests.get(f"{MODRINTH}/search", params=params, headers=UA, timeout=15)
    r.raise_for_status()
    data = r.json()
    meta = load_json(meta_path(p), {}).get("modrinth", {})
    d = profile_dir(p)
    for h in data.get("hits", []):
        m = meta.get(h["project_id"])
        h["installed"] = bool(m and ((d / m["folder"] / m["file"]).exists()
                                     or (d / m["folder"] / (m["file"] + ".disabled")).exists()))
    return data


def modrinth_install(project_id, kind, p, _seen=None):
    seen = _seen if _seen is not None else set()
    if project_id in seen:
        return
    seen.add(project_id)
    params = {"game_versions": json.dumps([p["version"]])}
    if kind == "mods":
        params["loaders"] = json.dumps(["fabric"])
    r = requests.get(f"{MODRINTH}/project/{project_id}/version", params=params, headers=UA, timeout=15)
    r.raise_for_status()
    versions = r.json()
    if not versions:
        raise RuntimeError(f"Brak wersji tego projektu dla Minecraft {p['version']}.")
    v = versions[0]
    f = next((x for x in v["files"] if x.get("primary")), v["files"][0])
    proj = requests.get(f"{MODRINTH}/project/{project_id}", headers=UA, timeout=15).json()
    folder_name = KINDS[kind][0]
    dest = safe_child(profile_dir(p) / folder_name, f["filename"])
    task.update(status=f"Pobieranie {proj.get('title', f['filename'])}...")
    log(f"Pobieranie {f['filename']}")
    with requests.get(f["url"], headers=UA, stream=True, timeout=60) as resp:
        resp.raise_for_status()
        with open(dest, "wb") as out:
            shutil.copyfileobj(resp.raw, out)
    meta = load_json(meta_path(p), {})
    meta.setdefault("modrinth", {})[project_id] = {
        "file": f["filename"], "folder": folder_name, "title": proj.get("title"),
        "icon": proj.get("icon_url")}
    save_json(meta_path(p), meta)
    if kind == "mods":
        for dep in v.get("dependencies", []):
            if dep.get("dependency_type") == "required" and dep.get("project_id"):
                modrinth_install(dep["project_id"], "mods", p, seen)


# ---------------------------------------------------------------------------
#  Skiny (do postaci 3D na stronie startowej)
# ---------------------------------------------------------------------------

def skin_for(uuid_):
    """Zwraca (bajty PNG, model 'slim'/'classic') – z pamięci podręcznej albo od Mojanga."""
    key = re.sub(r"[^0-9a-fA-F]", "", uuid_ or "") or "steve"
    png = SKIN_DIR / f"{key}.png"
    info = SKIN_DIR / f"{key}.json"
    fresh = png.exists() and time.time() - png.stat().st_mtime < 6 * 3600
    if not fresh:
        try:
            if key == "steve":
                url, model = "https://mc-heads.net/skin/MHF_Steve", "classic"
            else:
                r = requests.get(f"https://sessionserver.mojang.com/session/minecraft/profile/{key}",
                                 headers=UA, timeout=10)
                r.raise_for_status()
                prop = next(x for x in r.json()["properties"] if x["name"] == "textures")
                tex = json.loads(base64.b64decode(prop["value"]))["textures"].get("SKIN")
                if not tex:
                    return skin_for(None)
                url = tex["url"]
                model = "slim" if tex.get("metadata", {}).get("model") == "slim" else "classic"
            img = requests.get(url, headers=UA, timeout=10)
            img.raise_for_status()
            png.write_bytes(img.content)
            save_json(info, {"model": model})
        except Exception as e:
            log(f"Nie udało się pobrać skina: {e}")
    if png.exists():
        return png.read_bytes(), load_json(info, {}).get("model", "classic")
    return None, "classic"


# ---------------------------------------------------------------------------
#  Discord Rich Presence (bez dodatkowych bibliotek – przez potok IPC Discorda)
# ---------------------------------------------------------------------------

class DiscordRPC:
    def __init__(self):
        self.pipe = None
        self.status = "wyłączone" if not DISCORD_APP_ID else "łączenie..."
        self.launcher_start = int(time.time())
        self.event = threading.Event()
        threading.Thread(target=self.loop, daemon=True).start()

    def update(self):
        self.event.set()

    def _send(self, op, payload):
        data = json.dumps(payload).encode()
        self.pipe.write(struct.pack("<II", op, len(data)) + data)
        self.pipe.flush()
        head = self.pipe.read(8)
        if len(head) < 8:
            raise ConnectionError("Discord zamknął połączenie")
        _, n = struct.unpack("<II", head)
        return json.loads(self.pipe.read(n) or b"{}")

    def connect(self):
        for i in range(10):
            path = rf"\\?\pipe\discord-ipc-{i}" if os.name == "nt" else \
                os.path.join(os.environ.get("XDG_RUNTIME_DIR", "/tmp"), f"discord-ipc-{i}")
            try:
                if os.name == "nt":
                    self.pipe = open(path, "r+b", buffering=0)
                else:
                    import socket
                    s = socket.socket(socket.AF_UNIX)
                    s.connect(path)
                    self.pipe = s.makefile("rwb", buffering=0)
                self._send(0, {"v": 1, "client_id": DISCORD_APP_ID})
                self.status = "połączono"
                return True
            except OSError:
                self.pipe = None
        self.status = "Discord nie jest uruchomiony"
        return False

    def activity(self):
        if game_running():
            p = get_profile(game["profile"])
            loader = " · Fabric" if p["loader"] == "fabric" else ""
            where = game["where"]
            details = f"Gra na: {where}" if where and where != "Świat jednoosobowy" else \
                (where or "W menu gry")
            act = {"details": details, "state": f"Minecraft {p['version']}{loader}",
                   "timestamps": {"start": game["start"]}}
        else:
            act = {"details": "W launcherze", "state": f"Profil: {get_profile()['name']}",
                   "timestamps": {"start": self.launcher_start}}
        act["assets"] = {"large_image": "logo", "large_text": NAZWA_DISCORD}
        act["buttons"] = [{"label": f"Pobierz {NAZWA_DISCORD}", "url": GITHUB_URL}]
        return act

    def loop(self):
        while True:
            self.event.wait(15)
            self.event.clear()
            if not DISCORD_APP_ID:
                self.status = "brak Application ID"
                continue
            if not settings.get("discord", True):
                if self.pipe:
                    try:
                        self._send(1, {"cmd": "SET_ACTIVITY", "args": {"pid": os.getpid()},
                                       "nonce": str(uuid.uuid4())})
                    except Exception:
                        pass
                self.status = "wyłączone w ustawieniach"
                continue
            try:
                if not self.pipe and not self.connect():
                    continue
                r = self._send(1, {"cmd": "SET_ACTIVITY", "nonce": str(uuid.uuid4()),
                                   "args": {"pid": os.getpid(), "activity": self.activity()}})
                if r.get("evt") == "ERROR":
                    self.status = "błąd: " + str(r.get("data", {}).get("message"))
            except Exception:
                try:
                    self.pipe.close()
                except Exception:
                    pass
                self.pipe = None
                self.status = "rozłączono – ponawiam"


discord = DiscordRPC()


# ---------------------------------------------------------------------------
#  Logowanie Microsoft
# ---------------------------------------------------------------------------

def ms_login_start():
    url, state, verifier = mll.microsoft_account.get_secure_login_data(AZURE_CLIENT_ID, REDIRECT_URI)
    pending_logins[state] = verifier
    webbrowser.open(url)
    return url


def ms_login_finish(full_url):
    params = urllib.parse.parse_qs(urllib.parse.urlparse(full_url).query)
    state = params.get("state", [""])[0]
    verifier = pending_logins.pop(state, None)
    if not verifier:
        raise RuntimeError("Nieprawidłowa lub przeterminowana próba logowania.")
    code = mll.microsoft_account.parse_auth_code_url(full_url, state)
    data = mll.microsoft_account.complete_login(AZURE_CLIENT_ID, None, REDIRECT_URI, code, verifier)
    if "id" not in data:
        raise RuntimeError("To konto Microsoft nie ma kupionego Minecrafta.")
    accounts[:] = [a for a in accounts if a["uuid"] != data["id"]]
    accounts.append({"uuid": data["id"], "name": data["name"], "refresh_token": data["refresh_token"]})
    settings["active_account"] = data["id"]
    save_accounts()
    save_settings()
    log(f"Dodano konto {data['name']}")
    return data["name"]


# ---------------------------------------------------------------------------
#  Serwer HTTP (komunikacja z oknem launchera)
# ---------------------------------------------------------------------------

def bg_file():
    for e in BG_EXTS:
        f = DATA_DIR / f"tlo{e}"
        if f.exists():
            return f
    f = UI_DIR / "assets" / "background.jpg"
    return f if f.exists() else None


def bg_version():
    f = bg_file()
    return int(f.stat().st_mtime) if f else 0


def state_json():
    p = get_profile()
    inst = installed_ids()
    groups, order = {}, []
    for vid in get_versions():
        k = group_key(vid)
        if k not in groups:
            groups[k] = []
            order.append(k)
        groups[k].append({"id": vid, "installed": vid in inst})
    return {
        "name": NAZWA_LAUNCHERA,
        "profile": p,
        "profiles": profiles,
        "versions": [{"group": k, "items": groups[k]} for k in order],
        "accounts": [{"uuid": a["uuid"], "name": a["name"]} for a in accounts],
        "active": settings["active_account"],
        "settings": {k: settings[k] for k in ("jvm_args", "remember_accounts", "discord")},
        "task": dict(task),
        "game_running": game_running(),
        "ms_configured": bool(AZURE_CLIENT_ID),
        "counts": counts(p),
        "login_message": login_message,
        "background": bg_version(),
        "data_dir": str(DATA_DIR),
        "discord_status": discord.status,
    }


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *a):
        pass

    def send(self, code, body, ctype="application/json; charset=utf-8"):
        if isinstance(body, (dict, list)):
            body = json.dumps(body, ensure_ascii=False)
        if isinstance(body, str):
            body = body.encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", ctype)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def body(self):
        n = int(self.headers.get("Content-Length") or 0)
        return self.rfile.read(n) if n else b""

    def json_body(self):
        try:
            return json.loads(self.body() or b"{}")
        except Exception:
            return {}

    def local_only(self):
        """Chroni przed stronami internetowymi, które próbowałyby wysyłać polecenia do launchera."""
        origin = self.headers.get("Origin")
        host = (self.headers.get("Host") or "").split(":")[0]
        if host not in ("127.0.0.1", "localhost"):
            return False
        return origin in (None, f"http://127.0.0.1:{PORT}", f"http://localhost:{PORT}")

    def do_GET(self):
        global last_ping
        u = urllib.parse.urlparse(self.path)
        q = {k: v[0] for k, v in urllib.parse.parse_qs(u.query).items()}
        try:
            if not self.local_only():
                return self.send(403, {"error": "zabronione"})
            if u.path in ("/", "/index.html"):
                return self.send(200, (UI_DIR / "index.html").read_bytes(), "text/html; charset=utf-8")
            if u.path.startswith("/assets/"):
                p = safe_child(UI_DIR / "assets", u.path[len("/assets/"):])
                if p.exists():
                    ctype = {".jpg": "image/jpeg", ".png": "image/png", ".svg": "image/svg+xml"}.get(
                        p.suffix.lower(), "application/octet-stream")
                    return self.send(200, p.read_bytes(), ctype)
                return self.send(404, {"error": "brak pliku"})
            if u.path == "/api/background":
                f = bg_file()
                if not f:
                    return self.send(404, {"error": "brak tła"})
                ctype = {".png": "image/png", ".webp": "image/webp", ".gif": "image/gif"}.get(
                    f.suffix.lower(), "image/jpeg")
                return self.send(200, f.read_bytes(), ctype)
            if u.path == "/api/state":
                last_ping = time.time()
                return self.send(200, state_json())
            if u.path == "/api/log":
                since = int(q.get("since", 0))
                with lock:
                    lines = [l for i, l in log_lines if i >= since]
                return self.send(200, {"lines": lines, "next": log_counter})
            if u.path == "/api/content":
                return self.send(200, list_content(get_profile(q.get("profile")), q["kind"]))
            if u.path == "/api/modrinth/search":
                return self.send(200, modrinth_search(q.get("q", ""), q.get("kind", "mods"),
                                                      q.get("cat", ""), q.get("sort", "relevance"),
                                                      get_profile(q.get("profile")),
                                                      int(q.get("offset", 0))))
            if u.path == "/api/skin":
                data, model = skin_for(q.get("uuid"))
                if not data:
                    return self.send(404, {"error": "brak skina"})
                self.send_response(200)
                self.send_header("Content-Type", "image/png")
                self.send_header("X-Skin-Model", model)
                self.send_header("Content-Length", str(len(data)))
                self.send_header("Cache-Control", "max-age=600")
                self.end_headers()
                return self.wfile.write(data)
            if u.path == "/api/skin_model":
                return self.send(200, {"model": skin_for(q.get("uuid"))[1]})
            if u.path == "/api/import/scan":
                return self.send(200, scan_launchers())
            if u.path == "/api/profiles/export":
                return self.send(200, {"code": export_code(get_profile(q.get("id")))})
            if u.path == "/callback":
                try:
                    name = ms_login_finish(self.path)
                    login_message.update(text=f"Zalogowano jako {name}", ok=True)
                    msg = f"Zalogowano jako <b>{html.escape(name)}</b>. Możesz zamknąć to okno i wrócić do launchera."
                except Exception as e:
                    err = friendly_error(e)
                    login_message.update(text=f"Logowanie nieudane: {err}", ok=False)
                    msg = html.escape(f"Logowanie nieudane: {err}")
                return self.send(200, f"<html><head><meta charset='utf-8'></head><body style='background:#111;"
                                      f"color:#eee;font-family:Segoe UI;display:grid;place-items:center;"
                                      f"height:100vh;margin:0'><p style='font-size:20px;max-width:640px;"
                                      f"text-align:center'>{msg}</p></body></html>",
                                 "text/html; charset=utf-8")
            self.send(404, {"error": "nie znaleziono"})
        except Exception as e:
            log(f"Błąd ({u.path}): {e}")
            self.send(500, {"error": friendly_error(e)})

    def do_POST(self):
        u = urllib.parse.urlparse(self.path)
        q = {k: v[0] for k, v in urllib.parse.parse_qs(u.query).items()}
        busy = {"error": "Poczekaj, aż skończy się bieżące zadanie."}
        try:
            if not self.local_only():
                return self.send(403, {"error": "zabronione"})
            if u.path == "/api/content/upload":   # przed json_body – tu body to plik
                p = get_profile(q.get("profile"))
                dest = safe_child(profile_dir(p) / KINDS[q["kind"]][0], q["name"])
                dest.write_bytes(self.body())
                log(f"Dodano plik {dest.name}")
                return self.send(200, {"ok": True})
            if u.path == "/api/background/upload":
                ext = Path(q.get("name", "")).suffix.lower()
                if ext not in BG_EXTS:
                    return self.send(400, {"error": "Wybierz obrazek JPG, PNG, WEBP albo GIF."})
                data = self.body()
                if len(data) > 25 * 1048576:
                    return self.send(400, {"error": "Obrazek jest za duży (max 25 MB)."})
                for e in BG_EXTS:
                    (DATA_DIR / f"tlo{e}").unlink(missing_ok=True)
                (DATA_DIR / f"tlo{'.jpg' if ext == '.jpeg' else ext}").write_bytes(data)
                return self.send(200, {"ok": True})
            d = self.json_body()
            path = u.path
            if path == "/api/background/reset":
                for e in BG_EXTS:
                    (DATA_DIR / f"tlo{e}").unlink(missing_ok=True)
                return self.send(200, {"ok": True})
            # --- gra
            if path == "/api/play":
                if game_running():
                    return self.send(400, {"error": "Gra już działa."})
                if not run_task(do_play, settings["active_profile"]):
                    return self.send(400, busy)
            elif path == "/api/repair":
                if not run_task(do_repair, d.get("id") or settings["active_profile"]):
                    return self.send(400, busy)
            # --- profile
            elif path == "/api/profiles/select":
                get_profile(d["id"])
                settings["active_profile"] = d["id"]
                save_settings()
                discord.update()
            elif path == "/api/profiles/create":
                if d.get("version") not in get_versions():
                    return self.send(400, {"error": "Wybierz wersję z listy."})
                make_profile(d.get("name"), d["version"], d.get("loader", "fabric"), d.get("ram_mb", 4096))
            elif path == "/api/profiles/update":
                p = get_profile(d["id"])
                if "name" in d:
                    p["name"] = str(d["name"]).strip()[:40] or p["name"]
                if d.get("version") in get_versions():
                    p["version"] = d["version"]
                if d.get("loader") in ("fabric", "vanilla"):
                    p["loader"] = d["loader"]
                if "ram_mb" in d:
                    p["ram_mb"] = max(1024, min(32768, int(d["ram_mb"])))
                save_profiles()
            elif path == "/api/profiles/delete":
                if len(profiles) <= 1:
                    return self.send(400, {"error": "Nie można usunąć ostatniego profilu."})
                p = get_profile(d["id"])
                profiles.remove(p)
                src = INST_DIR / p["id"]
                if src.exists():
                    TRASH_DIR.mkdir(exist_ok=True)
                    shutil.move(str(src), str(TRASH_DIR / f"{p['id']}-{int(time.time())}"))
                if settings["active_profile"] == p["id"]:
                    settings["active_profile"] = profiles[0]["id"]
                save_profiles()
                save_settings()
                log(f"Usunięto profil {p['name']} (pliki przeniesione do folderu 'usuniete')")
            elif path == "/api/profiles/import_code":
                data = decode_code(d.get("code"))
                if data["v"] not in get_versions():
                    return self.send(400, {"error": f"Ten profil jest na wersji {data['v']}, której launcher nie obsługuje."})
                if not run_task(do_import_code, data):
                    return self.send(400, busy)
            elif path == "/api/import/run":
                if d.get("version") not in get_versions():
                    return self.send(400, {"error": "Wybierz wersję gry z listy."})
                if not run_task(do_import_launcher, d):
                    return self.send(400, busy)
            # --- zawartość
            elif path == "/api/modrinth/install":
                if not run_task(modrinth_install, d["project_id"], d["kind"], get_profile(d.get("profile"))):
                    return self.send(400, busy)
            elif path == "/api/content/toggle":
                folder = profile_dir(get_profile(d.get("profile"))) / KINDS[d["kind"]][0]
                p = safe_child(folder, d["file"])
                new = p.name[:-9] if p.name.endswith(".disabled") else p.name + ".disabled"
                p.rename(folder / new)
            elif path == "/api/content/delete":
                p = safe_child(profile_dir(get_profile(d.get("profile"))) / KINDS[d["kind"]][0], d["file"])
                shutil.rmtree(p) if p.is_dir() else p.unlink()
            elif path == "/api/open_folder":
                target = profile_dir(get_profile(d.get("profile")))
                if d.get("kind"):
                    target = target / KINDS[d["kind"]][0]
                elif not d.get("profile"):
                    target = DATA_DIR
                if os.name == "nt":
                    os.startfile(str(target))
                else:
                    webbrowser.open(target.as_uri())
            # --- konta
            elif path == "/api/accounts/ms_start":
                login_message.update(text="", ok=True)
                return self.send(200, {"url": ms_login_start()})
            elif path == "/api/accounts/select":
                settings["active_account"] = d["uuid"]
                save_settings()
            elif path == "/api/accounts/delete":
                accounts[:] = [a for a in accounts if a["uuid"] != d["uuid"]]
                if settings["active_account"] == d["uuid"]:
                    settings["active_account"] = accounts[0]["uuid"] if accounts else None
                save_accounts()
                save_settings()
            # --- ustawienia
            elif path == "/api/settings":
                if "jvm_args" in d:
                    settings["jvm_args"] = str(d["jvm_args"])
                if "remember_accounts" in d:
                    settings["remember_accounts"] = bool(d["remember_accounts"])
                    save_accounts()
                if "discord" in d:
                    settings["discord"] = bool(d["discord"])
                    discord.update()
                save_settings()
            elif path == "/api/clear_error":
                task["error"] = ""
                task["done"] = ""
                login_message.update(text="")
            else:
                return self.send(404, {"error": "nie znaleziono"})
            self.send(200, {"ok": True})
        except Exception as e:
            log(f"Błąd ({u.path}): {e}")
            self.send(500 if not isinstance(e, (ValueError, RuntimeError)) else 400,
                      {"error": friendly_error(e)})


def find_edge():
    for base in (os.environ.get("ProgramFiles(x86)"), os.environ.get("ProgramFiles"),
                 os.environ.get("LOCALAPPDATA")):
        if base:
            p = Path(base) / "Microsoft" / "Edge" / "Application" / "msedge.exe"
            if p.exists():
                return str(p)
    return None


def open_window():
    url = f"http://127.0.0.1:{PORT}/"
    edge = find_edge()
    if edge:
        subprocess.Popen([edge, f"--app={url}", f"--user-data-dir={DATA_DIR / 'okno'}",
                          "--window-size=1280,800", "--no-first-run",
                          "--disable-background-timer-throttling"])
    else:
        webbrowser.open(url)


def watchdog():
    """Zamyka launcher, gdy okno jest zamknięte, a gra i pobieranie nie działają."""
    while True:
        time.sleep(5)
        busy = task["running"] or game_running()
        if not busy and time.time() - last_ping > 150:
            log("Okno zamknięte – kończę.")
            os._exit(0)


def main():
    try:
        server = ThreadingHTTPServer(("127.0.0.1", PORT), Handler)
    except OSError:
        print("Launcher już działa – otwieram okno.")
        open_window()
        return
    log(f"{NAZWA_LAUNCHERA} Launcher – dane w {DATA_DIR}")
    threading.Thread(target=get_versions, daemon=True).start()
    threading.Thread(target=watchdog, daemon=True).start()
    discord.update()
    if "--no-window" not in sys.argv:
        open_window()
    print("Nie zamykaj tego okna, dopóki używasz launchera.")
    server.serve_forever()


if __name__ == "__main__":
    main()
