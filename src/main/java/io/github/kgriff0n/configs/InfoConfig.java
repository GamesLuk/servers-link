package io.github.kgriff0n.configs;

public class InfoConfig {

    private final String group;
    private final boolean gateway;
    private final String gatewayIp;
    private final int gatewayPort;
    private final String serverName;
    private final String serverIp;
    private final int serverPort;
    private final String commandName;

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

    public InfoConfig(String group, boolean gateway, String gatewayIp, int gatewayPort, String serverName,
                      String serverIp, int serverPort, String commandName) {

        this.group = group;
        this.gateway = gateway;
        this.gatewayIp = gatewayIp;
        this.gatewayPort = gatewayPort;
        this.serverName = serverName;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.commandName = commandName;

    }

    public String getGroup() {
        return group;
    }

    public boolean isGateway() {
        return gateway;
    }
}
