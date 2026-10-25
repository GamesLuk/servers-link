package io.github.kgriff0n.packet.server;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.server.PlayerDataSyncCompletePacket;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;

public final class ShutdownPlayerDataSync {
    private static UUID activeSyncId;

    private static final Set<String> pendingServers = new TreeSet<>();
    private static final Set<String> failedServers = new TreeSet<>();

    private static final Map<String, String> failureReasons =
            new TreeMap<>();

    private static final Map<String, Set<UUID>> writtenPlayers =
            new HashMap<>();

    private static final Map<String, Set<UUID>> failedWrites =
            new HashMap<>();

    private ShutdownPlayerDataSync() {
    }

    public static synchronized void begin(
            UUID syncId,
            Collection<String> expectedServers,
            Map<String, String> preflightFailures
    ) {
        activeSyncId = syncId;

        pendingServers.clear();
        pendingServers.addAll(expectedServers);

        failedServers.clear();
        failureReasons.clear();
        writtenPlayers.clear();
        failedWrites.clear();

        preflightFailures.forEach((name, reason) -> {
            failedServers.add(name);
            failureReasons.put(name, reason);
        });

        ServersLink.LOGGER.info(
                "[ShutdownSync] Started syncId={} expectedServers={} "
                        + "preflightFailures={}",
                syncId,
                new TreeSet<>(pendingServers),
                new TreeMap<>(failureReasons)
        );
    }

    public static synchronized void recordSnapshotWrite(
            UUID syncId,
            String sourceServer,
            UUID playerUuid,
            boolean success
    ) {
        if (!ServersLink.isGateway
                || activeSyncId == null
                || !activeSyncId.equals(syncId)) {
            return;
        }

        if (!pendingServers.contains(sourceServer)) {
            return;
        }

        if (success) {
            writtenPlayers
                    .computeIfAbsent(sourceServer, ignored -> new HashSet<>())
                    .add(playerUuid);
        } else {
            failedWrites
                    .computeIfAbsent(sourceServer, ignored -> new HashSet<>())
                    .add(playerUuid);
        }
    }

    public static synchronized void markServerFailed(
            UUID syncId,
            String serverName,
            String reason
    ) {
        if (activeSyncId == null
                || !activeSyncId.equals(syncId)) {
            return;
        }

        pendingServers.remove(serverName);
        failedServers.add(serverName);
        failureReasons.put(serverName, reason);

        ServersLink.LOGGER.error(
                "[ShutdownSync] Server {} failed snapshot synchronization: {}",
                serverName,
                reason
        );
    }

    public static synchronized void acceptCompletion(
            PlayerDataSyncCompletePacket packet
    ) {
        if (!ServersLink.isGateway) {
            return;
        }

        if (activeSyncId == null
                || !activeSyncId.equals(packet.getSyncId())) {
            ServersLink.LOGGER.warn(
                    "[ShutdownSync] Ignoring stale completion from {} "
                            + "with syncId={} (active={})",
                    packet.getServerName(),
                    packet.getSyncId(),
                    activeSyncId
            );
            return;
        }

        String serverName = packet.getServerName();

        if (!pendingServers.contains(serverName)) {
            ServersLink.LOGGER.warn(
                    "[ShutdownSync] Unexpected or duplicate completion "
                            + "from {} for syncId={}",
                    serverName,
                    packet.getSyncId()
            );
            return;
        }

        Set<UUID> expected =
                new HashSet<>(packet.getSnapshotPlayers());

        Set<UUID> reportedFailures =
                new HashSet<>(packet.getFailedPlayers());

        Set<UUID> actualWrites =
                new HashSet<>(
                        writtenPlayers.getOrDefault(serverName, Set.of())
                );

        Set<UUID> writeFailures =
                new HashSet<>(
                        failedWrites.getOrDefault(serverName, Set.of())
                );

        boolean success =
                !packet.hasFatalFailure()
                        && reportedFailures.isEmpty()
                        && writeFailures.isEmpty()
                        && expected.equals(actualWrites);

        pendingServers.remove(serverName);

        if (success) {
            ServersLink.LOGGER.info(
                    "[ShutdownSync] Confirmed server {}: "
                            + "all {} player snapshots were written (syncId={})",
                    serverName,
                    expected.size(),
                    activeSyncId
            );
        } else {
            String reason =
                    "expected=" + expected.size()
                            + ", written=" + actualWrites.size()
                            + ", fatalFailure=" + packet.hasFatalFailure()
                            + ", reportedSnapshotFailures=" + reportedFailures
                            + ", writeFailures=" + writeFailures
                            + ", missingWrites=" + difference(expected, actualWrites)
                            + ", unexpectedWrites=" + difference(actualWrites, expected);

            failedServers.add(serverName);
            failureReasons.put(serverName, reason);

            ServersLink.LOGGER.error(
                    "[ShutdownSync] Snapshot verification FAILED "
                            + "for server {} (syncId={}): {}",
                    serverName,
                    activeSyncId,
                    reason
            );
        }
    }

    private static Set<UUID> difference(
            Set<UUID> left,
            Set<UUID> right
    ) {
        Set<UUID> result = new HashSet<>(left);
        result.removeAll(right);
        return result;
    }

    public static synchronized boolean isResolved() {
        return pendingServers.isEmpty();
    }

    public static synchronized boolean isSuccessful() {
        return pendingServers.isEmpty()
                && failedServers.isEmpty();
    }

    public static synchronized String describeState() {
        return "syncId=" + activeSyncId
                + ", pendingServers=" + new TreeSet<>(pendingServers)
                + ", failedServers=" + new TreeMap<>(failureReasons);
    }
}