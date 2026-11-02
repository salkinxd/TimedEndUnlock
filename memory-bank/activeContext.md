# Active Context

## Purpose
- Server/common Minecraft mod (`timed_end_unlock`) that controls and schedules player access to The End dimension.

## Current State & Immediate Focus
- Status: Base project scaffold initialized; core feature implementation pending.
- Completed:
  - Gradle Kotlin DSL build with `org.quiltmc.loom.no_remap` (`1.15.1`) and Java 25 toolchain.
  - Quilt mod manifest (`quilt.mod.json`) and Mixin descriptor (`timed_end_unlock.mixins.json`).
  - Main entrypoint `TimedEndUnlock` implementing Fabric `ModInitializer`.
  - End gating v1: `UseBlockCallback` blocks `ENDER_EYE` on `END_PORTAL_FRAME` until TOML `unlock_datetime` (`Config.java`, `config/timed_end_unlock.toml`, `unlock_message` + `datetime_format` placeholders); MC 26.3 `Player.sendSystemMessage`.
  - `/endunlock reload` command + `timed_end_unlock:reload`/`timed_end_unlock:bypass` permission nodes via fabric-permission-api (op fallback GAMEMASTERS, LuckPerms bridge automatic).
- Immediate Development Focus:
  - Timer/schedule state persistence still pending (config reload, commands).
  - Implement timer/schedule configuration and state persistence for End unlock status.
  - Add Mixins under `at.salkin.timedendunlock.mixin` (e.g., End Portal block collision / Eye of Ender placement / dimension teleport interception).
  - Register server lifecycle hooks and admin commands via Fabric API in `TimedEndUnlock.onInitialize()`.

## Memory Bank Index
- `memory-bank/architecture.md`:
  - Initialization lifecycle (`quilt.mod.json` -> `TimedEndUnlock.onInitialize()`).
  - Mixin injection pipeline (`timed_end_unlock.mixins.json` -> `at.salkin.timedendunlock.mixin.*`).
  - Planned state management and data flow for scheduled End dimension gating.
- `memory-bank/techContext.md`:
  - Exact versions and dependencies (`gradle/libs.versions.toml`, `gradle.properties`).
  - Complete file routing map linking build scripts, manifests, and Java sources to relative paths.

