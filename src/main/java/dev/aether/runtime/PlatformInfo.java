package dev.aether.runtime;

public final class PlatformInfo {
    private final OperatingSystem operatingSystem;
    private final String name;
    private final String architecture;
    private final String javaVersion;
    private final String javaHome;

    public PlatformInfo(OperatingSystem operatingSystem, String name, String architecture, String javaVersion, String javaHome) {
        this.operatingSystem = operatingSystem;
        this.name = name;
        this.architecture = architecture;
        this.javaVersion = javaVersion;
        this.javaHome = javaHome;
    }

    public OperatingSystem operatingSystem() {
        return operatingSystem;
    }

    public String name() {
        return name;
    }

    public String architecture() {
        return architecture;
    }

    public String javaVersion() {
        return javaVersion;
    }

    public String javaHome() {
        return javaHome;
    }

    public String displayName() {
        return operatingSystem.name().toLowerCase() + " " + architecture;
    }
}

