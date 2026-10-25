package io.github.kgriff0n.event.listener;

import io.github.kgriff0n.PlayersInformation;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.FakePlayerApi;
import io.github.kgriff0n.api.PlaceholderIntegration;
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
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ClientboundTransferPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.jetbrains.annotations.NotNull;

import java.util.Set;
import java.util.UUID;

public class PlayerJoin implements ServerPlayConnectionEvents.Join, ServerConfigurationConnectionEvents.Configure {

    @Override
    public void onPlayReady(ServerGamePacketListenerImpl serverPlayNetworkHandler, @NotNull PacketSender packetSender, @NotNull MinecraftServer minecraftServer) {

        ServerPlayer newPlayer = serverPlayNetworkHandler.player;

        /* Überprüfe, ob der Spieler zum Spawn teleportiert werden soll */
        if (PlaceholderIntegration.shouldSpawnOnJoin(newPlayer.getUUID())) {
            ServerLevel world = minecraftServer.getLevel(minecraftServer.getRespawnData().dimension());
            BlockPos spawnPos = minecraftServer.getRespawnData().pos();
            float spawnAngle = minecraftServer.getRespawnData().yaw();
            newPlayer.teleportTo(world, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, Set.of(), spawnAngle, 0.0F, false);
        }

        /* Dummy player packet */
        NewPlayerPacket dummyPlayer = new NewPlayerPacket(newPlayer.getGameProfile());

        /* Players can only connect from the hub */
        if (ServersLink.isGateway) {
            Gateway gateway = Gateway.getInstance();
            if (gateway.isConnectedPlayer(newPlayer.getUUID()) && !ServersLinkApi.getPreventConnect().contains(newPlayer.getUUID())) {
                ServersLinkApi.transferPlayer(newPlayer, ServersLink.getServerInfo().getName(), ServersLinkApi.whereIs(newPlayer.getUUID()));
            } else {
                String lastServer = PlayersInformation.getLastServer(newPlayer.getUUID());
                ServerInfo lastServerInfo = ServersLinkApi.getServer(lastServer);

                if (lastServer == null) {
                    ServersLinkEvents.FIRST_JOIN.invoker().onFirstJoin(newPlayer);
                }

                if (lastServer == null || lastServer.equals(ServersLink.getServerInfo().getName())
                        || lastServerInfo == null || lastServerInfo.isDown() || !gateway.shouldReconnectToLastServer()) {
                    ServersLinkApi.getServer(ServersLink.getServerInfo().getName()).addPlayer(newPlayer.getGameProfile());
                    /* Delete the fake player */
                    ServersLinkApi.getDummyPlayers().removeIf(player -> player.getName().equals(newPlayer.getName()));

                    /* Send player information to other servers */
                    gateway.sendToAllFrom(ServersLink.getServerInfo().getName(), dummyPlayer);
                    gateway.sendToAll(new ServersInfoPacket(ServersLinkApi.getServerList()));

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
            ServersLinkApi.removeWaitingPlayer(newPlayer.getUUID());
            /* Delete the fake player */
            ServersLinkApi.getDummyPlayers().removeIf(player -> player.getName().equals(newPlayer.getName()));
            /* Send player information to other servers */
            connection.send(dummyPlayer);
            if (!FakePlayerApi.isFake(minecraftServer, newPlayer)) connection.send(new PlayerAcknowledgementPacket(ServersLink.getServerInfo().getName(), newPlayer.getGameProfile()));
        }

    }

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

                /* Used to prevent the logout message */
                ServersLinkApi.getPreventConnect().add(uuid);
                ServersLinkApi.getPreventDisconnect().add(uuid);
            }
        }
    }
}
