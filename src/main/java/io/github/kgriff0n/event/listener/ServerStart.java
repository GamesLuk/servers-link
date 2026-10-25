package io.github.kgriff0n.event.listener;

import io.github.kgriff0n.PlayersInformation;
import io.github.kgriff0n.socket.SubServer;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.socket.Gateway;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import static io.github.kgriff0n.ServersLink.CONFIG_ERROR;

public class ServerStart implements ServerLifecycleEvents.ServerStarted {
    @Override
    public void onServerStarted(MinecraftServer minecraftServer) {
        ServersLink.LOGGER.info("[DEBUG] ServerStart - onServerStarted triggered");

        if (CONFIG_ERROR) {
            ServersLink.LOGGER.error("[DEBUG] ServerStart - CONFIG_ERROR detected, halting server");
            ServersLink.LOGGER.error("You must configure servers-link before starting your server");
            minecraftServer.halt(false);
        } else {
            /* Initialize SERVER */
            ServersLink.LOGGER.info("[DEBUG] ServerStart - Initializing SERVER instance");
            ServersLink.SERVER = minecraftServer;

            if (ServersLink.isGateway) {
                ServersLink.LOGGER.info("[DEBUG] ServerStart - Starting as GATEWAY");

                // Players information
                ServersLink.LOGGER.info("[DEBUG] ServerStart - Loading players information");
                PlayersInformation.loadNbt(minecraftServer);

                ServersLink.LOGGER.info("[DEBUG] ServerStart - Creating Gateway on port {}", ServersLink.getGatewayPort());
                Gateway gateway = new Gateway(ServersLink.getGatewayPort());
                ServersLink.LOGGER.info("[DEBUG] ServerStart - Loading Gateway config");
                gateway.loadConfig();
                gateway.setDaemon(true);
                ServersLink.LOGGER.info("[DEBUG] ServerStart - Starting Gateway thread");
                gateway.start();
                ServersLink.LOGGER.info("[DEBUG] ServerStart - Gateway started successfully");
            } else {
                ServersLink.LOGGER.info("[DEBUG] ServerStart - Starting as SUBSERVER");
                ServersLink.LOGGER.info("[DEBUG] ServerStart - Creating SubServer connection to {}:{}", 
                    ServersLink.getGatewayIp(), ServersLink.getGatewayPort());
                SubServer connection = new SubServer(ServersLink.getGatewayIp(), ServersLink.getGatewayPort());
                connection.setDaemon(true);
                ServersLink.LOGGER.info("[DEBUG] ServerStart - Starting SubServer thread");
                connection.start();
                ServersLink.LOGGER.info("[DEBUG] ServerStart - SubServer thread started");
            }
        }
    }
}
