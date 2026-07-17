package dev.aether.config;

import dev.aether.TestSupport;

import java.nio.file.Files;
import java.nio.file.Path;

public final class JsonConfigStoreTest {
    public static void main(String[] args) throws Exception {
        Path temp = Files.createTempFile("aether-config", ".json");
        JsonConfigStore store = new JsonConfigStore(temp);

        ConfigDocument document = ConfigDocument.builder()
            .putBoolean("module.hud.fps.enabled", true)
            .put("theme", "Aether \"Frost\"")
            .build();

        store.save(document);
        ConfigDocument loaded = store.load();

        TestSupport.assertTrue(loaded.getBoolean("module.hud.fps.enabled", false), "Boolean config should round-trip.");
        TestSupport.assertEquals("Aether \"Frost\"", loaded.get("theme", ""), "Escaped strings should round-trip.");
        Files.deleteIfExists(temp);
    }
}

