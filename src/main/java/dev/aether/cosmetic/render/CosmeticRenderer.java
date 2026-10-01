package dev.aether.cosmetic.render;

import dev.aether.cosmetic.CosmeticAsset;
import dev.aether.cosmetic.CosmeticType;

/**
 * Draws one cosmetic slot's asset onto a wearer.
 * <p>
 * The world renderer used to be a single 600-line class with a {@code drawCape},
 * {@code drawWings}, {@code drawHalo} and {@code drawHat} method each knowing the module
 * ids, setting keys and colour maths of its slot. Splitting those methods into small
 * implementations of this interface is the cleanup: the Forge-side class keeps everything
 * that is genuinely shared (tick motion, ribbon history, the PNG texture cache, wearer
 * placement) and delegates each slot to its own renderer, so adding a cosmetic type means
 * adding a class and registering it - not growing one class forever.
 * <p>
 * The interface is Minecraft-free on purpose: implementations live where MC classes are
 * allowed (the Forge integration package), but the contract itself only speaks Aether's
 * own types, so it can be enumerated and asserted by a headless test.
 */
public interface CosmeticRenderer {

    /** The slot this renderer draws. */
    CosmeticType type();

    /** The module whose enabled state gates this renderer. */
    String moduleId();

    /**
     * Draws {@code asset} in the current wearer-local space (already translated to the
     * wearer and rotated by body yaw). {@code asset} is never null - the caller resolves
     * the effective asset first - but a renderer may still return early when there is
     * nothing sensible to draw.
     */
    void render(CosmeticAsset asset, CosmeticRenderContext context);
}
