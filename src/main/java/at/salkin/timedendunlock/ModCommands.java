package at.salkin.timedendunlock;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.permission.v1.PermissionNode;
import net.fabricmc.fabric.api.permission.v1.PermissionPredicates;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.PermissionLevel;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ModCommands {
    public static final PermissionNode<Boolean> RELOAD = PermissionNode.of(TimedEndUnlock.MOD_ID, "reload");
    public static final PermissionNode<Boolean> BYPASS = PermissionNode.of(TimedEndUnlock.MOD_ID, "bypass");
    public static final PermissionNode<Boolean> MANAGE = PermissionNode.of(TimedEndUnlock.MOD_ID, "manage");

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final SimpleCommandExceptionType NOT_A_DATETIME =
            new SimpleCommandExceptionType(Component.literal("Expected \"yyyy-MM-dd HH:mm\", for example \"2026-12-31 20:00\"."));
    private static final SimpleCommandExceptionType NOT_IN_FUTURE =
            new SimpleCommandExceptionType(Component.literal("The date and time must be in the future."));

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("endunlock")
                        .executes(context -> {
                            CommandSourceStack source = context.getSource();
                            if (Config.isOpen()) {
                                source.sendSuccess(() -> Component.literal("The End is already unlocked and open.").withStyle(ChatFormatting.GREEN), false);
                            } else if (Config.isIndefinite()) {
                                source.sendSuccess(() -> Component.literal("The End is locked indefinitely. An administrator will announce when it reopens.").withStyle(ChatFormatting.RED), false);
                            } else {
                                source.sendSuccess(() -> Component.literal("The End will unlock on " + Config.format(Config.unlockAt()) + ".").withStyle(ChatFormatting.YELLOW), false);
                            }
                            return 1;
                        })
                        .then(Commands.literal("reload")
                                .requires(PermissionPredicates.require(RELOAD, PermissionLevel.GAMEMASTERS))
                                .executes(context -> {
                                    Config.load();
                                    TimedEndUnlock.armAnnouncement();
                                    if (!Config.isOpen()) {
                                        TimedEndUnlock.evictPlayersFromEnd(context.getSource().getServer());
                                    }
                                    context.getSource().sendSuccess(() -> Component.literal("Timed End Unlock config reloaded."), true);
                                    return 1;
                                }))
                        .then(Commands.literal("unlock")
                                .requires(PermissionPredicates.require(MANAGE, PermissionLevel.GAMEMASTERS))
                                .executes(context -> {
                                    Config.setUnlockDatetime(LocalDateTime.now().format(DATETIME));
                                    apply(context, Component.literal("The End is now open."));
                                    return 1;
                                })
                                .then(Commands.argument("datetime", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            builder.suggest(LocalDateTime.now().plusDays(7).format(DATETIME));
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            LocalDateTime when = parseDatetime(context, "datetime");
                                            Config.setUnlockDatetime(when.format(DATETIME));
                                            apply(context, Component.literal("The End will open on " + Config.format(when) + "."));
                                            return 1;
                                        })))
                        .then(Commands.literal("lock")
                                .requires(PermissionPredicates.require(MANAGE, PermissionLevel.GAMEMASTERS))
                                .executes(context -> {
                                    Config.setUnlockDatetime(Config.NEVER);
                                    apply(context, Component.literal("The End is now locked indefinitely."));
                                    return 1;
                                })
                                .then(Commands.argument("datetime", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            builder.suggest(LocalDateTime.now().plusDays(7).format(DATETIME));
                                            return builder.buildFuture();
                                        })
                                        .executes(context -> {
                                            LocalDateTime when = parseDatetime(context, "datetime");
                                            Config.setUnlockDatetime(when.format(DATETIME));
                                            apply(context, Component.literal("The End will be locked until " + Config.format(when) + "."));
                                            return 1;
                                        })))));
    }

    private static void apply(CommandContext<CommandSourceStack> context, Component success) {
        Config.load();
        TimedEndUnlock.armAnnouncement();
        if (!Config.isOpen()) {
            TimedEndUnlock.evictPlayersFromEnd(context.getSource().getServer());
        }
        context.getSource().sendSuccess(() -> success, true);
    }

    private static LocalDateTime parseDatetime(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
        String raw = StringArgumentType.getString(context, name);
        LocalDateTime when;
        try {
            when = LocalDateTime.parse(raw.replace(' ', 'T'), DATETIME);
        } catch (DateTimeException e) {
            throw NOT_A_DATETIME.create();
        }
        if (!when.isAfter(LocalDateTime.now())) {
            throw NOT_IN_FUTURE.create();
        }
        return when;
    }
}
