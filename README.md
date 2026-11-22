# Zorza Launcher

A small, free and non-commercial launcher for **Minecraft: Java Edition**, made as a hobby project for me, my friends and the players of my Minecraft server community.

## Features

- **Microsoft account sign-in** (OAuth 2.0 with PKCE). Players enter their credentials only on Microsoft's own sign-in page; the launcher never sees e-mail addresses or passwords.
- Game ownership check through the official Minecraft Services API. Offline/non-premium accounts are **not** supported.
- Downloads official game files and the Java runtime from Mojang, with Fabric loader support.
- Built-in **Modrinth** browser: install mods, resource packs and shaders in one click (required dependencies are installed automatically).
- Separate game folder per Minecraft version (own mods, resource packs, shaders and worlds).
- Version picker, "repair installation", RAM and JVM settings, live game console.

## Privacy

Only the Minecraft username, UUID and refresh token are stored, locally on the player's PC (this can be turned off in settings). No data is sent anywhere other than Microsoft, Mojang and Modrinth.

## Running (Windows)

1. Install Python 3 from python.org (tick "Add python.exe to PATH").
2. Download this repository and double-click `uruchom.bat`.

Instrukcja po polsku: [INSTRUKCJA.txt](INSTRUKCJA.txt)

## Built with

- [minecraft-launcher-lib](https://codeberg.org/JakobDev/minecraft-launcher-lib)
- [Modrinth API](https://docs.modrinth.com/)

Not affiliated with Mojang Studios or Microsoft.
