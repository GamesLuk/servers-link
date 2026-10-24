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
                Identifier.fromNamespaceAndPath("serverslink", "current_server_emoji"),
                (ctx, arg) -> {
                    if (!ctx.hasPlayer() || !(ctx.player() instanceof ServerPlayer player)) {
                        return PlaceholderResult.invalid();
                    }
                    String name = ServersLinkApi.whereIs(player.getUUID());
                    return switch (name) {
                        case "Citybuild" -> PlaceholderResult.value("\uD83C\uDFED");
                        case "Farming" -> PlaceholderResult.value("\u26CF");
                        case "Building" -> PlaceholderResult.value("\uD83C\uDFD7");
                        default -> PlaceholderResult.invalid();
                    };
                }
        );

        Placeholders.registerCommon(
                Identifier.fromNamespaceAndPath("serverslink", "current_server_player_count"),
                (ctx, arg) -> {
                    if (!ctx.hasPlayer() || !(ctx.player() instanceof ServerPlayer player)) {
                        return PlaceholderResult.invalid();
                    }

                    String name = ServersLinkApi.whereIs(player.getUUID());

                    for (ServerInfo server : ServersLinkApi.getServerList()) {
                        if (server.getName().equalsIgnoreCase(name)) {
                            return PlaceholderResult.value(Component.literal(String.valueOf(server.getPlayersList().size())));
                        }
                    }
                    return PlaceholderResult.invalid();
                }
        );
    }
}