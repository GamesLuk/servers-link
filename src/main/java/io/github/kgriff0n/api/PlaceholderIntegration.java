package io.github.kgriff0n.api;

import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.PlaceholderResult;
import io.github.kgriff0n.server.ServerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class PlaceholderIntegration {

    public static void register() {
        Placeholders.registerCommon(
                Identifier.fromNamespaceAndPath("serverslink", "current_server"),
                (ctx, arg) -> {
                    if (!ctx.hasPlayer() || !(ctx.player() instanceof ServerPlayer player)) {
                        return PlaceholderResult.invalid();
                    }
                    return PlaceholderResult.value(Component.literal(ServersLinkApi.whereIs(player.getUUID())));
                }
        );

        Placeholders.registerCommon(
                Identifier.fromNamespaceAndPath("serverslink", "player_count"),
                (ctx, arg) -> {
                    if (arg == null) {
                        return PlaceholderResult.invalid();
                    }

                    for (ServerInfo server : ServersLinkApi.getServerList()) {
                        if (server.getName().equalsIgnoreCase(arg)) {
                            return PlaceholderResult.value(Component.literal(String.valueOf(server.getPlayersList().size())));
                        }
                    }
                    return PlaceholderResult.invalid();
                }
        );
    }
}