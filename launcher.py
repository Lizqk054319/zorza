# -*- coding: utf-8 -*-
"""
Zorza Launcher – wersja próbna.

Uruchom:  uruchom.bat   (albo:  python launcher.py)

Backend w Pythonie (pobieranie gry, mody, konta), a wygląd to strona HTML
otwierana w Microsoft Edge w trybie aplikacji (bez pasków przeglądarki).
"""

import html
import json
import os
import shutil
import subprocess
import sys
import threading
import time
import traceback
import urllib.parse
import webbrowser
from collections import deque
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

# ---------------------------------------------------------------------------
#  USTAWIENIA – tu możesz zmieniać
# ---------------------------------------------------------------------------

NAZWA_LAUNCHERA = "Zorza"          # nazwa wyświetlana w launcherze
DOMYSLNA_WERSJA = "1.21.4"         # wersja wybrana na start
PORT = 47615                       # port lokalnego serwera (nie zmieniaj po rejestracji w Azure)

# Identyfikator aplikacji z Azure (logowanie Microsoft). Instrukcja w INSTRUKCJA.txt.
# Dopóki jest pusty, przycisk "Dodaj konto" pokaże, co trzeba zrobić.
AZURE_CLIENT_ID = "6553e03b-b905-4f30-a877-535ed1c3a975"
REDIRECT_URI = f"http://localhost:{PORT}/callback"

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
DATA_DIR = Path(os.environ.get("APPDATA", Path.home())) / "ZorzaLauncher"
MC_DIR = DATA_DIR / "minecraft"          # wspólne pliki gry (wersje, biblioteki, Java)
INST_DIR = DATA_DIR / "instances"        # osobny folder gry dla każdej wersji
ACCOUNTS_FILE = DATA_DIR / "accounts.json"
SETTINGS_FILE = DATA_DIR / "settings.json"
for d in (DATA_DIR, MC_DIR, INST_DIR):
    d.mkdir(parents=True, exist_ok=True)

KINDS = {  # rodzaj zawartości -> (folder w instancji, typ projektu na Modrinth)
    "mods": ("mods", "mod"),
    "resourcepacks": ("resourcepacks", "resourcepack"),
    "shaders": ("shaderpacks", "shader"),
}
UA = {"User-Agent": f"ZorzaLauncher/0.1 ({NAZWA_LAUNCHERA})"}
MODRINTH = "https://api.modrinth.com/v2"

lock = threading.RLock()
log_lines = deque(maxlen=3000)
log_counter = 0
task = {"running": False, "status": "", "progress": 0, "max": 0, "error": ""}
game_proc = None
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
    print(msg)


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
settings.setdefault("version", DOMYSLNA_WERSJA)
settings.setdefault("ram_mb", 4096)
settings.setdefault("jvm_args", "")
settings.setdefault("remember_accounts", True)
settings.setdefault("active_account", None)

# Konta: zapisywane tylko na tym komputerze (nick, UUID i token odświeżania).
# Hasło i e-mail nigdy nie trafiają do launchera.
accounts = load_json(ACCOUNTS_FILE, []) if settings["remember_accounts"] else []


def save_settings():
    save_json(SETTINGS_FILE, settings)


def save_accounts():
    if settings["remember_accounts"]:
        save_json(ACCOUNTS_FILE, accounts)
    elif ACCOUNTS_FILE.exists():
        ACCOUNTS_FILE.unlink()


def instance_dir(version):
    safe = "".join(c for c in version if c.isalnum() or c in ".-_")
    p = INST_DIR / safe
    for folder, _ in KINDS.values():
        (p / folder).mkdir(parents=True, exist_ok=True)
    return p


def meta_path(version):
    return instance_dir(version) / ".zorza.json"


# ---------------------------------------------------------------------------
#  Wersje gry
# ---------------------------------------------------------------------------

def get_versions():
    if version_cache["list"] is None:
        try:
            vl = mll.utils.get_version_list()
            version_cache["list"] = [v["id"] for v in vl if v["type"] == "release"]
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


def install_version(version, force=False):
    """Instaluje wersję (z Fabricem, jeśli dostępny). Zwraca id wersji do uruchomienia."""
    cb = make_callback()
    installed = installed_ids()
    if force or version not in installed:
        task.update(status=f"Pobieranie Minecraft {version}...")
        mll.install.install_minecraft_version(version, str(MC_DIR), callback=cb)
    try:
        fabric_ok = mll.fabric.is_minecraft_version_supported(version)
    except Exception:
        fabric_ok = False
    if not fabric_ok:
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
    task.update(running=True, status="Przygotowanie...", progress=0, max=0, error="")

    def worker():
        try:
            fn(*args)
        except Exception as e:
            task["error"] = str(e)
            log("BŁĄD: " + "".join(traceback.format_exception(e)))
        finally:
            task.update(running=False, status="")
    threading.Thread(target=worker, daemon=True).start()
    return True


def do_play(version):
    global game_proc
    acc = next((a for a in accounts if a["uuid"] == settings["active_account"]), None)
    if not acc:
        raise RuntimeError("Najpierw dodaj konto Microsoft.")
    launch_id = install_version(version)
    task.update(status="Logowanie...", max=0)
    token = refresh_account(acc)
    ram = int(settings["ram_mb"])
    jvm = [f"-Xmx{ram}M", f"-Xms{min(ram, 1024)}M"] + settings["jvm_args"].split()
    options = {
        "username": acc["name"],
        "uuid": acc["uuid"],
        "token": token,
        "gameDirectory": str(instance_dir(version)),
        "jvmArguments": jvm,
        "launcherName": NAZWA_LAUNCHERA,
        "launcherVersion": "0.1",
    }
    cmd = mll.command.get_minecraft_command(launch_id, str(MC_DIR), options)
    task.update(status="Uruchamianie gry...")
    log(f"Uruchamiam {launch_id} jako {acc['name']}")
    flags = subprocess.CREATE_NO_WINDOW if os.name == "nt" else 0
    game_proc = subprocess.Popen(cmd, cwd=str(instance_dir(version)), stdout=subprocess.PIPE,
                                 stderr=subprocess.STDOUT, text=True, encoding="utf-8",
                                 errors="replace", creationflags=flags)

    def reader(p):
        for line in p.stdout:
            log(line.rstrip())
        log(f"Gra zamknięta (kod {p.wait()}).")
    threading.Thread(target=reader, args=(game_proc,), daemon=True).start()


def do_repair(version):
    install_version(version, force=True)
    log(f"Naprawa {version} zakończona.")


# ---------------------------------------------------------------------------
#  Mody, paczki zasobów, shadery
# ---------------------------------------------------------------------------

def list_content(version, kind):
    folder = instance_dir(version) / KINDS[kind][0]
    meta = load_json(meta_path(version), {}).get("modrinth", {})
    by_file = {m["file"]: {"title": m.get("title"), "icon": m.get("icon")} for m in meta.values()}
    items = []
    for f in sorted(folder.iterdir(), key=lambda p: p.name.lower()):
        if f.name.startswith("."):
            continue
        enabled = not f.name.endswith(".disabled")
        base = f.name[:-9] if not enabled else f.name
        info = by_file.get(base, {})
        items.append({"file": f.name, "name": info.get("title") or base, "icon": info.get("icon"),
                      "enabled": enabled, "size": f.stat().st_size if f.is_file() else 0})
    return items


def counts(version):
    out = {}
    for kind, (folder, _) in KINDS.items():
        p = instance_dir(version) / folder
        out[kind] = sum(1 for f in p.iterdir() if not f.name.startswith("."))
    return out


def safe_child(folder, name):
    p = (folder / name).resolve()
    if p.parent != folder.resolve():
        raise ValueError("Nieprawidłowa nazwa pliku")
    return p


def modrinth_search(q, kind, cat, sort, version, offset):
    facets = [[f"project_type:{KINDS[kind][1]}"], [f"versions:{version}"]]
    if kind == "mods":
        facets.append(["categories:fabric"])
    if cat:
        facets.append([f"categories:{cat}"])
    params = {"query": q, "facets": json.dumps(facets), "index": sort or "relevance",
              "limit": 20, "offset": offset}
    r = requests.get(f"{MODRINTH}/search", params=params, headers=UA, timeout=15)
    r.raise_for_status()
    data = r.json()
    meta = load_json(meta_path(version), {}).get("modrinth", {})
    folder = instance_dir(version)
    for h in data.get("hits", []):
        m = meta.get(h["project_id"])
        h["installed"] = bool(m and ((folder / m["folder"] / m["file"]).exists()
                                     or (folder / m["folder"] / (m["file"] + ".disabled")).exists()))
    return data


def modrinth_install(project_id, kind, version, _seen=None):
    seen = _seen if _seen is not None else set()
    if project_id in seen:
        return
    seen.add(project_id)
    params = {"game_versions": json.dumps([version])}
    if kind == "mods":
        params["loaders"] = json.dumps(["fabric"])
    r = requests.get(f"{MODRINTH}/project/{project_id}/version", params=params, headers=UA, timeout=15)
    r.raise_for_status()
    versions = r.json()
    if not versions:
        raise RuntimeError(f"Brak wersji tego projektu dla Minecraft {version}.")
    v = versions[0]
    f = next((x for x in v["files"] if x.get("primary")), v["files"][0])
    proj = requests.get(f"{MODRINTH}/project/{project_id}", headers=UA, timeout=15).json()
    folder_name = KINDS[kind][0]
    dest_dir = instance_dir(version) / folder_name
    dest = safe_child(dest_dir, f["filename"])
    task.update(status=f"Pobieranie {proj.get('title', f['filename'])}...")
    log(f"Pobieranie {f['filename']}")
    with requests.get(f["url"], headers=UA, stream=True, timeout=60) as resp:
        resp.raise_for_status()
        with open(dest, "wb") as out:
            shutil.copyfileobj(resp.raw, out)
    meta = load_json(meta_path(version), {})
    meta.setdefault("modrinth", {})[project_id] = {
        "file": f["filename"], "folder": folder_name, "title": proj.get("title"),
        "icon": proj.get("icon_url")}
    save_json(meta_path(version), meta)
    if kind == "mods":
        for dep in v.get("dependencies", []):
            if dep.get("dependency_type") == "required" and dep.get("project_id"):
                modrinth_install(dep["project_id"], "mods", version, seen)


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

def state_json():
    v = settings["version"]
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
        "version": v,
        "versions": [{"group": k, "items": groups[k]} for k in order],
        "accounts": [{"uuid": a["uuid"], "name": a["name"]} for a in accounts],
        "active": settings["active_account"],
        "settings": {k: settings[k] for k in ("ram_mb", "jvm_args", "remember_accounts")},
        "task": dict(task),
        "game_running": game_proc is not None and game_proc.poll() is None,
        "ms_configured": bool(AZURE_CLIENT_ID),
        "counts": counts(v),
        "login_message": login_message,
        "has_background": (UI_DIR / "assets" / "background.jpg").exists(),
        "data_dir": str(DATA_DIR),
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

    def do_GET(self):
        global last_ping
        u = urllib.parse.urlparse(self.path)
        q = {k: v[0] for k, v in urllib.parse.parse_qs(u.query).items()}
        try:
            if u.path in ("/", "/index.html"):
                return self.send(200, (UI_DIR / "index.html").read_bytes(), "text/html; charset=utf-8")
            if u.path.startswith("/assets/"):
                p = safe_child(UI_DIR / "assets", u.path[len("/assets/"):])
                if p.exists():
                    ctype = {".jpg": "image/jpeg", ".png": "image/png", ".svg": "image/svg+xml"}.get(
                        p.suffix.lower(), "application/octet-stream")
                    return self.send(200, p.read_bytes(), ctype)
                return self.send(404, {"error": "brak pliku"})
            if u.path == "/api/state":
                last_ping = time.time()
                return self.send(200, state_json())
            if u.path == "/api/log":
                since = int(q.get("since", 0))
                with lock:
                    lines = [l for i, l in log_lines if i >= since]
                return self.send(200, {"lines": lines, "next": log_counter})
            if u.path == "/api/content":
                return self.send(200, list_content(q["version"], q["kind"]))
            if u.path == "/api/modrinth/search":
                return self.send(200, modrinth_search(q.get("q", ""), q.get("kind", "mods"),
                                                      q.get("cat", ""), q.get("sort", "relevance"),
                                                      q.get("version", settings["version"]),
                                                      int(q.get("offset", 0))))
            if u.path == "/callback":
                try:
                    name = ms_login_finish(self.path)
                    login_message.update(text=f"Zalogowano jako {name}", ok=True)
                    msg = f"Zalogowano jako <b>{html.escape(name)}</b>. Możesz zamknąć to okno i wrócić do launchera."
                except Exception as e:
                    login_message.update(text=f"Logowanie nieudane: {e}", ok=False)
                    msg = html.escape(f"Logowanie nieudane: {e}")
                return self.send(200, f"<html><body style='background:#111;color:#eee;font-family:"
                                      f"Segoe UI;display:grid;place-items:center;height:100vh'>"
                                      f"<p style='font-size:20px'>{msg}</p></body></html>",
                                 "text/html; charset=utf-8")
            self.send(404, {"error": "nie znaleziono"})
        except Exception as e:
            log(f"Błąd ({u.path}): {e}")
            self.send(500, {"error": str(e)})

    def do_POST(self):
        u = urllib.parse.urlparse(self.path)
        q = {k: v[0] for k, v in urllib.parse.parse_qs(u.query).items()}
        try:
            if u.path == "/api/content/upload":   # przed json_body – tu body to plik
                kind, version = q["kind"], q["version"]
                dest = safe_child(instance_dir(version) / KINDS[kind][0], q["name"])
                dest.write_bytes(self.body())
                log(f"Dodano plik {dest.name}")
                return self.send(200, {"ok": True})
            d = self.json_body()
            if u.path == "/api/select_version":
                settings["version"] = d["version"]
                save_settings()
            elif u.path == "/api/play":
                if game_proc is not None and game_proc.poll() is None:
                    return self.send(400, {"error": "Gra już działa."})
                if not run_task(do_play, settings["version"]):
                    return self.send(400, {"error": "Poczekaj, aż skończy się bieżące zadanie."})
            elif u.path == "/api/repair":
                if not run_task(do_repair, d.get("version") or settings["version"]):
                    return self.send(400, {"error": "Poczekaj, aż skończy się bieżące zadanie."})
            elif u.path == "/api/modrinth/install":
                if not run_task(modrinth_install, d["project_id"], d["kind"], d["version"]):
                    return self.send(400, {"error": "Poczekaj, aż skończy się bieżące zadanie."})
            elif u.path == "/api/content/toggle":
                folder = instance_dir(d["version"]) / KINDS[d["kind"]][0]
                p = safe_child(folder, d["file"])
                new = p.name[:-9] if p.name.endswith(".disabled") else p.name + ".disabled"
                p.rename(folder / new)
            elif u.path == "/api/content/delete":
                p = safe_child(instance_dir(d["version"]) / KINDS[d["kind"]][0], d["file"])
                shutil.rmtree(p) if p.is_dir() else p.unlink()
            elif u.path == "/api/open_folder":
                if d.get("kind"):
                    p = instance_dir(d["version"]) / KINDS[d["kind"]][0]
                else:
                    p = DATA_DIR
                if os.name == "nt":
                    os.startfile(str(p))
                else:
                    webbrowser.open(p.as_uri())
            elif u.path == "/api/accounts/ms_start":
                if not AZURE_CLIENT_ID:
                    return self.send(400, {"error": "not_configured"})
                login_message.update(text="", ok=True)
                return self.send(200, {"url": ms_login_start()})
            elif u.path == "/api/accounts/select":
                settings["active_account"] = d["uuid"]
                save_settings()
            elif u.path == "/api/accounts/delete":
                accounts[:] = [a for a in accounts if a["uuid"] != d["uuid"]]
                if settings["active_account"] == d["uuid"]:
                    settings["active_account"] = accounts[0]["uuid"] if accounts else None
                save_accounts()
                save_settings()
            elif u.path == "/api/settings":
                if "ram_mb" in d:
                    settings["ram_mb"] = max(1024, min(32768, int(d["ram_mb"])))
                if "jvm_args" in d:
                    settings["jvm_args"] = str(d["jvm_args"])
                if "remember_accounts" in d:
                    settings["remember_accounts"] = bool(d["remember_accounts"])
                    save_accounts()
                save_settings()
            elif u.path == "/api/clear_error":
                task["error"] = ""
                login_message.update(text="")
            else:
                return self.send(404, {"error": "nie znaleziono"})
            self.send(200, {"ok": True})
        except Exception as e:
            log(f"Błąd ({u.path}): {e}")
            self.send(500, {"error": str(e)})


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
        busy = task["running"] or (game_proc is not None and game_proc.poll() is None)
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
    if "--no-window" not in sys.argv:
        open_window()
    print("Nie zamykaj tego okna, dopóki używasz launchera.")
    server.serve_forever()


if __name__ == "__main__":
    main()
