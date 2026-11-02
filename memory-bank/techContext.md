# Tech Context & File Routing Map

## Tech Stack
- Language: Java 25 (`JavaLanguageVersion.of(25)`, `options.release.set(25)`, UTF-8)
- Game Target: Minecraft `26.3` (`com.mojang:minecraft:26.3`)
- Mod Loader: Quilt Loader `0.30.2` (`org.quiltmc:quilt-loader`)
- Build System: Gradle Kotlin DSL + Quilt Loom No-Remap `1.15.1` (`org.quiltmc.loom.no_remap`)
- API Framework: Fabric API `0.165.0+26.3` (`net.fabricmc.fabric-api:fabric-api`)
- Bytecode Weaving: SpongePowered Mixin `0.8` (`compatibilityLevel: JAVA_25`)
- Logging: SLF4J (`org.slf4j.Logger`, `org.slf4j.LoggerFactory`)

## File Routing Map

### Build & Configuration
- `README.md` -> Project overview, requirements, build command, license.
- `watch-server.ps1` -> PowerShell dev watcher: monitors `build/libs/timed-end-unlock-1.0.0.jar`, stops/restarts test server (`D:\Minecraft\Test-Environment\server-base`), and deploys rebuilt JAR to `mods/`.
- `build.gradle.kts` -> Gradle build logic, Quilt/Fabric repositories, dependencies, Loom mod/run configuration (`-Dloader.disableEnvironmentStrip=true`), Java 25 toolchain, `quilt.mod.json` property expansion.
- `settings.gradle.kts` -> Plugin management repositories (Quilt, Fabric, Gradle Portal), root project name (`timed-end-unlock`).
- `gradle.properties` -> Mod coordinates (`mod_version=1.0.0`, `maven_group=at.salkin.timedendunlock`, `archives_base_name=timed-end-unlock`), Gradle JVM flags (`-Xmx4G`, `org.gradle.parallel=true`).
- `gradle/libs.versions.toml` -> Central version catalog (`minecraft`, `quilt_loader`, `quilt_loom`, `fabric_api`).

### Mod Manifests & Resources
- `src/main/resources/quilt.mod.json` -> Quilt metadata, `main` entrypoint registration, dependency rules, mixin config link.
- `src/main/resources/timed_end_unlock.mixins.json` -> Mixin package routing (`at.salkin.timedendunlock.mixin`), `JAVA_25` compatibility, injector requirements.

### Java Source Code
- `src/main/java/at/salkin/timedendunlock/TimedEndUnlock.java` -> Primary entrypoint (`ModInitializer`), `MOD_ID`, `LOGGER`, `onInitialize()` (Config.load, loadEntries, `armAnnouncement()`, ModCommands.register, `UseBlockCallback` hook, `ServerTickEvents.END_SERVER_TICK`). Hook: eye-on-frame; bypass via `Player.checkPermission(BYPASS.key(), GAMEMASTERS)`; locked -> `sendLockedMessage` (throttled 3s, `Config.message()`) + `FAIL`. Tick: on lock->open crossing announces title+end-portal-sound to all `ServerPlayer`s. Lock ops -> `evictPlayersFromEnd(server)`: `Level.END` -> `players()`, skip bypass, teleport via `TeleportTransition(target, Vec3, Vec3.ZERO, yaw, pitch, DO_NOTHING)` to entry loc (`ENTRY_LOCATIONS` persisted `config/timed_end_unlock_entries.dat`) or overworld `getRespawnData()` fallback; `recordEntry` called by mixin. MC 26.3: `Player.sendSystemMessage` (no `sendMessage`); `Identifier` (renamed ResourceLocation); `ServerLevel.players()`.
- `src/main/java/at/salkin/timedendunlock/ModCommands.java` -> Fabric `CommandRegistrationCallback`: `/endunlock reload` (`timed_end_unlock:reload`), `/endunlock unlock [datetime]` + `/endunlock lock [datetime]` (`timed_end_unlock:manage`); all require node OR op>=GAMEMASTERS; commands write config via `Config.setUnlockDatetime` then `apply()` = load + armAnnouncement + evict-if-locked. Arg `StringArgumentType.string()` `yyyy-MM-dd HH:mm` (quoted), future-only, `SimpleCommandExceptionType`s. `PermissionNode.of` booleans reload/bypass/manage (fabric-permission-api-v1; LuckPerms bridges, no LP dep).
- `src/main/java/at/salkin/timedendunlock/Config.java` -> Loads `<server>/config/timed_end_unlock.toml` via `QuiltLoader.getConfigDir()`, creates commented default, regex-reads `unlock_datetime` (`yyyy-MM-dd HH:mm` | `never` -> `LocalDateTime.MAX`, server-local), `unlock_message` (`{datetime}`), `lock_message_indefinite` (gate message when indefinite), `datetime_format` (default `dd/MM/yyyy HH:mm`), `title_message`; `setUnlockDatetime(value)` rewrites `unlock_datetime` line in TOML (appends if missing); `isOpen()`, `message()` (indefinite-aware), `format()`, `title()`, fail-open, pattern fallback.
- `src/main/java/at/salkin/timedendunlock/mixin/EndPortalBlockMixin.java` -> `@Inject entityInside` (protected, HEAD, cancellable): blocks ServerPlayer overworld->End portal travel when locked (bypass exempt), throttled message; records entry location when allowed; skips when level dim == END (exit portal always works). Registered in `timed_end_unlock.mixins.json` (no_remap: runtime official names, no refmap).
- `src/main/java/at/salkin/timedendunlock/mixin/` -> Designated package for Mixin classes (currently empty).

