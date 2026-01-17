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
        resourcePath = "/" + resourcePath;

        if (Files.exists(target)) {
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
            sendConfigWarning();
        } catch (IOException e) {
            ServersLink.LOGGER.error("Error while creating config file: {}", e.getMessage());
        }
    }

    public static void sendConfigWarning(){
        ServersLink.LOGGER.warn("""
                ---------------------------------------------------------------------------------
                New configuration files created! Please configure servers-link.
                The configuration file(s) can be found in the 'config/servers-link' folder.
                For more information, visit https://github.com/kgriff0n/servers-link/blob/master/README.md
                For help, join the Discord: https://discord.gg/ZeHm57BEyt
                Please edit the files and then restart the server.
                ---------------------------------------------------------------------------------
                """);
    }
}