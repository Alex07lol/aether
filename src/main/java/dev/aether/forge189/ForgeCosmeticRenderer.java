package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.cosmetic.CosmeticAsset;
import dev.aether.cosmetic.CosmeticType;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.module.impl.cosmetics.CapePreviewModule;
import dev.aether.module.impl.cosmetics.CurrentCapeModule;
import dev.aether.module.impl.cosmetics.CurrentHaloModule;
import dev.aether.module.impl.cosmetics.CurrentHatModule;
import dev.aether.module.impl.cosmetics.CurrentTrailModule;
import dev.aether.module.impl.cosmetics.CurrentWingsModule;
import dev.aether.module.impl.cosmetics.PlayerPreviewModule;
import dev.aether.module.impl.cosmetics.TrailCosmeticsModule;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Draws Aether cosmetics in the world: capes, wings, halos, hats, a particle trail and a
 * ribbon trail.
 * <p>
 * Everything happens in {@code RenderWorldLastEvent}, where the matrix is camera-relative
 * world space. Each item is placed relative to a wearer's interpolated position and body
 * yaw, so the same code renders the local player and the nearby players a preview module
 * includes, and it works the same in first person and third person.
 * <p>
 * Imported cape PNGs are uploaded once per asset as a dynamic texture and sampled across the
 * whole image, so what the cosmetics screen previews is what appears on the player's back.
 */
final class ForgeCosmeticRenderer {
    private static final double PLAYER_HEIGHT = 1.8D;
    private static final double PLAYER_WIDTH = 0.6D;
    private static final int GL_TRIANGLE_STRIP = 5;
    private static final int GL_TRIANGLE_FAN = 6;
    private static final int GL_QUADS = 7;
    private static final int RIBBON_LIMIT = 64;
    private static final double WALK_SPEED = 4.3D;

    private final AetherClient client;
    private final Map<String, Integer> capeTextures = new HashMap<String, Integer>();
    private final Set<String> unreadableCapes = new HashSet<String>();
    private final List<Object> liveTextures = new ArrayList<Object>();
    private final List<double[]> ribbon = new ArrayList<double[]>();
    private final double[] sample = new double[3];

    private boolean sampled;
    private double speed;
    private int particleCooldown;

    ForgeCosmeticRenderer(AetherClient client) {
        this.client = client;
    }

    /* ------------------------------------------------------------------ */
    /*  Per tick: motion state, ribbon history, particle trail             */
    /* ------------------------------------------------------------------ */

    void onClientTick() {
        Object minecraft = Mc189Compat.minecraft();
        Object player = Mc189Compat.player(minecraft);
        Object world = Mc189Compat.world(minecraft);
        if (player == null || world == null) {
            forget();
            return;
        }
        trackMotion(player);
        sampleRibbon(player);
        emitTrailParticles(world, player);
    }

    private void forget() {
        this.sampled = false;
        this.speed = 0.0D;
        this.ribbon.clear();
        this.particleCooldown = 0;
    }

    private void trackMotion(Object player) {
        double x = Mc189Compat.posX(player);
        double y = Mc189Compat.posY(player);
        double z = Mc189Compat.posZ(player);
        if (this.sampled) {
            double dx = x - this.sample[0];
            double dz = z - this.sample[2];
            double moved = Math.sqrt(dx * dx + dz * dz);
            // Smoothed blocks-per-tick, used by the cape swing and the trail threshold.
            this.speed = this.speed * 0.6D + moved * 0.4D;
        }
        this.sample[0] = x;
        this.sample[1] = y;
        this.sample[2] = z;
        this.sampled = true;
    }

    private void sampleRibbon(Object player) {
        if (!enabled(TrailCosmeticsModule.ID)) {
            this.ribbon.clear();
            return;
        }
        double x = Mc189Compat.posX(player);
        double y = Mc189Compat.posY(player) + 0.15D;
        double z = Mc189Compat.posZ(player);
        if (!this.ribbon.isEmpty()) {
            double[] previous = this.ribbon.get(this.ribbon.size() - 1);
            double dx = x - previous[0];
            double dy = y - previous[1];
            double dz = z - previous[2];
            if (dx * dx + dy * dy + dz * dz > 64.0D) {
                // A teleport or a world switch: do not draw a ribbon across the map.
                this.ribbon.clear();
            }
        }
        this.ribbon.add(new double[] {x, y, z});
        int limit = clamp(settingInt(TrailCosmeticsModule.ID, "length", 24), 2, RIBBON_LIMIT);
        while (this.ribbon.size() > limit) {
            this.ribbon.remove(0);
        }
    }

    private void emitTrailParticles(Object world, Object player) {
        if (!enabled(CurrentTrailModule.ID)) {
            this.particleCooldown = 0;
            return;
        }
        if (this.particleCooldown > 0) {
            this.particleCooldown--;
            return;
        }
        this.particleCooldown = clamp(settingInt(CurrentTrailModule.ID, "rate", 2), 1, 10);

        int threshold = clamp(settingInt(CurrentTrailModule.ID, "speed_threshold", 0), 0, 100);
        if (threshold > 0 && this.speed * 20.0D < WALK_SPEED * threshold / 100.0D) {
            return;
        }

        double spread = clamp(settingInt(CurrentTrailModule.ID, "spread", 30), 0, 100) / 100.0D * 0.5D;
        double velocity = clamp(settingInt(CurrentTrailModule.ID, "size", 100), 50, 200) / 100.0D;
        float yaw = (float) Math.toRadians(Mc189Compat.rotationYaw(player));
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double jitter = (Math.random() - 0.5D) * spread;

        Mc189Compat.spawnParticle(world, particleName(settingString(CurrentTrailModule.ID, "particle", "Cloud")),
            Mc189Compat.posX(player) - forwardX * 0.35D + jitter,
            Mc189Compat.posY(player) + 0.15D + (Math.random() - 0.5D) * spread * 0.5D,
            Mc189Compat.posZ(player) - forwardZ * 0.35D + jitter,
            -forwardX * 0.06D * velocity, 0.01D * velocity, -forwardZ * 0.06D * velocity);
    }

    private static String particleName(String label) {
        if ("Flame".equalsIgnoreCase(label)) {
            return "FLAME";
        }
        if ("Crit".equalsIgnoreCase(label)) {
            return "CRIT";
        }
        if ("Magic Crit".equalsIgnoreCase(label)) {
            return "CRIT_MAGIC";
        }
        if ("Smoke".equalsIgnoreCase(label)) {
            return "SMOKE_NORMAL";
        }
        if ("Heart".equalsIgnoreCase(label)) {
            return "HEART";
        }
        if ("Spark".equalsIgnoreCase(label)) {
            return "FIREWORKS_SPARK";
        }
        return "CLOUD";
    }

    /* ------------------------------------------------------------------ */
    /*  Per frame: all worn cosmetics                                      */
    /* ------------------------------------------------------------------ */

    void onRenderWorldLast(float partialTicks) {
        Object minecraft = Mc189Compat.minecraft();
        Object world = Mc189Compat.world(minecraft);
        Object player = Mc189Compat.player(minecraft);
        if (world == null || player == null) {
            return;
        }

        double cameraX = interpolated(player, 0, partialTicks);
        double cameraY = interpolated(player, 1, partialTicks);
        double cameraZ = interpolated(player, 2, partialTicks);

        boolean wearing = enabled(CurrentCapeModule.ID) || enabled(CurrentWingsModule.ID)
            || enabled(CurrentHaloModule.ID) || enabled(CurrentHatModule.ID);
        if (wearing) {
            renderWearer(player, cameraX, cameraY, cameraZ, partialTicks);
            if (enabled(PlayerPreviewModule.ID)) {
                renderNearbyWearers(world, player, cameraX, cameraY, cameraZ, partialTicks);
            }
        }
        if (enabled(TrailCosmeticsModule.ID)) {
            drawRibbon(cameraX, cameraY, cameraZ);
        }
    }

    private void renderNearbyWearers(Object world, Object localPlayer, double cameraX, double cameraY, double cameraZ,
                                     float partialTicks) {
        double maxDistance = clamp(settingInt(PlayerPreviewModule.ID, "max_distance", 12), 2, 32);
        boolean skipInvisible = settingBool(PlayerPreviewModule.ID, "skip_invisible", true);
        double playerX = Mc189Compat.posX(localPlayer);
        double playerY = Mc189Compat.posY(localPlayer);
        double playerZ = Mc189Compat.posZ(localPlayer);

        for (Object other : Mc189Compat.worldPlayers(world)) {
            if (other == null || other == localPlayer) {
                continue;
            }
            if (skipInvisible && Mc189Compat.isInvisible(other)) {
                continue;
            }
            if (Mc189Compat.distanceTo(other, playerX, playerY, playerZ) > maxDistance) {
                continue;
            }
            renderWearer(other, cameraX, cameraY, cameraZ, partialTicks);
        }
    }

    /**
     * Places one wearer's cosmetics. The matrix is moved to the wearer and rotated by the
     * body yaw, so every item below is drawn in local space: +Y up, -Z behind the player.
     */
    private void renderWearer(Object entity, double cameraX, double cameraY, double cameraZ, float partialTicks) {
        double x = interpolated(entity, 0, partialTicks) - cameraX;
        double y = interpolated(entity, 1, partialTicks) - cameraY;
        double z = interpolated(entity, 2, partialTicks) - cameraZ;
        float yaw = Mc189Compat.interpolatedYaw(entity, partialTicks);

        double seconds = seconds();
        Mc189Compat.pushMatrix();
        try {
            Mc189Compat.disableLighting();
            Mc189Compat.disableCull();
            Mc189Compat.enableBlend();
            Mc189Compat.tryBlendFuncSeparate(770, 771, 1, 0);
            Mc189Compat.depthMask(false);
            Mc189Compat.translate((float) x, (float) y, (float) z);
            Mc189Compat.rotate(-yaw, 0.0F, 1.0F, 0.0F);
            if (enabled(CurrentCapeModule.ID)) {
                drawCape(seconds);
            }
            if (enabled(CurrentWingsModule.ID)) {
                drawWings(seconds);
            }
            if (enabled(CurrentHaloModule.ID)) {
                drawHalo(seconds);
            }
            if (enabled(CurrentHatModule.ID)) {
                drawHat();
            }
        } finally {
            Mc189Compat.depthMask(true);
            Mc189Compat.resetColor();
            Mc189Compat.disableBlend();
            Mc189Compat.enableTexture2D();
            Mc189Compat.enableCull();
            Mc189Compat.enableLighting();
            Mc189Compat.popMatrix();
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Cape                                                               */
    /* ------------------------------------------------------------------ */

    private void drawCape(double seconds) {
        CosmeticAsset cape = client.cosmetics().effective(CosmeticType.STATIC_CAPE);
        if (cape == null) {
            return;
        }
        boolean flat = enabled(CapePreviewModule.ID);
        double opacity = clamp(settingInt(CurrentCapeModule.ID, "opacity", 90), 0, 100) / 100.0D;
        double wave = flat ? 0.0D : clamp(settingInt(CurrentCapeModule.ID, "wave", 50), 0, 100) / 100.0D;
        double length = PLAYER_HEIGHT * 0.44D * clamp(settingInt(CurrentCapeModule.ID, "length", 100), 50, 150) / 100.0D;
        if (flat) {
            length *= clamp(settingInt(CapePreviewModule.ID, "scale", 100), 50, 200) / 100.0D;
        }
        double swing = !flat && settingBool(CurrentCapeModule.ID, "swing", true)
            ? Math.min(0.35D, this.speed * 1.8D) : 0.0D;
        int segments = flat ? 1 : 6;

        double shoulder = PLAYER_HEIGHT * 0.80D;
        double halfWidth = PLAYER_WIDTH * (flat ? 0.62D : 0.52D);
        double segment = length / segments;
        double amplitude = 0.06D * wave;
        double baseZ = -0.16D;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        Integer texture = cape.localFile() == null ? null : capeTexture(cape);

        if (texture != null) {
            // Imported cape: sample the whole PNG across the cloth, one quad per segment.
            Mc189Compat.bindTexture(texture.intValue());
            Mc189Compat.color(1.0F, 1.0F, 1.0F, (float) opacity);
            renderer.begin(GL_QUADS, DefaultVertexFormats.POSITION_TEX);
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
        renderer.begin(GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < segments; i++) {
            double y0 = shoulder - i * segment + swing * i * 0.03D;
            double y1 = shoulder - (i + 1) * segment + swing * (i + 1) * 0.03D;
            double z0 = baseZ - sway(i, seconds, amplitude) - swing * i * 0.12D;
            double z1 = baseZ - sway(i + 1, seconds, amplitude) - swing * (i + 1) * 0.12D;
            double w0 = halfWidth * (1.0D - 0.06D * i);
            double w1 = halfWidth * (1.0D - 0.06D * (i + 1));
            int top = modulate(cape.primaryColor(), opacity * (1.0D - 0.08D * i));
            int bottom = modulate(cape.secondaryColor(), opacity * (1.0D - 0.08D * (i + 1)));
            vertex(renderer, -w0, y0, z0, top);
            vertex(renderer, w0, y0, z0, top);
            vertex(renderer, w1, y1, z1, bottom);
            vertex(renderer, -w1, y1, z1, bottom);
        }
        tessellator.draw();
    }

    private static double sway(double index, double seconds, double amplitude) {
        if (amplitude <= 0.0D) {
            return 0.0D;
        }
        return amplitude * (Math.sin(seconds * 2.1D + index * 0.7D) + 0.5D * Math.sin(seconds * 3.7D + index * 0.4D));
    }

    /* ------------------------------------------------------------------ */
    /*  Wings                                                              */
    /* ------------------------------------------------------------------ */

    private void drawWings(double seconds) {
        CosmeticAsset wings = client.cosmetics().effective(CosmeticType.WINGS);
        if (wings == null) {
            return;
        }
        double opacity = clamp(settingInt(CurrentWingsModule.ID, "opacity", 85), 0, 100) / 100.0D;
        double spread = clamp(settingInt(CurrentWingsModule.ID, "spread", 45), 10, 80);
        double flapRate = clamp(settingInt(CurrentWingsModule.ID, "flap_speed", 50), 0, 100) / 100.0D;
        double size = clamp(settingInt(CurrentWingsModule.ID, "size", 100), 50, 200) / 100.0D;
        double flap = Math.sin(seconds * (1.0D + 3.0D * flapRate)) * (5.0D + 15.0D * flapRate);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (int side = -1; side <= 1; side += 2) {
            drawWing(renderer, side, spread + flap, size, wings, opacity);
        }
        tessellator.draw();
    }

    /** Draws three tapered feathers for one side; each feather is a plate facing front/back. */
    private static void drawWing(WorldRenderer renderer, int side, double angleDegrees, double size,
                                 CosmeticAsset wings, double opacity) {
        double shoulder = PLAYER_HEIGHT * 0.78D;
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
            int rootColor = modulate(wings.primaryColor(), opacity);
            int tipColor = modulate(wings.secondaryColor(), opacity * 0.25D);

            vertex(renderer, rootX - perpendicularX * featherWidth, rootY - perpendicularY * featherWidth, plane, rootColor);
            vertex(renderer, rootX + perpendicularX * featherWidth, rootY + perpendicularY * featherWidth, plane, rootColor);
            vertex(renderer, rootX + directionX * featherLength + perpendicularX * featherWidth * 0.2D,
                rootY + directionY * featherLength + perpendicularY * featherWidth * 0.2D, plane, tipColor);
            vertex(renderer, rootX + directionX * featherLength - perpendicularX * featherWidth * 0.2D,
                rootY + directionY * featherLength - perpendicularY * featherWidth * 0.2D, plane, tipColor);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Halo and hat                                                       */
    /* ------------------------------------------------------------------ */

    private void drawHalo(double seconds) {
        CosmeticAsset halo = client.cosmetics().effective(CosmeticType.HALO);
        if (halo == null) {
            return;
        }
        double opacity = clamp(settingInt(CurrentHaloModule.ID, "opacity", 90), 0, 100) / 100.0D;
        double radius = 0.10D + 0.30D * clamp(settingInt(CurrentHaloModule.ID, "radius", 30), 10, 80) / 100.0D;
        boolean glow = settingBool(CurrentHaloModule.ID, "glow", true);
        boolean bob = settingBool(CurrentHaloModule.ID, "bob", true);
        double y = PLAYER_HEIGHT + 0.10D + (bob ? 0.035D * Math.sin(seconds * 2.0D) : 0.0D);
        int inner = modulate(halo.primaryColor(), opacity * 0.55D);
        int outer = modulate(halo.secondaryColor(), opacity);

        if (glow) {
            // Additive: the ring reads as light rather than as painted geometry.
            Mc189Compat.tryBlendFuncSeparate(770, 1, 1, 0);
        }
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i <= 32; i++) {
            double angle = i / 32.0D * Math.PI * 2.0D;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            vertex(renderer, cos * radius * 0.72D, y, sin * radius * 0.72D, inner);
            vertex(renderer, cos * radius, y, sin * radius, outer);
        }
        tessellator.draw();
        if (glow) {
            Mc189Compat.tryBlendFuncSeparate(770, 771, 1, 0);
        }
    }

    private void drawHat() {
        CosmeticAsset hat = client.cosmetics().effective(CosmeticType.HAT);
        if (hat == null) {
            return;
        }
        double radius = PLAYER_WIDTH * 0.42D * clamp(settingInt(CurrentHatModule.ID, "radius", 100), 50, 150) / 100.0D;
        double height = 0.16D * clamp(settingInt(CurrentHatModule.ID, "height", 100), 50, 200) / 100.0D;
        double tilt = clamp(settingInt(CurrentHatModule.ID, "tilt", 0), 0, 30);
        boolean brim = settingBool(CurrentHatModule.ID, "brim", true);
        int crown = hat.primaryColor();
        int crownTop = modulate(hat.primaryColor(), 0.85D);
        int band = hat.secondaryColor();

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        Mc189Compat.pushMatrix();
        try {
            Mc189Compat.translate(0.0F, (float) (PLAYER_HEIGHT - 0.14D), 0.0F);
            Mc189Compat.rotate((float) tilt, 0.0F, 0.0F, 1.0F);

            // Crown: a tapered tube plus a closed top so the hat reads as solid from below.
            renderer.begin(GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i <= 16; i++) {
                double angle = i / 16.0D * Math.PI * 2.0D;
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                vertex(renderer, cos * radius, 0.0D, sin * radius, band);
                vertex(renderer, cos * radius * 0.92D, height, sin * radius * 0.92D, crown);
            }
            tessellator.draw();

            renderer.begin(GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
            vertex(renderer, 0.0D, height, 0.0D, crownTop);
            for (int i = 0; i <= 16; i++) {
                double angle = i / 16.0D * Math.PI * 2.0D;
                vertex(renderer, Math.cos(angle) * radius * 0.92D, height, Math.sin(angle) * radius * 0.92D, crownTop);
            }
            tessellator.draw();

            if (brim) {
                renderer.begin(GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
                for (int i = 0; i <= 16; i++) {
                    double angle = i / 16.0D * Math.PI * 2.0D;
                    double cos = Math.cos(angle);
                    double sin = Math.sin(angle);
                    vertex(renderer, cos * radius * 1.05D, 0.005D, sin * radius * 1.05D, crown);
                    vertex(renderer, cos * radius * 1.55D, 0.0D, sin * radius * 1.55D, band);
                }
                tessellator.draw();
            }
        } finally {
            Mc189Compat.popMatrix();
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Ribbon trail                                                       */
    /* ------------------------------------------------------------------ */

    private void drawRibbon(double cameraX, double cameraY, double cameraZ) {
        int points = this.ribbon.size();
        if (points < 2) {
            return;
        }
        CosmeticAsset trail = client.cosmetics().effective(CosmeticType.TRAIL);
        double halfWidth = 0.05D * clamp(settingInt(TrailCosmeticsModule.ID, "width", 100), 10, 150) / 100.0D;
        double opacity = clamp(settingInt(TrailCosmeticsModule.ID, "opacity", 60), 0, 100) / 100.0D;
        boolean glow = settingBool(TrailCosmeticsModule.ID, "glow", false);
        int head = trail == null ? 0xFFFFFFFF : trail.primaryColor();
        int tail = trail == null ? 0x66FFFFFF : trail.secondaryColor();

        if (glow) {
            Mc189Compat.tryBlendFuncSeparate(770, 1, 1, 0);
        }
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
        for (int i = 0; i < points; i++) {
            double[] point = this.ribbon.get(i);
            double[] previous = this.ribbon.get(Math.max(i - 1, 0));
            double[] next = this.ribbon.get(Math.min(i + 1, points - 1));
            double fraction = (double) i / (points - 1);

            double dx = next[0] - previous[0];
            double dz = next[2] - previous[2];
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length < 1.0E-4D) {
                dx = 0.0D;
                dz = 1.0D;
                length = 1.0D;
            }
            // Horizontal normal of the path segment: cross(direction, up).
            double sideX = -dz / length;
            double sideZ = dx / length;
            double width = halfWidth * (0.35D + 0.65D * fraction);
            int color = fraction >= 1.0D
                ? modulate(head, opacity)
                : mix(modulate(tail, opacity * 0.2D), modulate(head, opacity), fraction);

            vertex(renderer, point[0] - sideX * width - cameraX, point[1] - cameraY, point[2] - sideZ * width - cameraZ, color);
            vertex(renderer, point[0] + sideX * width - cameraX, point[1] - cameraY, point[2] + sideZ * width - cameraZ, color);
        }
        tessellator.draw();
        if (glow) {
            Mc189Compat.tryBlendFuncSeparate(770, 771, 1, 0);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Cape textures                                                      */
    /* ------------------------------------------------------------------ */

    /** Uploads one cape PNG, once. Returns {@code null} when the file cannot be read. */
    private Integer capeTexture(CosmeticAsset cape) {
        String id = cape.id();
        Integer cached = this.capeTextures.get(id);
        if (cached != null) {
            return cached;
        }
        if (this.unreadableCapes.contains(id)) {
            return null;
        }
        try {
            BufferedImage image = ImageIO.read(cape.localFile().toFile());
            if (image == null) {
                this.unreadableCapes.add(id);
                return null;
            }
            DynamicTexture texture = new DynamicTexture(image);
            texture.updateDynamicTexture();
            this.liveTextures.add(texture);
            Integer handle = Integer.valueOf(texture.getGlTextureId());
            this.capeTextures.put(id, handle);
            return handle;
        } catch (Throwable failure) {
            // A broken image or an absent GL context must not break the whole frame.
            this.unreadableCapes.add(id);
            return null;
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Helpers                                                            */
    /* ------------------------------------------------------------------ */

    private static void vertex(WorldRenderer renderer, double x, double y, double z, int argb) {
        renderer.pos(x, y, z)
            .color((argb >> 16 & 255) / 255.0F, (argb >> 8 & 255) / 255.0F, (argb & 255) / 255.0F,
                (argb >>> 24) / 255.0F)
            .endVertex();
    }

    private static int modulate(int color, double alphaScale) {
        int alpha = (int) Math.round((color >>> 24 & 255) * Math.max(0.0D, Math.min(1.0D, alphaScale)));
        return (color & 0xFFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    private static int mix(int from, int to, double fraction) {
        double amount = Math.max(0.0D, Math.min(1.0D, fraction));
        int alpha = (int) Math.round((from >>> 24 & 255) + ((to >>> 24 & 255) - (from >>> 24 & 255)) * amount);
        int red = (int) Math.round((from >> 16 & 255) + ((to >> 16 & 255) - (from >> 16 & 255)) * amount);
        int green = (int) Math.round((from >> 8 & 255) + ((to >> 8 & 255) - (from >> 8 & 255)) * amount);
        int blue = (int) Math.round((from & 255) + ((to & 255) - (from & 255)) * amount);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    private static double interpolated(Object entity, int axis, float partialTicks) {
        double now = axis == 0 ? Mc189Compat.posX(entity) : axis == 1 ? Mc189Compat.posY(entity) : Mc189Compat.posZ(entity);
        double previous = axis == 0 ? Mc189Compat.lastTickPosX(entity)
            : axis == 1 ? Mc189Compat.lastTickPosY(entity) : Mc189Compat.lastTickPosZ(entity);
        return previous + (now - previous) * partialTicks;
    }

    private static double seconds() {
        return (System.currentTimeMillis() % 600000L) / 1000.0D;
    }

    private boolean enabled(String id) {
        try {
            return client.modules().get(id).state() == ModuleState.ENABLED;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean settingBool(String moduleId, String settingId, boolean fallback) {
        try {
            for (dev.aether.module.setting.Setting<?> setting : client.modules().get(moduleId).settings()) {
                if (settingId.equals(setting.id()) && setting.value() instanceof Boolean) {
                    return ((Boolean) setting.value()).booleanValue();
                }
            }
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
        return fallback;
    }

    private int settingInt(String moduleId, String settingId, int fallback) {
        try {
            for (dev.aether.module.setting.Setting<?> setting : client.modules().get(moduleId).settings()) {
                if (settingId.equals(setting.id()) && setting.value() instanceof Number) {
                    return ((Number) setting.value()).intValue();
                }
            }
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
        return fallback;
    }

    private String settingString(String moduleId, String settingId, String fallback) {
        try {
            for (dev.aether.module.setting.Setting<?> setting : client.modules().get(moduleId).settings()) {
                if (settingId.equals(setting.id()) && setting.value() instanceof String) {
                    return (String) setting.value();
                }
            }
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
        return fallback;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
