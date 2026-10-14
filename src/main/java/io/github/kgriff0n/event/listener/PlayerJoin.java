package io.github.kgriff0n.event.listener;

import io.github.kgriff0n.PlayersInformation;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.event.ServersLinkEvents;
import io.github.kgriff0n.packet.info.NewPlayerPacket;
import io.github.kgriff0n.packet.server.PlayerAcknowledgementPacket;
import io.github.kgriff0n.packet.info.ServersInfoPacket;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.server.ServerInfo;
import io.github.kgriff0n.api.ServersLinkApi;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;

import java.util.UUID;

public class PlayerJoin implements ServerConfigurationConnectionEvents.Configure, ServerPlayerEvents.Join {

    @Override
    public void onSendConfiguration(ServerConfigurationPacketListenerImpl handler, MinecraftServer minecraftServer) {
        // Handle Players not from the Gateway
        if(!ServersLink.isGateway) {
            UUID uuid = handler.getOwner().id();
            if (!ServersLinkApi.getWaitingPlayers().contains(uuid)) {
                if(ServersLink.isGatewayAvailable() && ServersLink.getGatewayGameIp() != null){
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

    @Override
    public void onJoin(ServerPlayer serverPlayer) {
        /* Dummy player packet */
        NewPlayerPacket dummyPlayer = new NewPlayerPacket(serverPlayer.getGameProfile());

        /* Players can only connect from the hub */
        if (ServersLink.isGateway) {
            Gateway gateway = Gateway.getInstance();
            if (gateway.isConnectedPlayer(serverPlayer.getUUID()) && !ServersLinkApi.getPreventConnect().contains(serverPlayer.getUUID())) {
                ServersLinkApi.transferPlayer(serverPlayer, ServersLink.getServerInfo().getName(), ServersLinkApi.whereIs(serverPlayer.getUUID()));
                ServersLinkApi.getPreventConnect().add(serverPlayer.getUUID());
                ServersLinkApi.getPreventDisconnect().add(serverPlayer.getUUID());
            } else {
                String lastServer = PlayersInformation.getLastServer(serverPlayer.getUUID());
                ServerInfo lastServerInfo = ServersLinkApi.getServer(lastServer);

                if (lastServer == null) {
                    ServersLinkEvents.FIRST_JOIN.invoker().onFirstJoin(serverPlayer);
                }

                if (lastServer == null || lastServer.equals(ServersLink.getServerInfo().getName())
                        || lastServerInfo == null || lastServerInfo.isDown() || !gateway.shouldReconnectToLastServer()) {
                    ServersLinkApi.getServer(ServersLink.getServerInfo().getName()).addPlayer(serverPlayer.getGameProfile());
                    /* Delete the fake player */
                    ServersLinkApi.getDummyPlayers().removeIf(dummy -> dummy.getName().equals(serverPlayer.getName()));

                    /* Send player information to other servers */
                    gateway.sendToAllFrom(ServersLink.getServerInfo().getName(), dummyPlayer);
                    gateway.sendToAll(new ServersInfoPacket(ServersLinkApi.getImmutableServerList()));

                    if (gateway.shouldReconnectToLastServer() && lastServer != null && !lastServer.isEmpty() && (lastServerInfo == null || lastServerInfo.isDown())) {
                        serverPlayer.sendSystemMessage(Component.literal("An unexpected error occurred while attempting to reconnect you to your previous server").withStyle(ChatFormatting.RED));
                    }
                } else {
                    ServersLinkApi.transferPlayer(serverPlayer, ServersLink.getServerInfo().getName(), lastServer);
                }
            }
        } else {
            SubServer connection = SubServer.getInstance();
            if (!ServersLinkApi.getWaitingPlayers().contains(serverPlayer.getUUID())) {
                serverPlayer.connection.disconnect(Component.translatable("multiplayer.status.cannot_connect").withStyle(ChatFormatting.RED));
                /* Used to prevent the logout message in ServerPlayNetworkHandlerMixin#preventDisconnectMessage */
                ServersLinkApi.getPreventConnect().add(serverPlayer.connection.player.getUUID());
                ServersLinkApi.getPreventDisconnect().add(serverPlayer.connection.player.getUUID());
            } else {
                /* The player logs in and is removed from the list of waiting players */
                ServersLinkApi.removeWaitingPlayer(serverPlayer.getUUID());
                /* Delete the fake player */
                ServersLinkApi.getDummyPlayers().removeIf(dummy -> dummy.getName().equals(serverPlayer.getName()));
                /* Send player information to other servers */
                connection.send(dummyPlayer);
                connection.send(new PlayerAcknowledgementPacket(ServersLink.getServerInfo().getName(), serverPlayer.getGameProfile()));
            }
        }
    }
}
