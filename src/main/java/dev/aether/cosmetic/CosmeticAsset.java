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

    public CosmeticAsset(String id, String name, CosmeticType type, Path localFile, boolean builtIn) {
        this(id, name, type, localFile, builtIn, 0xFF52BEEB, 0xFF20476B);
    }

    public CosmeticAsset(String id, String name, CosmeticType type, Path localFile, boolean builtIn,
                         int primaryColor, int secondaryColor) {
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
    }

    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public CosmeticType type() {
        return type;
    }

    public Path localFile() {
        return localFile;
    }

    public boolean builtIn() {
        return builtIn;
    }

    /** @return the ARGB tint used for the main body of this cosmetic. */
    public int primaryColor() {
        return primaryColor;
    }

    /** @return the ARGB tint used for the shaded end of this cosmetic. */
    public int secondaryColor() {
        return secondaryColor;
    }

    /** @return a copy with new tints; used by the built-in catalogue. */
    public CosmeticAsset colors(int primary, int secondary) {
        return new CosmeticAsset(id, name, type, localFile, builtIn, primary, secondary);
    }

    /** @return a copy of this asset for a different slot. */
    public CosmeticAsset asType(CosmeticType newType) {
        return new CosmeticAsset(id, name, newType, localFile, builtIn, primaryColor, secondaryColor);
    }
}
