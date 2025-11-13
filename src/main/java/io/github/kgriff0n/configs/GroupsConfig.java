package io.github.kgriff0n.configs;

import io.github.kgriff0n.server.Group;
import io.github.kgriff0n.server.Settings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GroupsConfig {

    private final HashMap<String, Group> groups;

    public static HashMap<String, Group> loadConfig(String path) {
        try {
            YamlConfigLoader loader = new YamlConfigLoader();
            Map<String, Object> configMap = loader.loadConfig(path);
            return new GroupsConfig(configMap).groups;
        } catch (Exception e) {
            System.err.println("Error while loading groups.yml: " + e.getMessage());
            return null;
        }
    }

    public GroupsConfig(Map<String, Object> configMap) {
        groups = new HashMap<>();

        try {
            // ALL GROUPS
            Map<String, Object> groupsMap = (HashMap<String, Object>) configMap.get("groups");

            // GLOBAL
            Map<String, Object> globalGroup = (Map<String, Object>) groupsMap.get("global");
            Settings globalSettings = new Settings(
                    (Boolean) globalGroup.get("chat"),
                    (Boolean) globalGroup.get("player_data"),
                    (Boolean) globalGroup.get("player_list"),
                    (Boolean) globalGroup.get("roles"),
                    (Boolean) globalGroup.get("whitelist")
            );
            groups.put("global", new Group("global", globalSettings));

            //OTHER
            for (Map.Entry<String, Object> entry : groupsMap.entrySet()) {
                Map<String, Object> otherGroup = (Map<String, Object>) entry.getValue();
                Settings otherSettings = new Settings(
                        otherGroup.containsKey("chat") ? (boolean) otherGroup.get("chat")
                                : globalSettings.isChatSynced(),
                        otherGroup.containsKey("player_data") ? (boolean) otherGroup.get("player_data")
                                : globalSettings.isPlayerDataSynced(),
                        otherGroup.containsKey("player_list") ? (boolean) otherGroup.get("player_list")
                                : globalSettings.isPlayerListSynced(),
                        otherGroup.containsKey("roles") ? (boolean) otherGroup.get("roles")
                                : globalSettings.isRolesSynced(),
                        otherGroup.containsKey("whitelist") ? (boolean) otherGroup.get("whitelist")
                                : globalSettings.isWhitelistSynced()
                        );
                if (!entry.getKey().equals("global")) { // Doesn't re-add global group
                    groups.put(entry.getKey(), new Group(entry.getKey(), otherSettings));
                }
            }

            // RULES
            Map<String, Object> rules = (Map<String, Object>) configMap.get("rules");
            for (Map.Entry<String, Object> entry : rules.entrySet()) {
                Map<String, Object> rule = (Map<String, Object>) entry.getValue();
                ArrayList<String> ruleGroups = (ArrayList<String>) rule.get("groups");
                Settings ruleSettings = new Settings(
                        rule.containsKey("chat") ? (boolean) rule.get("chat") : globalSettings.isChatSynced(),
                        rule.containsKey("player_list") ? (boolean) rule.get("player_list") : globalSettings.isPlayerListSynced(),
                        rule.containsKey("player_data") ? (boolean) rule.get("player_data") : globalSettings.isPlayerDataSynced(),
                        rule.containsKey("roles") ? (boolean) rule.get("roles") : globalSettings.isRolesSynced(),
                        rule.containsKey("whitelist") ? (boolean) rule.get("whitelist") : globalSettings.isWhitelistSynced()
                        );
                for (int i = 0; i < ruleGroups.size(); i++) {
                    String groupId = ruleGroups.get(i);
                    for (int j = 0; j < ruleGroups.size(); j++) {
                        if (i != j) {
                            groups.get(groupId).addRule(ruleGroups.get(j), ruleSettings);
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Error while parsing groups.yml: " + e.getMessage());
        }
    }

}
