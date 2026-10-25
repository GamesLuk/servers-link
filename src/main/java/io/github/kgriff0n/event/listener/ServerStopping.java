package io.github.kgriff0n.event.listener;

import io.github.kgriff0n.PlayersInformation;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.mixin.accessor.MinecraftServerAccessor;
import io.github.kgriff0n.packet.info.ServerStatusPacket;
import io.github.kgriff0n.packet.server.PlayerDataSyncPacket;
import io.github.kgriff0n.packet.server.ServerStopPacket;
import io.github.kgriff0n.socket.Gateway;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.socket.SubServer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;

import static io.github.kgriff0n.ServersLink.*;

public class ServerStopping implements ServerLifecycleEvents.ServerStopping {

    private static final long SYNC_WAIT_MS = 2000;
    private static final long SHUTDOWN_TIMEOUT_MS = 10000;

    @Override
    public void onServerStopping(@NotNull MinecraftServer server) {
        LOGGER.info("[DEBUG] ServerStopping - onServerStopping triggered");

        if (!CONFIG_ERROR) {
            if (isGateway) {
                LOGGER.info("[DEBUG] ServerStopping - GATEWAY shutdown sequence");
                Gateway.getInstance().sendToAll(new PlayerDataSyncPacket());

                LOGGER.info("Begin servers synchronization");
                /* Keep processing server tasks, otherwise received PlayerDataPackets are never written */
                waitAndProcessTasks(server, SYNC_WAIT_MS, () -> false);

                Gateway.getInstance().sendToAll(new ServerStopPacket());

                LOGGER.info("[DEBUG] ServerStopping - Waiting for {} sub-servers", ServersLinkApi.getRunningSubServers());
                waitAndProcessTasks(server, SHUTDOWN_TIMEOUT_MS, () -> ServersLinkApi.getRunningSubServers() <= 0);
                if (ServersLinkApi.getRunningSubServers() > 0) {
                    LOGGER.warn("[DEBUG] ServerStopping - Timeout, {} sub-servers still connected", ServersLinkApi.getRunningSubServers());
                }

                IS_RUNNING = false;
                PlayersInformation.saveNbt(server);
                LOGGER.info("[DEBUG] ServerStopping - Gateway shutdown sequence completed");
            } else {
                LOGGER.info("[DEBUG] ServerStopping - SUBSERVER shutdown sequence");

                /* Confirm shutdown */
                SubServer subServer = SubServer.getInstance();
                LOGGER.info("[DEBUG] ServerStopping - Sending ServerStatusPacket (shutdown=true) to gateway");
                subServer.send(new ServerStatusPacket(ServersLink.getServerInfo().getName(), 0.0f, true));
                LOGGER.info("[DEBUG] ServerStopping - SubServer shutdown sequence completed");
            }
        } else {
            LOGGER.warn("[DEBUG] ServerStopping - CONFIG_ERROR is true, skipping shutdown sequence");
        }
    }

    /**
     * Waits until the timeout is over or the condition is true,
     * while the server thread keeps executing its queued tasks.
     */
    private static void waitAndProcessTasks(MinecraftServer server, long timeoutMs, BooleanSupplier done) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline && !done.getAsBoolean()) {
            ((MinecraftServerAccessor) server).invokeRunAllTasks();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("[DEBUG] ServerStopping - interrupted while waiting", e);
                return;
            }
        }
        ((MinecraftServerAccessor) server).invokeRunAllTasks();
    }
}