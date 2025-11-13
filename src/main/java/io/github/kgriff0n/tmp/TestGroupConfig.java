package io.github.kgriff0n.tmp;

import io.github.kgriff0n.configs.YamlConfigLoader;

import java.util.HashMap;
import java.util.Map;

public class TestGroupConfig {

    private TestGroupConfig(Map<String, Object> configMap) {
        System.out.println("TestGroupConfig loaded with config: " + configMap);
        System.out.println("Groups: " + configMap.get("groups"));
        Map<String, Object> groups = (Map<String, Object>) configMap.get("groups");

        System.out.println(groups.size());
    }

    public static TestGroupConfig loadConfig(String path) {
        try {
            YamlConfigLoader loader = new YamlConfigLoader();
            Map<String, Object> configMap = loader.loadConfig(path);
            return new TestGroupConfig(configMap);
        } catch (Exception e) {
            System.err.println("Error while loading InfoConfig: " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        TestGroupConfig config = TestGroupConfig.loadConfig("C:\\Users\\lukas\\Documents\\Programming_Programs\\servers-link\\src\\main\\java\\io\\github\\kgriff0n\\tmp\\groups.yml");
        if (config != null) {
            System.out.println("Config loaded!");
        }
    }
}
