package dev.aether.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class JsonConfigStore {
    private static final String SCHEMA = "aether.config.v1";

    private final Path file;

    public JsonConfigStore(Path file) {
        if (file == null) {
            throw new IllegalArgumentException("Config file path is required.");
        }
        this.file = file;
    }

    public boolean exists() {
        return Files.isRegularFile(file);
    }

    public Path file() {
        return file;
    }

    public ConfigDocument load() throws IOException {
        String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
        SimpleJsonObject root = SimpleJsonObject.parse(json);
        String schema = root.getString("schema");
        if (!SCHEMA.equals(schema)) {
            throw new IOException("Unsupported Aether config schema: " + schema);
        }
        return ConfigDocument.builder().putAll(root.getObject("values").strings()).build();
    }

    public void save(ConfigDocument document) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }

        SimpleJsonObject root = new SimpleJsonObject();
        root.putString("schema", SCHEMA);
        SimpleJsonObject values = new SimpleJsonObject();
        for (Map.Entry<String, String> entry : document.values().entrySet()) {
            values.putString(entry.getKey(), entry.getValue());
        }
        root.putObject("values", values);
        Files.write(file, root.toPrettyJson().getBytes(StandardCharsets.UTF_8));
    }
}
