package dev.aether.cosmetic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * The optional {@code <name>.png.meta} sidecar that travels with a cosmetic PNG.
 * <p>
 * Two syntaxes are understood on every non-blank, non-comment line:
 * <pre>
 *   type=wings              # canonical key=value form
 *   frames: 8               # legacy prefix form shipped by older builds
 * </pre>
 * Recognised keys: {@code type}, {@code name}, {@code scale}, {@code offset_x/y/z},
 * {@code rotation_x/y/z}, {@code animation}, {@code frames}, {@code fps}. Unrecognised
 * keys and malformed numbers are ignored so a hand-edited file never breaks the scan.
 * Reading never throws: a missing or garbled sidecar yields an empty instance.
 */
public final class CosmeticMetadata {

    private static final CosmeticMetadata EMPTY =
            new CosmeticMetadata(null, null, null, null, null, null, null, null, null, null, -1, -1);

    private final CosmeticType type;
    private final String name;
    private final Double scale;
    private final Double offsetX;
    private final Double offsetY;
    private final Double offsetZ;
    private final Double rotationX;
    private final Double rotationY;
    private final Double rotationZ;
    private final String animation;
    private final int frameCount;
    private final int frameRate;

    private CosmeticMetadata(CosmeticType type, String name, Double scale,
                             Double offsetX, Double offsetY, Double offsetZ,
                             Double rotationX, Double rotationY, Double rotationZ,
                             String animation, int frameCount, int frameRate) {
        this.type = type;
        this.name = name;
        this.scale = scale;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.rotationX = rotationX;
        this.rotationY = rotationY;
        this.rotationZ = rotationZ;
        this.animation = animation;
        this.frameCount = frameCount;
        this.frameRate = frameRate;
    }

    /** @return the sidecar for {@code png}, or an empty instance when absent/unreadable. */
    public static CosmeticMetadata read(Path png) {
        if (png == null) return EMPTY;
        Path meta = sidecarFor(png);
        if (meta == null || !Files.isRegularFile(meta)) return EMPTY;
        try {
            return parse(Files.readAllLines(meta));
        } catch (IOException | RuntimeException ignored) {
            return EMPTY;
        }
    }

    /** @return the sidecar path conventionally used for a cosmetic PNG. */
    public static Path sidecarFor(Path png) {
        if (png == null || png.getFileName() == null) return null;
        return png.resolveSibling(png.getFileName().toString() + ".meta");
    }

    static CosmeticMetadata parse(List<String> lines) {
        CosmeticType type = null;
        String name = null;
        Double scale = null;
        Double offsetX = null, offsetY = null, offsetZ = null;
        Double rotationX = null, rotationY = null, rotationZ = null;
        String animation = null;
        int frameCount = -1;
        int frameRate = -1;

        for (String raw : lines) {
            String line = raw == null ? "" : raw.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) continue;

            int sep = indexOfSeparator(line);
            if (sep < 0) continue;
            String key = line.substring(0, sep).trim().toLowerCase(Locale.ENGLISH).replace('-', '_');
            String value = line.substring(sep + 1).trim();
            if (key.isEmpty() || value.isEmpty()) continue;
            // Strip a trailing comment from the value ("8  # frames").
            int hash = value.indexOf('#');
            if (hash >= 0) value = value.substring(0, hash).trim();
            if (value.isEmpty()) continue;

            switch (key) {
                case "type":
                    CosmeticType parsed = typeOf(value);
                    if (parsed != null) type = parsed;
                    break;
                case "name":
                    name = value;
                    break;
                case "scale":
                    scale = parseDouble(value, scale);
                    break;
                case "offset_x": offsetX = parseDouble(value, offsetX); break;
                case "offset_y": offsetY = parseDouble(value, offsetY); break;
                case "offset_z": offsetZ = parseDouble(value, offsetZ); break;
                case "rotation_x": rotationX = parseDouble(value, rotationX); break;
                case "rotation_y": rotationY = parseDouble(value, rotationY); break;
                case "rotation_z": rotationZ = parseDouble(value, rotationZ); break;
                case "animation": animation = value; break;
                case "frames": frameCount = parseInt(value, frameCount); break;
                case "fps": frameRate = parseInt(value, frameRate); break;
                default:
                    break;
            }
        }
        return new CosmeticMetadata(type, name, scale, offsetX, offsetY, offsetZ,
                rotationX, rotationY, rotationZ, animation, frameCount, frameRate);
    }

    private static int indexOfSeparator(String line) {
        int eq = line.indexOf('=');
        int colon = line.indexOf(':');
        if (eq < 0) return colon;
        if (colon < 0) return eq;
        return Math.min(eq, colon);
    }

    private static CosmeticType typeOf(String value) {
        String normalised = value.trim().toLowerCase(Locale.ENGLISH).replace('-', '_').replace(' ', '_');
        for (CosmeticType candidate : CosmeticType.values()) {
            if (candidate.name().toLowerCase(Locale.ENGLISH).equals(normalised)) return candidate;
        }
        // Convenience aliases for the core types.
        switch (normalised) {
            case "cape": return CosmeticType.STATIC_CAPE;
            case "wings": return CosmeticType.WINGS;
            case "hat": return CosmeticType.HAT;
            case "halo": return CosmeticType.HALO;
            case "trail": return CosmeticType.TRAIL;
            default: return null;
        }
    }

    private static Double parseDouble(String value, Double fallback) {
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    /** @return the slot this sidecar claims for the file; null to defer to the folder. */
    public CosmeticType type() { return type; }

    /** @return a display name override; null to use the file name. */
    public String name() { return name; }

    /** @return render scale override; null when unset. */
    public Double scale() { return scale; }

    public Double offsetX() { return offsetX; }
    public Double offsetY() { return offsetY; }
    public Double offsetZ() { return offsetZ; }
    public Double rotationX() { return rotationX; }
    public Double rotationY() { return rotationY; }
    public Double rotationZ() { return rotationZ; }

    /** @return the raw {@code animation} value; null when the key is absent. */
    public String animation() { return animation; }

    /** @return declared frame count, or -1 when unset. */
    public int frameCount() { return frameCount; }

    /** @return declared frames-per-second, or -1 when unset. */
    public int frameRate() { return frameRate; }

    /**
     * @return true when this sidecar explicitly marks the file animated — an
     * {@code animation} flag that is not an explicit "false", or any declared
     * frame count / frame rate (the legacy animating signals).
     */
    public boolean animated() {
        if (frameCount > 0 || frameRate > 0) return true;
        if (animation == null) return false;
        String value = animation.trim().toLowerCase(Locale.ENGLISH);
        return !(value.isEmpty() || value.equals("false") || value.equals("no") || value.equals("off") || value.equals("0"));
    }

    public boolean isEmpty() { return this == EMPTY; }
}
