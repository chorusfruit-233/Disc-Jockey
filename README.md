# Disc Jockey

Play note block songs (.nbs) in Minecraft.

This is an unofficial fork ported to **Minecraft 26.2** with **Java 25**.

[![Build](https://github.com/SMxNcn/Disc-Jockey/actions/workflows/build.yml/badge.svg)](https://github.com/SMxNcn/Disc-Jockey/actions/workflows/build.yml)

## Requirements

- Minecraft **26.2**
- Java **25+**
- Fabric Loader **>= 0.19.3**
- Fabric API **>= 0.155.2**
- Cloth Config **>= 26.2.155**
- Mod Menu (optional, recommended)

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.2
2. Download the latest `.jar` from the [Releases](https://github.com/SMxNcn/Disc-Jockey/releases) page
3. Place the jar in your `mods/` folder along with Fabric API and Cloth Config

## Song Directory

Place your `.nbs` files in:

```
.minecraft/config/disc_jockey/songs/
```

Use `/discjockey reload` to reload songs after adding new files.

## Controls

| Key | Action |
|-----|--------|
| J   | Open Disc Jockey GUI |

## Commands

| Command | Description |
|---------|-------------|
| `/discjockey` | Open the GUI |
| `/discjockey reload` | Reload songs from disk |
| `/discjockey play <song>` | Play a song by name |
| `/discjockey stop` | Stop playback |
| `/discjockey info` | Show current song info |
| `/discjockey speed <value>` | Set playback speed (0.0001–15) |
| `/discjockey loop yes/no` | Toggle song looping |
| `/discjockey remapInstruments` | Remap note block instruments |

## Building from Source

```bash
./gradlew clean build
```

The output JARs will be in `build/libs/`.

## Upstream and Fork Relationship

| | Original | This Fork |
|---|----------|-----------|
| Author | [SemmieDev](https://github.com/SemmieDev) | [SMxNcn](https://github.com/SMxNcn) |
| Minecraft | 1.20.x / 1.21.x | **26.2** |
| Java | 17 | **25** |
| Download | [Modrinth](https://modrinth.com/mod/disc-jockey) / [CurseForge](https://www.curseforge.com/minecraft/mc-mods/disc-jockey) | [GitHub Releases](https://github.com/SMxNcn/Disc-Jockey/releases) |

## License

[MIT](LICENSE) — Original work by SemmieDev and EnderKill98.
