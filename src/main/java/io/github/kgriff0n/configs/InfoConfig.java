package io.github.kgriff0n.configs;

import io.github.kgriff0n.ServersLink;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class InfoConfig {

    private final String group;
    private final boolean gateway;
    private final String gatewayIp;
    private final int gatewayPort;
    private final String serverName;
    private final String serverIp;
    private final int serverPort;
    private final String commandName;

    public String getGroup() {
        return group;
    }
    public boolean isGateway() {
        return gateway;
    }
    public String getGatewayIp() {
        return gatewayIp;
    }
    public int getGatewayPort() {
        return gatewayPort;
    }
    public String getServerName() {
        return serverName;
    }
    public String getServerIp() {
        return serverIp;
    }
    public int getServerPort() {
        return serverPort;
    }
    public String getCommandName() {
        return commandName;
    }

    private InfoConfig(Map<String, Object> configMap) {

        this.group = (String) configMap.get("group");
        this.gateway = (Boolean) configMap.get("gateway");
        this.gatewayIp = (String) configMap.get("gateway-ip");
        this.gatewayPort = (Integer) configMap.get("gateway-port");
        this.serverName = (String) configMap.get("server-name");
        this.serverIp = (String) configMap.get("server-ip");
        this.serverPort = (Integer) configMap.get("server-port");
        this.commandName = (String) configMap.getOrDefault("command-name", "server");

    }

    public static InfoConfig loadConfig(String path) {
        try {
            YamlConfig loader = new YamlConfig();
            Map<String, Object> configMap = loader.loadConfig(path);
            return new InfoConfig(configMap);
        } catch (Exception e) {
            ServersLink.LOGGER.error("Error while loading info.yml: {}", e.getMessage());
            return null;
        }
    }
}
