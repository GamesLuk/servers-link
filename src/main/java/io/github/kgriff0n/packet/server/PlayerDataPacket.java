package io.github.kgriff0n.packet.server;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.mixin.accessor.PlayerManagerAccessor;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.server.Settings;
import io.github.kgriff0n.util.PlayerData;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import net.minecraft.server.players.PlayerList;

import static io.github.kgriff0n.ServersLink.SERVER;

public class PlayerDataPacket implements Packet {

    private final UUID uuid;

    private final byte[] data;
    private final byte[] advancements;
    private final byte[] stats;

    private final UUID shutdownSyncId;
    private final String snapshotServer;

    public PlayerDataPacket(UUID uuid) throws IOException {
        this(uuid, null, null);
    }

    public PlayerDataPacket(
            UUID uuid,
            UUID shutdownSyncId,
            String snapshotServer
    ) throws IOException {
        this.uuid = uuid;
        this.shutdownSyncId = shutdownSyncId;
        this.snapshotServer = snapshotServer;

        this.data = readIfExists(PlayerData.getDataPath(uuid), "data");
        this.advancements = readIfExists(
                PlayerData.getAdvancementsPath(uuid),
                "advancements"
        );
        this.stats = readIfExists(
                PlayerData.getStatsPath(uuid),
                "stats"
        );

        if (shutdownSyncId != null && data == null) {
            throw new IOException(
                    "Player data file is missing after saveAll for " + uuid
            );
        }
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
            ServersLink.LOGGER.error(
                    "[PlayerDataSync] Failed to write {} for player {} to {}",
                    label,
                    uuid,
                    path,
                    e
            );
        }
    }

    private boolean writeIfPresent(
            Path path,
            byte[] bytes,
            String label,
            boolean mirrorMissingFiles
    ) {
        if (bytes == null) {
            if (!mirrorMissingFiles) {
                return true;
            }

            try {
                Files.deleteIfExists(path);
                return true;
            } catch (IOException e) {
                ServersLink.LOGGER.error(
                        "[PlayerDataSync] Failed to remove absent {} file "
                                + "for player {} at {}",
                        label,
                        uuid,
                        path,
                        e
                );
                return false;
            }
        }

        Path temp = null;

        try {
            Files.createDirectories(path.getParent());

            temp = Files.createTempFile(
                    path.getParent(),
                    path.getFileName().toString(),
                    ".servers-link.tmp"
            );

            Files.write(temp, bytes);

            try {
                Files.move(
                        temp,
                        path,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                );
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(
                        temp,
                        path,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            return true;

        } catch (IOException e) {
            ServersLink.LOGGER.error(
                    "[PlayerDataSync] Failed to write {} file "
                            + "for player {} at {}",
                    label,
                    uuid,
                    path,
                    e
            );
            return false;

        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (IOException e) {
                    ServersLink.LOGGER.warn(
                            "[PlayerDataSync] Could not remove temporary file {} "
                                    + "for player {}",
                            temp,
                            uuid,
                            e
                    );
                }
            }
        }
    }

    private static int size(byte[] b) {
        return b == null ? 0 : b.length;
    }

    private boolean writeData() {
        boolean mirrorMissingFiles = shutdownSyncId != null;

        if (mirrorMissingFiles && data == null) {
            ServersLink.LOGGER.error(
                    "[ShutdownSync] Refusing incomplete player snapshot "
                            + "for {} from {}: primary NBT is absent",
                    uuid,
                    snapshotServer
            );
            return false;
        }

        boolean success = true;

        success &= writeIfPresent(
                PlayerData.getDataPath(uuid),
                data,
                "data",
                mirrorMissingFiles
        );

        success &= writeIfPresent(
                PlayerData.getAdvancementsPath(uuid),
                advancements,
                "advancements",
                mirrorMissingFiles
        );

        success &= writeIfPresent(
                PlayerData.getStatsPath(uuid),
                stats,
                "stats",
                mirrorMissingFiles
        );

        return success;
    }

    @Override
    public boolean shouldReceive(Settings settings) {
        return settings.isPlayerDataSynced();
    }

    @Override
    public void onReceive() {
        boolean success = writeData();

        if (success) {
            PlayerList playerManager = SERVER.getPlayerList();

            ((PlayerManagerAccessor) playerManager)
                    .getAdvancements()
                    .remove(this.uuid);

            ((PlayerManagerAccessor) playerManager)
                    .getStats()
                    .remove(this.uuid);

        } else {
            ServersLink.LOGGER.error(
                    "[PlayerDataSync] Incomplete/failed data write "
                            + "for player {}{}",
                    uuid,
                    shutdownSyncId != null
                            ? " during shutdown sync " + shutdownSyncId
                            : ""
            );
        }

        if (shutdownSyncId != null && ServersLink.isGateway) {
            ShutdownPlayerDataSync.recordSnapshotWrite(
                    shutdownSyncId,
                    snapshotServer,
                    uuid,
                    success
            );
        }
    }
}
