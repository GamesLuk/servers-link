package io.github.kgriff0n.packet.server;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class UpdateRolesPacket implements Packet {

    private static final Path OP_PATH = FabricLoader.getInstance().getGameDir().resolve("ops.json");
    private static final Path ROLES_PATH = FabricLoader.getInstance().getGameDir().resolve("playerdata").resolve("player_roles");
    private static final Path PLAYER_BAN_PATH = FabricLoader.getInstance().getGameDir().resolve("banned-players.json");
    private static final Path IP_BAN_PATH = FabricLoader.getInstance().getGameDir().resolve("banned-ips.json");

    private final byte[] ops;
    private final byte[] playerRoles;
    private final byte[] playerBans;
    private final byte[] ipBans;

    public UpdateRolesPacket() throws IOException {
        this.ops = readOps();
        this.playerBans = readPlayerBans();
        this.ipBans = readIpBans();
        if (FabricLoader.getInstance().isModLoaded("player-roles") && ROLES_PATH.toFile().exists()) {
            this.playerRoles = readPlayerRoles();
        } else {
            this.playerRoles = null;
        }
    }

    @Override
    public void onReceive() {
        try {
            writeOps();
            writeBans();
            if (FabricLoader.getInstance().isModLoaded("player-roles")) {
                writePlayerRoles();
            }
        } catch (IOException e) {
            ServersLink.LOGGER.error("Unable to write Roles");
        }
    }

    private byte[] readOps() throws IOException {
        return Files.readAllBytes(OP_PATH);
    }

    private byte[] readPlayerBans() throws IOException {
        return Files.readAllBytes(PLAYER_BAN_PATH);
    }

    private byte[] readIpBans() throws IOException {
        return Files.readAllBytes(IP_BAN_PATH);
    }

    private byte[] readPlayerRoles() throws IOException {
        return Files.readAllBytes(ROLES_PATH);
    }

    private void writeOps() throws IOException {
        Files.write(OP_PATH, ops);
    }

    private void writeBans() throws IOException {
        Files.write(PLAYER_BAN_PATH, playerBans);
        Files.write(IP_BAN_PATH, ipBans);
    }

    private void writePlayerRoles() throws IOException {
        Files.write(ROLES_PATH, playerRoles);
    }
}
