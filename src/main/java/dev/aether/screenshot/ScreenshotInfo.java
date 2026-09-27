package dev.aether.screenshot;

/**
 * The metadata for one screenshot on disk: its name, size in bytes and last-modified time.
 * <p>
 * A value object because the menu lists screenshots and the store sorts and returns them; the
 * pixels themselves are never loaded to show this list.
 */
public final class ScreenshotInfo {
    private final String name;
    private final long sizeBytes;
    private final long lastModified;

    public ScreenshotInfo(String name, long sizeBytes, long lastModified) {
        this.name = name;
        this.sizeBytes = sizeBytes;
        this.lastModified = lastModified;
    }
    /** The file name, e.g. {@code aether_2026-09-27_12-00-00.png}. */
    public String name() {
        return this.name;
    }

    public long sizeBytes() {
        return this.sizeBytes;
    }

    public long lastModified() {
        return this.lastModified;
    }

    /** @return the modification time formatted as {@code yyyy-MM-dd HH:mm}; never throws. */
    public String timestamp() {
        try {
            return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date(this.lastModified));
        } catch (RuntimeException ignored) {
            return "";
        }
    }
}
