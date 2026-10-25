package io.github.kgriff0n.api;

import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.PlaceholderResult;
import io.github.kgriff0n.server.ServerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

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
                        default -> PlaceholderResult.value("?");
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

    /**
     * Überprüft per Reflection, ob ein Spieler den LuckPerms Meta-Tag "spawn-on-join" auf "true" gesetzt hat.
     * Benötigt keine direkten Imports oder festen Compile-Dependencies auf LuckPerms.
     *
     * @param uuid Die UUID des Spielers
     * @return true wenn der Meta-Tag auf "true" gesetzt ist, sonst false
     */
    public static boolean shouldSpawnOnJoin(UUID uuid) {
        try {
            // Entspricht: LuckPerms luckPerms = LuckPermsProvider.get();
            Class<?> providerClass = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object luckPerms = providerClass.getMethod("get").invoke(null);
            if (luckPerms == null) return false;

            // Entspricht: userManager = luckPerms.getUserManager();
            Object userManager = luckPerms.getClass().getMethod("getUserManager").invoke(luckPerms);
            if (userManager == null) return false;

            // Entspricht: User user = userManager.getUser(uuid);
            Object user = userManager.getClass().getMethod("getUser", UUID.class).invoke(userManager, uuid);
            if (user == null) {
                return false;
            }

            // Entspricht: user.getCachedData()
            Object cachedData = user.getClass().getMethod("getCachedData").invoke(user);
            if (cachedData == null) return false;

            // Entspricht: cachedData.getMetaData()
            Object metaData = cachedData.getClass().getMethod("getMetaData").invoke(cachedData);
            if (metaData == null) return false;

            // Entspricht: metaData.getMetaValue("spawn-on-join")
            Object metaValueObj = metaData.getClass().getMethod("getMetaValue", String.class).invoke(metaData, "spawn-on-join");

            if (metaValueObj instanceof String metaValue) {
                return "true".equalsIgnoreCase(metaValue);
            }

            return false;
        } catch (Exception e) {
            // Greift, wenn LuckPerms nicht installiert ist oder Klassen/Methoden fehlen
            return false;
        }
    }
}