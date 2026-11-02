# Architecture

## System Overview
- Mod Type: Hybrid Quilt Loader + Fabric API Minecraft mod (`id: timed_end_unlock`).
- Mapping Strategy: Unobfuscated Minecraft `26.3` via `org.quiltmc.loom.no_remap` plugin; `quilt.mod.json` declares `net.fabricmc:intermediary`.
- Runtime Target: Java 25 (`JAVA_25` Mixin compatibility).

## Core Application Flow
- Boot & Discovery:
  - Quilt Loader parses `src/main/resources/quilt.mod.json`.
  - Validates dependencies: `quilt_loader >= 0.30.2`, `fabric-api *`, `minecraft ~26.3`.
  - Registers Mixin config `src/main/resources/timed_end_unlock.mixins.json`.
- Bytecode Transformation:
  - SpongePowered Mixin (`minVersion: 0.8`) loads classes from `at.salkin.timedendunlock.mixin` (`defaultRequire: 1`).
  - Current `mixins` array is empty (`[]`); all vanilla hooks for End access interception attach here.
- Mod Initialization:
  - Quilt invokes `main` entrypoint: `at.salkin.timedendunlock.TimedEndUnlock`.
  - `TimedEndUnlock.onInitialize()` executes (`net.fabricmc.api.ModInitializer`), initializing SLF4J logger (`LoggerFactory.getLogger("timed_end_unlock")`) and registering Fabric API events/commands.

## State Management & Data Lifecycle
- Static Constants:
  - `TimedEndUnlock.MOD_ID`: `"timed_end_unlock"`
  - `TimedEndUnlock.LOGGER`: SLF4J `Logger` instance.
- Runtime State (Scaffolded / Planned):
  - Configuration & unlock timestamp state loaded on server start.
  - Dimension transition / portal activation checks evaluated against scheduled unlock time.

## Structural Patterns
- Entrypoint Pattern: Single `ModInitializer` class (`TimedEndUnlock`) acting as central bootstrap registry.
- Mixin Interception Pattern: Isolated bytecode hooks in `at.salkin.timedendunlock.mixin` delegating policy checks to core mod logic.
- Resource Templating: Gradle `processResources` expands `${version}`, `${group}`, `${minecraft_version}`, and `${loader_version}` into `quilt.mod.json`.

