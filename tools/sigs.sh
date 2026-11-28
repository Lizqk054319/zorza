#!/bin/bash
# Zrzut sygnatur klas Minecrafta (po mapowaniu) – do przenoszenia moda między wersjami.
OUT="$1"; MC="$2"; mkdir -p "$OUT"
J=$(find ~/.gradle/caches/fabric-loom mod/.gradle -name "minecraft-*${MC}-net.fabricmc.yarn*.jar" 2>/dev/null | grep -iv "sources" | head -1)
find ~/.gradle/caches/fabric-loom mod/.gradle -name "*.jar" 2>/dev/null | grep -i minecraft | head -40 > "$OUT/_all_jars.txt"
echo "jar: $J" > "$OUT/_jar.txt"
[ -z "$J" ] && exit 0
CP="$J:$(find ~/.gradle/caches -name 'blaze3d*.jar' -o -name 'joml*.jar' 2>/dev/null | tr '\n' ':')"
while read -r C; do
  [ -z "$C" ] && continue
  javap -p -cp "$CP" "$C" > "$OUT/$C.txt" 2>&1
done < tools/sigs-classes.txt
unzip -l "$J" | grep -oE "net/minecraft/client/(gl|render|gui|texture)/[A-Za-z]+\.class|com/mojang/blaze3d/[A-Za-z/]+\.class" | sort > "$OUT/_classes.txt"
# kod (bytecode) wybranych klas – żeby zobaczyć, jak Minecraft sam coś rysuje
for C in net.minecraft.client.gl.Framebuffer net.minecraft.client.gl.RenderPipelines net.minecraft.client.render.RenderLayer; do
  javap -c -p -cp "$CP" "$C" > "$OUT/code-$C.txt" 2>&1
done
# klasy Fabric API związane z rysowaniem świata i HUD (zmieniają się między wersjami)
for FJ in $(find ~/.gradle/caches -name "fabric-rendering-v1-*.jar" -o -name "fabric-key-binding-api-v1-*.jar" 2>/dev/null | grep -v sources | head -4); do
  for C in $(unzip -l "$FJ" | grep -oE "net/fabricmc/fabric/api/[A-Za-z0-9/]+\.class" | grep -iE "WorldRender|HudRender|HudElement|KeyBinding" | sed 's/\.class$//; s#/#.#g'); do
    javap -p -cp "$FJ:$CP" "$C" > "$OUT/fabric-$C.txt" 2>&1
  done
done
# wszystkie klasy z "Hitbox" w nazwie (hitboxy przeniesiono w 1.21.9+)
for C in $(unzip -l "$J" | grep -oE "net/minecraft/[A-Za-z0-9/$]*Hitbox[A-Za-z0-9$]*\.class" | sed 's/\.class$//; s#/#.#g' | sort -u); do
  javap -p -cp "$CP" "$C" > "$OUT/hb-$C.txt" 2>&1
done
