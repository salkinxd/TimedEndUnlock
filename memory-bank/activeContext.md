# Active Context

## Purpose
- Server/common Minecraft mod (`timed_end_unlock`) that controls and schedules player access to The End dimension.

## Current State & Immediate Focus
- Status: Base project scaffold initialized; core feature implementation pending.
- Completed:
  - Gradle Kotlin DSL build with `org.quiltmc.loom.no_remap` (`1.15.1`) and Java 25 toolchain.
  - Quilt mod manifest (`quilt.mod.json`) and Mixin descriptor (`timed_end_unlock.mixins.json`).
  - Main entrypoint `TimedEndUnlock` implementing Fabric `ModInitializer`.
- Immediate Development Focus:
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

