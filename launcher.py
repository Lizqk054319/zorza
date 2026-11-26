# -*- coding: utf-8 -*-
"""
Zora Client

Uruchom:  uruchom.bat   (albo:  python launcher.py)

Backend w Pythonie (profile, pobieranie gry, mody, konta, Discord), a wygląd to strona
HTML otwierana w Microsoft Edge w trybie aplikacji (bez pasków przeglądarki).
"""

import base64
import ctypes
import hashlib
import html
import io
import json
import os
import re
import random
import shutil
import socket
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
import zipfile
import zlib
from collections import deque
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

# ---------------------------------------------------------------------------
#  USTAWIENIA – tu możesz zmieniać
# ---------------------------------------------------------------------------

NAZWA_LAUNCHERA = "Zora Client"  # nazwa wyświetlana w launcherze
NAZWA_DISCORD = "Zora Client"    # tak ma się nazywać aplikacja w Discord Developer Portal
DOMYSLNA_WERSJA = "1.21.4"         # wersja pierwszego profilu
NAJSTARSZA_WERSJA = "1.20.1"       # starszych wersji launcher nie pokazuje
PORT = 47615                       # NIE ZMIENIAJ – jest wpisany w Azure
GITHUB_URL = "https://github.com/Lizqk054319/zorza"
DISCORD_INVITE = "https://discord.gg/WxNAUBdmcg"
WERSJA_CLIENTA = "0.8.9"

# Co nowego – na ekranie startowym widać tylko tę i poprzednią aktualizację (najważniejsze rzeczy)
NOWOSCI_WERSJE = [
    ("0.8.9", [
        "Przy przenoszeniu z innego launchera jest przycisk Anuluj instalację",
        "Naprawione „Failed to fetch” przy Graj i przenoszeniu z innego launchera",
    ]),
    ("0.8.6", [
        "W grze: Armor Bar (chowa zbroję i głód), Motion Blur bez smug, lżejsze Ambient Particles",
        "Ustawienia modów typu CreamyKeys widoczne w Zora Mods, nowy wbudowany mod Fast IP Ping",
    ]),
    ("0.8.5", [
        "W grze: Custom Hitboxes, serca z Minecrafta w rzędach (strzałki ↑ ↓ zmieniają wysokość)",
        "Cooldowny z teksturami z paczek, grubsze i dłuższe Trails na plecach, naprawiony Motion Blur",
    ]),
    ("0.8.3", [
        "W grze: Trails, Jump Circles, Jump Particles, Ambient Particles i Motion Blur",
        "Naprawiony Freelook, pikselowe serca z połówkami, polski format czasu",
    ]),
    ("0.8.2", [
        "Więcej modów optymalizacyjnych: Dynamic FPS, Krypton, ScalableLux, FastQuit",
        "W grze: Player Health, Cooldowns, podgląd serwera w Direct Connection, rozmiar Tab/Scoreboard",
    ]),
    ("0.8.1", [
        "W grze: ViewModel, Freelook 360°, ciemny ekwipunek i własny pasek przedmiotów",
        "Przyciski Czcionka WŁ/WYŁ i Styl WŁ/WYŁ w menu gry",
    ]),
    ("0.8.0", [
        "Automatyczne aktualizacje z paskiem postępu – bez pobierania instalatora",
        "W grze: Freelook (lewy Alt) i nowe moduły: Low Fire, Time Changer, Clear Weather, Day Counter",
        "Ostrzejsze napisy i szybsze płynne przewijanie w grze",
    ]),
    ("0.7.0", [
        "Logowanie kontem Microsoft działa – Mojang zatwierdził Zora Client!",
        "W grze: moduł Gamma (rozjaśnianie), ładniejszy HUD i rozmiar elementów w ustawieniach",
        "Przyciski Zora Mods i folder modów w menu gry",
        "Lepsza wydajność: ustawienia Javy pod wyższe i stabilniejsze FPS",
    ]),
    ("0.6.2", [
        "Import profilu przenosi też ustawienia modów",
        "W grze: zakładka Zora Mods z twoimi modami",
    ]),
    ("0.6.0", [
        "Serwery: dodaj IP, zobacz opis i graczy, graj z automatycznym połączeniem",
        "Więcej modów wbudowanych (AppleSkin, ModernFix, More Culling, Simple Voice Chat i inne)",
        "Launcher sam sprawdza aktualizacje modów po uruchomieniu",
        "Skiny: wyszukiwanie skinów graczy po nicku",
        "W grze: własne dźwięki dla Hit Sounds i nowy moduł Kill Sounds",
    ]),
    ("0.5.2", [
        "Instalacja z Modrinth: wybierasz profil i wersję",
        "Nowa nazwa: Zora Client",
    ]),
]
NOWOSCI = [x for _, items in NOWOSCI_WERSJE[:2] for x in items]

# slug z Modrinth -> id moda w fabric.mod.json (do wykrywania duplikatów)
MOD_ID = {"fast-ip-ping": "fastipping", "ferrite-core": "ferritecore", "dynamic-fps": "dynamic_fps", "ebe": "enhancedblockentities", "simple-voice-chat": "voicechat",
          "zfastnoise": "fastnoise"}

# Napis "Logowanie Microsoft będzie dostępne wkrótce" – zmień na False po zgodzie Mojanga
MICROSOFT_WKROTCE = False

# Mod Zora Client (budowany automatycznie na GitHubie) – wersja gry -> adres pliku
# Mod Zora Client ma osobny plik na każdą wersję gry (zorza-client-<wersja>.jar w wydaniu mod-latest).
# Lista wersji bierze się z wydania na GitHubie, więc nowe wersje dochodzą bez aktualizacji launchera.
ZORZA_MOD_URL = "https://github.com/Lizqk054319/zorza/releases/download/mod-latest/zorza-client-{}.jar"
ZORZA_MOD_ZNANE = ["1.21.4"]

# Mody wbudowane (slug z Modrinth, nazwa) – instalują się same w profilach z Fabric
MODY_WBUDOWANE = [
    ("fabric-api", "Fabric API"),
    ("sodium", "Sodium"),
    ("sodium-extra", "Sodium Extra"),
    ("reeses-sodium-options", "Reese's Sodium Options"),
    ("lithium", "Lithium"),
    ("ferrite-core", "FerriteCore"),
    ("entityculling", "Entity Culling"),
    ("moreculling", "More Culling"),
    ("immediatelyfast", "ImmediatelyFast"),
    ("modernfix", "ModernFix"),
    ("badoptimizations", "BadOptimizations"),
    ("ebe", "Enhanced Block Entities"),
    ("threadtweak", "ThreadTweak"),
    ("zconfig", "ZConfig"),
    ("zfastnoise", "Fast Noise"),
    ("cloth-config", "Cloth Config API"),
    ("appleskin", "AppleSkin"),
    ("simple-voice-chat", "Simple Voice Chat"),
    ("modmenu", "Mod Menu"),
    ("dynamic-fps", "Dynamic FPS"),
    ("krypton", "Krypton"),
    ("scalablelux", "ScalableLux"),
    ("fastquit", "FastQuit"),
    ("fast-ip-ping", "Fast IP Ping"),
]
# id projektów na Modrinth (do rozpoznawania, że gracz ma u siebie mod, który jest wbudowany)
MODY_WBUDOWANE_ID = {
    "fabric-api": "P7dR8mSH", "sodium": "AANobbMI", "sodium-extra": "PtjYWJkn", "reeses-sodium-options": "Bh37bMuy",
    "lithium": "gvQqBUqZ", "ferrite-core": "uXXizFIs", "entityculling": "NNAgCjsB", "moreculling": "51shyZVL",
    "immediatelyfast": "5ZwdcRci", "modernfix": "nmDcB62a", "badoptimizations": "g96Z4WVZ", "ebe": "OVuFYfre",
    "threadtweak": "vSEH1ERy", "zconfig": "4qmvXRB9", "cloth-config": "9s6osm5g", "appleskin": "EsAfCjCV",
    "simple-voice-chat": "9eGKb6K1", "modmenu": "mOgUt4GM",
    "dynamic-fps": "LQ3K71Q1", "krypton": "fQEb0iXm", "scalablelux": "Ps1zyz6x", "fastquit": "x1hIzbuY",
    "fast-ip-ping": "9mtu0sUO",
}

# Logowanie Microsoft – NIE ZMIENIAJ (zatwierdzane przez Mojang dla tego ID)
AZURE_CLIENT_ID = "6553e03b-b905-4f30-a877-535ed1c3a975"
REDIRECT_URI = f"http://localhost:{PORT}/callback"

# Discord Rich Presence – wklej tu "Application ID" z discord.com/developers (instrukcja w INSTRUKCJA.txt)
DISCORD_APP_ID = "1574093349517004952"

# ---------------------------------------------------------------------------

try:
    import requests
    import minecraft_launcher_lib as mll
except ImportError:
    print("Brakuje bibliotek. Uruchom launcher przez plik uruchom.bat")
    print("albo wpisz:  python -m pip install -r requirements.txt")
    input("Naciśnij Enter, aby zamknąć...")
    sys.exit(1)

# W wersji .exe (PyInstaller) pliki ui są w folderze _MEIPASS
APP_DIR = Path(getattr(sys, "_MEIPASS", Path(__file__).resolve().parent))
UI_DIR = APP_DIR / "ui"
APPDATA = Path(os.environ.get("APPDATA", Path.home()))
HOME = Path.home()
DATA_DIR = APPDATA / "ZorzaLauncher"
MC_DIR = DATA_DIR / "minecraft"          # wspólne pliki gry (wersje, biblioteki, Java)
INST_DIR = DATA_DIR / "instances"        # osobny folder gry dla każdego profilu
TRASH_DIR = DATA_DIR / "usuniete"        # usunięte profile trafiają tu, nie znikają od razu
SKIN_DIR = DATA_DIR / "skiny"            # pamięć podręczna skinów kont
MY_SKINS = DATA_DIR / "moje_skiny"        # biblioteka skinów z zakładki Skiny
ACCOUNTS_FILE = DATA_DIR / "accounts.json"
SETTINGS_FILE = DATA_DIR / "settings.json"
PROFILES_FILE = DATA_DIR / "profiles.json"
BG_EXTS = (".jpg", ".jpeg", ".png", ".webp", ".gif")
for d in (DATA_DIR, MC_DIR, INST_DIR, SKIN_DIR, MY_SKINS):
    d.mkdir(parents=True, exist_ok=True)

KINDS = {  # rodzaj zawartości -> (folder w profilu, typ projektu na Modrinth)
    "mods": ("mods", "mod"),
    "resourcepacks": ("resourcepacks", "resourcepack"),
    "shaders": ("shaderpacks", "shader"),
}
UA = {"User-Agent": f"ZorzaLauncher/{WERSJA_CLIENTA} ({GITHUB_URL})"}
MODRINTH = "https://api.modrinth.com/v2"

lock = threading.RLock()
log_lines = deque(maxlen=3000)
log_counter = 0
task = {"running": False, "status": "", "progress": 0, "max": 0, "error": "", "done": "", "after": "",
        "kind": "", "cancel": False}


class Cancelled(Exception):
    """Użytkownik kliknął "Anuluj instalację"."""


def check_cancel():
    if task.get("cancel"):
        raise Cancelled()
game = {"proc": None, "profile": None, "where": "", "start": 0}
last_ping = time.time()
bye_at = 0.0          # okno zgłosiło zamknięcie
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
settings.setdefault("lang", "pl")
settings.setdefault("accent", "#1f6bff")
settings.setdefault("log_days", 30)        # 0 = nie usuwaj
settings.setdefault("skin", "startowy.png")
settings.setdefault("skin_chosen", False)   # False = na starcie pokazuj skin konta

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
         "ram_mb": max(1024, min(32768, int(ram_mb))), "created": int(time.time()),
         "playtime": 0, "last_played": 0}
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
        raise ValueError("To nie jest kod profilu Zora Client (powinien zaczynać się od ZORZA-).")
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
    task["after"] = "check_mods"


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
    lp = None
    for name in ("launcher_profiles.json", "launcher_profiles_microsoft_store.json"):
        data = load_json(root / name, None)
        if isinstance(data, dict):
            if lp is None:
                lp = {"profiles": {}}
            for key, pr in (data.get("profiles") or {}).items():
                if not key.startswith("zorza-"):        # pomiń profile dodane przez Zorzę
                    lp["profiles"].setdefault(key, pr)
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
    candidates = []
    if pdir.is_dir():          # tylko 3 poziomy w głąb – pełne przeszukiwanie Lunara trwa wieki
        try:
            for a in os.scandir(pdir):
                if not a.is_dir():
                    continue
                if os.path.isdir(os.path.join(a.path, "mods")):
                    candidates.append(Path(a.path) / "mods")
                for b in os.scandir(a.path):
                    if b.is_dir() and b.name != "mods":
                        if os.path.isdir(os.path.join(b.path, "mods")):
                            candidates.append(Path(b.path) / "mods")
                        for c in os.scandir(b.path):
                            if c.is_dir() and c.name != "mods" and os.path.isdir(os.path.join(c.path, "mods")):
                                candidates.append(Path(c.path) / "mods")
        except OSError:
            pass
    if candidates:
        for mods in candidates:
            rel = mods.relative_to(pdir).parts
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


def scan_ogulniega():
    """Ogulniega: wspólny folder gry w .ogulniega/profile, a mody osobno dla każdej wersji
    w profile/mods/<wersja>-<wariant> (np. 1.21.4-sodium)."""
    root = APPDATA / ".ogulniega"
    gd = root / "profile"
    mods_root = gd / "mods"
    if not mods_root.is_dir():
        return None
    out = []
    for d in sorted(mods_root.iterdir(), key=lambda x: x.name, reverse=True):
        if not d.is_dir():
            continue
        m = re.match(r"(\d+\.\d+(?:\.\d+)?)", d.name)
        if not m:
            continue
        bundled = set()     # mody dokładane przez sam launcher Ogulniega – nie kopiujemy ich
        info = load_json(root / "launcher" / f"{d.name}.json", {})
        for mod in info.get("mods", []) if isinstance(info, dict) else []:
            if isinstance(mod, dict) and mod.get("filename"):
                bundled.add(mod["filename"])
        try:
            jars = [f.name for f in d.iterdir() if f.suffix == ".jar" and f.name not in bundled]
        except OSError:
            jars = []
        variant = d.name[len(m.group(1)):].strip("-_ ") or "fabric"
        out.append({"key": d.name, "name": f"Ogulniega {m.group(1)} ({variant})", "version": m.group(1),
                    "loader": "fabric", "ram_mb": 4096, "path": str(gd), "mods_path": str(d),
                    "skip_mods": sorted(bundled), "counts": {**dir_counts(gd), "mods": len(jars)},
                    "options": (gd / "options.txt").exists()})
    return {"id": "ogulniega", "name": "Ogulniega", "path": str(root), "profiles": out}


def scan_launchers():
    """Każdy launcher sprawdzany osobno i równolegle – jeden wolny lub zepsuty nie blokuje reszty."""
    fns = (scan_official, scan_ogulniega, scan_modrinth, scan_prism, scan_lunar)
    results = [None] * len(fns)

    def run(i, fn):
        try:
            results[i] = fn()
        except Exception as e:
            log(f"Błąd skanowania ({fn.__name__}): {e}")

    threads = [threading.Thread(target=run, args=(i, fn), daemon=True) for i, fn in enumerate(fns)]
    for t in threads:
        t.start()
    deadline = time.time() + 8
    for t, fn in zip(threads, fns):
        t.join(max(0.0, deadline - time.time()))
        if t.is_alive():
            log(f"Skanowanie ({fn.__name__}) trwa za długo – pomijam.")
    return [r for r in results if r]


def copy_tree(src, dst):
    if not src.exists():
        return 0
    n = 0
    if src.is_file():
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dst)
        return 1
    for f in src.rglob("*"):
        check_cancel()
        if f.is_file():
            t = dst / f.relative_to(src)
            t.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(f, t)
            n += 1
            if n % 25 == 0:
                task["progress"] = n
    return n


# Foldery z ustawieniami i danymi modów (poza config/), które warto przenieść razem z modami
MOD_DATA_DIRS = ("config", "xaero", "XaeroWaypoints", "XaeroWorldMap", "journeymap", "schematics",
                 "syncmatica", "voxelmap", "replay_recordings", "emotes", "itemscroller", "litematica")
# Nigdy nie kopiujemy niczego, co może zawierać konta lub tokeny logowania
RE_SECRET = re.compile(r"account|token|auth|session|credential|password|secret|_ias_|^ias\.", re.I)


def copy_mod_settings(gd, dst):
    """Ustawienia modów z innego launchera – bez plików z kontami/tokenami."""
    n = 0
    for name in MOD_DATA_DIRS:
        src = gd / name
        if not src.is_dir():
            continue
        for f in src.rglob("*"):
            check_cancel()
            rel = f.relative_to(src)
            if not f.is_file() or any(RE_SECRET.search(part) for part in rel.parts):
                continue
            t = dst / name / rel
            try:
                t.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(f, t)
                n += 1
            except OSError:
                pass
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
    task["kind"] = "import"                 # można anulować
    p = make_profile(d.get("name") or src["name"], d["version"], d.get("loader", src["loader"]),
                     d.get("ram_mb", src["ram_mb"]))
    try:
        _import_launcher_files(launcher, src, p, d)
    except Cancelled:
        drop_profile(p)
        task["done"] = "Anulowano instalację"
        log(f"Import z {launcher['name']} anulowany – usunięto niedokończony profil")


def drop_profile(p):
    """Usuwa niedokończony profil (po anulowaniu)."""
    if p in profiles:
        profiles.remove(p)
    if settings.get("active_profile") == p["id"] and profiles:
        settings["active_profile"] = profiles[0]["id"]
    save_profiles()
    save_settings()
    shutil.rmtree(INST_DIR / p["id"], ignore_errors=True)


def _import_launcher_files(launcher, src, p, d):
    dst = profile_dir(p)
    gd = Path(src["path"])
    c = d.get("copy", {})
    task.update(status=f"Kopiowanie z: {launcher['name']}...", max=0)
    if c.get("mods"):
        msrc = Path(src.get("mods_path") or gd / "mods")
        skip = set(src.get("skip_mods") or [])
        if msrc.is_dir():
            for f in msrc.iterdir():
                check_cancel()
                if f.is_file() and f.suffix in (".jar", ".disabled") and f.name not in skip:
                    shutil.copy2(f, dst / "mods" / f.name)
    if c.get("mods") or c.get("settings"):
        task.update(status="Kopiowanie ustawień modów...")
        n = copy_mod_settings(gd, dst)
        log(f"Skopiowano ustawienia modów: {n} plików")
    if c.get("settings"):
        for f in ("options.txt", "optionsof.txt", "optionsshaders.txt"):
            copy_tree(gd / f, dst / f)
    if c.get("packs"):
        copy_tree(gd / "resourcepacks", dst / "resourcepacks")
        copy_tree(gd / "shaderpacks", dst / "shaderpacks")
    if c.get("servers"):
        copy_tree(gd / "servers.dat", dst / "servers.dat")
    if c.get("saves"):
        copy_tree(gd / "saves", dst / "saves")
    check_cancel()
    if p.get("loader") != "vanilla":
        try:
            ensure_builtins(p)          # mody wbudowane (Fabric API, Sodium...) od razu w profilu
        except Cancelled:
            raise
        except Exception as e:
            log(f"Mody wbudowane pobiorą się przy pierwszym uruchomieniu: {e}")
    check_cancel()
    task["kind"] = ""
    task["done"] = f"Zaimportowano profil {p['name']}"
    task["after"] = "check_mods"
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
    task.update(running=True, status="Przygotowanie...", progress=0, max=0, error="", done="", after="",
                kind="", cancel=False)

    def worker():
        try:
            fn(*args)
        except Exception as e:
            task["error"] = friendly_error(e)
            log("BŁĄD: " + "".join(traceback.format_exception(e)))
        finally:
            task.update(running=False, status="", kind="", cancel=False)
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


# Ustawienia Javy pod płynniejszą grę (mniej przycięć od Garbage Collectora)
JVM_WYDAJNOSC = ["-XX:+UseG1GC", "-XX:+ParallelRefProcEnabled", "-XX:MaxGCPauseMillis=40",
                 "-XX:+UnlockExperimentalVMOptions", "-XX:+DisableExplicitGC",
                 "-XX:G1NewSizePercent=20", "-XX:G1ReservePercent=20", "-XX:G1HeapRegionSize=16M",
                 "-XX:+UseStringDeduplication"]


def jvm_args(p):
    ram = int(p["ram_mb"])
    user = settings["jvm_args"].split()
    extra = [] if any(a.startswith("-XX:+Use") and "GC" in a for a in user) else JVM_WYDAJNOSC
    return [f"-Xmx{ram}M", f"-Xms{min(ram, 2048)}M"] + extra + user


def do_play(pid):
    p = get_profile(pid)
    acc = next((a for a in accounts if a["uuid"] == settings["active_account"]), None)
    if not acc:
        raise RuntimeError("Najpierw dodaj konto Microsoft.")
    launch_id = install_profile(p)
    if launch_id.startswith("fabric-loader"):
        apply_game_toggles(p)
        ensure_builtins(p)
        ensure_zorza_mod(p)
        ensure_deps(p)
    task.update(status="Logowanie...", max=0)
    token = refresh_account(acc)
    jvm = jvm_args(p)
    options = {
        "username": acc["name"],
        "uuid": acc["uuid"],
        "token": token,
        "gameDirectory": str(profile_dir(p)),
        "jvmArguments": jvm,
        "launcherName": NAZWA_LAUNCHERA,
        "launcherVersion": WERSJA_CLIENTA,
    }
    cmd = mll.command.get_minecraft_command(launch_id, str(MC_DIR), options)
    task.update(status="Uruchamianie gry...")
    log(f"Uruchamiam {launch_id} ({p['name']}) jako {acc['name']}")
    flags = subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0
    proc = subprocess.Popen(cmd, cwd=str(profile_dir(p)), stdout=subprocess.PIPE,
                            stderr=subprocess.STDOUT, text=True, encoding="utf-8",
                            errors="replace", creationflags=flags)
    game.update(proc=proc, profile=p["id"], where="", start=int(time.time()))
    p["last_played"] = int(time.time())
    save_profiles()
    discord.update()

    def reader(pr):
        for line in pr.stdout:
            line = line.rstrip()
            log(line)
            watch_log_line(line)
        log(f"Gra zamknięta (kod {pr.wait()}).")
        try:
            gp = get_profile(game["profile"])
            gp["playtime"] = int(gp.get("playtime", 0)) + int(time.time()) - game["start"]
            save_profiles()
        except Exception:
            pass
        game.update(proc=None, where="")
        discord.update()
    threading.Thread(target=reader, args=(proc,), daemon=True).start()


OFFICIAL_DIR = APPDATA / ".minecraft"


def official_launcher_paths():
    pf86 = os.environ.get("ProgramFiles(x86)", r"C:\Program Files (x86)")
    pf = os.environ.get("ProgramFiles", r"C:\Program Files")
    return [Path(pf86) / "Minecraft Launcher" / "MinecraftLauncher.exe",
            Path(pf) / "Minecraft Launcher" / "MinecraftLauncher.exe",
            Path(r"C:\XboxGames\Minecraft Launcher\Content\Minecraft.exe")]


def do_play_official(pid):
    """Przygotowuje profil w oficjalnym launcherze (Fabric, mody, folder Zorzy) i go otwiera."""
    p = get_profile(pid)
    launch_id = install_profile(p)
    if launch_id.startswith("fabric-loader"):
        apply_game_toggles(p)
        ensure_builtins(p)
        ensure_zorza_mod(p)
        ensure_deps(p)
        src = MC_DIR / "versions" / launch_id / f"{launch_id}.json"
        dst = OFFICIAL_DIR / "versions" / launch_id / f"{launch_id}.json"
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dst)
    task.update(status="Dodawanie profilu do oficjalnego launchera...", max=0)
    now = time.strftime("%Y-%m-%dT%H:%M:%S.000Z", time.gmtime())
    icon = ""
    try:
        icon = "data:image/png;base64," + base64.b64encode((UI_DIR / "assets" / "logo-128.png").read_bytes()).decode()
    except OSError:
        pass
    entry = {"name": f"Zora Client – {p['name']}", "type": "custom", "lastVersionId": launch_id,
             "gameDir": str(profile_dir(p)), "javaArgs": " ".join(jvm_args(p)),
             "created": now, "lastUsed": now}
    if icon:
        entry["icon"] = icon
    files = [f for f in (OFFICIAL_DIR / "launcher_profiles.json",
                         OFFICIAL_DIR / "launcher_profiles_microsoft_store.json") if f.exists()]
    if not files:
        files = [OFFICIAL_DIR / "launcher_profiles.json"]
    for f in files:
        data = load_json(f, {"profiles": {}, "version": 3})
        if f.exists():
            shutil.copy2(f, f.with_name(f.name + ".zorza-kopia"))
        data.setdefault("profiles", {})[f"zorza-{p['id']}"] = entry
        save_json(f, data)
    log(f"Dodano profil 'Zorza – {p['name']}' do oficjalnego launchera.")
    opened = False
    for exe in official_launcher_paths():
        if exe.exists():
            subprocess.Popen([str(exe)])
            opened = True
            break
    if not opened and os.name == "nt":
        try:
            subprocess.Popen(["explorer.exe", r"shell:AppsFolder\Microsoft.4297127D64EC6_8wekyb3d8bbwe!Minecraft"])
            opened = True
        except OSError:
            pass
    task["done"] = (f"Otwieram oficjalny launcher – wybierz profil „Zorza – {p['name']}” i kliknij Graj."
                    if opened else f"Gotowe! Otwórz oficjalny launcher Minecraft i wybierz profil „Zorza – {p['name']}”.")


def game_running():
    return game["proc"] is not None and game["proc"].poll() is None


def do_repair(pid):
    p = get_profile(pid)
    install_profile(p, force=True)
    task["done"] = f"Gra w profilu {p['name']} jest gotowa."


# ---------------------------------------------------------------------------
#  Mody, paczki zasobów, shadery
# ---------------------------------------------------------------------------

_jar_cache = {}


def jar_info(f):
    """Nazwa, wersja, autorzy i ikona z fabric.mod.json w pliku .jar (z pamięcią podręczną)."""
    key = (str(f), f.stat().st_mtime)
    if key in _jar_cache:
        return _jar_cache[key]
    info = {}
    try:
        with zipfile.ZipFile(f) as z:
            for name in ("fabric.mod.json", "quilt.mod.json"):
                if name in z.namelist():
                    j = json.loads(z.read(name).decode("utf-8", "replace"), strict=False)
                    if name == "quilt.mod.json":
                        j = j.get("quilt_loader", {}).get("metadata", {}) | {"version": j.get("quilt_loader", {}).get("version")}
                    authors = j.get("authors") or j.get("contributors") or []
                    if isinstance(authors, dict):
                        authors = list(authors)
                    info = {"id": j.get("id"), "title": j.get("name"), "version": j.get("version"),
                            "author": ", ".join(a["name"] if isinstance(a, dict) else str(a) for a in authors[:3]),
                            "icon": j.get("icon") if isinstance(j.get("icon"), str) else None}
                    break
    except Exception:
        pass
    _jar_cache[key] = info
    return info


_manifest_cache = {}
# id-y, których nie trzeba instalować jako modów
DEP_IGNORE = {"minecraft", "java", "fabricloader", "fabric-loader", "mixinextras", "quilt_loader"}
# id moda -> slug na Modrinth (dla bibliotek, gdzie się różnią)
DEP_SLUGS = {
    "fabric": "fabric-api", "fabric-api": "fabric-api", "architectury": "architectury-api",
    "cloth-config": "cloth-config", "cloth-config2": "cloth-config", "owo": "owo-lib",
    "yet_another_config_lib_v3": "yacl", "yet-another-config-lib": "yacl", "modmenu": "modmenu",
    "fabric-language-kotlin": "fabric-language-kotlin", "forgeconfigapiport": "forge-config-api-port",
    "cardinal-components": "cardinal-components-api", "cardinal-components-base": "cardinal-components-api",
    "placeholder-api": "placeholder-api", "resourcefullib": "resourceful-lib", "balm-fabric": "balm",
    "balm": "balm", "collective": "collective", "puzzleslib": "puzzles-lib", "fzzy_config": "fzzy-config",
    "geckolib": "geckolib", "trinkets": "trinkets", "midnightlib": "midnightlib", "ukulib": "ukulib",
    "libjf": "libjf", "creativecore": "creativecore", "indium": "indium", "iris": "iris",
    "sodium": "sodium", "lithium": "lithium", "ferritecore": "ferrite-core", "zoomify": "zoomify",
    "entity_model_features": "entity-model-features", "entity_texture_features": "entitytexturefeatures",
    "konkrete": "konkrete", "fancymenu": "fancymenu", "iceberg": "iceberg", "prism": "prism-lib",
    "terrablender": "terrablender", "kotlinforforge": "kotlin-for-forge", "silk": "silk",
    "libipn": "libipn", "malilib": "malilib", "yungsapi": "yungs-api", "moonlight": "moonlight",
    "supermartijn642corelib": "supermartijn642s-core-lib", "searchables": "searchables",
    "athena": "athena-ctm", "playeranimator": "playeranimator", "bookshelf": "bookshelf-lib",
    "notenoughanimations": "not-enough-animations", "skinlayers3d": "3dskinlayers",
}


def _read_manifest(data, depth=0):
    """Z bajtów .jar: (zbiór id, które mod dostarcza, słownik zależności, nazwa)."""
    ids, deps, name = set(), {}, None
    try:
        with zipfile.ZipFile(io.BytesIO(data)) as z:
            names = set(z.namelist())
            if "fabric.mod.json" not in names:
                return ids, deps, name
            j = json.loads(z.read("fabric.mod.json").decode("utf-8", "replace"), strict=False)
            if j.get("id"):
                ids.add(j["id"])
            for pr in j.get("provides") or []:
                ids.add(pr if isinstance(pr, str) else pr.get("id", ""))
            for k, v in (j.get("depends") or {}).items():
                deps[k] = v
            name = j.get("name") or j.get("id")
            if depth < 3:
                for nested in j.get("jars") or []:
                    path = nested.get("file") if isinstance(nested, dict) else None
                    if path in names:
                        sub_ids, _, _ = _read_manifest(z.read(path), depth + 1)
                        ids |= sub_ids
    except Exception:
        pass
    return ids, deps, name


def mod_manifest(f):
    key = (str(f), f.stat().st_mtime)
    if key not in _manifest_cache:
        _manifest_cache[key] = _read_manifest(f.read_bytes())
    return _manifest_cache[key]


def missing_deps(p):
    """Zależności wymagane przez włączone mody, których nie ma w folderze mods.
    Zwraca {id: [nazwy modów, które go wymagają]}."""
    d = profile_dir(p) / "mods"
    provided, needs = set(DEP_IGNORE), {}
    # mody wbudowane i tak trafią do profilu przed uruchomieniem gry
    provided |= {MOD_ID.get(slug, slug) for slug, _ in MODY_WBUDOWANE} | {"fabric", "cloth-config2", "cloth_config"}
    for f in d.iterdir():
        if not (f.is_file() and f.suffix == ".jar"):
            continue
        ids, deps, name = mod_manifest(f)
        provided |= ids
        for dep in deps:
            needs.setdefault(dep, []).append(name or f.name)
    missing = {}
    for dep, who in needs.items():
        if dep in provided:
            continue
        if dep == "fabric" or (dep.startswith("fabric-") and "fabric-api" in provided):
            if "fabric-api" in provided or any(x.startswith("fabric-api") for x in provided):
                continue
        missing[dep] = sorted(set(who))
    return missing


def resolve_dep(dep, version):
    """Szuka moda o danym id na Modrinth (dla wersji gry i Fabric). Zwraca (slug, nazwa) albo None."""
    slug = DEP_SLUGS.get(dep) or ("fabric-api" if dep.startswith("fabric-") else None)
    params = {"game_versions": json.dumps([version]), "loaders": json.dumps(["fabric"])}
    for cand in [c for c in (slug, dep, dep.replace("_", "-")) if c]:
        try:
            r = requests.get(f"{MODRINTH}/project/{cand}/version", params=params, headers=UA, timeout=15)
            if r.status_code == 200 and r.json():
                proj = requests.get(f"{MODRINTH}/project/{cand}", headers=UA, timeout=15).json()
                return proj.get("slug", cand), proj.get("title", cand)
        except Exception:
            pass
    try:   # ostatnia próba: wyszukiwarka
        facets = json.dumps([["project_type:mod"], [f"versions:{version}"], ["categories:fabric"]])
        r = requests.get(f"{MODRINTH}/search", params={"query": dep, "facets": facets, "limit": 5},
                         headers=UA, timeout=15)
        norm = re.sub(r"[^a-z0-9]", "", dep.lower())
        for h in r.json().get("hits", []):
            if norm in (re.sub(r"[^a-z0-9]", "", h["slug"].lower()), re.sub(r"[^a-z0-9]", "", h["title"].lower())):
                return h["slug"], h["title"]
    except Exception:
        pass
    return None


def ensure_deps(p, quiet=True):
    """Doinstalowuje brakujące wymagane mody (do 3 rund – nowe mody mogą mieć własne zależności)."""
    unresolved = {}
    for _ in range(3):
        miss = missing_deps(p)
        miss = {k: v for k, v in miss.items() if k not in unresolved}
        if not miss:
            break
        for dep, who in miss.items():
            found = resolve_dep(dep, p["version"])
            if not found:
                unresolved[dep] = who
                log(f"Nie znaleziono na Modrinth wymaganego moda '{dep}' (potrzebny dla: {', '.join(who)})")
                continue
            task.update(status=f"Pobieranie wymaganego moda: {found[1]}...")
            try:
                modrinth_install(found[0], "mods", p)
                log(f"Doinstalowano wymagany mod {found[1]} (dla: {', '.join(who)})")
            except Exception as e:
                unresolved[dep] = who
                log(f"Nie udało się pobrać {found[1]}: {e}")
    return unresolved


def list_content(p, kind):
    folder = profile_dir(p) / KINDS[kind][0]
    meta = load_json(meta_path(p), {})
    mr = meta.get("modrinth", {})
    by_file = {m["file"]: m for m in mr.values()}
    builtin = {m["file"]: slug for slug, m in meta.get("builtin", {}).items()}
    items = []
    for f in sorted(folder.iterdir(), key=lambda x: x.name.lower()):
        if f.name.startswith("."):
            continue
        enabled = not f.name.endswith(".disabled")
        base = f.name[:-9] if not enabled else f.name
        m = by_file.get(base, {})
        ji = jar_info(f) if f.is_file() and base.endswith(".jar") else {}
        if base in builtin and not m:
            m = {"icon": builtin_icons.get(builtin[base])}
        if base == "zorza-client.jar":
            m = {"icon": "/assets/zorza-logo.png", "title": "Zora Client"}
        items.append({"file": f.name, "name": ji.get("title") or m.get("title") or base,
                      "author": ji.get("author") or "", "version": ji.get("version") or m.get("version") or "",
                      "icon": m.get("icon") or (f"/api/modicon?profile={urllib.parse.quote(p['id'])}"
                                                f"&file={urllib.parse.quote(f.name)}" if ji.get("icon") else None),
                      "source": "builtin" if base in builtin or base == "zorza-client.jar" else "modrinth" if m else "local",
                      "enabled": enabled, "size": f.stat().st_size if f.is_file() else 0})
    if kind == "mods":   # wbudowane, których jeszcze nie pobrano
        have = set(builtin.values())
        own_ids = set()
        for f in folder.iterdir():
            base = f.name[:-9] if f.name.endswith(".disabled") else f.name
            if base.endswith(".jar") and base not in builtin:
                own_ids.add(jar_info(f).get("id"))
        for slug, title in MODY_WBUDOWANE:
            if MOD_ID.get(slug, slug) in own_ids:
                continue
            if slug not in have and not any(m.get("slug") == slug for m in mr.values()):
                items.append({"file": "", "name": title, "author": "", "version": "",
                              "icon": builtin_icons.get(slug), "source": "builtin", "pending": True,
                              "enabled": p["loader"] == "fabric", "size": 0})
        has_z = (folder / "zorza-client.jar").exists() or (folder / "zorza-client.jar.disabled").exists()
        if not has_z and p["version"] in zorza_mod_versions(cached_only=True):
            items.insert(0, {"file": "", "name": "Zora Client", "author": "UwU_Lizaczek", "version": "",
                             "icon": "/assets/zorza-logo.png", "source": "builtin", "pending": True,
                             "enabled": p["loader"] == "fabric", "size": 0})
    return items


def list_worlds(p):
    d = profile_dir(p) / "saves"
    out = []
    if d.is_dir():
        for w in d.iterdir():
            if w.is_dir():
                lvl = w / "level.dat"
                out.append({"folder": w.name, "name": w.name, "icon": (w / "icon.png").exists(),
                            "modified": int((lvl if lvl.exists() else w).stat().st_mtime)})
    return sorted(out, key=lambda x: -x["modified"])


def counts(p):
    out = {"saves": len(list_worlds(p))}
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
    bi = load_json(meta_path(p), {}).get("builtin", {})
    bi_ids = {MODY_WBUDOWANE_ID.get(sl) for sl in bi}
    for h in data.get("hits", []):
        h["builtin"] = h["project_id"] in bi_ids or h.get("slug") in bi
        m = meta.get(h["project_id"])
        h["installed"] = bool(m and ((d / m["folder"] / m["file"]).exists()
                                     or (d / m["folder"] / (m["file"] + ".disabled")).exists()))
    return data


def modrinth_versions(project_id, kind, p):
    """Wersje projektu pasujące do profilu (wersja gry + Fabric dla modów), najnowsze pierwsze."""
    params = {"game_versions": json.dumps([p["version"]])}
    if kind == "mods":
        params["loaders"] = json.dumps(["fabric"])
    r = requests.get(f"{MODRINTH}/project/{project_id}/version", params=params, headers=UA, timeout=15)
    r.raise_for_status()
    return r.json()


def modrinth_install(project_id, kind, p, _seen=None, version_id=None):
    seen = _seen if _seen is not None else set()
    if project_id in seen:
        return
    seen.add(project_id)
    if kind == "mods" and p.get("loader") == "vanilla":
        raise RuntimeError("Ten profil jest bez modów (Vanilla). Wybierz profil z Fabric.")
    if kind == "mods":
        bi = load_json(meta_path(p), {}).get("builtin", {})
        if project_id in bi or project_id in {MODY_WBUDOWANE_ID.get(sl) for sl in bi}:
            if _seen is not None and len(seen) > 1:
                return                    # zależność, która już jest wbudowana
            raise RuntimeError("Ten mod jest już wbudowany w Zora Client.")
    versions = modrinth_versions(project_id, kind, p)
    if not versions:
        raise RuntimeError(f"Brak wersji tego projektu dla Minecraft {p['version']}.")
    v = next((x for x in versions if x.get("id") == version_id), None) if version_id else None
    v = v or versions[0]
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
    old = meta.get("modrinth", {}).get(project_id)
    if old and old.get("file") and old["file"] != f["filename"]:   # inna wersja – usuń starą
        for name in (old["file"], old["file"] + ".disabled"):
            try:
                op = safe_child(profile_dir(p) / old.get("folder", folder_name), name)
                if op.exists():
                    shutil.rmtree(op) if op.is_dir() else op.unlink()
            except Exception:
                pass
    meta.setdefault("modrinth", {})[project_id] = {
        "file": f["filename"], "folder": folder_name, "title": proj.get("title"),
        "icon": proj.get("icon_url"), "slug": proj.get("slug"), "version": v.get("version_number")}
    save_json(meta_path(p), meta)
    if kind == "mods":
        for dep in v.get("dependencies", []):
            if dep.get("dependency_type") == "required" and dep.get("project_id"):
                modrinth_install(dep["project_id"], "mods", p, seen)


ICONS_FILE = DATA_DIR / "ikony_modow.json"
builtin_icons = load_json(ICONS_FILE, {})


def fetch_builtin_icons():
    """Ikonki modów wbudowanych z Modrinth (raz, potem z pamięci)."""
    missing = [slug for slug, _ in MODY_WBUDOWANE if slug not in builtin_icons]
    if not missing:
        return
    try:
        r = requests.get(f"{MODRINTH}/projects", params={"ids": json.dumps(missing)}, headers=UA, timeout=15)
        r.raise_for_status()
        data = r.json()
        for proj in data if isinstance(data, list) else []:
            if isinstance(proj, dict) and proj.get("icon_url"):
                builtin_icons[proj["slug"]] = proj["icon_url"]
        save_json(ICONS_FILE, builtin_icons)
    except Exception as e:
        log(f"Nie udało się pobrać ikonek modów: {e}")


_mod_release = {"t": 0, "assets": {}}


def zorza_mod_release():
    """Najnowsze wydanie moda Zora Client na GitHubie: {nazwa pliku: (id, data, rozmiar)} – pamiętane 5 min."""
    if time.time() - _mod_release["t"] < 300 and _mod_release["assets"]:
        return _mod_release["assets"]
    r = requests.get("https://api.github.com/repos/Lizqk054319/zorza/releases/tags/mod-latest",
                     headers={**UA, "Accept": "application/vnd.github+json"}, timeout=15)
    r.raise_for_status()
    _mod_release["assets"] = {a["name"]: (a["id"], a["updated_at"], a["size"]) for a in r.json().get("assets", [])}
    _mod_release["t"] = time.time()
    return _mod_release["assets"]


def ver_key(v):
    return tuple(int(x) for x in re.findall(r"\d+", v))


def zorza_mod_versions(cached_only=False):
    """Wersje Minecrafta, na które jest gotowy mod Zora Client."""
    path = DATA_DIR / "zorza_mod_versions.json"
    if not cached_only or _mod_release["assets"]:
        try:
            assets = _mod_release["assets"] if cached_only else zorza_mod_release()
            vs = sorted({m.group(1) for n in assets if (m := re.fullmatch(r"zorza-client-(.+)\.jar", n))}, key=ver_key)
            if vs:
                if vs != load_json(path, []):
                    save_json(path, vs)
                return set(vs)
        except Exception:
            pass
    return set(load_json(path, ZORZA_MOD_ZNANE))


def ensure_zorza_mod(p):
    """Mod Zora Client – przy każdym uruchomieniu sprawdza, czy na GitHubie jest nowsza wersja (dla każdego profilu)."""
    url = ZORZA_MOD_URL.format(p["version"]) if p["version"] in zorza_mod_versions() else None
    d = profile_dir(p) / "mods"
    d.mkdir(parents=True, exist_ok=True)
    dest, off = d / "zorza-client.jar", d / "zorza-client.jar.disabled"
    for f in d.glob("zorza-client-*.jar"):
        f.unlink(missing_ok=True)
    if not url:
        dest.unlink(missing_ok=True)
        return
    if off.exists():                       # gracz sam wyłączył moda
        return
    meta = load_json(meta_path(p), {})
    have = meta.get("zorza_mod")
    want = None
    try:
        want = zorza_mod_release().get(url.rsplit("/", 1)[1])
    except Exception as e:
        log(f"Nie udało się sprawdzić aktualizacji moda Zora Client: {e}")
        if dest.exists():
            return
    if dest.exists() and want and have == list(want) and dest.stat().st_size == want[2]:
        return                              # już najnowszy
    task.update(status="Aktualizacja moda Zora Client...", max=0)
    try:
        tmp = dest.with_suffix(".tmp")
        with requests.get(url, headers=UA, stream=True, timeout=60) as resp:
            resp.raise_for_status()
            with open(tmp, "wb") as out:
                shutil.copyfileobj(resp.raw, out)
        tmp.replace(dest)
        if want:
            meta["zorza_mod"] = list(want)
            save_json(meta_path(p), meta)
        log("Pobrano najnowszy mod Zora Client.")
    except Exception as e:
        log(f"Nie udało się pobrać moda Zora Client: {e}")


def builtin_dups(p):
    """Mody gracza, które są też wbudowane: {plik: (slug, nazwa)}."""
    d = profile_dir(p) / "mods"
    meta = load_json(meta_path(p), {})
    bi = meta.get("builtin", {})
    builtin_files = {m["file"] for m in bi.values()}
    want = {}
    for slug, title in MODY_WBUDOWANE:
        want[MOD_ID.get(slug, slug)] = (slug, title)
        if bi.get(slug, {}).get("mod_id"):
            want[bi[slug]["mod_id"]] = (slug, title)
    out = {}
    if not d.is_dir():
        return out
    for f in d.iterdir():
        base = f.name[:-9] if f.name.endswith(".disabled") else f.name
        if f.is_file() and base.endswith(".jar") and base not in builtin_files and base != "zorza-client.jar":
            mid = jar_info(f).get("id")
            if mid in want:
                out[f.name] = want[mid]
    return out


def sha1_file(f):
    return hashlib.sha1(f.read_bytes()).hexdigest()


def check_mods(p, auto=False):
    """Sprawdza mody gracza: duplikaty wbudowanych, brakujące zależności, nieaktualne,
    na inną wersję gry albo bez wersji na tę grę. auto=True pomija to, na co gracz już odpowiedział "Nie"."""
    d = profile_dir(p) / "mods"
    if not d.is_dir():
        return []
    meta = load_json(meta_path(p), {})
    dismissed = set(meta.get("dismissed", []))
    good = meta.get("updated", {})           # sha1 pliku -> id wersji, na którą gracz już zaktualizował
    out_first = []
    dups = builtin_dups(p)
    for fname, (slug, title) in dups.items():
        info = jar_info(d / fname)
        out_first.append({"kind": "builtin_dup", "file": fname, "name": info.get("title") or title,
                          "icon": builtin_icons.get(slug), "game": p["version"], "slug": slug})
    for dep, who in missing_deps(p).items():
        found = resolve_dep(dep, p["version"])
        out_first.append({"kind": "missing", "dep": dep, "name": found[1] if found else dep,
                          "slug": found[0] if found else None, "required_by": who, "game": p["version"],
                          "file": "", "icon": None})
    skip = {m["file"] for m in meta.get("builtin", {}).values()} | {"zorza-client.jar"} | set(dups)
    files = {}
    for f in d.iterdir():
        base = f.name[:-9] if f.name.endswith(".disabled") else f.name
        if f.is_file() and base.endswith(".jar") and base not in skip and f.name not in skip:
            files[sha1_file(f)] = f
    out = []
    if files:
        hashes = list(files)
        r = requests.post(f"{MODRINTH}/version_files", json={"hashes": hashes, "algorithm": "sha1"},
                          headers=UA, timeout=20)
        r.raise_for_status()
        current = r.json()
        known = [h for h in hashes if h in current]
        if known:
            r = requests.post(f"{MODRINTH}/version_files/update", headers=UA, timeout=20,
                              json={"hashes": known, "algorithm": "sha1", "loaders": ["fabric"],
                                    "game_versions": [p["version"]]})
            r.raise_for_status()
            updates = r.json()
            ids = list({current[h]["project_id"] for h in known})
            titles = {}
            try:
                pr = requests.get(f"{MODRINTH}/projects", params={"ids": json.dumps(ids)}, headers=UA, timeout=20)
                for x in pr.json():
                    titles[x["id"]] = {"title": x.get("title"), "icon": x.get("icon_url")}
            except Exception:
                pass
            for h in known:
                cur, upd, f = current[h], updates.get(h), files[h]
                info = titles.get(cur["project_id"], {})
                item = {"file": f.name, "name": info.get("title") or jar_info(f).get("title") or f.name,
                        "icon": info.get("icon"), "current": cur.get("version_number"),
                        "current_games": cur.get("game_versions", [])[-3:], "game": p["version"],
                        "enabled": not f.name.endswith(".disabled")}
                # ten sam plik co najnowsza wersja albo gracz już na nią zaktualizował -> wszystko OK
                same_file = bool(upd) and any(x.get("hashes", {}).get("sha1") == h for x in upd.get("files", []))
                if same_file or (upd and good.get(h) == upd["id"]):
                    continue
                fits = p["version"] in cur.get("game_versions", []) or h in good
                if fits:
                    if upd and upd["id"] != cur["id"]:
                        out.append({**item, "kind": "outdated", "new": upd.get("version_number"), "version_id": upd["id"]})
                elif upd:
                    out.append({**item, "kind": "wrong_version", "new": upd.get("version_number"), "version_id": upd["id"]})
                elif item["enabled"]:
                    out.append({**item, "kind": "no_version"})
    order = {"outdated": 0, "wrong_version": 1, "no_version": 2}
    res = out_first + sorted(out, key=lambda x: (order[x["kind"]], x["name"].lower()))
    for it in res:
        it["key"] = f"{it['kind']}|{it.get('file') or it.get('dep')}|{it.get('version_id', '')}"
    if auto:
        res = [it for it in res if it["key"] not in dismissed]
    return res


def dismiss_issues(p, keys):
    meta = load_json(meta_path(p), {})
    have = meta.setdefault("dismissed", [])
    for k in keys:
        if k not in have:
            have.append(k)
    meta["dismissed"] = have[-300:]
    save_json(meta_path(p), meta)


def do_remove_dups(p, files):
    """Usuwa z modów gracza kopie modów wbudowanych i wgrywa wersje wbudowane (ustawienia w config/ zostają)."""
    d = profile_dir(p) / "mods"
    meta = load_json(meta_path(p), {})
    for name in files:
        try:
            f = safe_child(d, name)
            if f.exists():
                f.unlink()
                log(f"Usunięto {name} – ten mod jest wbudowany.")
            base = name[:-9] if name.endswith(".disabled") else name
            for pid in [k for k, m in meta.get("modrinth", {}).items() if m.get("file") == base]:
                meta["modrinth"].pop(pid)
        except Exception as e:
            log(f"Nie udało się usunąć {name}: {e}")
    save_json(meta_path(p), meta)
    ensure_builtins(p)
    task["done"] = f"Usunięto zdublowane mody: {len(files)}. Używasz teraz wersji wbudowanych."


def do_update_mods(p, items):
    d = profile_dir(p) / "mods"
    meta = load_json(meta_path(p), {})
    done = 0
    for i, it in enumerate(items):
        old = safe_child(d, it["file"])
        task.update(max=len(items), progress=i)
        try:
            v = requests.get(f"{MODRINTH}/version/{it['version_id']}", headers=UA, timeout=20).json()
            f = next((x for x in v["files"] if x.get("primary")), v["files"][0])
            disabled = old.name.endswith(".disabled")
            dest = safe_child(d, f["filename"] + (".disabled" if disabled else ""))
            task.update(status=f"Pobieranie {f['filename']}...")
            with requests.get(f["url"], headers=UA, stream=True, timeout=60) as resp:
                resp.raise_for_status()
                tmp = d / (f["filename"] + ".pobieranie")
                with open(tmp, "wb") as out:
                    shutil.copyfileobj(resp.raw, out)
            if old.exists() and old != dest:
                old.unlink()
            tmp.replace(dest)
            meta.setdefault("updated", {})[sha1_file(dest)] = it["version_id"]
            for pid, m in meta.get("modrinth", {}).items():
                if m.get("file") == (old.name[:-9] if disabled else old.name):
                    m["file"], m["version"] = f["filename"], v.get("version_number")
            done += 1
            log(f"Zaktualizowano {old.name} -> {dest.name}")
        except Exception as e:
            log(f"Nie udało się zaktualizować {it.get('file')}: {e}")
    save_json(meta_path(p), meta)
    task["done"] = f"Zaktualizowano mody: {done}"


def _download_builtin(d, slug, title, v):
    f = next((x for x in v["files"] if x.get("primary")), v["files"][0])
    dest = safe_child(d, f["filename"])
    tmp = d / (f["filename"] + ".pobieranie")
    with requests.get(f["url"], headers=UA, stream=True, timeout=60) as resp:
        resp.raise_for_status()
        with open(tmp, "wb") as out:
            shutil.copyfileobj(resp.raw, out)
    tmp.replace(dest)
    return f["filename"], jar_info(dest).get("id")


def apply_game_toggles(p):
    """Zmiany z zakładki ZORA MODS w grze (włącz/wyłącz mod) – stosowane przed uruchomieniem."""
    f = profile_dir(p) / "config" / "zorza" / "toggle.json"
    data = load_json(f, {})
    d = profile_dir(p) / "mods"
    for name, on in (data.items() if isinstance(data, dict) else []):
        try:
            if not str(name).endswith(".jar"):
                continue
            jar, off = safe_child(d, name), safe_child(d, name + ".disabled")
            if on and off.exists() and not jar.exists():
                off.replace(jar)
            elif not on and jar.exists():
                jar.replace(off)
        except Exception as e:
            log(f"Nie udało się przełączyć {name}: {e}")
    f.unlink(missing_ok=True)


def write_builtin_list(p, bi):
    """Dla moda Zora Client – które pliki to mody wbudowane (zakładka ZORA MODS)."""
    f = profile_dir(p) / "config" / "zorza" / "builtin.json"
    f.parent.mkdir(parents=True, exist_ok=True)
    save_json(f, {"files": sorted(m["file"] for m in bi.values())})
    mm = profile_dir(p) / "config" / "modmenu.json"     # Mod Menu tylko dla ekranów ustawień – bez swoich przycisków
    if "modmenu" in bi and not mm.exists():
        save_json(mm, {"modify_title_screen": False, "modify_game_menu": False})


def ensure_builtins(p):
    """Pobiera brakujące mody wbudowane dla wersji profilu i raz na 12 h je aktualizuje.
    Wyłączonych (.disabled) nie rusza. Jeśli gracz ma własną kopię takiego moda, wbudowana czeka,
    aż gracz zgodzi się usunąć swoją (inaczej gra by się wysypała przez duplikat)."""
    d = profile_dir(p) / "mods"
    d.mkdir(parents=True, exist_ok=True)
    meta = load_json(meta_path(p), {})
    bi = meta.setdefault("builtin", {})
    for slug, m in list(bi.items()):          # zmieniono wersję profilu albo mod wypadł z listy – usuń stare pliki
        if m.get("game_version") != p["version"] or slug not in dict(MODY_WBUDOWANE):
            for name in (m["file"], m["file"] + ".disabled"):
                (d / name).unlink(missing_ok=True)
            bi.pop(slug)
    dup_slugs = {slug for slug, _ in builtin_dups(p).values()}
    now = int(time.time())
    for i, (slug, title) in enumerate(MODY_WBUDOWANE):
        check_cancel()
        m = bi.get(slug)
        present = m and ((d / m["file"]).exists() or (d / (m["file"] + ".disabled")).exists())
        if slug in dup_slugs:
            if present:                        # gracz ma swoją kopię – usuń wbudowaną, żeby nie było dwóch
                for name in (m["file"], m["file"] + ".disabled"):
                    (d / name).unlink(missing_ok=True)
                bi.pop(slug, None)
            continue
        if present and now - m.get("checked", 0) < 12 * 3600:
            continue
        task.update(status=f"Mody wbudowane: {title}...", max=len(MODY_WBUDOWANE), progress=i)
        try:
            r = requests.get(f"{MODRINTH}/project/{slug}/version", headers=UA, timeout=15,
                             params={"game_versions": json.dumps([p["version"]]),
                                     "loaders": json.dumps(["fabric"])})
            r.raise_for_status()
            vs = [v for v in r.json() if v.get("version_type") == "release"] or r.json()
            if not vs:
                continue      # brak wersji tego moda dla tej wersji gry
            v = vs[0]
            if present and m.get("version_id") == v["id"]:
                m["checked"] = now
                continue
            disabled = present and (d / (m["file"] + ".disabled")).exists()
            fname, mod_id = _download_builtin(d, slug, title, v)
            if present and m["file"] != fname:
                for name in (m["file"], m["file"] + ".disabled"):
                    (d / name).unlink(missing_ok=True)
            if disabled:
                (d / fname).replace(d / (fname + ".disabled"))
            bi[slug] = {"file": fname, "title": title, "version": v.get("version_number"), "version_id": v["id"],
                        "game_version": p["version"], "mod_id": mod_id, "checked": now}
            save_json(meta_path(p), meta)
            log(f"Mod wbudowany: {fname}")
        except Exception as e:
            log(f"Nie udało się pobrać moda wbudowanego {title}: {e}")
    # bezpiecznik: gdyby gracz miał kopię moda o tym samym id, zostaje tylko jego wersja
    for slug in {sl for sl, _ in builtin_dups(p).values()} - dup_slugs:
        m = bi.pop(slug, None)
        if m:
            for name in (m["file"], m["file"] + ".disabled"):
                (d / name).unlink(missing_ok=True)
    write_builtin_list(p, bi)
    save_json(meta_path(p), meta)


# ---------------------------------------------------------------------------
#  Logi – automatyczne usuwanie starych
# ---------------------------------------------------------------------------

def clean_logs():
    days = int(settings.get("log_days") or 0)
    if days <= 0:
        return
    limit = time.time() - days * 86400
    n = 0
    for pdir in INST_DIR.iterdir():
        for sub in ("logs", "crash-reports"):
            folder = pdir / sub
            if not folder.is_dir():
                continue
            for f in folder.rglob("*"):
                try:
                    if f.is_file() and f.stat().st_mtime < limit:
                        f.unlink()
                        n += 1
                except OSError:
                    pass
    if n:
        log(f"Usunięto {n} starych plików logów (starszych niż {days} dni).")


def log_cleaner():
    while True:
        try:
            clean_logs()
        except Exception as e:
            log(f"Czyszczenie logów: {e}")
        time.sleep(6 * 3600)


# ---------------------------------------------------------------------------
#  Biblioteka skinów (zakładka Skiny)
# ---------------------------------------------------------------------------

SKINS_META = MY_SKINS / "skiny.json"


def png_size(data):
    if data[:8] != b"\x89PNG\r\n\x1a\n" or data[12:16] != b"IHDR":
        return None
    return struct.unpack(">II", data[16:24])


def skins_list():
    meta = load_json(SKINS_META, {})
    start = MY_SKINS / "startowy.png"
    if not start.exists() and (UI_DIR / "assets" / "skins" / "startowy.png").exists():
        shutil.copy2(UI_DIR / "assets" / "skins" / "startowy.png", start)
        meta.setdefault("startowy.png", {"name": "Startowy", "model": "classic", "added": 0})
        save_json(SKINS_META, meta)
    out = []
    for f in MY_SKINS.glob("*.png"):
        m = meta.get(f.name, {})
        out.append({"file": f.name, "name": m.get("name", f.stem), "model": m.get("model", "classic"),
                    "added": m.get("added", int(f.stat().st_mtime)), "v": int(f.stat().st_mtime)})
    return sorted(out, key=lambda x: x["added"])


def skin_add(name, data, model):
    size = png_size(data)
    if size not in ((64, 64), (64, 32)):
        raise ValueError("To nie jest skin Minecrafta – potrzebny plik PNG 64×64 (albo 64×32).")
    stem = re.sub(r"[^A-Za-z0-9_-]+", "_", Path(name).stem)[:30] or "skin"
    fname, n = f"{stem}.png", 2
    while (MY_SKINS / fname).exists():
        fname, n = f"{stem}_{n}.png", n + 1
    (MY_SKINS / fname).write_bytes(data)
    meta = load_json(SKINS_META, {})
    meta[fname] = {"name": Path(name).stem[:30], "model": "slim" if model == "slim" else "classic",
                   "added": int(time.time())}
    save_json(SKINS_META, meta)
    return fname


def do_apply_skin(fname):
    meta = load_json(SKINS_META, {}).get(fname, {})
    data = safe_child(MY_SKINS, fname).read_bytes()
    settings["skin"] = fname
    settings["skin_chosen"] = True
    save_settings()
    acc = next((a for a in accounts if a["uuid"] == settings["active_account"]), None)
    if not acc:
        task["done"] = "Skin ustawiony w launcherze. W grze zmieni się po dodaniu konta Microsoft i ponownym kliknięciu Zastosuj."
        return
    task.update(status="Wysyłanie skina do Minecrafta...")
    token = refresh_account(acc)
    r = requests.post("https://api.minecraftservices.com/minecraft/profile/skins",
                      headers={"Authorization": f"Bearer {token}", **UA},
                      data={"variant": meta.get("model", "classic")},
                      files={"file": (fname, data, "image/png")}, timeout=30)
    if r.status_code >= 400:
        raise RuntimeError(f"Minecraft nie przyjął skina (błąd {r.status_code}).")
    for f in SKIN_DIR.glob(f"{acc['uuid'].replace('-', '')}.*"):
        f.unlink(missing_ok=True)
    task["done"] = "Gotowe! Twój skin w Minecrafcie został zmieniony."
    log(f"Zmieniono skin konta {acc['name']} na {fname}")


# ---------------------------------------------------------------------------
#  Skiny (do postaci 3D na stronie startowej)
# ---------------------------------------------------------------------------

# ---------------------------------------------------------------------------
#  Serwery – lista, sprawdzanie (Server List Ping) i automatyczne łączenie w grze
# ---------------------------------------------------------------------------

SERVERS_FILE = DATA_DIR / "serwery.json"
servers = load_json(SERVERS_FILE, [])
RE_HOST = re.compile(r"^[A-Za-z0-9.\-_]{1,253}(:\d{1,5})?$|^\[[0-9a-fA-F:]+\](:\d{1,5})?$")
MC_COLORS = {"0": "#000000", "1": "#0000AA", "2": "#00AA00", "3": "#00AAAA", "4": "#AA0000", "5": "#AA00AA",
             "6": "#FFAA00", "7": "#AAAAAA", "8": "#555555", "9": "#5555FF", "a": "#55FF55", "b": "#55FFFF",
             "c": "#FF5555", "d": "#FF55FF", "e": "#FFFF55", "f": "#FFFFFF"}
MC_NAMED = {"black": "0", "dark_blue": "1", "dark_green": "2", "dark_aqua": "3", "dark_red": "4",
            "dark_purple": "5", "gold": "6", "gray": "7", "dark_gray": "8", "blue": "9", "green": "a",
            "aqua": "b", "red": "c", "light_purple": "d", "yellow": "e", "white": "f"}


def save_servers():
    save_json(SERVERS_FILE, servers)


def _dns_name(buf, i):
    labels, jumped, end = [], False, None
    for _ in range(64):
        n = buf[i]
        if n == 0:
            i += 1
            break
        if n & 0xC0 == 0xC0:
            if not jumped:
                end = i + 2
            i, jumped = ((n & 0x3F) << 8) | buf[i + 1], True
            continue
        labels.append(buf[i + 1:i + 1 + n].decode("ascii", "replace"))
        i += 1 + n
    return ".".join(labels), (end if jumped else i)


def srv_lookup(host):
    """Rekord SRV _minecraft._tcp (wiele serwerów go używa). Zwraca (host, port) albo None."""
    import struct
    qid = random.randint(0, 65535)
    q = struct.pack(">HHHHHH", qid, 0x0100, 1, 0, 0, 0)
    for part in f"_minecraft._tcp.{host}".split("."):
        q += bytes([len(part)]) + part.encode("ascii", "ignore")
    q += b"\x00" + struct.pack(">HH", 33, 1)
    for dns in ("1.1.1.1", "8.8.8.8"):
        try:
            with socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as sk:
                sk.settimeout(2)
                sk.sendto(q, (dns, 53))
                buf = sk.recv(4096)
            if struct.unpack(">H", buf[:2])[0] != qid:
                continue
            an = struct.unpack(">H", buf[6:8])[0]
            _, i = _dns_name(buf, 12)
            i += 4
            best = None
            for _ in range(an):
                _, i = _dns_name(buf, i)
                typ, _, _, rdlen = struct.unpack(">HHIH", buf[i:i + 10])
                i += 10
                if typ == 33:
                    prio, _, port = struct.unpack(">HHH", buf[i:i + 6])
                    target, _ = _dns_name(buf, i + 6)
                    if best is None or prio < best[0]:
                        best = (prio, target.rstrip("."), port)
                i += rdlen
            return (best[1], best[2]) if best else None
        except Exception:
            continue
    return None


def split_address(address):
    address = address.strip()
    if address.startswith("["):
        host, _, rest = address[1:].partition("]")
        return host, int(rest[1:]) if rest.startswith(":") else None
    if address.count(":") == 1:
        host, port = address.split(":")
        return host, int(port)
    return address, None


def motd_lines(desc):
    """Opis serwera (tekst z § albo komponent JSON) -> linie z kawałkami {t, c, b, i, u, s}."""
    segs = []

    def legacy(text, style):
        st = dict(style)
        parts = re.split(r"([§&][0-9a-fk-orA-FK-OR])", text) if "§" in text else [text]
        for part in parts:
            if len(part) == 2 and part[0] == "§":
                k = part[1].lower()
                if k in MC_COLORS:
                    st = {"c": MC_COLORS[k]}
                elif k == "l": st["b"] = True
                elif k == "o": st["i"] = True
                elif k == "n": st["u"] = True
                elif k == "m": st["s"] = True
                elif k == "r": st = {}
            elif part:
                segs.append({"t": part, **st})

    def walk(c, style):
        if isinstance(c, str):
            return legacy(c, style)
        if isinstance(c, list):
            for x in c:
                walk(x, style)
            return
        if not isinstance(c, dict):
            return
        st = dict(style)
        col = c.get("color")
        if isinstance(col, str):
            st["c"] = MC_COLORS.get(MC_NAMED.get(col, ""), col if col.startswith("#") else st.get("c"))
        for key, k in (("bold", "b"), ("italic", "i"), ("underlined", "u"), ("strikethrough", "s")):
            if key in c:
                st[k] = bool(c[key])
        legacy(str(c.get("text", c.get("translate", ""))), st)
        for x in c.get("extra", []) or []:
            walk(x, st)

    walk(desc, {})
    lines = [[]]
    for sg in segs:
        pieces = sg["t"].split("\n")
        for j, piece in enumerate(pieces):
            if j:
                lines.append([])
            if piece:
                lines[-1].append({**sg, "t": piece})
    return [ln for ln in lines][:2]


def _varint(n):
    out = b""
    n &= 0xFFFFFFFF
    while True:
        b = n & 0x7F
        n >>= 7
        out += bytes([b | (0x80 if n else 0)])
        if not n:
            return out


def _read_varint(sk):
    n = shift = 0
    for _ in range(5):
        b = sk.recv(1)
        if not b:
            raise ConnectionError("Serwer zamknął połączenie.")
        n |= (b[0] & 0x7F) << shift
        if not b[0] & 0x80:
            return n
        shift += 7
    raise ValueError("Zła odpowiedź serwera.")


def _recv_exact(sk, n):
    buf = b""
    while len(buf) < n:
        chunk = sk.recv(n - len(buf))
        if not chunk:
            raise ConnectionError("Serwer zamknął połączenie.")
        buf += chunk
    return buf


def mc_ping(address):
    """Sprawdza serwer Minecraft tak jak lista serwerów w grze."""
    import struct
    if not RE_HOST.match(address.strip()):
        raise ValueError("To nie wygląda na adres serwera (np. anarchia.gg albo 123.45.67.89:25565).")
    host, port = split_address(address)
    conn_host, conn_port = host, port or 25565
    if port is None and not re.match(r"^\d+\.\d+\.\d+\.\d+$", host):
        srv = srv_lookup(host)
        if srv:
            conn_host, conn_port = srv
    t0 = time.time()
    with socket.create_connection((conn_host, conn_port), timeout=5) as sk:
        sk.settimeout(5)
        hb = host.encode("utf-8")
        data = _varint(0) + _varint(769) + _varint(len(hb)) + hb + struct.pack(">H", conn_port) + _varint(1)
        sk.sendall(_varint(len(data)) + data + b"\x01\x00")
        _read_varint(sk)
        if _read_varint(sk) != 0:
            raise ValueError("Zła odpowiedź serwera.")
        ln = _read_varint(sk)
        if ln > 2_000_000:
            raise ValueError("Zła odpowiedź serwera.")
        st = json.loads(_recv_exact(sk, ln).decode("utf-8", "replace"))
        ping = int((time.time() - t0) * 1000)
        try:
            t1 = time.time()
            sk.sendall(_varint(9) + b"\x01" + struct.pack(">q", 1))
            _read_varint(sk)
            ping = max(1, int((time.time() - t1) * 1000))
        except Exception:
            pass
    pl = st.get("players") or {}
    fav = st.get("favicon") if isinstance(st.get("favicon"), str) and st["favicon"].startswith("data:image/png;base64,") else None
    ver = st.get("version") or {}
    return {"online": True, "motd": motd_lines(st.get("description", "")), "players": int(pl.get("online", 0) or 0),
            "max": int(pl.get("max", 0) or 0), "version": str(ver.get("name", ""))[:60],
            "favicon": fav, "ping": ping}


def server_status(address):
    try:
        return mc_ping(address)
    except ValueError as e:
        return {"online": False, "error": str(e)}
    except Exception:
        return {"online": False, "error": "Serwer nie odpowiada (jest wyłączony albo adres jest zły)."}


def write_autojoin(p, srv, status):
    """Plik dla moda Zora Client: po uruchomieniu gry zapyta, czy połączyć z serwerem."""
    f = profile_dir(p) / "config" / "zorza" / "autojoin.json"
    f.parent.mkdir(parents=True, exist_ok=True)
    save_json(f, {"address": srv["address"], "name": srv.get("name") or srv["address"],
                  "motd": status.get("motd", []), "players": status.get("players"), "max": status.get("max"),
                  "favicon": status.get("favicon"), "created": int(time.time() * 1000)})


def do_play_server(sid, mode, pid):
    srv = next((x for x in servers if x["id"] == sid), None)
    if not srv:
        raise RuntimeError("Nie ma takiego serwera.")
    p = get_profile(pid)
    task.update(status=f"Sprawdzanie serwera {srv['address']}...", max=0)
    write_autojoin(p, srv, server_status(srv["address"]))
    srv["profile"], srv["last"] = p["id"], int(time.time())
    save_servers()
    if mode == "microsoft":
        do_play(p["id"])
    else:
        do_play_official(p["id"])


# ---------------------------------------------------------------------------
#  Skiny graczy po nicku
# ---------------------------------------------------------------------------

def skin_by_nick(nick):
    nick = nick.strip()
    if not re.match(r"^[A-Za-z0-9_]{1,16}$", nick):
        raise ValueError("Nick może mieć tylko litery, cyfry i _ (maks. 16 znaków).")
    r = requests.get(f"https://api.mojang.com/users/profiles/minecraft/{nick}", headers=UA, timeout=10)
    if r.status_code in (204, 404) or not r.content:
        raise ValueError(f"Nie ma gracza o nicku {nick}.")
    r.raise_for_status()
    prof = r.json()
    r = requests.get(f"https://sessionserver.mojang.com/session/minecraft/profile/{prof['id']}", headers=UA, timeout=10)
    r.raise_for_status()
    tex = {}
    for prop in r.json().get("properties", []):
        if prop.get("name") == "textures":
            tex = json.loads(base64.b64decode(prop["value"])).get("textures", {})
    skin = tex.get("SKIN")
    if not skin or not str(skin.get("url", "")).startswith("http://textures.minecraft.net/"):
        raise ValueError(f"Gracz {prof['name']} ma domyślny skin.")
    data = requests.get(skin["url"].replace("http://", "https://", 1), headers=UA, timeout=15).content
    if png_size(data) not in ((64, 64), (64, 32)):
        raise ValueError("Nie udało się pobrać skina.")
    model = "slim" if (skin.get("metadata") or {}).get("model") == "slim" else "classic"
    return {"name": prof["name"], "model": model, "data": base64.b64encode(data).decode()}



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

DISCORD_TXT = {   # gra na / świat jednoosobowy / w menu / w launcherze / profil / pobierz
    "pl": ("Gra na", "Świat jednoosobowy", "W menu gry", "W launcherze", "Profil", "Pobierz"),
    "en": ("Playing on", "Singleplayer", "In the main menu", "In the launcher", "Profile", "Get"),
    "de": ("Spielt auf", "Einzelspieler", "Im Hauptmenü", "Im Launcher", "Profil", "Hol dir"),
    "uk": ("Грає на", "Одиночна гра", "У меню гри", "У лаунчері", "Профіль", "Завантажити"),
    "es": ("Jugando en", "Un jugador", "En el menú", "En el launcher", "Perfil", "Descarga"),
}


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
        L = DISCORD_TXT.get(settings.get("lang"), DISCORD_TXT["pl"])
        if game_running():
            p = get_profile(game["profile"])
            loader = " · Fabric" if p["loader"] == "fabric" else ""
            where = game["where"]
            details = f"{L[0]}: {where}" if where and where != "Świat jednoosobowy" else \
                (L[1] if where else L[2])
            act = {"details": details, "state": f"Minecraft {p['version']}{loader}",
                   "timestamps": {"start": game["start"]}}
        else:
            act = {"details": L[3], "state": f"{L[4]}: {get_profile()['name']}",
                   "timestamps": {"start": self.launcher_start}}
        act["assets"] = {"large_image": "logo", "large_text": NAZWA_DISCORD}
        act["buttons"] = [{"label": f"{L[5]} {NAZWA_DISCORD}", "url": GITHUB_URL},
                          {"label": "Discord", "url": DISCORD_INVITE}]
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

BG_SLOTS = {"home": "tlo", "accounts": "tlo_konta", "modrinth": "tlo_modrinth"}


def bg_file(slot="home"):
    stem = BG_SLOTS.get(slot, "tlo")
    for e in BG_EXTS:
        f = DATA_DIR / f"{stem}{e}"
        if f.exists():
            return f
    if slot == "home":
        f = UI_DIR / "assets" / "background.jpg"
        return f if f.exists() else None
    return None


def bg_version(slot="home"):
    f = bg_file(slot)
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
        "settings": {k: settings[k] for k in ("jvm_args", "remember_accounts", "discord", "lang", "accent", "log_days")},
        "task": dict(task),
        "game_running": game_running(),
        "ms_configured": bool(AZURE_CLIENT_ID),
        "counts": counts(p),
        "login_message": login_message,
        "background": bg_version(),
        "backgrounds": {k: bg_version(k) for k in BG_SLOTS},
        "data_dir": str(DATA_DIR),
        "discord_status": discord.status,
        "client_version": WERSJA_CLIENTA,
        "news": NOWOSCI,
        "discord_invite": DISCORD_INVITE,
        "builtin_names": [t for _, t in MODY_WBUDOWANE],
        "microsoft_soon": MICROSOFT_WKROTCE,
        "app_update": app_update,
        "zorza_mod_versions": sorted(zorza_mod_versions(cached_only=True), key=ver_key),
        "lang": settings["lang"],
        "accent": settings["accent"],
        "log_days": settings["log_days"],
        "skin": settings["skin"],
        "skin_chosen": settings["skin_chosen"],
    }


class Server(ThreadingHTTPServer):
    # Domyślnie Python trzyma w kolejce tylko 5 połączeń. Gdy okno wysyła kilka zapytań naraz
    # (stan, logi, serwery, ikonki), a launcher akurat jest zajęty, Windows odrzucał nadmiar
    # i w oknie wyskakiwało "Failed to fetch".
    request_queue_size = 256
    daemon_threads = True

    def handle_error(self, request, client_address):
        pass                                   # zerwane połączenie z oknem to nie błąd launchera


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
        global last_ping, bye_at
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
                f = bg_file(q.get("slot", "home"))
                if not f:
                    return self.send(404, {"error": "brak tła"})
                ctype = {".png": "image/png", ".webp": "image/webp", ".gif": "image/gif"}.get(
                    f.suffix.lower(), "image/jpeg")
                return self.send(200, f.read_bytes(), ctype)
            if u.path == "/api/bye":
                bye_at = time.time()
                return self.send(200, {"ok": True})
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
            if u.path == "/api/servers":
                return self.send(200, {"servers": servers})
            if u.path == "/api/servers/status":
                return self.send(200, server_status(q.get("address", "")))
            if u.path == "/api/skins/lookup":
                try:
                    return self.send(200, skin_by_nick(q.get("nick", "")))
                except ValueError as e:
                    return self.send(400, {"error": str(e)})
            if u.path == "/api/modrinth/versions":
                p = get_profile(q.get("profile"))
                kind = q.get("kind", "mods")
                if kind not in KINDS:
                    return self.send(400, {"error": "Zły rodzaj."})
                out = [{"id": v["id"], "number": v.get("version_number"), "name": v.get("name"),
                        "type": v.get("version_type"), "date": v.get("date_published", "")[:10],
                        "games": v.get("game_versions", [])} for v in modrinth_versions(q.get("id", ""), kind, p)]
                meta = load_json(meta_path(p), {}).get("modrinth", {}).get(q.get("id", ""))
                return self.send(200, {"versions": out[:60], "installed": meta.get("version") if meta else None,
                                       "game": p["version"], "vanilla": p.get("loader") == "vanilla"})
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
            if u.path == "/api/modicon":
                f = safe_child(profile_dir(get_profile(q.get("profile"))) / "mods", q["file"])
                icon = jar_info(f).get("icon")
                with zipfile.ZipFile(f) as z:
                    return self.send(200, z.read(icon.lstrip("/")), "image/png")
            if u.path == "/api/worlds":
                return self.send(200, list_worlds(get_profile(q.get("profile"))))
            if u.path == "/api/worldicon":
                w = safe_child(profile_dir(get_profile(q.get("profile"))) / "saves", q["folder"])
                return self.send(200, (w / "icon.png").read_bytes(), "image/png")
            if u.path == "/api/skins":
                return self.send(200, {"skins": skins_list(), "selected": settings["skin"]})
            if u.path == "/api/skins/file":
                f = safe_child(MY_SKINS, q["file"])
                data = f.read_bytes()
                model = load_json(SKINS_META, {}).get(f.name, {}).get("model", "classic")
                self.send_response(200)
                self.send_header("Content-Type", "image/png")
                self.send_header("X-Skin-Model", model)
                self.send_header("Content-Length", str(len(data)))
                self.send_header("Cache-Control", "no-store")
                self.end_headers()
                return self.wfile.write(data)
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
            if u.path == "/api/skins/upload":
                fname = skin_add(q.get("name", "skin.png"), self.body(), q.get("model"))
                return self.send(200, {"ok": True, "file": fname})
            if u.path == "/api/background/upload":
                ext = Path(q.get("name", "")).suffix.lower()
                if ext not in BG_EXTS:
                    return self.send(400, {"error": "Wybierz obrazek JPG, PNG, WEBP albo GIF."})
                data = self.body()
                if len(data) > 25 * 1048576:
                    return self.send(400, {"error": "Obrazek jest za duży (max 25 MB)."})
                stem = BG_SLOTS.get(q.get("slot", "home"), "tlo")
                for e in BG_EXTS:
                    (DATA_DIR / f"{stem}{e}").unlink(missing_ok=True)
                (DATA_DIR / f"{stem}{'.jpg' if ext == '.jpeg' else ext}").write_bytes(data)
                return self.send(200, {"ok": True})
            d = self.json_body()
            path = u.path
            if path == "/api/background/reset":
                stem = BG_SLOTS.get(d.get("slot", "home"), "tlo")
                for e in BG_EXTS:
                    (DATA_DIR / f"{stem}{e}").unlink(missing_ok=True)
                return self.send(200, {"ok": True})
            # --- gra
            if path == "/api/play":
                if game_running():
                    return self.send(400, {"error": "Gra już działa."})
                if not run_task(do_play, settings["active_profile"]):
                    return self.send(400, busy)
            elif path == "/api/play_official":
                if not run_task(do_play_official, d.get("id") or settings["active_profile"]):
                    return self.send(400, busy)
            elif path == "/api/mods/check":
                return self.send(200, {"issues": check_mods(get_profile(d.get("profile")), bool(d.get("auto")))})
            elif path == "/api/app_update/check":
                check_app_update()
                return self.send(200, {"app_update": app_update, "version": WERSJA_CLIENTA})
            elif path == "/api/app_update/start":
                if app_update["version"] and app_update["state"] in ("idle", "error") and not game_running() \
                        and not task.get("running"):
                    threading.Thread(target=do_app_update, daemon=True).start()
            elif path == "/api/mods/dismiss":
                dismiss_issues(get_profile(d.get("profile")), [str(k) for k in d.get("keys", [])][:200])
            elif path == "/api/mods/remove_dups":
                if not run_task(do_remove_dups, get_profile(d.get("profile")), [str(x) for x in d.get("files", [])]):
                    return self.send(400, busy)
            # --- serwery
            elif path in ("/api/servers/add", "/api/servers/edit"):
                addr = str(d.get("address", "")).strip()
                if not RE_HOST.match(addr):
                    return self.send(400, {"error": "To nie wygląda na adres serwera (np. anarchia.gg albo 123.45.67.89:25565)."})
                name = str(d.get("name") or "").strip()[:40] or addr
                if path.endswith("add"):
                    if any(x["address"].lower() == addr.lower() for x in servers):
                        return self.send(400, {"error": "Ten serwer jest już na liście."})
                    servers.append({"id": uuid.uuid4().hex[:10], "name": name, "address": addr, "added": int(time.time())})
                else:
                    srv = next((x for x in servers if x["id"] == d.get("id")), None)
                    if not srv:
                        return self.send(404, {"error": "Nie ma takiego serwera."})
                    srv.update(name=name, address=addr)
                save_servers()
            elif path == "/api/servers/delete":
                servers[:] = [x for x in servers if x["id"] != d.get("id")]
                save_servers()
            elif path == "/api/servers/move":
                ids = [str(x) for x in d.get("ids", [])]
                servers.sort(key=lambda x: ids.index(x["id"]) if x["id"] in ids else 999)
                save_servers()
            elif path == "/api/servers/play":
                if d.get("mode") == "microsoft" and (MICROSOFT_WKROTCE or not accounts):
                    return self.send(400, {"error": "Logowanie przez Microsoft nie jest jeszcze dostępne – użyj oficjalnego launchera."})
                if not run_task(do_play_server, d.get("id"), d.get("mode"), d.get("profile") or settings["active_profile"]):
                    return self.send(400, busy)
            elif path == "/api/skins/add_nick":
                try:
                    sk = skin_by_nick(str(d.get("nick", "")))
                except ValueError as e:
                    return self.send(400, {"error": str(e)})
                return self.send(200, {"file": skin_add(sk["name"], base64.b64decode(sk["data"]), sk["model"])})
            elif path == "/api/mods/install_deps":
                def _inst(pp, slugs):
                    for i, sl in enumerate(slugs):
                        task.update(max=len(slugs), progress=i)
                        try:
                            modrinth_install(sl, "mods", pp)
                        except Exception as e:
                            log(f"Nie udało się pobrać {sl}: {e}")
                    ensure_deps(pp)
                    task["done"] = f"Pobrano wymagane mody: {len(slugs)}"
                if not run_task(_inst, get_profile(d.get("profile")), [x for x in d.get("slugs", []) if x]):
                    return self.send(400, busy)
            elif path == "/api/mods/update":
                if not run_task(do_update_mods, get_profile(d.get("profile")), list(d.get("items", []))):
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
            elif path == "/api/task/cancel":
                if task["running"] and task.get("kind") == "import":
                    task.update(cancel=True, status="Anulowanie...")
            elif path == "/api/import/run":
                if d.get("version") not in get_versions():
                    return self.send(400, {"error": "Wybierz wersję gry z listy."})
                if not run_task(do_import_launcher, d):
                    return self.send(400, busy)
            # --- zawartość
            elif path == "/api/modrinth/install":
                if not run_task(modrinth_install, d["project_id"], d["kind"], get_profile(d.get("profile")), None, d.get("version_id")):
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
                if d.get("lang") in ("pl", "en", "de", "uk", "es"):
                    settings["lang"] = d["lang"]
                    discord.update()
                if re.fullmatch(r"#[0-9a-fA-F]{6}", str(d.get("accent", ""))):
                    settings["accent"] = d["accent"]
                if "log_days" in d and int(d["log_days"]) in (0, 7, 14, 30, 90):
                    settings["log_days"] = int(d["log_days"])
                    threading.Thread(target=clean_logs, daemon=True).start()
                save_settings()
            elif path == "/api/open_url":
                if d.get("url") not in (DISCORD_INVITE, GITHUB_URL):
                    return self.send(400, {"error": "Ten adres nie jest dozwolony."})
                webbrowser.open(d["url"])
            elif path == "/api/worlds/delete":
                p = get_profile(d.get("profile"))
                w = safe_child(profile_dir(p) / "saves", d["folder"])
                TRASH_DIR.mkdir(exist_ok=True)
                shutil.move(str(w), str(TRASH_DIR / f"swiat-{w.name}-{int(time.time())}"))
                log(f"Świat {w.name} przeniesiony do folderu 'usuniete'")
            elif path == "/api/worlds/open":
                p = get_profile(d.get("profile"))
                target = profile_dir(p) / "saves"
                target.mkdir(exist_ok=True)
                if os.name == "nt":
                    os.startfile(str(target))
            elif path == "/api/skins/select":
                safe_child(MY_SKINS, d["file"])
                settings["skin"] = d["file"]
                settings["skin_chosen"] = True
                save_settings()
            elif path == "/api/skins/apply":
                safe_child(MY_SKINS, d["file"])
                if not run_task(do_apply_skin, d["file"]):
                    return self.send(400, busy)
            elif path == "/api/skins/model":
                meta = load_json(SKINS_META, {})
                meta.setdefault(d["file"], {})["model"] = "slim" if d.get("model") == "slim" else "classic"
                save_json(SKINS_META, meta)
            elif path == "/api/skins/delete":
                f = safe_child(MY_SKINS, d["file"])
                f.unlink(missing_ok=True)
                if settings["skin"] == d["file"]:
                    settings["skin"] = "startowy.png"
                    save_settings()
            elif path == "/api/quit":
                # nowa wersja launchera prosi starą o zamknięcie (gdy gra nie działa)
                if game_running() or task["running"]:
                    return self.send(409, {"error": "busy"})
                self.send(200, {"ok": True})
                threading.Timer(0.3, lambda: os._exit(0)).start()
                return
            elif path == "/api/clear_error":
                task["error"] = ""
                task["done"] = ""
                task["after"] = ""
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
        closed = bye_at > last_ping and time.time() - bye_at > 6     # okno zamknięte (nie odświeżone)
        if not busy and (closed or time.time() - last_ping > 150):
            log("Okno zamknięte – kończę.")
            os._exit(0)


# ---------------------------------------------------------------------------
#  Automatyczna aktualizacja aplikacji (wersja z instalatora)
# ---------------------------------------------------------------------------

APP_RELEASE = "https://github.com/Lizqk054319/zorza/releases/download/app-latest/"
app_update = {"version": None, "state": "idle", "progress": 0, "error": None,
              "current": None, "checked": 0, "check_error": None, "installed": False}


def _ver(v):
    return tuple(int(x) for x in re.findall(r"\d+", str(v))[:4])


def check_app_update():
    """Tylko zainstalowana aplikacja (.exe) – porównuje wersję z najnowszym wydaniem na GitHubie."""
    app_update["installed"] = bool(getattr(sys, "frozen", False))
    try:
        r = requests.get(APP_RELEASE + "version.json", headers={**UA, "Cache-Control": "no-cache"}, timeout=15)
        r.raise_for_status()
        latest = json.loads(r.content.decode("utf-8-sig")).get("version")
        app_update.update(current=latest, checked=int(time.time()), check_error=None)
        if latest and _ver(latest) > _ver(WERSJA_CLIENTA) and app_update["installed"]:
            app_update["version"] = latest
            log(f"Dostępna nowa wersja Zora Client: {latest}")
    except Exception as e:
        app_update.update(checked=int(time.time()), check_error=str(e))
        log(f"Nie udało się sprawdzić aktualizacji aplikacji: {e}")


def app_update_loop():
    """Sprawdza od razu po starcie, a potem co 15 minut – nie trzeba restartować launchera."""
    while True:
        if app_update["state"] in ("idle", "error"):
            check_app_update()
        time.sleep(15 * 60)


def do_app_update():
    try:
        app_update.update(state="downloading", progress=0, error=None)
        folder = DATA_DIR / "aktualizacja"
        folder.mkdir(exist_ok=True)
        dest = folder / "ZorzaSetup.exe"
        with requests.get(APP_RELEASE + "ZorzaSetup.exe", headers=UA, stream=True, timeout=60) as r:
            r.raise_for_status()
            total = int(r.headers.get("content-length") or 0)
            done = 0
            with open(dest, "wb") as out:
                for chunk in r.iter_content(256 * 1024):
                    out.write(chunk)
                    done += len(chunk)
                    if total:
                        app_update["progress"] = min(99, int(done * 100 / total))
        app_update.update(state="installing", progress=100)
        log("Instalowanie aktualizacji...")
        flags = 0x00000008 | 0x00000200 if os.name == "nt" else 0      # DETACHED_PROCESS | NEW_PROCESS_GROUP
        subprocess.Popen([str(dest), "/VERYSILENT", "/SUPPRESSMSGBOXES", "/NORESTART", "/AUTOUPDATE=1"],
                         creationflags=flags, close_fds=True)
        time.sleep(1.5)
        os._exit(0)                     # instalator podmieni pliki i uruchomi nową wersję
    except Exception as e:
        app_update.update(state="error", error=str(e))
        log(f"Aktualizacja nie powiodła się: {e}")


def main():
    try:
        server = Server(("127.0.0.1", PORT), Handler)
    except OSError:
        # Działa inna (np. starsza) kopia launchera – poproś ją o zamknięcie i wystartuj tę.
        server = None
        try:
            r = requests.post(f"http://127.0.0.1:{PORT}/api/quit", json={}, timeout=3,
                              headers={"Origin": f"http://127.0.0.1:{PORT}"})
            if r.status_code == 200:
                for _ in range(30):
                    time.sleep(0.2)
                    try:
                        server = Server(("127.0.0.1", PORT), Handler)
                        break
                    except OSError:
                        pass
        except Exception:
            pass
        if server is None:
            print("Launcher już działa (albo trwa gra/pobieranie) – otwieram jego okno.")
            print("Jeśli to stara wersja: zamknij wszystkie czarne okna launchera i uruchom ponownie.")
            open_window()
            return
    log(f"{NAZWA_LAUNCHERA} – dane w {DATA_DIR}")
    threading.Thread(target=get_versions, daemon=True).start()
    threading.Thread(target=watchdog, daemon=True).start()
    threading.Thread(target=log_cleaner, daemon=True).start()
    threading.Thread(target=fetch_builtin_icons, daemon=True).start()
    skins_list()
    discord.update()
    threading.Thread(target=lambda: zorza_mod_versions(), daemon=True).start()   # lista wersji moda
    threading.Thread(target=app_update_loop, daemon=True).start()
    if "--updated" in sys.argv:
        # po automatycznej aktualizacji stare okno samo się przeładuje – nowe otwieramy tylko, gdy go nie ma
        def maybe_open():
            time.sleep(7)
            if time.time() - last_ping > 6:
                open_window()
        threading.Thread(target=maybe_open, daemon=True).start()
    elif "--no-window" not in sys.argv:
        open_window()
    print("Nie zamykaj tego okna, dopóki używasz launchera.")
    server.serve_forever()


if __name__ == "__main__":
    main()
