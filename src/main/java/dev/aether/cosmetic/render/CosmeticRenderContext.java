package dev.aether.cosmetic.render;

import dev.aether.cosmetic.CosmeticAsset;
import dev.aether.cosmetic.CosmeticType;

/**
 * Everything a {@link CosmeticRenderer} needs from the outside world, gathered once per
 * frame by the world renderer: frame time, the wearer's smoothed speed, module settings,
 * the equipped asset for a slot, and the uploaded texture for a file-backed cosmetic.
 * <p>
 * The three access interfaces are what keep this class Minecraft-free - the Forge-side
 * renderer implements them over its own client/module/texture-cache access, and the
 * per-type renderers only ever see this context. The same instance is reused every frame
 * ({@link #beginFrame}) so a busy scene does not allocate one context per wearer.
 * <p>
 * The shared colour maths ({@link #modulate}, {@link #mix}) and the wearer geometry
 * constants live here too: they were private helpers of the old monolithic renderer and
 * every per-type renderer needs them.
 */
public final class CosmeticRenderContext {

    /** Vanilla player geometry, shared by every slot's placement maths. */
    public static final double PLAYER_HEIGHT = 1.8D;
    public static final double PLAYER_WIDTH = 0.6D;

    /** Module state and settings, as seen by the renderers. */
    public interface Settings {
        boolean enabled(String moduleId);

        boolean settingBool(String moduleId, String key, boolean fallback);

        int settingInt(String moduleId, String key, int fallback);

        String settingString(String moduleId, String key, String fallback);
    }

    /** Resolves the effective (selected-or-fallback) asset for a slot. */
    public interface AssetSource {
        CosmeticAsset effective(CosmeticType type);
    }

    /** Returns the uploaded GL texture for a file-backed asset, or null. */
    public interface TextureSource {
        Integer textureFor(CosmeticAsset asset);
    }

    private final Settings settings;
    private final AssetSource assets;
    private final TextureSource textures;

    private double seconds;
    private double speed;

    public CosmeticRenderContext(Settings settings, AssetSource assets, TextureSource textures) {
        if (settings == null || assets == null || textures == null) {
            throw new IllegalArgumentException("Render context requires all three sources.");
        }
        this.settings = settings;
        this.assets = assets;
        this.textures = textures;
    }

    /** Updates the per-frame values; returns this so a caller can chain. */
    public CosmeticRenderContext beginFrame(double seconds, double speed) {
        this.seconds = seconds;
        this.speed = speed;
        return this;
    }

    /** Wall-clock seconds for idle animations (halo bob, cape sway, wing flap). */
    public double seconds() {
        return seconds;
    }

    /** The wearer's smoothed blocks-per-tick, for the cape swing. */
    public double speed() {
        return speed;
    }

    public boolean enabled(String moduleId) {
        return settings.enabled(moduleId);
    }

    public boolean settingBool(String moduleId, String key, boolean fallback) {
        return settings.settingBool(moduleId, key, fallback);
    }

    public int settingInt(String moduleId, String key, int fallback) {
        return settings.settingInt(moduleId, key, fallback);
    }

    public String settingString(String moduleId, String key, String fallback) {
        return settings.settingString(moduleId, key, fallback);
    }

    public CosmeticAsset effective(CosmeticType type) {
        return assets.effective(type);
    }

    public Integer textureFor(CosmeticAsset asset) {
        return textures.textureFor(asset);
    }

    /* ── shared colour maths (moved from the old monolithic renderer) ─────── */

    /** Scales a colour's alpha, clamped to the valid range. */
    public static int modulate(int color, double alphaScale) {
        int alpha = (int) Math.round((color >>> 24 & 255) * Math.max(0.0D, Math.min(1.0D, alphaScale)));
        return (color & 0xFFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    /** Linearly interpolates two ARGB colours. */
    public static int mix(int from, int to, double fraction) {
        double amount = Math.max(0.0D, Math.min(1.0D, fraction));
        int alpha = (int) Math.round((from >>> 24 & 255) + ((to >>> 24 & 255) - (from >>> 24 & 255)) * amount);
        int red = (int) Math.round((from >> 16 & 255) + ((to >> 16 & 255) - (from >> 16 & 255)) * amount);
        int green = (int) Math.round((from >> 8 & 255) + ((to >> 8 & 255) - (from >> 8 & 255)) * amount);
        int blue = (int) Math.round((from & 255) + ((to & 255) - (from & 255)) * amount);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    /** Clamps a value into {@code [min, max]}. */
    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
