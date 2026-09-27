package dev.aether.forge189.mixin;

import dev.aether.forge189.Mc189Compat;
import dev.aether.graphics.HurtCamMath;
import dev.aether.graphics.ZoomMath;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The camera effects that need a hook inside vanilla's render path: the hurt-camera shake for
 * {@code graphics.no_hurt_cam} and the field of view override for {@code pvp.zoom}.
 * <p>
 * Neither of them rewrites game state. The shake scales vanilla's own rotation curve instead of
 * zeroing the player's hurt timers, and the zoom scales the value {@code getFOVModifier} is about
 * to return instead of writing {@code GameSettings.fovSetting} - so the player's own FOV slider is
 * never touched, nothing has to be restored, and another mod that changes the FOV is scaled rather
 * than overwritten.
 * <p>
 * Both injections are declared soft ({@code require = 0}): the bridge publishes plain static
 * values, so if a runtime's mappings ever disagree with the targets below the mixin is skipped and
 * the feature stays off rather than failing the whole client at startup. The bridge's outline
 * work (hit colour, block overlay) does not depend on this class at all.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    /* ------------------------------------------------------------------ */
    /*  graphics.no_hurt_cam                                               */
    /* ------------------------------------------------------------------ */

    /** Set by ForgeClientEventBridge while graphics.no_hurt_cam is on. */
    public static boolean hurtCameraScaled = false;

    /** The module's shake amount as a 0-1 scale; {@code 1.0} leaves vanilla's shake alone. */
    public static float hurtCameraScale = 1.0F;

    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true, require = 0)
    private void scaledHurtCameraEffect(float partialTicks, CallbackInfo ci) {
        float scale = HurtCamMath.clamp(hurtCameraScale, 0.0F, 1.0F);
        if (!hurtCameraScaled || !HurtCamMath.applies(scale)) {
            // Disabled, or asked for 100: vanilla keeps drawing its own shake.
            return;
        }
        Object player = Mc189Compat.localPlayer();
        if (player == null) {
            return;
        }
        // Replace vanilla's shake - including cancelling it completely at 0 - with the same
        // rotations multiplied by the module's dial.
        ci.cancel();
        float degrees = HurtCamMath.shakeDegrees(
            Mc189Compat.hurtTime(player), Mc189Compat.maxHurtTime(player), partialTicks, scale);
        if (degrees <= 0.0F) {
            return;
        }
        float attackYaw = Mc189Compat.attackedAtYaw(player);
        Mc189Compat.rotate(-attackYaw, 0.0F, 1.0F, 0.0F);
        Mc189Compat.rotate(-degrees, 0.0F, 0.0F, 1.0F);
        Mc189Compat.rotate(attackYaw, 0.0F, 0.0F, 1.0F);
        Mc189Compat.rotate(degrees, 1.0F, 0.0F, 0.0F);
    }

    /* ------------------------------------------------------------------ */
    /*  pvp.zoom                                                           */
    /* ------------------------------------------------------------------ */

    /** Target FOV multiplier published by ForgeClientEventBridge; 1.0 means "do not zoom". */
    public static float zoomTargetScale = ZoomMath.NO_ZOOM;

    /** Milliseconds the zoom spends travelling 63% of the way to its target. */
    private static final float ZOOM_SETTLE_MILLIS = 120.0F;

    private static float zoomCurrentScale = ZoomMath.NO_ZOOM;
    private static long zoomLastFrameNanos;

    /** Called by the bridge when the module is switched off: snap back, do not glide. */
    public static void resetZoomAnimation() {
        zoomTargetScale = ZoomMath.NO_ZOOM;
        zoomCurrentScale = ZoomMath.NO_ZOOM;
        zoomLastFrameNanos = 0L;
    }

    /**
     * Scales the field of view vanilla is about to use. The idle path is two comparisons and a
     * return, so a client that never touches zoom pays nothing per frame.
     */
    @Inject(method = "getFOVModifier", at = @At("RETURN"), cancellable = true, require = 0)
    private void aetherZoom(float partialTicks, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        float target = ZoomMath.clamp(zoomTargetScale, 0.05F, ZoomMath.NO_ZOOM);
        if (ZoomMath.settled(zoomCurrentScale, target) && ZoomMath.settled(target, ZoomMath.NO_ZOOM)) {
            zoomCurrentScale = ZoomMath.NO_ZOOM;
            zoomLastFrameNanos = 0L;
            return;
        }
        long now = System.nanoTime();
        float deltaMillis = zoomLastFrameNanos == 0L ? 0.0F : (now - zoomLastFrameNanos) / 1000000.0F;
        zoomLastFrameNanos = now;
        if (ZoomMath.settled(zoomCurrentScale, target)) {
            zoomCurrentScale = target;
        } else {
            zoomCurrentScale = ZoomMath.smooth(zoomCurrentScale, target, deltaMillis, ZOOM_SETTLE_MILLIS);
        }
        if (zoomCurrentScale < ZoomMath.NO_ZOOM) {
            // Primitive return overload: no boxing on a path that runs every frame while zooming.
            cir.setReturnValue(cir.getReturnValueF() * zoomCurrentScale);
        }
    }
}
