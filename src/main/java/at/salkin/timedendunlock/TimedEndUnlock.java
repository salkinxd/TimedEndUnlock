package at.salkin.timedendunlock;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("deprecation")
public class TimedEndUnlock implements ModInitializer {
    public static final String MOD_ID = "timed_end_unlock";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean announcementPending;

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Timed End Unlock");
        Config.load();
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
                player.sendSystemMessage(Component.literal(Config.message()).withStyle(ChatFormatting.RED));
                LOGGER.info("{} tried to open the End at {}", player.getName().getString(), pos.toShortString());
                return InteractionResult.FAIL;
            }
        }
        return InteractionResult.PASS;
    }
}

