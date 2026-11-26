#!/usr/bin/env python3
"""Ustawia w mod/gradle.properties wersje yarn / Fabric API / loadera dla podanego Minecrafta (CI)."""
import json
import re
import sys
import urllib.request

mc = sys.argv[1]
props = sys.argv[2]


def get(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "zorza-ci"}), timeout=60) as r:
        return r.read().decode()


yarn = json.loads(get(f"https://meta.fabricmc.net/v2/versions/yarn/{mc}"))
yarn = max(yarn, key=lambda y: y["build"])["version"] if yarn else ""
loader = json.loads(get("https://meta.fabricmc.net/v2/versions/loader"))
loader = next(l["version"] for l in loader if l.get("stable"))
meta = get("https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml")
apis = [v for v in re.findall(r"<version>([^<]+)</version>", meta) if v.endswith("+" + mc)]


def key(v):
    return tuple(int(x) for x in re.findall(r"\d+", v.split("+")[0]))


api = max(apis, key=key)
# Klientowe testy (gametest) istnieją w Fabric API dopiero od 1.21.4
mcv = tuple(int(x) for x in mc.split("."))
gametest = "true" if mcv >= (1, 21, 4) else "false"
java = "17" if mcv < (1, 20, 5) else "21"

text = open(props, encoding="utf-8").read()
for k, v in (("minecraft_version", mc), ("yarn_mappings", yarn), ("loader_version", loader),
             ("fabric_version", api), ("gametest", gametest), ("java_version", java)):
    if re.search(rf"^{k}=", text, re.M):
        text = re.sub(rf"^{k}=.*$", f"{k}={v}", text, flags=re.M)
    else:
        text += f"\n{k}={v}"
open(props, "w", encoding="utf-8").write(text + "\n")
print(f"mc={mc} yarn={yarn} loader={loader} fabric-api={api} gametest={gametest} java={java}")
