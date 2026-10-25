package io.github.kgriff0n.event.listener;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.play.PlayerDisconnectPacket;
import io.github.kgriff0n.packet.info.ServersInfoPacket;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.api.ServersLinkApi;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.util.Objects;
import java.util.UUID;

public class PlayerDisconnect implements ServerPlayConnectionEvents.Disconnect {
    @Override
    public void onPlayDisconnect(ServerGamePacketListenerImpl serverPlayNetworkHandler, MinecraftServer minecraftServer) {
        ServerPlayer player = serverPlayNetworkHandler.player;
        UUID uuid = player.getUUID();
        String name = player.getName().getString();
        ServersLink.LOGGER.info("[DEBUG] PlayerDisconnect triggered for player {} (UUID: {})", name, uuid);

        PlayerDisconnectPacket packet = new PlayerDisconnectPacket(uuid, name);

        // Remove player from list
        ServersLink.LOGGER.info("[DEBUG] Removing player {} from server {}", name, ServersLink.getServerInfo().getName());
        Objects.requireNonNull(ServersLinkApi.getServer(ServersLink.getServerInfo().getName())).removePlayer(uuid);

        if (ServersLink.isGateway) {
            Gateway gateway = Gateway.getInstance();
            boolean preventDisconnect = ServersLinkApi.getPreventDisconnect().contains(uuid);
            ServersLink.LOGGER.info("[DEBUG] GATEWAY - Player disconnect - PreventDisconnect: {}", preventDisconnect);

            /* Delete player from list and send packet ONLY if the player is not transferred */
            if (!preventDisconnect) {
                ServersLink.LOGGER.info("[DEBUG] GATEWAY - Sending PlayerDisconnectPacket to all servers for {}", name);
                gateway.sendToAll(packet);
                ServersLink.LOGGER.info("[DEBUG] GATEWAY - Sending updated ServersInfoPacket");
                gateway.sendToAll(new ServersInfoPacket(ServersLinkApi.getServerList()));
            } else {
                ServersLink.LOGGER.info("[DEBUG] GATEWAY - Skipping disconnect broadcast (player is being transferred)");
            }
        } else {
            SubServer connection = SubServer.getInstance();
            ServersLink.LOGGER.info("[DEBUG] SUBSERVER - Sending PlayerDisconnectPacket to gateway for {}", name);
            connection.send(packet);
        }
    }
}
