package io.github.kgriff0n.api;

import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.PlaceholderResult;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class PlaceholderIntegration {

    public static void register() {
        Placeholders.registerCommon(
                Identifier.fromNamespaceAndPath("serverslink", "server"),
                (ctx, arg) -> {
                    if (!ctx.hasPlayer() || !(ctx.player() instanceof ServerPlayer player)) {
                        return PlaceholderResult.invalid();
                    }
                    return PlaceholderResult.value(Component.literal(ServersLinkApi.whereIs(player.getUUID())));
                }
        );
    }
}