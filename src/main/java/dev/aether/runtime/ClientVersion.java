package dev.aether.runtime;

public final class ClientVersion {
    private final String name;
    private final GameVersion primaryGameVersion;
    private final GameVersion compatibilityGameVersion;

    private ClientVersion(String name, GameVersion primaryGameVersion, GameVersion compatibilityGameVersion) {
        this.name = name;
        this.primaryGameVersion = primaryGameVersion;
        this.compatibilityGameVersion = compatibilityGameVersion;
    }

    public static ClientVersion current() {
        return new ClientVersion("0.1.0", GameVersion.MINECRAFT_1_8_9, GameVersion.MINECRAFT_1_7_10);
    }

    public String name() {
        return name;
    }

    public GameVersion primaryGameVersion() {
        return primaryGameVersion;
    }

    public GameVersion compatibilityGameVersion() {
        return compatibilityGameVersion;
    }
}

