package dev.aether.forge189;

import dev.aether.cosmetic.CosmeticAsset;
import dev.aether.cosmetic.CosmeticType;
import dev.aether.cosmetic.render.CosmeticRenderContext;
import dev.aether.cosmetic.render.CosmeticRenderer;
import dev.aether.module.impl.cosmetics.CurrentHatModule;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/**
 * Draws the wearer's hat: a tilted tapered crown with an optional brim, built from a
 * strip, a fan cap and a brim ring. Body moved verbatim from the old monolithic world
 * renderer.
 */
final class HatCosmeticRenderer implements CosmeticRenderer {

    @Override
    public CosmeticType type() {
        return CosmeticType.HAT;
    }

    @Override
    public String moduleId() {
        return CurrentHatModule.ID;
    }

    @Override
    public void render(CosmeticAsset hat, CosmeticRenderContext context) {
        if (hat == null) {
            return;
        }
        double radius = CosmeticRenderContext.PLAYER_WIDTH * 0.42D
            * CosmeticRenderContext.clamp(context.settingInt(CurrentHatModule.ID, "radius", 100), 50, 150) / 100.0D;
        double height = 0.16D
            * CosmeticRenderContext.clamp(context.settingInt(CurrentHatModule.ID, "height", 100), 50, 200) / 100.0D;
        double tilt = CosmeticRenderContext.clamp(context.settingInt(CurrentHatModule.ID, "tilt", 0), 0, 30);
        boolean brim = context.settingBool(CurrentHatModule.ID, "brim", true);
        int crown = hat.primaryColor();
        int crownTop = CosmeticRenderContext.modulate(hat.primaryColor(), 0.85D);
        int band = hat.secondaryColor();

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        Mc189Compat.pushMatrix();
        try {
            Mc189Compat.translate(0.0F, (float) (CosmeticRenderContext.PLAYER_HEIGHT - 0.14D), 0.0F);
            Mc189Compat.rotate((float) tilt, 0.0F, 0.0F, 1.0F);

            // Crown: a tapered tube plus a closed top so the hat reads as solid from below.
            renderer.begin(CosmeticGeometry.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i <= 16; i++) {
                double angle = i / 16.0D * Math.PI * 2.0D;
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                CosmeticGeometry.vertex(renderer, cos * radius, 0.0D, sin * radius, band);
                CosmeticGeometry.vertex(renderer, cos * radius * 0.92D, height, sin * radius * 0.92D, crown);
            }
            tessellator.draw();

            renderer.begin(CosmeticGeometry.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
            CosmeticGeometry.vertex(renderer, 0.0D, height, 0.0D, crownTop);
            for (int i = 0; i <= 16; i++) {
                double angle = i / 16.0D * Math.PI * 2.0D;
                CosmeticGeometry.vertex(renderer, Math.cos(angle) * radius * 0.92D, height,
                    Math.sin(angle) * radius * 0.92D, crownTop);
            }
            tessellator.draw();

            if (brim) {
                renderer.begin(CosmeticGeometry.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
                for (int i = 0; i <= 16; i++) {
                    double angle = i / 16.0D * Math.PI * 2.0D;
                    double cos = Math.cos(angle);
                    double sin = Math.sin(angle);
                    CosmeticGeometry.vertex(renderer, cos * radius * 1.05D, 0.005D, sin * radius * 1.05D, crown);
                    CosmeticGeometry.vertex(renderer, cos * radius * 1.55D, 0.0D, sin * radius * 1.55D, band);
                }
                tessellator.draw();
            }
        } finally {
            Mc189Compat.popMatrix();
        }
    }
}
