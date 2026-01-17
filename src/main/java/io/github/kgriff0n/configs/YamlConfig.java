package io.github.kgriff0n.configs;

import io.github.kgriff0n.ServersLink;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class YamlConfig {

    private final Yaml yaml;

    public YamlConfig() {
        this.yaml = new Yaml();
    }

    public Map<String, Object> loadConfig(String filePath) throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(filePath);
        return yaml.load(inputStream);
    }

    public static void generateConfig(String resourcePath, String targetPath) {
        Path target = ServersLink.CONFIG.resolve(targetPath);

        if (Files.exists(target)) {
            ServersLink.LOGGER.warn("Config file already exists at: {}", targetPath);
            return;
        }

        try (InputStream inputStream = InfoConfig.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                ServersLink.LOGGER.error("Resource file '{}' not found in resources!", resourcePath);
                return;
            }

            Files.createDirectories(target.getParent());
            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);

            ServersLink.LOGGER.info("Config file created at: {}", targetPath);
        } catch (IOException e) {
            ServersLink.LOGGER.error("Error while creating config file: {}", e.getMessage());
        }
    }
}