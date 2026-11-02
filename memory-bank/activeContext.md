# Active Context

## Purpose
- Server/common Minecraft mod (`timed_end_unlock`) that controls and schedules player access to The End dimension.

## Current State & Immediate Focus
- Status: All planned features implemented and compiling; live-server runtime testing pending.
- Completed:
  - Gradle Kotlin DSL build with `org.quiltmc.loom.no_remap` (`1.15.1`) and Java 25 toolchain.
  - Quilt mod manifest (`quilt.mod.json`) and Mixin descriptor (`timed_end_unlock.mixins.json`).
  - Main entrypoint `TimedEndUnlock` implementing Fabric `ModInitializer`.
  - End gating v1: `UseBlockCallback` blocks `ENDER_EYE` on `END_PORTAL_FRAME` until TOML `unlock_datetime` (`Config.java`, `config/timed_end_unlock.toml`, `unlock_message` + `datetime_format` placeholders); MC 26.3 `Player.sendSystemMessage`.
  - `/endunlock reload` command + `timed_end_unlock:reload`/`timed_end_unlock:bypass` permission nodes via fabric-permission-api (op fallback GAMEMASTERS, LuckPerms bridge automatic).
  - Unlock announcement: `ServerTickEvents.END_SERVER_TICK` watches `Config.isOpen()`; on crossing (only if unlock was future at load; `armAnnouncement()` re-armed on reload/commands) sends per-`ServerPlayer` packets: `ClientboundSetTitlesAnimationPacket(10,70,20)` + `ClientboundSetTitleTextPacket(Component.literal(Config.title()))` + `ClientboundSoundPacket(SoundEvents.END_PORTAL_SPAWN, MASTER, at player pos)`; `title_message` config key; `ServerPlayer.connection.send` public; sound needs `Holder<SoundEvent>` via `BuiltInRegistries.SOUND_EVENT.wrapAsHolder`.
  - Runtime lock/unlock commands (all write `unlock_datetime` back to TOML then `Config.load()`): `/endunlock unlock [datetime]` (no arg = open now), `/endunlock lock [datetime]` (no arg = `"never"` = indefinite, `unlock_datetime = "never"` parsed as `LocalDateTime.MAX`), gated by `timed_end_unlock:manage` node (op fallback). Datetime arg `yyyy-MM-dd HH:mm` quoted, must be future; suggestion provided. `lock_message_indefinite` config key -> gate message when indefinite (eye hook + portal). `MC 26.3: ResourceLocation renamed net.minecraft.resources.Identifier; ServerLevel.players(); LevelData.RespawnData` fallback.
  - Lock enforcement: `EndPortalBlockMixin.entityInside` (HEAD, cancellable): overworld-side portal entry blocked when locked (message throttled 3s via GATE_NOTICES), bypass/op allowed; End-side exit portal never blocked; allowed entries record origin to `config/timed_end_unlock_entries.dat` (uuid dim x y z yaw pitch). Locking (lock cmd / reload-to-locked) evicts all non-bypass players in `Level.END` back to recorded entry (fallback overworld respawn data) via `player.teleport(new TeleportTransition(level, Vec3, Vec3.ZERO, yaw, pitch, DO_NOTHING))`.
  - Locked portal attempt: `EndPortalBlockMixin` gate branch grants `MobEffects.FIRE_RESISTANCE` (`new MobEffectInstance(Holder,int)` 60*20 ticks, refreshed each tick in portal) so blocked players falling into stronghold lava pool survive.
  - Bare `/endunlock` (no args, no permission gate, everyone): reports status — open ("already unlocked and open", GREEN) / indefinite (RED) / pending "will unlock on {formatted}" (YELLOW); `Config.isIndefinite()` + `Config.unlockAt()` public accessors.
- Immediate Development Focus:
  - Live-server smoke test: mixin gate + fire resistance over lava pool, eviction, command write-back round-trip, title/sound announcement.

## Memory Bank Index
- `memory-bank/architecture.md`:
  - Initialization lifecycle (`quilt.mod.json` -> `TimedEndUnlock.onInitialize()`).
  - Mixin injection pipeline (`timed_end_unlock.mixins.json` -> `at.salkin.timedendunlock.mixin.*`).
  - Planned state management and data flow for scheduled End dimension gating.
- `memory-bank/techContext.md`:
  - Exact versions and dependencies (`gradle/libs.versions.toml`, `gradle.properties`).
  - Complete file routing map linking build scripts, manifests, and Java sources to relative paths.

