# Zorza Launcher

A small, free and non-commercial launcher for **Minecraft: Java Edition**, made as a hobby project for me, my friends and the players of my Minecraft server community.

## Features

- **Microsoft account sign-in** (OAuth 2.0 with PKCE). Players enter their credentials only on Microsoft's own sign-in page; the launcher never sees e-mail addresses or passwords.
- Game ownership check through the official Minecraft Services API. Offline/non-premium accounts are **not** supported.
- Downloads official game files and the Java runtime from Mojang, with Fabric loader support.
- Built-in **Modrinth** browser: install mods, resource packs and shaders in one click (required dependencies are installed automatically).
- Profiles (Minecraft 1.20.1 and newer): own version, Fabric/Vanilla, RAM and a separate game folder per profile.
- Shareable profile codes and import from other launchers (official launcher, Modrinth App, Prism/MultiMC, Lunar Client).
- 3D player model of your skin on the home screen, custom background, Discord Rich Presence.
- "Repair installation", JVM settings, live game console.

## Privacy

Only the Minecraft username, UUID and refresh token are stored, locally on the player's PC, encrypted with Windows DPAPI (this can be turned off in settings). No data is sent anywhere other than Microsoft, Mojang and Modrinth.

## Running (Windows)

1. Install Python 3 from python.org (tick "Add python.exe to PATH").
2. Download this repository and double-click `uruchom.bat`.

Instrukcja po polsku: [INSTRUKCJA.txt](INSTRUKCJA.txt)

## Built with

- [minecraft-launcher-lib](https://codeberg.org/JakobDev/minecraft-launcher-lib)
- [Modrinth API](https://docs.modrinth.com/)

Not affiliated with Mojang Studios or Microsoft.
