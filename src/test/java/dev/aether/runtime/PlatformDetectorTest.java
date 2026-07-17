package dev.aether.runtime;

import dev.aether.TestSupport;

public final class PlatformDetectorTest {
    public static void main(String[] args) {
        PlatformInfo info = PlatformDetector.detect();

        TestSupport.assertTrue(info.name().length() > 0, "OS name should be detected.");
        TestSupport.assertTrue(info.architecture().length() > 0, "Architecture should be detected.");
        TestSupport.assertTrue(info.javaVersion().length() > 0, "Java version should be detected.");
    }
}

