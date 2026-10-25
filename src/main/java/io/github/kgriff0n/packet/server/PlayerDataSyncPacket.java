package io.github.kgriff0n.packet.server;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.FakePlayerApi;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.socket.SubServer;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.github.kgriff0n.ServersLink.SERVER;

public class PlayerDataSyncPacket implements Packet {
    private final UUID syncId;

    public PlayerDataSyncPacket(UUID syncId) {
        this.syncId = syncId;
    }

    @Override
    public void onReceive() {
        String serverName = ServersLinkApi.getServerName();
        SubServer connection = SubServer.getInstance();

        if (connection == null) {
            ServersLink.LOGGER.error(
                    "[ShutdownSync] Cannot snapshot on {}: "
                            + "SubServer connection is missing (syncId={})",
                    serverName,
                    syncId
            );
            return;
        }

        List<ServerPlayer> players = new ArrayList<>();

        for (ServerPlayer player : SERVER.getPlayerList().getPlayers()) {
            if (!FakePlayerApi.isFake(SERVER, player)) {
                players.add(player);
            }
        }

        try {
            // Persist current in-memory player state first.
            SERVER.getPlayerList().saveAll();
        } catch (RuntimeException e) {
            List<UUID> failed = players.stream()
                    .map(ServerPlayer::getUUID)
                    .toList();

            ServersLink.LOGGER.error(
                    "[ShutdownSync] saveAll failed on {} (syncId={})",
                    serverName,
                    syncId,
                    e
            );

            connection.send(new PlayerDataSyncCompletePacket(
                    syncId,
                    serverName,
                    List.of(),
                    failed,
                    true
            ));
            return;
        }

        List<UUID> snapshotPlayers = new ArrayList<>();
        List<UUID> failedPlayers = new ArrayList<>();

        for (ServerPlayer player : players) {
            UUID uuid = player.getUUID();

            try {
                PlayerDataPacket snapshot =
                        new PlayerDataPacket(uuid, syncId, serverName);

                connection.send(snapshot);
                snapshotPlayers.add(uuid);

            } catch (IOException | RuntimeException e) {
                failedPlayers.add(uuid);

                ServersLink.LOGGER.error(
                        "[ShutdownSync] Could not create/queue snapshot "
                                + "for {} ({}) on {}",
                        player.getName().getString(),
                        uuid,
                        serverName,
                        e
                );
            }
        }

        // Queue the completion after the snapshot packets on the same connection.
        connection.send(new PlayerDataSyncCompletePacket(
                syncId,
                serverName,
                snapshotPlayers,
                failedPlayers
        ));

        ServersLink.LOGGER.info(
                "[ShutdownSync] Queued snapshot batch on {}: "
                        + "syncId={}, snapshots={}, failures={}",
                serverName,
                syncId,
                snapshotPlayers.size(),
                failedPlayers.size()
        );
    }
}