package at.salkin.timedendunlock;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.quiltmc.loader.api.QuiltLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class TimedEndUnlock implements ModInitializer {
    public static final String MOD_ID = "timed_end_unlock";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private record EntryLocation(String dimension, double x, double y, double z, float yaw, float pitch) {
    }

    private static final Map<UUID, EntryLocation> ENTRY_LOCATIONS = new HashMap<>();
    private static final Map<UUID, Long> GATE_NOTICES = new HashMap<>();
    private static boolean announcementPending;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Timed End Unlock");
        Config.load();
        loadEntries();
        armAnnouncement();
        ModCommands.register();
        UseBlockCallback.EVENT.register(TimedEndUnlock::onUseBlock);
        ServerTickEvents.END_SERVER_TICK.register(TimedEndUnlock::onEndTick);
    }

    public static void armAnnouncement() {
        announcementPending = !Config.isOpen();
    }

    private static void onEndTick(MinecraftServer server) {
        if (announcementPending && Config.isOpen()) {
            announcementPending = false;
            Component title = Component.literal(Config.title());
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                player.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
                player.connection.send(new ClientboundSetTitleTextPacket(title));
                player.connection.send(new ClientboundSoundPacket(
                        BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.END_PORTAL_SPAWN),
                        SoundSource.MASTER,
                        player.getX(), player.getY(), player.getZ(),
                        1.0F, 1.0F,
                        player.level().getRandom().nextLong()));
            }
            LOGGER.info("The End has been opened, announced to {} player(s)", server.getPlayerCount());
        }
    }

    public static void recordEntry(ServerPlayer player, ServerLevel from) {
        ENTRY_LOCATIONS.put(player.getUUID(), new EntryLocation(
                from.dimension().identifier().toString(),
                player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot()));
        saveEntries();
    }

    public static void evictPlayersFromEnd(MinecraftServer server) {
        ServerLevel end = server.getLevel(Level.END);
        if (end == null) {
            return;
        }
        for (ServerPlayer player : List.copyOf(end.players())) {
            if (player.isRemoved()) {
                continue;
            }
            if (player.checkPermission(ModCommands.BYPASS.key(), PermissionLevel.GAMEMASTERS)) {
                continue;
            }
            EntryLocation entry = ENTRY_LOCATIONS.remove(player.getUUID());
            ServerLevel target = null;
            double x;
            double y;
            double z;
            float yaw = 0.0F;
            float pitch = 0.0F;
            if (entry != null) {
                target = server.getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(entry.dimension())));
                if (target != null) {
                    x = entry.x();
                    y = entry.y();
                    z = entry.z();
                    yaw = entry.yaw();
                    pitch = entry.pitch();
                } else {
                    x = y = z = 0;
                }
            } else {
                target = server.getLevel(Level.OVERWORLD);
                LevelData.RespawnData spawn = target.getRespawnData();
                x = spawn.pos().getX() + 0.5;
                y = spawn.pos().getY();
                z = spawn.pos().getZ() + 0.5;
                yaw = spawn.yaw();
                pitch = spawn.pitch();
            }
            if (target == null) {
                continue;
            }
            player.teleport(new TeleportTransition(target, new Vec3(x, y, z), Vec3.ZERO, yaw, pitch, TeleportTransition.DO_NOTHING));
            player.sendSystemMessage(Component.literal("The End has been locked; you were returned to " + target.dimension().identifier() + ".").withStyle(ChatFormatting.RED));
            LOGGER.info("Returned {} from The End to {}", player.getName().getString(), target.dimension().identifier());
        }
        saveEntries();
    }

    public static void sendLockedMessage(Player player) {
        long now = System.currentTimeMillis();
        Long last = GATE_NOTICES.put(player.getUUID(), now);
        if (last != null && now - last < 3000) {
            return;
        }
        player.sendSystemMessage(Component.literal(Config.message()).withStyle(ChatFormatting.RED));
    }

    private static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        if (world.isClientSide() || hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hitResult.getBlockPos();
        if (player.getItemInHand(hand).is(Items.ENDER_EYE) && world.getBlockState(pos).is(Blocks.END_PORTAL_FRAME)) {
            if (player.checkPermission(ModCommands.BYPASS.key(), PermissionLevel.GAMEMASTERS)) {
                return InteractionResult.PASS;
            }
            if (!Config.isOpen()) {
                sendLockedMessage(player);
                LOGGER.info("{} tried to open the End at {}", player.getName().getString(), pos.toShortString());
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }

    private static void loadEntries() {
        try {
            Path file = entriesFile();
            if (!Files.exists(file)) {
                return;
            }
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length != 7) {
                    continue;
                }
                ENTRY_LOCATIONS.put(UUID.fromString(parts[0]), new EntryLocation(
                        parts[1], Double.parseDouble(parts[2]), Double.parseDouble(parts[3]), Double.parseDouble(parts[4]),
                        Float.parseFloat(parts[5]), Float.parseFloat(parts[6])));
            }
        } catch (Exception e) {
            TimedEndUnlock.LOGGER.error("Failed to load entry locations", e);
        }
    }

    private static void saveEntries() {
        try {
            Path file = entriesFile();
            Files.createDirectories(file.getParent());
            StringBuilder builder = new StringBuilder();
            for (Map.Entry<UUID, EntryLocation> entry : ENTRY_LOCATIONS.entrySet()) {
                EntryLocation location = entry.getValue();
                builder.append(entry.getKey()).append(' ')
                        .append(location.dimension()).append(' ')
                        .append(location.x()).append(' ').append(location.y()).append(' ').append(location.z()).append(' ')
                        .append(location.yaw()).append(' ').append(location.pitch()).append('\n');
            }
            Files.writeString(file, builder.toString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            TimedEndUnlock.LOGGER.error("Failed to save entry locations", e);
        }
    }

    private static Path entriesFile() {
        return QuiltLoader.getConfigDir().resolve("timed_end_unlock_entries.dat");
    }
}
