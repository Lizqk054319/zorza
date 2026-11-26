#!/usr/bin/env python3
"""
Prosty preprocesor wersji dla moda Zora Client.

Jeden kod źródłowy, osobny plik .jar na każdą wersję Minecrafta. W plikach .java:

    //#if MC >= 1.21.5
    kod tylko dla 1.21.5 i nowszych
    //#elseif MC >= 1.21.2
    kod dla 1.21.2 - 1.21.4
    //#else
    //$$ kod dla starszych (wyłączone linie mają przedrostek //$$)
    //#endif

Na początku pliku można dać  //#file-if MC >= 1.21.5  – na innych wersjach plik jest usuwany
(i znika z zorza.mixins.json, jeśli to mixin).

Użycie:  python3 tools/preprocess.py 1.21.5 mod/src
Zmienia pliki w miejscu (w CI na świeżej kopii repo). Uruchomione z wersją bazową (1.21.4)
przywraca kod do stanu bazowego, więc można też przełączać wersje lokalnie.
"""
import json
import re
import sys
from pathlib import Path

PREFIX = "//$$ "


def ver(s):
    return tuple(int(x) for x in re.findall(r"\d+", s))


def cond(expr, mc):
    expr = expr.strip()
    if "||" in expr:
        return any(cond(e, mc) for e in expr.split("||"))
    if "&&" in expr:
        return all(cond(e, mc) for e in expr.split("&&"))
    m = re.fullmatch(r"MC\s*(>=|<=|==|!=|>|<)\s*([\d.]+)", expr)
    if not m:
        raise ValueError(f"Zły warunek: {expr}")
    a, b = mc, ver(m.group(2))
    return {">=": a >= b, "<=": a <= b, "==": a == b, "!=": a != b, ">": a > b, "<": a < b}[m.group(1)]


def process_text(text, mc, name):
    out = []
    stack = []  # (aktywny_rodzic, czy_jakaś_gałąź_była_aktywna, aktywna_teraz)
    for no, line in enumerate(text.split("\n"), 1):
        s = line.strip()
        active = all(fr[2] for fr in stack)
        if s.startswith("//#if "):
            c = cond(s[6:], mc)
            stack.append([active, c, c])
            out.append(line)
            continue
        if s.startswith("//#elseif ") or s.startswith("//#elif "):
            if not stack:
                raise ValueError(f"{name}:{no}: #elseif bez #if")
            fr = stack[-1]
            c = (not fr[1]) and cond(s.split(" ", 1)[1], mc)
            fr[2] = c
            fr[1] = fr[1] or c
            out.append(line)
            continue
        if s == "//#else":
            fr = stack[-1]
            fr[2] = not fr[1]
            fr[1] = True
            out.append(line)
            continue
        if s == "//#endif":
            if not stack:
                raise ValueError(f"{name}:{no}: #endif bez #if")
            stack.pop()
            out.append(line)
            continue
        if not stack:
            out.append(line)
            continue
        indent = line[: len(line) - len(line.lstrip())]
        body = line.lstrip()
        if active:
            if body.startswith(PREFIX):
                body = body[len(PREFIX):]
            elif body.startswith("//$$"):
                body = body[4:]
            out.append(indent + body)
        else:
            if body and not body.startswith("//$$"):
                body = PREFIX + body
            out.append(indent + body)
    if stack:
        raise ValueError(f"{name}: brakuje #endif")
    return "\n".join(out)


def main():
    mc = ver(sys.argv[1])
    root = Path(sys.argv[2])
    removed = set()
    changed = 0
    for f in root.rglob("*.java"):
        text = f.read_text(encoding="utf-8")
        first = text.lstrip().split("\n", 1)[0].strip()
        if first.startswith("//#file-if ") and not cond(first[len("//#file-if "):], mc):
            removed.add(f.stem)
            f.unlink()
            continue
        if "//#" not in text:
            continue
        new = process_text(text, mc, str(f))
        if new != text:
            f.write_text(new, encoding="utf-8")
            changed += 1
    # usunięte mixiny wypadają z konfiguracji
    for mj in root.rglob("*.mixins.json"):
        data = json.loads(mj.read_text(encoding="utf-8"))
        for key in ("client", "mixins", "server"):
            if key in data:
                data[key] = [m for m in data[key] if m.rsplit(".", 1)[-1] not in removed]
        mj.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    print(f"preprocess {sys.argv[1]}: zmienione pliki {changed}, usunięte {sorted(removed)}")


if __name__ == "__main__":
    main()
