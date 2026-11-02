package at.salkin.timedendunlock;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents.Chat;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
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

public class TimedEndUnlock implements ModInitializer {
    public static final String MOD_ID = "timed_end_unlock";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Timed End Unlock");
        UseBlockCallback.EVENT.register(TimedEndUnlock::onUseBlock);
    }

    private static InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        if (world.isClientSide() || hitResult.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hitResult.getBlockPos();
        if (player.getItemInHand(hand).is(Items.ENDER_EYE) && world.getBlockState(pos).is(Blocks.END_PORTAL_FRAME)) {
            LOGGER.info("ender_eye used on end_portal_frame at {} by {}", pos.toShortString(),
                    player.getName().getString());
            return InteractionResult.FAIL;
        }
        return InteractionResult.PASS;
    }
}

