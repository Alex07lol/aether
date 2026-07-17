package dev.aether.runtime;

public enum GameVersion {
    MINECRAFT_1_8_9("1.8.9"),
    MINECRAFT_1_7_10("1.7.10");

    private final String displayName;

    GameVersion(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}

