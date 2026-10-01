package dev.aether.forge189;

import dev.aether.cosmetic.CosmeticAsset;
import dev.aether.cosmetic.CosmeticType;
import dev.aether.cosmetic.render.CosmeticRenderContext;
import dev.aether.cosmetic.render.CosmeticRenderer;
import dev.aether.module.impl.cosmetics.CurrentHaloModule;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/**
 * Draws the wearer's halo: a ring above the head that bobs on a sine, optionally
 * composited additively so it reads as light rather than painted geometry. Body moved
 * verbatim from the old monolithic world renderer.
 */
final class HaloCosmeticRenderer implements CosmeticRenderer {

    @Override
    public CosmeticType type() {
        return CosmeticType.HALO;
    }

    @Override
    public String moduleId() {
        return CurrentHaloModule.ID;
    }

    @Override
    public void render(CosmeticAsset halo, CosmeticRenderContext context) {
        if (halo == null) {
            return;
        }
        double seconds = context.seconds();
        double opacity = CosmeticRenderContext.clamp(
            context.settingInt(CurrentHaloModule.ID, "opacity", 90), 0, 100) / 100.0D;
        double radius = 0.10D + 0.30D * CosmeticRenderContext.clamp(
            context.settingInt(CurrentHaloModule.ID, "radius", 30), 10, 80) / 100.0D;
        boolean glow = context.settingBool(CurrentHaloModule.ID, "glow", true);
        boolean bob = context.settingBool(CurrentHaloModule.ID, "bob", true);
        double y = CosmeticRenderContext.PLAYER_HEIGHT + 0.10D
            + (bob ? 0.035D * Math.sin(seconds * 2.0D) : 0.0D);
        int inner = CosmeticRenderContext.modulate(halo.primaryColor(), opacity * 0.55D);
        int outer = CosmeticRenderContext.modulate(halo.secondaryColor(), opacity);

        if (glow) {
            // Additive: the ring reads as light rather than as painted geometry.
            Mc189Compat.tryBlendFuncSeparate(770, 1, 1, 0);
        }
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(CosmeticGeometry.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= 32; i++) {
            double angle = i / 32.0D * Math.PI * 2.0D;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            CosmeticGeometry.vertex(renderer, cos * radius * 0.72D, y, sin * radius * 0.72D, inner);
            CosmeticGeometry.vertex(renderer, cos * radius, y, sin * radius, outer);
        }
        tessellator.draw();
        if (glow) {
            Mc189Compat.tryBlendFuncSeparate(770, 771, 1, 0);
        }
    }
}
