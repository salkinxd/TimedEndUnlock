# Timed End Unlock

**The End doesn't open on its own — it opens when *you* say it does.**

Timed End Unlock is a server-side Quilt/Fabric-API mod that lets server administrators gate, schedule, and revoke access to The End dimension. Players can explore the Overworld and the Nether freely, but the stronghold's End Portal stays dormant until the scheduled unlock moment — turning a seasonal event, a community countdown, or a hardcore progression rule into something the server enforces for everyone at once.

## Features

- **Scheduled unlock** — set a single date/time in a TOML config; Eyes of Ender simply refuse to activate the portal frame until that moment.
- **Indefinite lock mode** — `unlock_datetime = "never"` keeps The End closed until an admin decides otherwise.
- **Live admin control** — lock or unlock at any time with `/endunlock lock` and `/endunlock unlock`, with or without a future datetime. Changes are written back to the config file, so they survive restarts.
- **Grand opening announcement** — the instant the End opens, every online player gets a full-screen title plus the iconic End Portal sound.
- **The End closes again** — locking mid-session evicts every non-bypass player from The End, teleporting them back to the exact spot they entered from (persisted across restarts), or to their spawn point as a fallback.
- **Falling safety net** — players blocked at an activated-looking portal are given Refreshed Fire Resistance so the lava pool under a naturally generated End Portal can't kill them.
- **Permission-ready** — first-class permission nodes that LuckPerms picks up automatically, with vanilla op level as a fallback. No LuckPerms dependency.
- **Everyone can check the countdown** — plain `/endunlock` tells any player the current status: open, indefinitely locked, or when it will open.

## Unique Mechanics

Most gating mods just cancel an interaction. Timed End Unlock models the *state transition* of the dimension itself:

| Mechanic | What it does |
|---|---|
| **Portal interception** | A Mixin on `EndPortalBlock` blocks Overworld→End travel while locked. The End-side exit portal always works — nobody is ever trapped in The End. |
| **Entry recording** | Every allowed crossing writes the player's origin (dimension, position, rotation) to `config/timed_end_unlock_entries.dat`. Eviction on lock returns players to that exact spot, even after a server restart. |
| **Eviction on lock** | Locking (via command or config reload) instantly removes all non-bypass players from The End, using recorded entry points or overworld respawn data. |
| **Permission watcher** | Bypass status is re-checked for players inside The End every second, so revoking `timed_end_unlock:bypass` in LuckPerms applies live — the player is evicted within one tick window. |
| **Armed announcement** | The title+sound fanfare only fires if the unlock was in the future when armed — admins opening the End early don't trigger a fake countdown celebration. |
| **Fail-open safety** | If the config can't be read, the mod logs the error and leaves The End open rather than locking everyone out. |

## Commands

| Command | Permission | Description |
|---|---|---|
| `/endunlock` | none (everyone) | Shows current status: open, locked indefinitely, or the scheduled unlock time. |
| `/endunlock reload` | `timed_end_unlock:reload` | Reloads the config file; evicts players if the End is now locked. |
| `/endunlock unlock` | `timed_end_unlock:manage` | Opens The End immediately (writes now-time to config). |
| `/endunlock unlock "yyyy-MM-dd HH:mm"` | `timed_end_unlock:manage` | Schedules a future unlock. Must be in the future; tab-completion suggests a date. |
| `/endunlock lock` | `timed_end_unlock:manage` | Locks The End indefinitely (`never`) and evicts non-bypass players. |
| `/endunlock lock "yyyy-MM-dd HH:mm"` | `timed_end_unlock:manage` | Locks until the given future datetime. |

Subcommands require the permission node **or** vanilla op level `GAMEMASTERS` or higher.

### Permission nodes

- `timed_end_unlock:reload` — reload the config
- `timed_end_unlock:manage` — run lock/unlock commands
- `timed_end_unlock:bypass` — exempt from all gating and eviction (intended for staff)

Nodes are registered through Fabric Permission API v1; LuckPerms bridges them automatically.

## Configuration

The mod creates a fully commented `config/timed_end_unlock.toml` on first run:

```toml
unlock_datetime = "2026-12-31 20:00"   # yyyy-MM-dd HH:mm (server-local) or "never"
unlock_message = "The End is not open yet. It will open on {datetime}."
lock_message_indefinite = "The End is closed indefinitely. An administrator will announce when it reopens."
datetime_format = "dd/MM/yyyy HH:mm"   # pattern used for {datetime}
title_message = "The End has been opened!"
```

- `{datetime}` in `unlock_message` is replaced with the formatted unlock time.
- All messages are plain text (no color codes or JSON components).
- `/endunlock reload` applies edits without a restart; lock/unlock commands edit this file for you.

## Requirements

| Component | Version |
|---|---|
| Minecraft | `26.3` |
| Java | `25` |
| Quilt Loader | `>= 0.30.2` (works with Fabric-API entrypoints in no-remap mode) |
| Fabric API | `0.165.0+26.3` |

Drop the mod JAR into your server's `mods/` folder alongside Fabric API. It runs on dedicated servers and single-player/LAN worlds alike.

## Building

```bash
./gradlew build
```

The JAR is produced in `build/libs/`. The build uses Gradle Kotlin DSL, Quilt Loom (no-remap), and a Java 25 toolchain.

## Known Limitations

- **Server-local time** — the unlock datetime is interpreted in the server's timezone with no timezone config; DST shifts affect the moment directly.
- **Simple TOML parsing** — the config is read with a line-based regex for string keys only; exotic TOML syntax (multi-line strings, inline tables) is not supported.
- **Gate message is plain text** — messages cannot contain formatting codes or translatable components.
- **Command datetimes are minute-precision** — seconds are not accepted in `/endunlock lock|unlock` arguments.
- **Bypass exemption is per-player at check time** — players without bypass who entered while open are evicted on lock, but no action is taken against entities, items, or mobs already in The End.
- **Runtime testing** — the mod compiles and is feature-complete for 1.0.0; extended live-server soak testing is still in progress.

## Credits

- **salkin** — author and maintainer (`at.salkin.timedendunlock`).
- Built on **Quilt Loader**, **Fabric API** (including Fabric Permission API), and **SpongePowered Mixin**; Minecraft is a trademark of Mojang/Microsoft. This project is not affiliated with Mojang or the Quilt/Fabric projects.

## License

MIT
