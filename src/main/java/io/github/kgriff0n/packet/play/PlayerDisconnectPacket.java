package io.github.kgriff0n.packet.play;

import io.github.kgriff0n.api.FakePlayerApi;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.info.ServersInfoPacket;
import io.github.kgriff0n.server.Settings;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.api.ServersLinkApi;
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static io.github.kgriff0n.ServersLink.SERVER;

public class PlayerDisconnectPacket implements Packet {

    private final UUID uuid;
    private final String name;

    public PlayerDisconnectPacket(UUID uuid) {
        this.uuid = uuid;
        this.name = Objects.requireNonNull(SERVER.getPlayerManager().getPlayer(uuid)).getName().getString();
    }

    @Override
    public boolean shouldReceive(Settings settings) {
        return settings.isPlayerListSynced();
    }

    @Override
    public void onReceive() {
        boolean removed = FakePlayerApi.deleteFake(SERVER, name);
        System.out.println("PlayerDisconnectPacket: removed fake player: " + removed);

        if(!removed) {
            List<ServerPlayerEntity> playerList = SERVER.getPlayerManager().getPlayerList();
            /* Delete the fake player */
            ServersLinkApi.getDummyPlayers().removeIf(player -> player.getUuid().equals(uuid));

            /* Update player list for all players */
            for (ServerPlayerEntity player : playerList) {
                player.networkHandler.sendPacket(new PlayerRemoveS2CPacket(List.of(uuid)));
            }
        }
    }

    @Override
    public void onGatewayReceive(String sender) {
        boolean removed = FakePlayerApi.deleteFake(SERVER, name);
        System.out.println("PlayerDisconnectPacket: removed fake player: " + removed);

        Packet.super.onGatewayReceive(sender);
        Gateway.getInstance().removePlayer(uuid);
        Gateway.getInstance().sendAll(new ServersInfoPacket(ServersLinkApi.getServerList()));
    }
}
