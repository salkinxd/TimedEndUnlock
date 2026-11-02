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
- `src/main/java/at/salkin/timedendunlock/TimedEndUnlock.java` -> Primary entrypoint (`ModInitializer`), `MOD_ID`, `LOGGER`, `onInitialize()` (Config.load, ModCommands.register, `UseBlockCallback` hook). Hook: eye-on-frame; bypass via `Player.checkPermission(BYPASS.key(), GAMEMASTERS)`; locked -> red `sendSystemMessage(Config.message())` + `FAIL`. MC 26.3: `Player.sendSystemMessage` (no `sendMessage`).
- `src/main/java/at/salkin/timedendunlock/ModCommands.java` -> Fabric `CommandRegistrationCallback`: `/endunlock reload` (requires `timed_end_unlock:reload` OR vanilla op>=GAMEMASTERS). `PermissionNode.of` booleans `timed_end_unlock:reload` + `timed_end_unlock:bypass` (fabric-permission-api-v1, in fabric-api aggregate; LuckPerms bridges via `PermissionEvents.ON_REQUEST`, no LP compile dep).
- `src/main/java/at/salkin/timedendunlock/Config.java` -> Loads `<server>/config/timed_end_unlock.toml` via `org.quiltmc.loader.api.QuiltLoader.getConfigDir()` (native Quilt API; `FabricLoader` deprecated in quilt-loader compat), creates commented default, regex-reads string keys `unlock_datetime` (`yyyy-MM-dd HH:mm`, server-local), `unlock_message` (`{datetime}` placeholder), `datetime_format` (`DateTimeFormatter` pattern, UK default `dd/MM/yyyy HH:mm`); null-guarded `value()`; `isOpen()`, `message()`, fail-open on error, pattern fallback.
- `src/main/java/at/salkin/timedendunlock/mixin/` -> Designated package for Mixin classes (currently empty).

