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

    private final byte[] data;
    private final byte[] advancements;
    private final byte[] stats;

    public PlayerDataPacket(UUID uuid) throws IOException {
        this.uuid = uuid;
        this.data = readIfExists(PlayerData.getDataPath(uuid), "data");
        this.advancements = readIfExists(PlayerData.getAdvancementsPath(uuid), "advancements");
        this.stats = readIfExists(PlayerData.getStatsPath(uuid), "stats");
    }

    private byte[] readIfExists(Path path, String label) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }
        return Files.readAllBytes(path);
    }

    private void writeIfPresent(Path path, byte[] bytes, String label) {
        if (bytes == null) {
            return;
        }
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);
        } catch (IOException e) {
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
        return result;
    }

    @Override
    public void onReceive() {
        /* Remove cached data so it is reloaded from the files */
        PlayerList playerManager = SERVER.getPlayerList();
        ((PlayerManagerAccessor) playerManager).getAdvancements().remove(this.uuid);
        ((PlayerManagerAccessor) playerManager).getStats().remove(this.uuid);
        writeData();
    }
}
