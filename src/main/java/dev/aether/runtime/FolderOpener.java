package dev.aether.runtime;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Opens a directory in the OS file manager, without any per-screen shelling-out.
 * <p>
 * The command is built from the detected {@link OperatingSystem} and the path is passed
 * as a single argument (no shell string concatenation), so folders with spaces or odd
 * characters are safe. The process is spawned detached: the caller never waits on it,
 * because file managers routinely outlive (and out-status) the request that opened them.
 */
public final class FolderOpener {

    private FolderOpener() {
    }

    /**
     * @return the argv that reveals {@code directory} on {@code os} —
     * {@code explorer} on Windows, {@code open} on macOS, {@code xdg-open} on Linux and
     * anywhere else (UNKNOWN falls back to the FreeDesktop opener, which is the only
     * sane guess). The path is always one argument.
     */
    public static List<String> commandFor(OperatingSystem os, Path directory) {
        String target = directory == null ? "" : directory.toString();
        String launcher;
        switch (os == null ? OperatingSystem.UNKNOWN : os) {
            case WINDOWS:
                launcher = "explorer";
                break;
            case MACOS:
                launcher = "open";
                break;
            case LINUX:
            case UNKNOWN:
            default:
                launcher = "xdg-open";
                break;
        }
        List<String> command = new ArrayList<String>(2);
        command.add(launcher);
        command.add(target);
        return Collections.unmodifiableList(command);
    }

    /**
     * Ensure {@code directory} exists, then ask the OS to reveal it.
     *
     * @return true when the directory exists and the launcher process was started.
     */
    public static boolean open(Path directory) {
        if (directory == null) return false;
        try {
            Files.createDirectories(directory);
        } catch (IOException ignored) {
            return false;
        }
        if (!Files.isDirectory(directory)) return false;
        try {
            ProcessBuilder builder = new ProcessBuilder(commandFor(PlatformDetector.detect().operatingSystem(), directory));
            // Redirect to the platform null device (Java 8 has no Redirect.DISCARD):
            // a detached launcher must never block the game on a full pipe.
            File nullDevice = new File(System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).contains("windows") ? "NUL" : "/dev/null");
            builder.redirectOutput(nullDevice);
            builder.redirectError(nullDevice);
            builder.start();
            return true;
        } catch (IOException | RuntimeException ignored) {
            return false;
        }
    }
}
