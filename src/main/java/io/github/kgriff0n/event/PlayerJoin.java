package io.github.kgriff0n.event;

import io.github.kgriff0n.PlayersInformation;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.FakePlayerApi;
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

import java.util.UUID;

public class PlayerJoin implements ServerPlayConnectionEvents.Join, ServerConfigurationConnectionEvents.Configure {

    @Override
    public void onPlayReady(ServerGamePacketListenerImpl serverPlayNetworkHandler, PacketSender packetSender, MinecraftServer minecraftServer) {

        ServerPlayer newPlayer = serverPlayNetworkHandler.player;

        if (FakePlayerApi.isFake(minecraftServer, newPlayer)) return;

        /* Dummy player packet */
        NewPlayerPacket dummyPlayer = new NewPlayerPacket(newPlayer.getGameProfile());

        /* Players can only connect from the hub */
        if (ServersLink.isGateway) {
            Gateway gateway = Gateway.getInstance();
            if (gateway.isConnectedPlayer(newPlayer.getUUID()) && !ServersLinkApi.getPreventConnect().contains(newPlayer.getUUID())) {
                ServersLinkApi.transferPlayer(newPlayer, ServersLink.getServerInfo().getName(), ServersLinkApi.whereIs(newPlayer.getUUID()));
                ServersLinkApi.getPreventConnect().add(newPlayer.getUUID());
                ServersLinkApi.getPreventDisconnect().add(newPlayer.getUUID());
            } else {
                String lastServer = PlayersInformation.getLastServer(newPlayer.getUUID());
                ServerInfo lastServerInfo = ServersLinkApi.getServer(lastServer);
                if (lastServer == null || lastServer.equals(ServersLink.getServerInfo().getName())
                        || lastServerInfo == null || lastServerInfo.isDown() || !gateway.shouldReconnectToLastServer()) {
                    ServersLinkApi.getServer(ServersLink.getServerInfo().getName()).addPlayer(newPlayer.getGameProfile());
                    /* Delete the fake player */
                    ServersLinkApi.getDummyPlayers().removeIf(player -> player.getName().equals(newPlayer.getName()));

                    /* Send player information to other servers */
                    gateway.forward(dummyPlayer, ServersLink.getServerInfo().getName());
                    gateway.sendAll(new ServersInfoPacket(ServersLinkApi.getServerList()));

                    if (gateway.shouldReconnectToLastServer() && lastServer != null && !lastServer.isEmpty() && (lastServerInfo == null || lastServerInfo.isDown())) {
                        newPlayer.sendSystemMessage(Component.literal("An unexpected error occurred while attempting to reconnect you to your previous server").withStyle(ChatFormatting.RED));
                    }
                } else {
                    ServersLinkApi.transferPlayer(newPlayer, ServersLink.getServerInfo().getName(), lastServer);
                }
            }
        } else {
            SubServer connection = SubServer.getInstance();
            /* The player logs in and is removed from the list of waiting players */
            connection.removeWaitingPlayer(newPlayer.getUUID());
            /* Delete the fake player */
            ServersLinkApi.getDummyPlayers().removeIf(player -> player.getName().equals(newPlayer.getName()));
            /* Send player information to other servers */
            connection.send(dummyPlayer);
            connection.send(new PlayerAcknowledgementPacket(ServersLink.getServerInfo().getName(), newPlayer.getGameProfile()));
        }

    }

    @Override
    public void onSendConfiguration(ServerConfigurationPacketListenerImpl handler, MinecraftServer minecraftServer) {
        // Handle Players not from the Gateway
        if(!ServersLink.isGateway) {
            UUID uuid = handler.getOwner().id();
            SubServer connection = SubServer.getInstance();
            if (!connection.getWaitingPlayers().contains(uuid)) {
                if(ServersLink.isGatewayAvailable()){
                    // If Gateway is connected (reachable) then transfer player to gateway
                    handler.send(new ClientboundTransferPacket(ServersLink.getGatewayGameIp(), ServersLink.getGatewayGamePort()));
                } else {
                    // Else just disconnect him
                    handler.disconnect(Component.translatable("multiplayer.status.cannot_connect").withStyle(ChatFormatting.RED));
                }

                /* Used to prevent the logout message in ServerPlayNetworkHandlerMixin#preventDisconnectMessage */
                ServersLinkApi.getPreventConnect().add(uuid);
                ServersLinkApi.getPreventDisconnect().add(uuid);
            }
        }
    }
}
