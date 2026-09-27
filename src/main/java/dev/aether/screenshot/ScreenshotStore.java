package dev.aether.screenshot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The screenshots directory: listing, safe naming and deletion.
 * <p>
 * Screenshots live under the game directory and are plain PNG files, so they outlive the client
 * and can be opened with any image viewer. Every path this class hands out is resolved inside the
 * directory: a name that tries to escape it (path separators, {@code ..}) is rejected rather than
 * resolved, because names end up in a GUI driven by user data.
 * <p>
 * Listing reads only file metadata - names, sizes, timestamps - never pixel data. Decoding an
 * image is the expensive part and belongs to whatever UI actually shows a preview, not to a list
 * that a menu refreshes.
 */
public final class ScreenshotStore {
    private final Path directory;

    public ScreenshotStore(Path directory) {
        this.directory = directory;
    }

    public Path directory() {
        return this.directory;
    }

    /** Creates the directory when missing; an existing file in its place is reported, not thrown over. */
    public boolean ensureDirectory() {
        File file = this.directory.toFile();
        if (file.isDirectory() || file.mkdirs()) {
            return true;
        }
        return file.isDirectory();
    }

    /**
     * @return the screenshots, newest first. Metadata only; an unreadable directory comes back
     *     empty instead of failing a render pass.
     */
    public List<ScreenshotInfo> list() {
        File[] files = this.directory.toFile().listFiles();
        if (files == null) {
            return Collections.emptyList();
        }
        List<ScreenshotInfo> shots = new ArrayList<ScreenshotInfo>();
        for (File file : files) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".png")) {
                shots.add(new ScreenshotInfo(file.getName(), file.length(), file.lastModified()));
            }
        }
        Collections.sort(shots, new Comparator<ScreenshotInfo>() {
            public int compare(ScreenshotInfo a, ScreenshotInfo b) {
                // Newest first; names break ties so the order is stable across calls.
                if (a.lastModified() != b.lastModified()) {
                    return a.lastModified() > b.lastModified() ? -1 : 1;
                }
                return a.name().compareTo(b.name());
            }
        });
        return Collections.unmodifiableList(shots);
    }

    /** @return a timestamped file that does not collide with an existing screenshot. */
    public Path newScreenshotFile() {
        String base = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
        for (int attempt = 0; attempt < 100; attempt++) {
            String name = attempt == 0 ? "aether_" + base + ".png" : "aether_" + base + "_" + attempt + ".png";
            Path candidate = safeResolve(name);
            if (candidate != null && !candidate.toFile().exists()) {
                return candidate;
            }
        }
        return this.directory.resolve("aether_" + System.nanoTime() + ".png");
    }

    /** @return the path for {@code name} when it stays inside the store, otherwise {@code null}. */
    public Path safeResolve(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0 || name.contains("..")) {
            return null;
        }
        Path resolved = this.directory.resolve(name).normalize();
        return resolved.startsWith(this.directory) ? resolved : null;
    }

    /** @return true when the screenshot was deleted; unknown or unsafe names are a quiet no. */
    public boolean delete(String name) {
        Path target = safeResolve(name);
        if (target == null) {
            return false;
        }
        File file = target.toFile();
        return file.isFile() && file.delete();
    }

    /** Creates a PNG at {@code target} from ARGB pixels. Package-private so tests can drive it. */
    static void writePng(int[] argb, int width, int height, Path target) throws IOException {
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
            width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, width, height, argb, 0, width);
        java.io.File file = target.toFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Could not create " + parent);
        }
        javax.imageio.ImageIO.write(image, "png", file);
    }
}
