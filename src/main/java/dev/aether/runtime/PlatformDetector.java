package dev.aether.runtime;

import java.util.Locale;

public final class PlatformDetector {
    private PlatformDetector() {
    }

    public static PlatformInfo detect() {
        String osName = System.getProperty("os.name", "unknown");
        String lower = osName.toLowerCase(Locale.ENGLISH);
        OperatingSystem operatingSystem;
        if (lower.contains("linux")) {
            operatingSystem = OperatingSystem.LINUX;
        } else if (lower.contains("windows")) {
            operatingSystem = OperatingSystem.WINDOWS;
        } else if (lower.contains("mac") || lower.contains("darwin")) {
            operatingSystem = OperatingSystem.MACOS;
        } else {
            operatingSystem = OperatingSystem.UNKNOWN;
        }

        return new PlatformInfo(
            operatingSystem,
            osName,
            System.getProperty("os.arch", "unknown"),
            System.getProperty("java.version", "unknown"),
            System.getProperty("java.home", "unknown")
        );
    }
}

