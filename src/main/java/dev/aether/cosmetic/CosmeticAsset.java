package dev.aether.cosmetic;

import java.nio.file.Path;

public final class CosmeticAsset {
    private final String id;
    private final String name;
    private final CosmeticType type;
    private final Path localFile;
    private final boolean builtIn;

    public CosmeticAsset(String id, String name, CosmeticType type, Path localFile, boolean builtIn) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Cosmetic id cannot be blank.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Cosmetic name cannot be blank.");
        }
        this.id = id;
        this.name = name;
        this.type = type;
        this.localFile = localFile;
        this.builtIn = builtIn;
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public CosmeticType type() {
        return type;
    }

    public Path localFile() {
        return localFile;
    }

    public boolean builtIn() {
        return builtIn;
    }
}

