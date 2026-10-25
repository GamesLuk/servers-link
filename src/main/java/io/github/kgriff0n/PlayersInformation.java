package io.github.kgriff0n;

import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;

public class PlayersInformation {

    private static final HashMap<UUID, String> lastServer = new HashMap<>();

    public static void setLastServer(UUID player, String serverName) {
        ServersLink.LOGGER.info("[DEBUG] PlayersInformation - setLastServer: {} -> {}", player, serverName);
        lastServer.put(player, serverName);
    }

    public static String getLastServer(UUID player) {
        String server = lastServer.get(player);
        ServersLink.LOGGER.info("[DEBUG] PlayersInformation - getLastServer: {} -> {}", player, server);
        return server;
    }

    public static void saveNbt(MinecraftServer server) {
        ServersLink.LOGGER.info("[DEBUG] PlayersInformation - Saving NBT data");
        CompoundTag nbt = new CompoundTag();
        ServersLink.LOGGER.info("[DEBUG] PlayersInformation - Entries to save: {}", lastServer.size());
        lastServer.forEach((uuid, name) -> {
            ServersLink.LOGGER.info("[DEBUG] PlayersInformation - Saving: {} -> {}", uuid, name);
            nbt.putString(uuid.toString(), name);
        });

        Path dataFile = server
                .getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve("servers_link.nbt");
        ServersLink.LOGGER.info("[DEBUG] PlayersInformation - Writing to file: {}", dataFile.toAbsolutePath());

        try (OutputStream os = Files.newOutputStream(dataFile)) {
            NbtIo.writeCompressed(nbt, os);
            ServersLink.LOGGER.info("[DEBUG] PlayersInformation - Successfully saved NBT data");
        } catch (IOException e) {
            ServersLink.LOGGER.error("[DEBUG] PlayersInformation - Unable to save data: {}", e.getMessage(), e);
        }
    }

    public static void loadNbt(MinecraftServer server) {
        Path dataFile = server
                .getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve("servers_link.nbt");

        ServersLink.LOGGER.info("[DEBUG] PlayersInformation - Loading NBT data from: {}", dataFile.toAbsolutePath());
        ServersLink.LOGGER.info("[DEBUG] PlayersInformation - File exists: {}", Files.exists(dataFile));

        if (Files.exists(dataFile)) {
            try (InputStream is = Files.newInputStream(dataFile)) {
                CompoundTag nbt = NbtIo.readCompressed(is, NbtAccounter.unlimitedHeap());
                ServersLink.LOGGER.info("[DEBUG] PlayersInformation - NBT keys found: {}", nbt.keySet().size());

                int loadedCount = 0;
                for (String uuid : nbt.keySet()) {
                    UUID player = UUID.fromString(uuid);
                    nbt.getString(uuid).ifPresent(string -> {
                        ServersLink.LOGGER.info("[DEBUG] PlayersInformation - Loading: {} -> {}", player, string);
                        lastServer.put(player, string);
                    });
                    loadedCount++;
                }
                ServersLink.LOGGER.info("[DEBUG] PlayersInformation - Successfully loaded {} player entries", loadedCount);
            } catch (IOException e) {
                ServersLink.LOGGER.error("[DEBUG] PlayersInformation - Unable to load data: {}", e.getMessage(), e);
            }
        } else {
            ServersLink.LOGGER.info("[DEBUG] PlayersInformation - No existing data file, starting fresh");
        }
    }
}
