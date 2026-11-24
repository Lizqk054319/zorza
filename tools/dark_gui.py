"""Tworzy paczkę "Zora Dark GUI": przyciemnia szare tekstury ekwipunku/skrzyń z oryginalnego Minecrafta.
Użycie: python dark_gui.py <minecraft-client.jar> <folder-paczki>"""
import io
import re
import sys
import zipfile
from pathlib import Path

from PIL import Image

PATTERNS = [
    r"^assets/minecraft/textures/gui/container/.*\.png$",
    r"^assets/minecraft/textures/gui/sprites/container/.*\.png$",
    r"^assets/minecraft/textures/gui/recipe_book\.png$",
    r"^assets/minecraft/textures/gui/sprites/recipe_book/.*\.png$",
]
# jasność oryginału -> jasność ciemnej wersji (panel 198 -> bardzo ciemny, sloty 139 -> trochę jaśniejsze)
POINTS = [(0, 0), (55, 10), (85, 14), (110, 22), (139, 44), (170, 34), (198, 26), (230, 44), (255, 66)]


def remap(v):
    for (a, x), (b, y) in zip(POINTS, POINTS[1:]):
        if a <= v <= b:
            return int(round(x + (y - x) * (v - a) / (b - a)))
    return v


def darken(img):
    img = img.convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0 or max(r, g, b) - min(r, g, b) > 14:
                continue                                   # kolorowe elementy (ikony, strzałki) zostają
            v = remap((r + g + b) // 3)
            px[x, y] = (v, v, min(255, v + 3), a)         # lekko chłodny odcień
    return img


def main(jar, out):
    out = Path(out)
    n = 0
    with zipfile.ZipFile(jar) as z:
        for name in z.namelist():
            if not any(re.match(p, name) for p in PATTERNS):
                continue
            img = darken(Image.open(io.BytesIO(z.read(name))))
            dest = out / name
            dest.parent.mkdir(parents=True, exist_ok=True)
            img.save(dest)
            mcmeta = name + ".mcmeta"
            if mcmeta in z.namelist():
                (out / mcmeta).write_bytes(z.read(mcmeta))
            n += 1
    (out / "pack.mcmeta").write_text('{"pack": {"pack_format": 46, "description": "Zora Client – ciemny ekwipunek"}}')
    print(f"Ciemne tekstury: {n}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
