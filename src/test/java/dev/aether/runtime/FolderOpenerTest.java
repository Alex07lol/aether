package dev.aether.runtime;

import dev.aether.TestSupport;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class FolderOpenerTest {
    public static void main(String[] args) {
        Path spaced = Paths.get("some", "cosmetics folder");

        List<String> windows = FolderOpener.commandFor(OperatingSystem.WINDOWS, spaced);
        TestSupport.assertEquals("explorer", windows.get(0), "Windows should use explorer.");
        TestSupport.assertEquals(2, windows.size(), "Explorer takes exactly the path argument.");
        TestSupport.assertEquals(spaced.toString(), windows.get(1),
                "The path must stay one argument even with spaces.");

        List<String> macos = FolderOpener.commandFor(OperatingSystem.MACOS, spaced);
        TestSupport.assertEquals("open", macos.get(0), "macOS should use open.");
        TestSupport.assertEquals(spaced.toString(), macos.get(1), "macOS path stays one argument.");

        List<String> linux = FolderOpener.commandFor(OperatingSystem.LINUX, spaced);
        TestSupport.assertEquals("xdg-open", linux.get(0), "Linux should use xdg-open.");

        List<String> unknown = FolderOpener.commandFor(OperatingSystem.UNKNOWN, spaced);
        TestSupport.assertEquals("xdg-open", unknown.get(0),
                "Unknown platforms fall back to the FreeDesktop opener.");

        List<String> nullOs = FolderOpener.commandFor(null, spaced);
        TestSupport.assertEquals("xdg-open", nullOs.get(0), "A null OS must not throw.");
    }
}
