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
