package io.github.kgriff0n.event.listener;

import io.github.kgriff0n.PlayersInformation;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.mixin.accessor.MinecraftServerAccessor;
import io.github.kgriff0n.packet.info.ServerStatusPacket;
import io.github.kgriff0n.packet.server.PlayerDataSyncPacket;
import io.github.kgriff0n.packet.server.ServerStopPacket;
import io.github.kgriff0n.packet.server.ShutdownPlayerDataSync;
import io.github.kgriff0n.server.ServerInfo;
import io.github.kgriff0n.server.Settings;
import io.github.kgriff0n.socket.G2SConnection;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.api.ServersLinkApi;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BooleanSupplier;

import static io.github.kgriff0n.ServersLink.*;

public class ServerStopping implements ServerLifecycleEvents.ServerStopping {

    private static final long SYNC_WAIT_MS = 2000;
    private static final long PLAYER_DATA_SYNC_TIMEOUT_MS = 30_000;
    private static final long SHUTDOWN_TIMEOUT_MS = 10_000;

    @Override
    public void onServerStopping(@NotNull MinecraftServer server) {
        if (CONFIG_ERROR) {
            return;
        }

        if (!isGateway) {
            SubServer subServer = SubServer.getInstance();
            if (subServer != null) {
                subServer.send(new ServerStatusPacket(
                        ServersLink.getServerInfo().getName(),
                        0.0f,
                        true
                ));
            }
            return;
        }

        Gateway gateway = Gateway.getInstance();

        if (gateway == null) {
            LOGGER.error(
                    "[ShutdownSync] Gateway instance is unavailable; "
                            + "no subserver stop packet will be sent."
            );
            IS_RUNNING = false;
            PlayersInformation.saveNbt(server);
            return;
        }

        UUID syncId = UUID.randomUUID();
        String gatewayName = ServersLinkApi.getServerName();
        String gatewayGroup = ServersLink.getServerInfo().getGroupId();

        Map<String, G2SConnection> targets = new LinkedHashMap<>();
        Set<String> expectedServers = new TreeSet<>();
        Map<String, String> preflightFailures = new TreeMap<>();

        // Determine which connected subservers must participate.
        for (Map.Entry<ServerInfo, G2SConnection> entry
                : ServersLinkApi.getServerMap().entrySet()) {

            ServerInfo target = entry.getKey();
            G2SConnection connection = entry.getValue();

            if (target == null
                    || target.getName().equals(gatewayName)
                    || connection == null
                    || !connection.isConnected()) {
                continue;
            }

            try {
                Settings settings = gateway.getSettings(
                        gatewayGroup,
                        target.getGroupId()
                );

                if (!settings.isPlayerDataSynced()) {
                    LOGGER.info(
                            "[ShutdownSync] Skipping {}: "
                                    + "effective player_data setting is disabled",
                            target.getName()
                    );
                    continue;
                }

                expectedServers.add(target.getName());
                targets.put(target.getName(), connection);

            } catch (RuntimeException e) {
                String reason =
                        "Unable to resolve effective player_data settings "
                                + "for group " + target.getGroupId();

                preflightFailures.put(target.getName(), reason);

                LOGGER.error(
                        "[ShutdownSync] {}; refusing automatic subserver shutdown",
                        reason,
                        e
                );
            }
        }

        ShutdownPlayerDataSync.begin(
                syncId,
                expectedServers,
                preflightFailures
        );

        // Request a snapshot from every required subserver.
        for (Map.Entry<String, G2SConnection> entry : targets.entrySet()) {
            try {
                entry.getValue().send(
                        new PlayerDataSyncPacket(syncId)
                );

                LOGGER.info(
                        "[ShutdownSync] Requested snapshot from {} (syncId={})",
                        entry.getKey(),
                        syncId
                );

            } catch (RuntimeException e) {
                ShutdownPlayerDataSync.markServerFailed(
                        syncId,
                        entry.getKey(),
                        "Could not queue snapshot request: " + e.getMessage()
                );
            }
        }

        // Keep processing incoming packets and their file writes.
        waitAndProcessTasks(
                server,
                PLAYER_DATA_SYNC_TIMEOUT_MS,
                ShutdownPlayerDataSync::isResolved
        );

        if (ShutdownPlayerDataSync.isSuccessful()) {
            LOGGER.info(
                    "[ShutdownSync] All required snapshots confirmed. "
                            + "Sending ServerStopPacket. ({})",
                    ShutdownPlayerDataSync.describeState()
            );

            gateway.sendToAll(new ServerStopPacket());

            waitAndProcessTasks(
                    server,
                    SHUTDOWN_TIMEOUT_MS,
                    () -> ServersLinkApi.getRunningSubServers() <= 0
            );

        } else {
            // Fail-safe: never intentionally stop subservers after an incomplete sync.
            LOGGER.error(
                    "[ShutdownSync] Snapshot synchronization was incomplete. "
                            + "NOT sending ServerStopPacket. {}",
                    ShutdownPlayerDataSync.describeState()
            );
        }

        IS_RUNNING = false;
        PlayersInformation.saveNbt(server);
    }

    /**
     * Waits until the timeout is over or the condition is true,
     * while the server thread keeps executing its queued tasks.
     */
    private static void waitAndProcessTasks(
            MinecraftServer server,
            long timeoutMs,
            BooleanSupplier done
    ) {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline
                && !done.getAsBoolean()) {

            ((MinecraftServerAccessor) server).invokeRunAllTasks();

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error(
                        "[ShutdownSync] Interrupted while waiting "
                                + "for shutdown synchronization",
                        e
                );
                return;
            }
        }

        ((MinecraftServerAccessor) server).invokeRunAllTasks();
    }
}
