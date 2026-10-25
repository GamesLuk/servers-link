package io.github.kgriff0n.event.listener;

import io.github.kgriff0n.PlayersInformation;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.FakePlayerApi;
import io.github.kgriff0n.event.ServersLinkEvents;
import io.github.kgriff0n.packet.info.NewPlayerPacket;
import io.github.kgriff0n.packet.server.PlayerAcknowledgementPacket;
import io.github.kgriff0n.packet.info.ServersInfoPacket;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.server.ServerInfo;
import io.github.kgriff0n.api.ServersLinkApi;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class PlayerJoin implements ServerPlayConnectionEvents.Join, ServerConfigurationConnectionEvents.Configure {

    @Override
    public void onPlayReady(ServerGamePacketListenerImpl serverPlayNetworkHandler, @NotNull PacketSender packetSender, @NotNull MinecraftServer minecraftServer) {

        ServerPlayer newPlayer = serverPlayNetworkHandler.player;
        ServersLink.LOGGER.info("[DEBUG] PlayerJoin.onPlayReady triggered for player: {} (UUID: {})", 
            newPlayer.getName().getString(), newPlayer.getUUID());

        if (FakePlayerApi.isFake(minecraftServer, newPlayer)) {
            ServersLink.LOGGER.info("[DEBUG] Player {} is a fake player, skipping join handling", newPlayer.getName().getString());
            return;
        }

        /* Dummy player packet */
        NewPlayerPacket dummyPlayer = new NewPlayerPacket(newPlayer.getGameProfile());
        ServersLink.LOGGER.info("[DEBUG] Created dummy player packet for {}", newPlayer.getName().getString());

        /* Players can only connect from the hub */
        ServersLink.LOGGER.info("[DEBUG] Server type check - isGateway: {}", ServersLink.isGateway);
        if (ServersLink.isGateway) {
            Gateway gateway = Gateway.getInstance();
            ServersLink.LOGGER.info("[DEBUG] GATEWAY - Processing player join for {}", newPlayer.getName().getString());

            boolean isConnected = gateway.isConnectedPlayer(newPlayer.getUUID());
            boolean preventConnect = ServersLinkApi.getPreventConnect().contains(newPlayer.getUUID());
            ServersLink.LOGGER.info("[DEBUG] GATEWAY - Player already connected: {}, PreventConnect: {}", isConnected, preventConnect);

            if (isConnected && !preventConnect) {
                String whereIs = ServersLinkApi.whereIs(newPlayer.getUUID());
                ServersLink.LOGGER.info("[DEBUG] GATEWAY - Player already connected on server: {}, transferring...", whereIs);
                ServersLinkApi.transferPlayer(newPlayer, ServersLink.getServerInfo().getName(), whereIs);
            } else {
                String lastServer = PlayersInformation.getLastServer(newPlayer.getUUID());
                ServersLink.LOGGER.info("[DEBUG] GATEWAY - Last server for player: {}", lastServer);

                ServerInfo lastServerInfo = ServersLinkApi.getServer(lastServer);
                ServersLink.LOGGER.info("[DEBUG] GATEWAY - Last server info: {}", lastServerInfo != null ? lastServerInfo.getName() : "NULL");

                if (lastServer == null) {
                    ServersLink.LOGGER.info("[DEBUG] GATEWAY - First join for player {}", newPlayer.getName().getString());
                    ServersLinkEvents.FIRST_JOIN.invoker().onFirstJoin(newPlayer);
                }

                boolean shouldStayOnGateway = lastServer == null || lastServer.equals(ServersLink.getServerInfo().getName())
                        || lastServerInfo == null || lastServerInfo.isDown() || !gateway.shouldReconnectToLastServer();
                ServersLink.LOGGER.info("[DEBUG] GATEWAY - Should stay on gateway: {} (lastServer==null: {}, isCurrentServer: {}, serverInfo==null: {}, isDown: {}, shouldReconnect: {})", 
                    shouldStayOnGateway, lastServer == null, lastServer != null && lastServer.equals(ServersLink.getServerInfo().getName()), 
                    lastServerInfo == null, lastServerInfo != null && lastServerInfo.isDown(), gateway.shouldReconnectToLastServer());

                if (shouldStayOnGateway) {
                    ServersLink.LOGGER.info("[DEBUG] GATEWAY - Adding player {} to gateway server", newPlayer.getName().getString());
                    ServersLinkApi.getServer(ServersLink.getServerInfo().getName()).addPlayer(newPlayer.getGameProfile());
                    /* Delete the fake player */
                    ServersLink.LOGGER.info("[DEBUG] GATEWAY - Removing dummy player for {}", newPlayer.getName().getString());
                    ServersLinkApi.getDummyPlayers().removeIf(player -> player.getName().equals(newPlayer.getName()));

                    /* Send player information to other servers */
                    ServersLink.LOGGER.info("[DEBUG] GATEWAY - Sending NewPlayerPacket to all servers from {}", ServersLink.getServerInfo().getName());
                    gateway.sendToAllFrom(ServersLink.getServerInfo().getName(), dummyPlayer);
                    ServersLink.LOGGER.info("[DEBUG] GATEWAY - Sending ServersInfoPacket to all servers");
                    gateway.sendToAll(new ServersInfoPacket(ServersLinkApi.getServerList()));

                    if (gateway.shouldReconnectToLastServer() && lastServer != null && !lastServer.isEmpty() && (lastServerInfo == null || lastServerInfo.isDown())) {
                        ServersLink.LOGGER.warn("[DEBUG] GATEWAY - Failed to reconnect player {} to last server {}", newPlayer.getName().getString(), lastServer);
                        newPlayer.sendSystemMessage(Component.literal("An unexpected error occurred while attempting to reconnect you to your previous server").withStyle(ChatFormatting.RED));
                    }
                } else {
                    ServersLink.LOGGER.info("[DEBUG] GATEWAY - Transferring player {} from {} to {}", newPlayer.getName().getString(), ServersLink.getServerInfo().getName(), lastServer);
                    ServersLinkApi.transferPlayer(newPlayer, ServersLink.getServerInfo().getName(), lastServer);
                }
            }
        } else {
            SubServer connection = SubServer.getInstance();
            ServersLink.LOGGER.info("[DEBUG] SUBSERVER - Processing player join for {} on server {}", newPlayer.getName().getString(), ServersLink.getServerInfo().getName());

            /* The player logs in and is removed from the list of waiting players */
            ServersLink.LOGGER.info("[DEBUG] SUBSERVER - Removing {} from waiting players list", newPlayer.getUUID());
            ServersLinkApi.removeWaitingPlayer(newPlayer.getUUID());

            /* Delete the fake player */
            ServersLink.LOGGER.info("[DEBUG] SUBSERVER - Removing dummy player for {}", newPlayer.getName().getString());
            ServersLinkApi.getDummyPlayers().removeIf(player -> player.getName().equals(newPlayer.getName()));

            /* Send player information to other servers */
            ServersLink.LOGGER.info("[DEBUG] SUBSERVER - Sending NewPlayerPacket to gateway for {}", newPlayer.getName().getString());
            connection.send(dummyPlayer);
            if (!FakePlayerApi.isFake(minecraftServer, newPlayer)) {
                ServersLink.LOGGER.info("[DEBUG] SUBSERVER - Sending PlayerAcknowledgementPacket for {}", newPlayer.getName().getString());
                connection.send(new PlayerAcknowledgementPacket(ServersLink.getServerInfo().getName(), newPlayer.getGameProfile()));
            }
        }

    }

    @Override
    public void onSendConfiguration(ServerConfigurationPacketListenerImpl handler, MinecraftServer minecraftServer) {
        // Handle Players not from the Gateway
        ServersLink.LOGGER.info("[DEBUG] onSendConfiguration - isGateway: {}", ServersLink.isGateway);
        if(!ServersLink.isGateway) {
            UUID uuid = handler.getOwner().id();
            ServersLink.LOGGER.info("[DEBUG] onSendConfiguration - Player UUID: {}, isWaiting: {}", uuid, ServersLinkApi.getWaitingPlayers().contains(uuid));

            if (!ServersLinkApi.getWaitingPlayers().contains(uuid)) {
                ServersLink.LOGGER.warn("[DEBUG] onSendConfiguration - Player {} not in waiting list! Gateway available: {}, Gateway IP: {}", 
                    uuid, ServersLink.isGatewayAvailable(), ServersLink.getGatewayGameIp());

                if(ServersLink.isGatewayAvailable() && ServersLink.getGatewayGameIp() != null){
                    // If Gateway is connected (reachable) then transfer player to gateway
                    ServersLink.LOGGER.info("[DEBUG] onSendConfiguration - Transferring player {} to gateway {}:{}", 
                        uuid, ServersLink.getGatewayGameIp(), ServersLink.getGatewayGamePort());
                    handler.send(new ClientboundTransferPacket(ServersLink.getGatewayGameIp(), ServersLink.getGatewayGamePort()));
                } else {
                    // Else just disconnect him
                    ServersLink.LOGGER.warn("[DEBUG] onSendConfiguration - Disconnecting player {} - Gateway not available", uuid);
                    handler.disconnect(Component.translatable("multiplayer.status.cannot_connect").withStyle(ChatFormatting.RED));
                }

                /* Used to prevent the logout message */
                ServersLink.LOGGER.info("[DEBUG] onSendConfiguration - Adding {} to prevent connect/disconnect lists", uuid);
                ServersLinkApi.getPreventConnect().add(uuid);
                ServersLinkApi.getPreventDisconnect().add(uuid);
            }
        }
    }
}
