package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.cosmetic.CosmeticAsset;
import dev.aether.cosmetic.CosmeticType;
import dev.aether.cosmetic.render.CosmeticRenderContext;
import dev.aether.cosmetic.render.CosmeticRenderer;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.module.impl.cosmetics.CurrentTrailModule;
import dev.aether.module.impl.cosmetics.PlayerPreviewModule;
import dev.aether.module.impl.cosmetics.TrailCosmeticsModule;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.aether.cosmetic.render.CosmeticRenderContext.clamp;
import static dev.aether.cosmetic.render.CosmeticRenderContext.mix;
import static dev.aether.cosmetic.render.CosmeticRenderContext.modulate;

/**
 * Draws Aether cosmetics in the world: capes, wings, halos, hats, a particle trail and a
 * ribbon trail.
 * <p>
 * Everything happens in {@code RenderWorldLastEvent}, where the matrix is camera-relative
 * world space. Each item is placed relative to a wearer's interpolated position and body
 * yaw, so the same code renders the local player and the nearby players a preview module
 * includes, and it works the same in first person and third person.
 * <p>
 * The per-slot geometry (cape cloth, wing feathers, halo ring, hat crown) is delegated to
 * the {@link CosmeticRenderer} implementations in this package - one class per type,
 * registered in {@link #wearerRenderers} - while this class keeps what is genuinely
 * shared: wearer placement, the tick-time motion/ribbon/particle state, and the PNG
 * texture cache. The ribbon is deliberately not a per-type renderer: it is drawn once in
 * camera space from the shared history, not per wearer.
 * <p>
 * Imported cape PNGs are uploaded once per asset as a dynamic texture and sampled across the
 * whole image, so what the cosmetics screen previews is what appears on the player's back.
 */
final class ForgeCosmeticRenderer {
    private static final int RIBBON_LIMIT = 64;
    private static final double WALK_SPEED = 4.3D;

    private final AetherClient client;
    /** One renderer per wearer-local slot, in draw order. */
    private final List<CosmeticRenderer> wearerRenderers = Arrays.<CosmeticRenderer>asList(
        new CapeCosmeticRenderer(),
        new WingsCosmeticRenderer(),
        new HaloCosmeticRenderer(),
        new HatCosmeticRenderer());
    /** Reused every frame; the per-frame values are refreshed in {@link #renderWearer}. */
    private final CosmeticRenderContext renderContext;
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
        this.renderContext = new CosmeticRenderContext(
            new CosmeticRenderContext.Settings() {
                @Override
                public boolean enabled(String moduleId) {
                    return ForgeCosmeticRenderer.this.enabled(moduleId);
                }

                @Override
                public boolean settingBool(String moduleId, String key, boolean fallback) {
                    return ForgeCosmeticRenderer.this.settingBool(moduleId, key, fallback);
                }

                @Override
                public int settingInt(String moduleId, String key, int fallback) {
                    return ForgeCosmeticRenderer.this.settingInt(moduleId, key, fallback);
                }

                @Override
                public String settingString(String moduleId, String key, String fallback) {
                    return ForgeCosmeticRenderer.this.settingString(moduleId, key, fallback);
                }
            },
            new CosmeticRenderContext.AssetSource() {
                @Override
                public CosmeticAsset effective(CosmeticType type) {
                    return client.cosmetics().effective(type);
                }
            },
            new CosmeticRenderContext.TextureSource() {
                @Override
                public Integer textureFor(CosmeticAsset asset) {
                    return capeTexture(asset);
                }
            });
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

        boolean wearing = false;
        for (CosmeticRenderer renderer : wearerRenderers) {
            if (enabled(renderer.moduleId())) {
                wearing = true;
                break;
            }
        }
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
        renderContext.beginFrame(seconds, this.speed);
        Mc189Compat.pushMatrix();
        try {
            Mc189Compat.disableLighting();
            Mc189Compat.disableCull();
            Mc189Compat.enableBlend();
            Mc189Compat.tryBlendFuncSeparate(770, 771, 1, 0);
            Mc189Compat.depthMask(false);
            Mc189Compat.translate((float) x, (float) y, (float) z);
            Mc189Compat.rotate(-yaw, 0.0F, 1.0F, 0.0F);
            for (CosmeticRenderer renderer : wearerRenderers) {
                if (enabled(renderer.moduleId())) {
                    CosmeticAsset asset = client.cosmetics().effective(renderer.type());
                    if (asset != null) {
                        renderer.render(asset, renderContext);
                    }
                }
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
        renderer.begin(CosmeticGeometry.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
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

            CosmeticGeometry.vertex(renderer, point[0] - sideX * width - cameraX, point[1] - cameraY, point[2] - sideZ * width - cameraZ, color);
            CosmeticGeometry.vertex(renderer, point[0] + sideX * width - cameraX, point[1] - cameraY, point[2] + sideZ * width - cameraZ, color);
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
}
