package dev.aether.ui;

import dev.aether.forge189.Mc189Compat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;

/**
 * The vector layer the whole menu draws with: feathered rounded rectangles, gradients,
 * the rotating angular-gradient accent fill, soft shadows and scissored clipping, all
 * in GUI-scale pixels.
 * <p>
 * This is Aether's native answer to the reference client's NanoVG layer. It does not
 * try to be NanoVG - it is the handful of primitives the visual grammar needs, built
 * on GL11 immediate mode with per-vertex colour, with a one-pixel alpha feather on
 * every path edge so corners read as smooth vectors at any display resolution. Every
 * batch is wrapped in {@link #begin()}/{@link #end()}, which establishes and restores
 * the GL state so nothing leaks into Minecraft's own rendering.
 */
public final class UiCanvas {

    /** Alpha feather width at the path edge, in GUI pixels. */
    private static final float FEATHER = 0.85F;
    /** Triangle segments per rounded corner. */
    private static final int CORNER_SEGMENTS = 7;

    private UiCanvas() {
    }

    /* ── batch state ────────────────────────────────────────────────────── */

    private static boolean active;
    private static float scale = 1.0F;

    /**
     * Establishes the state the vector layer needs and scales the matrix so all
     * coordinates are menu units. The draw matrix already carries Minecraft's GUI
     * scale, so the additional scale is {@code unit / guiScale} - the product is one
     * menu unit per unit coordinate at any GUI-scale setting. Safe to nest-guard via
     * {@link #active}.
     */
    public static void begin() {
        if (active) {
            return;
        }
        active = true;
        UiScale.update();
        float guiScale = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
        scale = UiScale.unit() / guiScale;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glScaled(scale, scale, 1.0D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Begins the vector layer WITHOUT the menu-unit scale (GUI-pixel callers, e.g. the HUD editor). */
    public static void beginRaw() {
        if (active) {
            return;
        }
        active = true;
        scale = 1.0F;
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Restores Minecraft's state after a vector batch. */
    public static void end() {
        if (!active) {
            return;
        }
        active = false;
        if (scale != 1.0F) {
            GL11.glPopMatrix();
        }
        scale = 1.0F;
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopAttrib();
        Mc189Compat.enableTexture2D();
        Mc189Compat.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /* ── clipping ───────────────────────────────────────────────────────── */

    /** Restricts drawing to a rectangle in the current coordinate space. */
    public static void scissor(float x, float y, float w, float h) {
        try {
            Minecraft minecraft = Minecraft.getMinecraft();
            // Scissors work in physical pixels: menu units convert through the unit,
            // whatever the matrix scale is. GL counts from the bottom-left; the UI
            // counts from the top-left.
            float unit = scale == 1.0F
                ? new ScaledResolution(minecraft).getScaleFactor()
                : UiScale.unit();
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor(Math.round(x * unit), Math.round((minecraft.displayHeight / unit - y - h) * unit),
                Math.max(0, Math.round(w * unit)), Math.max(0, Math.round(h * unit)));
        } catch (Throwable ignored) {
        }
    }

    public static void clearScissor() {
        try {
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
        } catch (Throwable ignored) {
        }
    }

    /* ── fills ──────────────────────────────────────────────────────────── */

    /** Flat rounded rectangle. */
    public static void roundRect(float x, float y, float w, float h, float radius, int argb) {
        fillPath(roundedPath(x, y, w, h, radius, radius, radius, radius), argb, argb, 0.0F, 0.0F, null);
    }

    /** Rounded rectangle with independent corner radii (topLeft, topRight, bottomRight, bottomLeft). */
    public static void roundRectVarying(float x, float y, float w, float h,
                                        float tl, float tr, float br, float bl, int argb) {
        fillPath(roundedPath(x, y, w, h, tl, tr, br, bl), argb, argb, 0.0F, 0.0F, null);
    }

    /** Rounded rectangle with a vertical two-colour gradient. */
    public static void gradientRoundRect(float x, float y, float w, float h, float radius,
                                         int argbTop, int argbBottom) {
        fillPath(roundedPath(x, y, w, h, radius, radius, radius, radius), argbTop, argbBottom, 0.0F, 0.0F, null);
    }

    /**
     * The accent fill of the reference grammar: a two-colour gradient whose direction
     * rotates slowly around the rectangle's centre, so an active pill shimmers subtly
     * instead of sitting flat.
     *
     * @param timeMillis wall clock; the angle advances one full turn every 3.6 seconds
     */
    public static void angularGradientRoundRect(float x, float y, float w, float h, float radius,
                                                int argb1, int argb2, long timeMillis) {
        fillPath(roundedPath(x, y, w, h, radius, radius, radius, radius), argb1, argb2,
            x + w / 2.0F, y + h / 2.0F, AngularLerp.of(argb1, argb2, timeMillis));
    }

    /** A filled circle (used by toggles and radio dots). */
    public static void circle(float cx, float cy, float radius, int argb) {
        roundRect(cx - radius, cy - radius, radius * 2.0F, radius * 2.0F, radius, argb);
    }

    /* ── strokes ────────────────────────────────────────────────────────── */

    /** Rounds an outline around a rounded rectangle. */
    public static void outline(float x, float y, float w, float h, float radius, int argb, float stroke) {
        float[] path = roundedPath(x, y, w, h, radius, radius, radius, radius);
        float[] outer = expandPath(path, x + w / 2.0F, y + h / 2.0F, stroke / 2.0F);
        float[] inner = expandPath(path, x + w / 2.0F, y + h / 2.0F, -stroke / 2.0F);
        strip(outer, argb, inner, argb, 0.0F, 0.0F, null);
    }

    /**
     * A soft shadow: concentric expanding translucent outlines, the fake-blur the
     * reference uses instead of a real gaussian. {@code strength} is the stroke count.
     */
    public static void softShadow(float x, float y, float w, float h, float radius, int argbBase, int strength) {
        int alpha = (argbBase >>> 24) & 0xFF;
        if (alpha <= 0) {
            return;
        }
        for (int i = 0; i < strength; i++) {
            float grow = i;
            int stepAlpha = Math.max(0, alpha - (alpha / strength) * i);
            int stepColor = (stepAlpha << 24) | (argbBase & 0xFFFFFF);
            float[] path = roundedPath(x - grow, y - grow, w + grow * 2.0F, h + grow * 2.0F,
                radius + grow, radius + grow, radius + grow, radius + grow);
            fillPath(path, stepColor, stepColor, 0.0F, 0.0F, null);
        }
    }

    /* ── path machinery ─────────────────────────────────────────────────── */

    /** Rounded-rect outline points, clockwise from the top-left corner's left edge. */
    private static float[] roundedPath(float x, float y, float w, float h, float tl, float tr, float br, float bl) {
        float maxRadius = Math.min(w, h) / 2.0F;
        tl = clampRadius(tl, maxRadius);
        tr = clampRadius(tr, maxRadius);
        br = clampRadius(br, maxRadius);
        bl = clampRadius(bl, maxRadius);

        int points = CORNER_SEGMENTS * 4;
        float[] path = new float[(points + 1) * 2];
        int index = 0;
        // top-left corner (from the left edge to the top edge)
        index = corner(path, index, x + tl, y + tl, tl, 180.0F, 270.0F);
        // top-right
        index = corner(path, index, x + w - tr, y + tr, tr, 270.0F, 360.0F);
        // bottom-right
        index = corner(path, index, x + w - br, y + h - br, br, 0.0F, 90.0F);
        // bottom-left
        index = corner(path, index, x + bl, y + h - bl, bl, 90.0F, 180.0F);
        // Fill any tail of the array with the closing point: writers may not use every
        // slot (a sharp-corner variant emits one point per vertex), and an unwritten
        // zero would drag the fan's triangles to the screen origin.
        for (int fill = index; fill < path.length; fill += 2) {
            path[fill] = path[0];
            path[fill + 1] = path[1];
        }
        return path;
    }

    private static int corner(float[] path, int index, float cx, float cy, float radius,
                              float fromDegrees, float toDegrees) {
        if (radius <= 0.0F) {
            path[index] = cx;
            path[index + 1] = cy;
            return index + 2;
        }
        for (int i = 0; i <= CORNER_SEGMENTS; i++) {
            double angle = Math.toRadians(fromDegrees + (toDegrees - fromDegrees) * i / (float) CORNER_SEGMENTS);
            path[index] = cx + (float) Math.cos(angle) * radius;
            path[index + 1] = cy + (float) Math.sin(angle) * radius;
            index += 2;
        }
        return index - 2; // the next corner continues from this endpoint
    }

    private static float clampRadius(float radius, float max) {
        return Math.max(0.0F, Math.min(radius, max));
    }

    /** Offsets every path point away from (or towards) a centre by {@code amount}. */
    private static float[] expandPath(float[] path, float cx, float cy, float amount) {
        float[] out = new float[path.length];
        for (int i = 0; i < path.length; i += 2) {
            float dx = path[i] - cx;
            float dy = path[i + 1] - cy;
            float length = (float) Math.sqrt(dx * dx + dy * dy);
            if (length < 1.0E-4F) {
                out[i] = path[i];
                out[i + 1] = path[i + 1];
            } else {
                float scale = (length + amount) / length;
                out[i] = cx + dx * scale;
                out[i + 1] = cy + dy * scale;
            }
        }
        return out;
    }

    /** Convex fill: a fan over the path plus a feathered edge strip. */
    private static void fillPath(float[] path, int argbTop, int argbBottom,
                                 float originX, float originY, AngularLerp lerp) {
        // Text draws leave texture mode enabled (the glyph atlas stays bound); a flat
        // fill sampled against that atlas turns transparent, so every primitive
        // re-establishes the untextured state it needs.
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        float cx = centroid(path, 0);
        float cy = centroid(path, 1);
        float[] bounds = verticalBounds(path);
        float[] outer = expandPath(path, cx, cy, FEATHER);
        float[] inner = expandPath(path, cx, cy, 0.0F);

        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i < path.length; i += 2) {
            colorAt(outer[i], outer[i + 1], argbTop, argbBottom, bounds, originX, originY, lerp, 0.0F);
            GL11.glVertex2f(outer[i], outer[i + 1]);
            colorAt(inner[i], inner[i + 1], argbTop, argbBottom, bounds, originX, originY, lerp, 1.0F);
            GL11.glVertex2f(inner[i], inner[i + 1]);
        }
        // close the strip back to the first pair
        colorAt(outer[0], outer[1], argbTop, argbBottom, bounds, originX, originY, lerp, 0.0F);
        GL11.glVertex2f(outer[0], outer[1]);
        colorAt(inner[0], inner[1], argbTop, argbBottom, bounds, originX, originY, lerp, 1.0F);
        GL11.glVertex2f(inner[0], inner[1]);
        GL11.glEnd();

        // interior
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        colorAt(cx, cy, argbTop, argbBottom, bounds, originX, originY, lerp, 1.0F);
        GL11.glVertex2f(cx, cy);
        for (int i = 0; i < path.length; i += 2) {
            colorAt(path[i], path[i + 1], argbTop, argbBottom, bounds, originX, originY, lerp, 1.0F);
            GL11.glVertex2f(path[i], path[i + 1]);
        }
        colorAt(path[0], path[1], argbTop, argbBottom, bounds, originX, originY, lerp, 1.0F);
        GL11.glVertex2f(path[0], path[1]);
        GL11.glEnd();
    }

    /** A stroke between two expanded copies of the same path. */
    private static void strip(float[] outer, int outerColor, float[] inner, int innerColor,
                              float originX, float originY, AngularLerp lerp) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i < outer.length; i += 2) {
            colorAt(outer[i], outer[i + 1], outerColor, outerColor, FULL_BOUNDS, originX, originY, lerp, 1.0F);
            GL11.glVertex2f(outer[i], outer[i + 1]);
            colorAt(inner[i], inner[i + 1], innerColor, innerColor, FULL_BOUNDS, originX, originY, lerp, 1.0F);
            GL11.glVertex2f(inner[i], inner[i + 1]);
        }
        colorAt(outer[0], outer[1], outerColor, outerColor, FULL_BOUNDS, originX, originY, lerp, 1.0F);
        GL11.glVertex2f(outer[0], outer[1]);
        colorAt(inner[0], inner[1], innerColor, innerColor, FULL_BOUNDS, originX, originY, lerp, 1.0F);
        GL11.glVertex2f(inner[0], inner[1]);
        GL11.glEnd();
    }

    private static final float[] FULL_BOUNDS = {Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY};

    private static float[] verticalBounds(float[] path) {
        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;
        for (int i = 1; i < path.length; i += 2) {
            min = Math.min(min, path[i]);
            max = Math.max(max, path[i]);
        }
        return new float[] {min, max};
    }

    private static float centroid(float[] path, int axis) {
        float sum = 0.0F;
        int count = 0;
        for (int i = axis; i < path.length; i += 2) {
            sum += path[i];
            count++;
        }
        return count == 0 ? 0.0F : sum / count;
    }

    /** Vertical gradient blends by the vertex's y across the path's bounds; angular overrides. */
    private static void colorAt(float x, float y, int argbTop, int argbBottom, float[] bounds,
                                float originX, float originY, AngularLerp lerp, float alphaScale) {
        int argb;
        if (lerp != null) {
            argb = lerp.at(x - originX, y - originY);
        } else if (argbTop != argbBottom) {
            float span = Math.max(1.0F, bounds[1] - bounds[0]);
            float t = (y - bounds[0]) / span;
            argb = blend(argbTop, argbBottom, t);
        } else {
            argb = argbTop;
        }
        float a = ((argb >>> 24) & 0xFF) / 255.0F * alphaScale;
        float r = ((argb >>> 16) & 0xFF) / 255.0F;
        float g = ((argb >>> 8) & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;
        GL11.glColor4f(r, g, b, a);
    }

    static int blend(int from, int to, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int a = Math.round(((from >> 24) & 0xFF) + (((to >> 24) & 0xFF) - ((from >> 24) & 0xFF)) * t);
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** A colour that depends on the angle of the vertex around a centre. */
    private interface AngularLerp {
        int at(float dx, float dy);

        static AngularLerp of(final int argb1, final int argb2, final long timeMillis) {
            return new AngularLerp() {
                public int at(float dx, float dy) {
                    float angle = (float) ((Math.atan2(dy, dx) + Math.PI) / (2.0 * Math.PI));
                    float spin = (timeMillis % 3600L) / 3600.0F;
                    float t = (angle + spin) % 1.0F;
                    float shaped = Math.abs(t * 2.0F - 1.0F);
                    return blend(argb1, argb2, shaped);
                }
            };
        }
    }
}
