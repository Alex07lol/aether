package dev.aether.forge189;

import dev.aether.cosmetic.CosmeticAsset;
import dev.aether.cosmetic.CosmeticType;
import dev.aether.cosmetic.render.CosmeticRenderContext;
import dev.aether.cosmetic.render.CosmeticRenderer;
import dev.aether.module.impl.cosmetics.CurrentWingsModule;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

/**
 * Draws the wearer's wings: three tapered feathers per side that spread wider with the
 * configured spread and flap on a sine driven by the frame time. Body moved verbatim
 * from the old monolithic world renderer.
 */
final class WingsCosmeticRenderer implements CosmeticRenderer {

    @Override
    public CosmeticType type() {
        return CosmeticType.WINGS;
    }

    @Override
    public String moduleId() {
        return CurrentWingsModule.ID;
    }

    @Override
    public void render(CosmeticAsset wings, CosmeticRenderContext context) {
        if (wings == null) {
            return;
        }
        double seconds = context.seconds();
        double opacity = CosmeticRenderContext.clamp(
            context.settingInt(CurrentWingsModule.ID, "opacity", 85), 0, 100) / 100.0D;
        double spread = CosmeticRenderContext.clamp(
            context.settingInt(CurrentWingsModule.ID, "spread", 45), 10, 80);
        double flapRate = CosmeticRenderContext.clamp(
            context.settingInt(CurrentWingsModule.ID, "flap_speed", 50), 0, 100) / 100.0D;
        double size = CosmeticRenderContext.clamp(
            context.settingInt(CurrentWingsModule.ID, "size", 100), 50, 200) / 100.0D;
        double flap = Math.sin(seconds * (1.0D + 3.0D * flapRate)) * (5.0D + 15.0D * flapRate);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(CosmeticGeometry.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int side = -1; side <= 1; side += 2) {
            drawWing(renderer, side, spread + flap, size, wings, opacity);
        }
        tessellator.draw();
    }

    /** Draws three tapered feathers for one side; each feather is a plate facing front/back. */
    private static void drawWing(WorldRenderer renderer, int side, double angleDegrees, double size,
                                 CosmeticAsset wings, double opacity) {
        double shoulder = CosmeticRenderContext.PLAYER_HEIGHT * 0.78D;
        double length = 0.72D * size;
        double halfWidth = 0.20D * size;
        double baseX = side * 0.10D;
        double baseZ = -0.20D;

        for (int feather = 0; feather < 3; feather++) {
            double tilt = Math.toRadians(angleDegrees + feather * 7.0D - 7.0D);
            double directionX = side * Math.cos(tilt);
            double directionY = Math.sin(tilt);
            double perpendicularX = -directionY;
            double perpendicularY = directionX;
            double featherLength = length * (1.0D - feather * 0.18D);
            double featherWidth = halfWidth * (1.0D - feather * 0.15D);
            double rootX = baseX;
            double rootY = shoulder - feather * 0.05D;
            double plane = baseZ + feather * 0.06D;
            int rootColor = CosmeticRenderContext.modulate(wings.primaryColor(), opacity);
            int tipColor = CosmeticRenderContext.modulate(wings.secondaryColor(), opacity * 0.25D);

            CosmeticGeometry.vertex(renderer, rootX - perpendicularX * featherWidth,
                rootY - perpendicularY * featherWidth, plane, rootColor);
            CosmeticGeometry.vertex(renderer, rootX + perpendicularX * featherWidth,
                rootY + perpendicularY * featherWidth, plane, rootColor);
            CosmeticGeometry.vertex(renderer, rootX + directionX * featherLength + perpendicularX * featherWidth * 0.2D,
                rootY + directionY * featherLength + perpendicularY * featherWidth * 0.2D, plane, tipColor);
            CosmeticGeometry.vertex(renderer, rootX + directionX * featherLength - perpendicularX * featherWidth * 0.2D,
                rootY + directionY * featherLength - perpendicularY * featherWidth * 0.2D, plane, tipColor);
        }
    }
}
