package dev.aether.cosmetic;

import java.nio.file.Path;

/**
 * One selectable cosmetic entry.
 * <p>
 * Every asset carries the two colours the in-world renderer tints its geometry with, so a
 * built-in (which has no PNG behind it) still shows up as a real cape, wing pair, halo or
 * hat instead of a placeholder. Imported capes keep their own image and use the tint only
 * for geometry that has nothing to sample, such as a wing feather.
 */
public final class CosmeticAsset {
    private final String id;
    private final String name;
    private final CosmeticType type;
    private final Path localFile;
    private final boolean builtIn;
    private final int primaryColor;
    private final int secondaryColor;

    // New fields for animated cosmetics
    private final boolean animated;
    private final int frameCount;   // number of frames for animation (if animated)
    private final int frameRate;    // frames per second

    // Optional explicit preview image path (for gallery thumbnails)
    private final Path previewPath;

    // Runtime-only – not persisted – used to remember if this asset was marked favourite
    private boolean favorite;

    public CosmeticAsset(String id, String name, CosmeticType type, Path localFile, boolean builtIn) {
        this(id, name, type, localFile, builtIn,
             0xFF52BEEB, 0xFF20476B,
             false, 0, 0, null);
    }

    public CosmeticAsset(String id, String name, CosmeticType type, Path localFile, boolean builtIn,
                         int primaryColor, int secondaryColor) {
        this(id, name, type, localFile, builtIn,
             primaryColor, secondaryColor,
             false, 0, 0, null);
    }

    public CosmeticAsset(String id, String name, CosmeticType type, Path localFile, boolean builtIn,
                         int primaryColor, int secondaryColor,
                         boolean animated, int frameCount, int frameRate, Path previewPath) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Cosmetic id cannot be blank.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Cosmetic name cannot be blank.");
        }
        this.id = id;
        this.name = name;
        this.type = type;
        this.localFile = localFile;
        this.builtIn = builtIn;
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.animated = animated;
        this.frameCount = frameCount;
        this.frameRate = frameRate;
        this.previewPath = previewPath;
        this.favorite = false;
    }

    public String id() { return id; }
    public String name() { return name; }
    public CosmeticType type() { return type; }
    public Path localFile() { return localFile; }
    public boolean builtIn() { return builtIn; }
    public int primaryColor() { return primaryColor; }
    public int secondaryColor() { return secondaryColor; }

    public boolean animated() { return animated; }
    public int frameCount() { return frameCount; }
    public int frameRate() { return frameRate; }
    public Path previewPath() { return previewPath; }

    public boolean favorite() { return favorite; }
    public void setFavorite(boolean favorite) { this.favorite = favorite; }

    /** @return a copy with new tints; used by the built-in catalogue. */
    public CosmeticAsset colors(int primary, int secondary) {
        return new CosmeticAsset(id, name, type, localFile, builtIn,
                primary, secondary,
                animated, frameCount, frameRate, previewPath);
    }

    /** @return a copy of this asset for a different slot. */
    public CosmeticAsset asType(CosmeticType newType) {
        return new CosmeticAsset(id, name, newType, localFile, builtIn,
                primaryColor, secondaryColor,
                animated, frameCount, frameRate, previewPath);
    }
}