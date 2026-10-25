package io.github.kgriff0n.packet.server;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.api.FakePlayerApi;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.socket.SubServer;
import java.io.IOException;
import net.minecraft.server.level.ServerPlayer;

import static io.github.kgriff0n.ServersLink.SERVER;

public class PlayerDataSyncPacket implements Packet {
    @Override
    public void onReceive() {
        ServersLink.LOGGER.info("[DEBUG] PlayerDataSyncPacket - Sync request received");
        int playerCount = SERVER.getPlayerList().getPlayers().size();
        ServersLink.LOGGER.info("[DEBUG] PlayerDataSyncPacket - Syncing data for {} players", playerCount);

        int successCount = 0;
        int failCount = 0;
        int fakeCount = 0;

        for (ServerPlayer player : SERVER.getPlayerList().getPlayers()) {
            if (FakePlayerApi.isFake(SERVER, player)) {
                ServersLink.LOGGER.info("[DEBUG] PlayerDataSyncPacket - skipping fake player {}", player.getName().getString());
                fakeCount++;
                continue;
            }
            try {
                ServersLink.LOGGER.info("[DEBUG] PlayerDataSyncPacket - Creating and sending PlayerDataPacket for {} ({})", 
                    player.getName().getString(), player.getUUID());
                SubServer.getInstance().send(new PlayerDataPacket(player.getUUID()));
                successCount++;
                ServersLink.LOGGER.info("[DEBUG] PlayerDataSyncPacket - Successfully sent data for {}", player.getName().getString());
            } catch (IOException e) {
                failCount++;
                ServersLink.LOGGER.error("[DEBUG] PlayerDataSyncPacket - Unable to send player data for {}: {}", 
                    player.getName().getString(), e.getMessage(), e);
            }
        }

        ServersLink.LOGGER.info("[DEBUG] PlayerDataSyncPacket - Sync completed: {} success, {} failed, {} fake out of {} total", 
            successCount, failCount, fakeCount, playerCount);
    }
}
