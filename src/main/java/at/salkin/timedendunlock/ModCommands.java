package at.salkin.timedendunlock;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.permission.v1.PermissionNode;
import net.fabricmc.fabric.api.permission.v1.PermissionPredicates;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;

public final class ModCommands {
    public static final PermissionNode<Boolean> RELOAD = PermissionNode.of(TimedEndUnlock.MOD_ID, "reload");
    public static final PermissionNode<Boolean> BYPASS = PermissionNode.of(TimedEndUnlock.MOD_ID, "bypass");

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("endunlock")
                        .requires(PermissionPredicates.require(RELOAD, PermissionLevel.GAMEMASTERS))
                        .then(Commands.literal("reload")
                                .executes(context -> {
                                    Config.load();
                                    context.getSource().sendSuccess(() -> Component.literal("Timed End Unlock config reloaded."), true);
                                    return 1;
                                }))));
    }
}
