package io.github.kgriff0n.socket;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.configs.ConfigConfig;
import io.github.kgriff0n.configs.GroupsConfig;
import io.github.kgriff0n.configs.InfoConfig;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.server.Group;
import io.github.kgriff0n.server.ServerInfo;
import io.github.kgriff0n.api.ServersLinkApi;
import io.github.kgriff0n.server.Settings;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static io.github.kgriff0n.ServersLink.IS_RUNNING;
import static io.github.kgriff0n.ServersLink.SERVER;

public class Gateway extends Thread {

    public static Gateway gateway;

    private HashMap<String, Group> groups;
    private ServerSocket serverSocket;

    private boolean debug;
    private boolean globalPlayerCount;
    private boolean whitelistIp;
    private final List<String> whitelistedIp = new ArrayList<>();
    private boolean reconnectLastServer;

    public Gateway(int port) {
        if (gateway != null) {
            ServersLink.LOGGER.info("Gateway server already started");
        }
        try {
            serverSocket = new ServerSocket(port);
            gateway = this;
            groups = GroupsConfig.loadConfig(ServersLink.CONFIG.resolve("groups.yml").toString());
            ServersLinkApi.addServer(ServersLink.getServerInfo(), null);
        } catch (IOException e) {
            ServersLink.LOGGER.info("Unable to start central server");
        }
    }

    public static Gateway getInstance() {
        return gateway;
    }

    public void sendAll(Packet packet) {
        for (G2SConnection sub : ServersLinkApi.getServerMap().values()) {
            if (sub != null) sub.send(packet);
        }
    }

    public void sendTo(Packet packet, String serverName) {
        if (serverName.equals(ServersLink.getServerInfo().getName())) {
            SERVER.execute(packet::onReceive);
        } else {
            for (ServerInfo server : ServersLinkApi.getServerList()) {
                if (server.getName().equals(serverName)) {
                    ServersLinkApi.getServerMap().get(server).send(packet);
                }
            }
        }
    }

    public void forward(Packet packet, String sourceServer) {
        String sourceGroup = ServersLinkApi.getServer(sourceServer).getGroupId();
        for (ServerInfo server : ServersLinkApi.getServerList()) {
            G2SConnection sub = ServersLinkApi.getServerMap().get(server);
            if (sub != null && !server.getName().equals(sourceServer)) {
                if (isDebugEnabled()) ServersLink.LOGGER.info("\u001B[33mForward packet {} to {}?", packet.getClass().getName(), server.getName());
                if (packet.shouldReceive(getSettings(sourceGroup, server.getGroupId()))) {
                    if (isDebugEnabled()) ServersLink.LOGGER.info("\u001B[32mYes");
                    sub.send(packet);
                } else {
                    if (isDebugEnabled()) ServersLink.LOGGER.info("\u001B[31mNo");
                }
            }
        }
    }

    public void removePlayer(UUID uuid) {
        for (ServerInfo server : ServersLinkApi.getServerList()) {
            server.removePlayer(uuid);
        }
    }

    public boolean isConnectedPlayer(UUID uuid) {
        for (ServerInfo server : ServersLinkApi.getServerList()) {
            if (server.getPlayersList().containsKey(uuid)) {
                return true;
            }
        }
        return false;
    }

    public Settings getSettings(String sourceGroup, String destinationGroup) {
        Group a = groups.get(sourceGroup);
        if (a.getRules().containsKey(destinationGroup)) {
            return a.getRules().get(destinationGroup);
        } else if (sourceGroup.equals(destinationGroup)) {
            return a.getSettings();
        } else {
            return groups.get("global").getSettings();
        }
    }

    public Group getGroup(String groupId) {
        return groups.get(groupId);
    }

    public Collection<Group> getGroups() {
        return groups.values();
    }

    public void loadConfig() {
        Path path = ServersLink.CONFIG.resolve("config.yml");
        ConfigConfig config = ConfigConfig.loadConfig(path.toString());
        if (config == null) return;

        debug = config.isDebug();
        globalPlayerCount = config.isGlobalPlayerCount();
        whitelistIp = config.isWhitelistIps();
        whitelistedIp.addAll(config.getWhitelistedIps());
        reconnectLastServer = config.isReconnectLastServer();
    }

    public boolean isDebugEnabled() {
        return debug;
    }

    public boolean isGlobalPlayerCountEnabled() {
        return globalPlayerCount;
    }

    public boolean hasWhitelistIp() {
        return whitelistIp;
    }

    public List<String> getWhitelistedIp() {
        return whitelistedIp;
    }

    public boolean shouldReconnectToLastServer() {
        return reconnectLastServer;
    }

    @Override
    public void run() {
        while (IS_RUNNING) {
            try {
                Socket socket = serverSocket.accept();
                if (whitelistIp) {
                    if (whitelistedIp.contains(socket.getInetAddress().getHostAddress())) {
                        G2SConnection connection = new G2SConnection(socket);
                        connection.start();
                    } else {
                        ServersLink.LOGGER.warn("Unauthorized connection received from {}", socket.getInetAddress().getHostAddress());
                        socket.close();
                    }
                } else {
                    G2SConnection connection = new G2SConnection(socket);
                    connection.start();
                }
            } catch (IOException e) {
                ServersLink.LOGGER.info("Unable to accept connection");
            }

        }
    }
}
