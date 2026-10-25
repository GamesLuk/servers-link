package io.github.kgriff0n.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;

public class PrefixCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> {
                    dispatcher.register(
                            Commands.literal("updateprefix")
                                    .requires(source -> source.permissions()
                                            .hasPermission(
                                                    net.minecraft.server.permissions.Permissions.COMMANDS_MODERATOR
                                            ))
                                    .then(Commands.argument(
                                                    "target",
                                                    EntityArgument.player()
                                            )
                                            .executes(PrefixCommand::execute))
                    );
                }
        );
    }

    private static int execute(
            CommandContext<CommandSourceStack> context
    ) throws CommandSyntaxException {

        ServerPlayer targetPlayer =
                EntityArgument.getPlayer(context, "target");

        String clanName = "";
        String clanColor = "§f";
        String nameColor = "§f";

        try {
            Class<?> providerClass = Class.forName(
                    "net.luckperms.api.LuckPermsProvider"
            );

            Object luckPerms = providerClass
                    .getMethod("get")
                    .invoke(null);

            Object userManager = luckPerms.getClass()
                    .getMethod("getUserManager")
                    .invoke(luckPerms);

            Object user = userManager.getClass()
                    .getMethod("getUser", java.util.UUID.class)
                    .invoke(userManager, targetPlayer.getUUID());

            if (user != null) {
                Object cachedData = user.getClass()
                        .getMethod("getCachedData")
                        .invoke(user);

                Object metaData = cachedData.getClass()
                        .getMethod("getMetaData")
                        .invoke(cachedData);

                Method getMetaValue = metaData.getClass()
                        .getMethod("getMetaValue", String.class);

                String tmp1 = (String) getMetaValue.invoke(metaData, "clan-name");
                String tmp2 = (String) getMetaValue.invoke(metaData, "clan-color");
                String tmp3 = (String) getMetaValue.invoke(metaData, "name-color");

                if (tmp1 != null && !tmp1.isBlank()) clanName = tmp1;
                if (tmp2 != null && !tmp2.isBlank()) clanColor = tmp2;
                if (tmp3 != null && !tmp3.isBlank()) nameColor = tmp3;
            }

        } catch (Exception e) {
            context.getSource().sendFailure(
                    Component.literal(
                            "Meta-Tags konnten nicht via Reflection abgefragt werden: "
                                    + e.getClass().getSimpleName()
                    )
            );
            return 0;
        }

        String calculatedPrefix = !clanName.isBlank() ?
                "§8[" + clanColor + clanName + "§8] " + nameColor + targetPlayer.getName().getString() :
                nameColor + targetPlayer.getName().getString();

        MinecraftServer server = context.getSource().getServer();

        CommandSourceStack serverSource = server.createCommandSourceStack();

        server.getCommands().performPrefixedCommand(serverSource, "lp user " + targetPlayer.getGameProfile().name() + " meta setprefix \"" + calculatedPrefix + "\"");

        context.getSource().sendSuccess(
                () -> Component.literal(
                        "Prefix für "
                                + targetPlayer.getGameProfile().name()
                                + " verarbeitet: "
                                + calculatedPrefix
                ),
                false
        );

        return 1;
    }
}