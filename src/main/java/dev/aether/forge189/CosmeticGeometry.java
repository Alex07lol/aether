package dev.aether.forge189;

import dev.aether.cosmetic.render.CosmeticRenderContext;
import net.minecraft.client.renderer.WorldRenderer;

/**
 * The vertex-level primitives the per-type cosmetic renderers share: the immediate-mode
 * mode constants and the ARGB vertex writer. Kept package-private in the Forge integration
 * package because {@link WorldRenderer} is a Minecraft class - the render contract itself
 * ({@code CosmeticRenderer}) stays Minecraft-free.
 */
final class CosmeticGeometry {

    static final int GL_TRIANGLE_STRIP = 5;
    static final int GL_TRIANGLE_FAN = 6;
    static final int GL_QUADS = 7;

    private CosmeticGeometry() {
    }

    /** Writes one vertex in wearer-local space with an ARGB colour. */
    static void vertex(WorldRenderer renderer, double x, double y, double z, int argb) {
        renderer.pos(x, y, z)
            .color((argb >> 16 & 255) / 255.0F, (argb >> 8 & 255) / 255.0F, (argb & 255) / 255.0F,
                (argb >>> 24) / 255.0F)
            .endVertex();
    }

    static int modulate(int color, double alphaScale) {
        return CosmeticRenderContext.modulate(color, alphaScale);
    }
}
