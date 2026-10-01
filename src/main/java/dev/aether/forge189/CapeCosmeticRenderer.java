package dev.aether.forge189;

import dev.aether.cosmetic.CosmeticAsset;
import dev.aether.cosmetic.CosmeticType;
import dev.aether.cosmetic.render.CosmeticRenderContext;
import dev.aether.cosmetic.render.CosmeticRenderer;
import dev.aether.module.impl.cosmetics.CapePreviewModule;
import dev.aether.module.impl.cosmetics.CurrentCapeModule;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/**
 * Draws the wearer's cape: a segmented cloth that sways with the wearer's speed, either
 * as an imported PNG sampled across the whole image or as procedural tinted geometry for
 * built-ins. The "cape preview" module flattens the cloth into a rigid board for
 * inspecting a skin upload, which is why its module id lives here too.
 * <p>
 * Body moved verbatim from the old monolithic world renderer; the only behavioural
 * addition is falling back to the animated-cape slot when the static slot is empty,
 * mirroring what the GUI preview already did.
 */
final class CapeCosmeticRenderer implements CosmeticRenderer {

    @Override
    public CosmeticType type() {
        return CosmeticType.STATIC_CAPE;
    }

    @Override
    public String moduleId() {
        return CurrentCapeModule.ID;
    }

    @Override
    public void render(CosmeticAsset cape, CosmeticRenderContext context) {
        if (cape == null) {
            cape = context.effective(CosmeticType.ANIMATED_CAPE);
        }
        if (cape == null) {
            return;
        }
        double seconds = context.seconds();
        boolean flat = context.enabled(CapePreviewModule.ID);
        double opacity = CosmeticRenderContext.clamp(
            context.settingInt(CurrentCapeModule.ID, "opacity", 90), 0, 100) / 100.0D;
        double wave = flat ? 0.0D : CosmeticRenderContext.clamp(
            context.settingInt(CurrentCapeModule.ID, "wave", 50), 0, 100) / 100.0D;
        double length = CosmeticRenderContext.PLAYER_HEIGHT * 0.44D
            * CosmeticRenderContext.clamp(context.settingInt(CurrentCapeModule.ID, "length", 100), 50, 150) / 100.0D;
        if (flat) {
            length *= CosmeticRenderContext.clamp(
                context.settingInt(CapePreviewModule.ID, "scale", 100), 50, 200) / 100.0D;
        }
        double swing = !flat && context.settingBool(CurrentCapeModule.ID, "swing", true)
            ? Math.min(0.35D, context.speed() * 1.8D) : 0.0D;
        int segments = flat ? 1 : 6;

        double shoulder = CosmeticRenderContext.PLAYER_HEIGHT * 0.80D;
        double halfWidth = CosmeticRenderContext.PLAYER_WIDTH * (flat ? 0.62D : 0.52D);
        double segment = length / segments;
        double amplitude = 0.06D * wave;
        double baseZ = -0.16D;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        Integer texture = cape.localFile() == null ? null : context.textureFor(cape);

        if (texture != null) {
            // Imported cape: sample the whole PNG across the cloth, one quad per segment.
            Mc189Compat.bindTexture(texture.intValue());
            Mc189Compat.color(1.0F, 1.0F, 1.0F, (float) opacity);
            renderer.begin(CosmeticGeometry.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            for (int i = 0; i < segments; i++) {
                double y0 = shoulder - i * segment + swing * i * 0.03D;
                double y1 = shoulder - (i + 1) * segment + swing * (i + 1) * 0.03D;
                double z0 = baseZ - sway(i, seconds, amplitude) - swing * i * 0.12D;
                double z1 = baseZ - sway(i + 1, seconds, amplitude) - swing * (i + 1) * 0.12D;
                double w0 = halfWidth * (1.0D - 0.06D * i);
                double w1 = halfWidth * (1.0D - 0.06D * (i + 1));
                double u0 = (double) i / segments;
                double u1 = (double) (i + 1) / segments;
                renderer.pos(-w0, y0, z0).tex(u0, 0.0D).endVertex();
                renderer.pos(w0, y0, z0).tex(u1, 0.0D).endVertex();
                renderer.pos(w1, y1, z1).tex(u1, 1.0D).endVertex();
                renderer.pos(-w1, y1, z1).tex(u0, 1.0D).endVertex();
            }
            tessellator.draw();
            Mc189Compat.resetColor();
            return;
        }

        // Built-in cape: procedural cloth tinted from the asset palette.
        renderer.begin(CosmeticGeometry.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < segments; i++) {
            double y0 = shoulder - i * segment + swing * i * 0.03D;
            double y1 = shoulder - (i + 1) * segment + swing * (i + 1) * 0.03D;
            double z0 = baseZ - sway(i, seconds, amplitude) - swing * i * 0.12D;
            double z1 = baseZ - sway(i + 1, seconds, amplitude) - swing * (i + 1) * 0.12D;
            double w0 = halfWidth * (1.0D - 0.06D * i);
            double w1 = halfWidth * (1.0D - 0.06D * (i + 1));
            int top = CosmeticRenderContext.modulate(cape.primaryColor(), opacity * (1.0D - 0.08D * i));
            int bottom = CosmeticRenderContext.modulate(cape.secondaryColor(), opacity * (1.0D - 0.08D * (i + 1)));
            CosmeticGeometry.vertex(renderer, -w0, y0, z0, top);
            CosmeticGeometry.vertex(renderer, w0, y0, z0, top);
            CosmeticGeometry.vertex(renderer, w1, y1, z1, bottom);
            CosmeticGeometry.vertex(renderer, -w1, y1, z1, bottom);
        }
        tessellator.draw();
    }

    private static double sway(double index, double seconds, double amplitude) {
        if (amplitude <= 0.0D) {
            return 0.0D;
        }
        return amplitude * (Math.sin(seconds * 2.1D + index * 0.7D) + 0.5D * Math.sin(seconds * 3.7D + index * 0.4D));
    }
}
