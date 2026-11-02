package at.salkin.timedendunlock.mixin;

import at.salkin.timedendunlock.Config;
import at.salkin.timedendunlock.ModCommands;
import at.salkin.timedendunlock.TimedEndUnlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EndPortalBlock.class)
public abstract class EndPortalBlockMixin {
    private static final int FIRE_RESISTANCE_TICKS = 60 * 20;

    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void timedEndUnlock$gateEndEntry(BlockState state, Level level, BlockPos pos, Entity entity,
                                             InsideBlockEffectApplier applier, boolean condition, CallbackInfo ci) {
        if (level.isClientSide() || !(entity instanceof ServerPlayer player)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level;
        if (serverLevel.dimension().equals(Level.END)) {
            return;
        }
        if (!Config.isOpen() && !player.checkPermission(ModCommands.BYPASS.key(), PermissionLevel.GAMEMASTERS)) {
            TimedEndUnlock.sendLockedMessage(player);
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, FIRE_RESISTANCE_TICKS));
            ci.cancel();
        } else {
            TimedEndUnlock.recordEntry(player, serverLevel);
        }
    }
}
