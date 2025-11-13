package io.github.kgriff0n.configs;

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

        this.group = (String) configMap.getOrDefault("group", "global");
        this.gateway = (Boolean) configMap.getOrDefault("gateway", false);
        this.gatewayIp = (String) configMap.getOrDefault("gateway-ip", "127.0.0.1");
        this.gatewayPort = (Integer) configMap.getOrDefault("gateway-port", 59001);
        this.serverName = (String) configMap.getOrDefault("server-name", "Hub");
        this.serverIp = (String) configMap.getOrDefault("server-ip", "127.0.0.1");
        this.serverPort = (Integer) configMap.getOrDefault("server-port", 25565);
        this.commandName = (String) configMap.getOrDefault("command-name", "server");

    }

    public static InfoConfig loadConfig(String path) {
        try {
            YamlConfigLoader loader = new YamlConfigLoader();
            Map<String, Object> configMap = loader.loadConfig(path);
            return new InfoConfig(configMap);
        } catch (Exception e) {
            System.err.println("Error while loading InfoConfig: " + e.getMessage());
            return null;
        }
    }

}
