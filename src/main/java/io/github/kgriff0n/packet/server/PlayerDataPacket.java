package io.github.kgriff0n.packet.server;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.mixin.accessor.PlayerManagerAccessor;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.server.Settings;
import io.github.kgriff0n.util.PlayerData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.server.players.PlayerList;

import static io.github.kgriff0n.ServersLink.SERVER;

public class PlayerDataPacket implements Packet {

    private final UUID uuid;

    /* A field is null if the corresponding file does not exist */
    private final byte[] data;
    private final byte[] advancements;
    private final byte[] stats;

    public PlayerDataPacket(UUID uuid) throws IOException {
        this.uuid = uuid;
        this.data = readIfExists(PlayerData.getDataPath(uuid), "data");
        this.advancements = readIfExists(PlayerData.getAdvancementsPath(uuid), "advancements");
        this.stats = readIfExists(PlayerData.getStatsPath(uuid), "stats");
        ServersLink.LOGGER.info("[DEBUG] PlayerDataPacket - created for {} (data={}B, advancements={}B, stats={}B)",
                uuid, size(data), size(advancements), size(stats));
    }

    private byte[] readIfExists(Path path, String label) throws IOException {
        if (!Files.exists(path)) {
            ServersLink.LOGGER.warn("[DEBUG] PlayerDataPacket - {} file missing for {}: {}", label, uuid, path);
            return null;
        }
        return Files.readAllBytes(path);
    }

    private void writeIfPresent(Path path, byte[] bytes, String label) {
        if (bytes == null) {
            ServersLink.LOGGER.info("[DEBUG] PlayerDataPacket - no {} received for {}, keeping local file", label, uuid);
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
            ServersLink.LOGGER.info("[DEBUG] PlayerDataPacket - wrote {} for {} ({} bytes) -> {}", label, uuid, bytes.length, path);
        } catch (IOException e) {
            ServersLink.LOGGER.error("[DEBUG] PlayerDataPacket - unable to write {} for {}", label, uuid, e);
        }
    }

    private static int size(byte[] b) {
        return b == null ? 0 : b.length;
    }

    private void writeData() {
        writeIfPresent(PlayerData.getDataPath(uuid), data, "data");
        writeIfPresent(PlayerData.getAdvancementsPath(uuid), advancements, "advancements");
        writeIfPresent(PlayerData.getStatsPath(uuid), stats, "stats");
    }

    @Override
    public boolean shouldReceive(Settings settings) {
        boolean result = settings.isPlayerDataSynced();
        ServersLink.LOGGER.info("[DEBUG] PlayerDataPacket - shouldReceive: {}", result);
        return result;
    }

    @Override
    public void onReceive() {
        ServersLink.LOGGER.info("[DEBUG] PlayerDataPacket - onReceive for {}", uuid);
        /* Remove cached data so it is reloaded from the files */
        PlayerList playerManager = SERVER.getPlayerList();
        ((PlayerManagerAccessor) playerManager).getAdvancements().remove(this.uuid);
        ((PlayerManagerAccessor) playerManager).getStats().remove(this.uuid);
        writeData();
    }
}
