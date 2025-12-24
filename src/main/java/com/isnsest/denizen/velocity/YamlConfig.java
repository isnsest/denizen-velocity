package com.isnsest.denizen.velocity;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class YamlConfig {

    private final Path file;
    private int port = 25565;

    public YamlConfig(Path dataFolder) {
        if (dataFolder == null) {
            throw new IllegalArgumentException("Data folder is null!");
        }

        if (!Files.exists(dataFolder)) {
            try {
                Files.createDirectories(dataFolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        this.file = dataFolder.resolve("config.yml");
        reload();
    }

    public void reload() {
        if (file == null) return;

        Yaml yaml = new Yaml(getDumperOptions());
        Map<String, Object> data = new HashMap<>();

        if (Files.exists(file)) {
            try (InputStream in = Files.newInputStream(file)) {
                Object loaded = yaml.load(in);
                if (loaded instanceof Map<?, ?> map) {
                    Object p = map.get("port");
                    if (p instanceof Number) {
                        port = ((Number) p).intValue();
                    } else if (p instanceof String) {
                        try {
                            port = Integer.parseInt((String) p);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassCastException ignored) {}
        }

        data.put("port", port);
        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(file))) {
            yaml.dump(data, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getPort() {
        return port;
    }

    private DumperOptions getDumperOptions() {
        DumperOptions options = new DumperOptions();
        options.setIndent(2);
        options.setPrettyFlow(true);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return options;
    }
}
