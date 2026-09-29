package dev.aether.forge189;

import dev.aether.graphics.ZoomMath;

/**
 * Plain state holders shared between {@link ForgeClientEventBridge} and the mixins.
 * <p>
 * Mixin 0.7 refuses to apply a mixin class that declares a non-private static field
 * ({@code InvalidMixinException: contains non-private static field}). The first real
 * {@code runClient} proved every Aether mixin was dying on exactly that rule - as a
 * WARN, so the client kept running with all features silently off. Every value the
 * bridge publishes therefore lives here instead, and the mixins read these fields
 * directly. All of them are written and read on the client thread only.
 * <p>
 * One holder per mixin target, named after it, so ownership stays obvious.
 */
public final class MixinFeatures {

    private MixinFeatures() {
    }

    /** State for {@code EntityMixin}: the freelook rotation freeze. */
    public static final class Entity {

        private Entity() {
        }

        /** Set while freelook is held; {@code EntityMixin} cancels {@code setAngles} for the local player. */
        public static boolean freelookFreezesRotation = false;
    }

    /** State for {@code EntityRendererMixin}: no-hurt-cam and zoom. */
    public static final class EntityRenderer {

        private EntityRenderer() {
        }

        /** Set while {@code graphics.no_hurt_cam} is on; scales vanilla's shake. */
        public static boolean hurtCameraScaled = false;

        /** The module's shake amount as a 0-1 scale; {@code 1.0} leaves vanilla's shake alone. */
        public static float hurtCameraScale = 1.0F;

        /** Target FOV multiplier; {@link ZoomMath#NO_ZOOM} means "do not zoom". */
        public static float zoomTargetScale = ZoomMath.NO_ZOOM;

        /** The animated current multiplier travelling towards {@link #zoomTargetScale}. */
        public static float zoomCurrentScale = ZoomMath.NO_ZOOM;

        /** Nanos of the last frame the zoom animation advanced; {@code 0} means "reset". */
        public static long zoomLastFrameNanos = 0L;

        /** Called by the bridge when the zoom module is switched off: snap back, do not glide. */
        public static void resetZoomAnimation() {
            zoomTargetScale = ZoomMath.NO_ZOOM;
            zoomCurrentScale = ZoomMath.NO_ZOOM;
            zoomLastFrameNanos = 0L;
        }
    }

    /** State for {@code ItemRendererMixin}: the 1.7 first-person animation toggles. */
    public static final class ItemRenderer {

        private ItemRenderer() {
        }

        /** Set while {@code graphics.animation > block_animation} is on. */
        public static boolean blockAnimationEnabled = false;

        /** Set while {@code graphics.animation > eat_drink_animation} is on. */
        public static boolean eatDrinkAnimationEnabled = false;

        /** Set while {@code graphics.animation > bow_animation} is on. */
        public static boolean bowAnimationEnabled = false;

        /** Set while {@code graphics.animation > rod_animation} is on. */
        public static boolean fishingRodAnimationEnabled = false;

        /** Partial ticks of the frame being rendered; captured by {@code ItemRendererMixin}. */
        public static float renderingPartialTicks;
    }

    /** State for {@code RendererLivingEntityMixin}: the hit flash tint. */
    public static final class RendererLivingEntity {

        private RendererLivingEntity() {
        }

        /** Set while {@code graphics.hit_color} is on. */
        public static boolean customHitColorEnabled = false;

        /** The tint channels; alpha doubles as the tint strength. */
        public static float hitColorRed = 1.0F;
        public static float hitColorGreen = 0.0F;
        public static float hitColorBlue = 0.0F;
        public static float hitColorAlpha = 0.3F;
    }

    /** State for {@code WorldMixin}: render-only time and weather overrides. */
    public static final class World {

        private World() {
        }

        /** Set while {@code graphics.time_changer} is on in the overworld. */
        public static boolean visualTimeActive = false;

        /** The real world time sampled on the last tick; never written back. */
        public static long visualTimeWorldSnapshot = 0L;

        /** The offset in ticks the sky is drawn with. */
        public static int visualTimeOffset = 0;

        /** Set while {@code graphics.weather_toggle} is overriding the weather. */
        public static boolean weatherOverrideActive = false;

        /** The strengths to report while the override is active, as 0-1 values. */
        public static float weatherRainStrength = 0.0F;
        public static float weatherThunderStrength = 0.0F;
    }
}
