package io.github.kgriff0n.configs;

import java.util.ArrayList;
import java.util.Map;

public class ConfigConfig {

    private final boolean debug;
    private final boolean globalPlayerCount;
    private final boolean whitelistIps;
    private final ArrayList<String> whitelistedIps;
    private final boolean reconnectLastServer;

    public boolean isDebug() {
        return debug;
    }
    public boolean isGlobalPlayerCount() {
        return globalPlayerCount;
    }
    public boolean isWhitelistIps() {
        return whitelistIps;
    }
    public ArrayList<String> getWhitelistedIps() {
        return whitelistedIps;
    }
    public boolean isReconnectLastServer() {
        return reconnectLastServer;
    }

    private ConfigConfig(Map<String, Object> configMap) {

        this.debug = (boolean) configMap.get("debug");
        this.globalPlayerCount = (boolean) configMap.get("global_player_count");
        this.whitelistIps = (boolean) configMap.get("whitelist_ip");
        this.whitelistedIps = (ArrayList<String>) configMap.get("whitelisted_ips");
        this.reconnectLastServer = (boolean) configMap.get("reconnect_last_server");
    }

    public static ConfigConfig loadConfig(String path) {
        try {
            YamlConfigLoader loader = new YamlConfigLoader();
            Map<String, Object> configMap = loader.loadConfig(path);
            return new ConfigConfig(configMap);
        } catch (Exception e) {
            System.err.println("Error while loading config.yml: " + e.getMessage());
            return null;
        }
    }

}
