package io.github.kgriff0n.configs;

import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

public class YamlConfigLoader {

    private final Yaml yaml;

    public YamlConfigLoader() {
        this.yaml = new Yaml();
    }

    public Map<String, Object> loadConfig(String filePath) throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(filePath);
        return yaml.load(inputStream);
    }

    public <T> T loadConfigAs(String filePath, Class<T> configClass) throws FileNotFoundException {
        InputStream inputStream = new FileInputStream(filePath);
        return yaml.loadAs(inputStream, configClass);
    }
}
