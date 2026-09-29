package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.waypoint.Waypoint;

import java.util.List;
import java.util.Locale;

/**
 * Draws the saved waypoints in the world on {@code RenderWorldLastEvent}.
 * <p>
 * Rendering rules, in priority order:
 * <ul>
 *   <li>only waypoints of the player's current dimension are drawn ({@code worldDimension()}),
 *       so a Nether waypoint never ghosts through an Overworld wall,</li>
 *   <li>only enabled waypoints are drawn ({@code forWorld} already filters those),</li>
 *   <li>waypoints beyond the draw distance are culled - the manager hands out the full list and
 *       this is the only per-frame work a far waypoint costs,</li>
 *   <li>the distance label is only re-measured when its rounded distance changes, never every
 *       frame ({@link TextWidthCache}),</li>
 *   <li>GL state is restored in a {@code finally} block, so a failed draw cannot poison the
 *       rest of the frame's world rendering.</li>
 * </ul>
 * The marker is a vertical beacon line plus a filled box at the target: both are cheap
 * {@code drawSelectionBoundingBox}-style line paths already used by the block overlay, so no new
 * geometry pipeline is introduced for the feature.
 */
final class ForgeWaypointRenderer {
    /** Beyond this many blocks a waypoint costs nothing but a distance computation. */
    private static final double DRAW_DISTANCE = 512.0D;

    private final AetherClient client;
    /** Per-waypoint label cache; entries die with the renderer, waypoint counts are small. */
    private final TextWidthCache textCache = new TextWidthCache();
    private Object font;
    private int fontWidthPadding = 6;

    ForgeWaypointRenderer(AetherClient client) {
        this.client = client;
    }

    void onRenderWorldLast(float partialTicks) {
        Object minecraft = Mc189Compat.minecraft();
        Object world = Mc189Compat.world(minecraft);
        Object player = Mc189Compat.player(minecraft);
        if (world == null || player == null) {
            return;
        }
        Integer dimensionId = Mc189Compat.worldDimension(world);
        if (dimensionId == null) {
            return;
        }
        List<Waypoint> waypoints = client.waypoints().forWorld(dimensionName(dimensionId));
        if (waypoints.isEmpty()) {
            return;
        }
        if (this.font == null) {
            this.font = Mc189Compat.fontRenderer(minecraft);
        }

        double px = Mc189Compat.lastTickPosX(player) + (Mc189Compat.posX(player) - Mc189Compat.lastTickPosX(player)) * (double) partialTicks;
        double py = Mc189Compat.lastTickPosY(player) + (Mc189Compat.posY(player) - Mc189Compat.lastTickPosY(player)) * (double) partialTicks;
        double pz = Mc189Compat.lastTickPosZ(player) + (Mc189Compat.posZ(player) - Mc189Compat.lastTickPosZ(player)) * (double) partialTicks;

        for (int i = 0; i < waypoints.size(); i++) {
            Waypoint waypoint = waypoints.get(i);
            double dx = waypoint.x() + 0.5D - px;
            double dy = waypoint.y() + 0.5D - py;
            double dz = waypoint.z() + 0.5D - pz;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (distance > DRAW_DISTANCE) {
                continue;
            }
            drawWaypoint(waypoint, px, py, pz, distance);
        }
    }

    /** Maps the numeric dimension id to the name the waypoint model stores. */
    private static String dimensionName(int id) {
        switch (id) {
            case -1:
                return "minecraft:the_nether";
            case 1:
                return "minecraft:the_end";
            default:
                return "minecraft:overworld";
        }
    }

    private void drawWaypoint(Waypoint waypoint, double px, double py, double pz, double distance) {
        Mc189Compat.enableBlend();
        Mc189Compat.tryBlendFuncSeparate(770, 771, 1, 0);
        Mc189Compat.disableTexture2D();
        Mc189Compat.depthMask(false);
        try {
            int color = waypoint.color() | 0xFF000000;
            float a = (color >> 24 & 0xFF) / 255.0F;
            float r = (color >> 16 & 0xFF) / 255.0F;
            float g = (color >> 8 & 0xFF) / 255.0F;
            float b = (color & 0xFF) / 255.0F;

            double ox = waypoint.x() - px;
            double oy = waypoint.y() - py;
            double oz = waypoint.z() - pz;

            // Beacon line: from the marker up to the build height so it is findable over terrain.
            net.minecraft.util.AxisAlignedBB column = new net.minecraft.util.AxisAlignedBB(
                ox + 0.35D, oy + 0.9D, oz + 0.35D,
                ox + 0.65D, oy + Math.max(2.0D, 258.0D - waypoint.y()), oz + 0.65D);
            Mc189Compat.color(r, g, b, a * 0.85F);
            Mc189Compat.drawSelectionBoundingBox(column);

            // Target box at the exact position.
            net.minecraft.util.AxisAlignedBB marker = new net.minecraft.util.AxisAlignedBB(
                ox - 0.1D, oy - 0.1D, oz - 0.1D,
                ox + 1.1D, oy + 1.1D, oz + 1.1D);
            Mc189Compat.color(r, g, b, a);
            Mc189Compat.drawSelectionBoundingBox(marker);

            if (this.font != null) {
                drawLabel(waypoint, ox + 0.5D, oy + 1.9D, oz + 0.5D, distance, r, g, b, a);
            }
        } finally {
            Mc189Compat.color(1.0F, 1.0F, 1.0F, 1.0F);
            Mc189Compat.glLineWidth(1.0F);
            Mc189Compat.depthMask(true);
            Mc189Compat.enableTexture2D();
            Mc189Compat.disableBlend();
        }
    }

    private void drawLabel(Waypoint waypoint, double x, double y, double z, double distance,
                           float r, float g, float b, float a) {
        int rounded = (int) Math.round(distance);
        String label = waypoint.name() + " " + rounded + "m";
        String text = trim(label);
        int color = ((int) (a * 255F) & 0xFF) << 24 | ((int) (r * 255F) & 0xFF) << 16
            | ((int) (g * 255F) & 0xFF) << 8 | ((int) (b * 255F) & 0xFF);
        Mc189Compat.drawWaypointLabel(this.font, text, x, y, z, rounded, color, this.textCache);
    }

    private String trim(String label) {
        // Waypoint names are capped at 32 chars by the manager; the distance suffix always fits.
        return label.length() > 48 ? label.substring(0, 45) + "..." : label;
    }

    /** Present for symmetry with the label cache; padding is not used by the current draw path. */
    int fontWidthPadding() {
        return this.fontWidthPadding;
    }

    /**
     * The label does not change while the player stands still, and its rounded distance changes
     * only when they cross a metre boundary - so the cache key is the rounded distance plus the
     * name, and a miss re-measures once instead of every frame.
     */
    static final class TextWidthCache {
        private final String[] keys = new String[8];
        private final int[] widths = new int[8];
        private int cursor;

        /** @return the cached width for {@code key}, or {@code -1} when it must be measured. */
        int get(String key) {
            for (int i = 0; i < keys.length; i++) {
                if (key.equals(keys[i])) {
                    return widths[i];
                }
            }
            return -1;
        }

        void put(String key, int width) {
            int slot = cursor++ % keys.length;
            keys[slot] = key;
            widths[slot] = width;
        }
    }
}
