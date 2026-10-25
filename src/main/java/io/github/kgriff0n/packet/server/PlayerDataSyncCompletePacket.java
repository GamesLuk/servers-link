package io.github.kgriff0n.packet.server;

import io.github.kgriff0n.packet.Packet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class PlayerDataSyncCompletePacket implements Packet {
    private final UUID syncId;
    private final String serverName;
    private final List<UUID> snapshotPlayers;
    private final List<UUID> failedPlayers;
    private final boolean fatalFailure;

    public PlayerDataSyncCompletePacket(
            UUID syncId,
            String serverName,
            Collection<UUID> snapshotPlayers,
            Collection<UUID> failedPlayers
    ) {
        this(syncId, serverName, snapshotPlayers, failedPlayers, false);
    }

    public PlayerDataSyncCompletePacket(
            UUID syncId,
            String serverName,
            Collection<UUID> snapshotPlayers,
            Collection<UUID> failedPlayers,
            boolean fatalFailure
    ) {
        this.syncId = syncId;
        this.serverName = serverName;
        this.snapshotPlayers = new ArrayList<>(snapshotPlayers);
        this.failedPlayers = new ArrayList<>(failedPlayers);
        this.fatalFailure = fatalFailure;
    }

    public UUID getSyncId() {
        return syncId;
    }

    public String getServerName() {
        return serverName;
    }

    public List<UUID> getSnapshotPlayers() {
        return snapshotPlayers;
    }

    public List<UUID> getFailedPlayers() {
        return failedPlayers;
    }

    public boolean hasFatalFailure() {
        return fatalFailure;
    }

    @Override
    public void onReceive() {
        ShutdownPlayerDataSync.acceptCompletion(this);
    }
}