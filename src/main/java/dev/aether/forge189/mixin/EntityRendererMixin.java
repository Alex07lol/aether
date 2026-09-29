package dev.aether.forge189.mixin;

import dev.aether.forge189.Mc189Compat;
import dev.aether.forge189.MixinFeatures;
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
 * Both injections are declared soft ({@code require = 0}): if a runtime's mappings ever disagree
 * with the targets below the mixin is skipped and the feature stays off rather than failing the
 * whole client at startup. The bridge's outline work (hit colour, block overlay) does not depend
 * on this class at all. The published state lives on {@link MixinFeatures.EntityRenderer} - a
 * mixin class may not carry non-private static fields.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    /* ------------------------------------------------------------------ */
    /*  graphics.no_hurt_cam                                               */
    /* ------------------------------------------------------------------ */

    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true, require = 0)
    private void scaledHurtCameraEffect(float partialTicks, CallbackInfo ci) {
        float scale = HurtCamMath.clamp(MixinFeatures.EntityRenderer.hurtCameraScale, 0.0F, 1.0F);
        if (!MixinFeatures.EntityRenderer.hurtCameraScaled || !HurtCamMath.applies(scale)) {
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

    /** Milliseconds the zoom spends travelling 63% of the way to its target. */
    private static final float ZOOM_SETTLE_MILLIS = 120.0F;

    /**
     * Scales the field of view vanilla is about to use. The idle path is two comparisons and a
     * return, so a client that never touches zoom pays nothing per frame.
     */
    @Inject(method = "getFOVModifier", at = @At("RETURN"), cancellable = true, require = 0)
    private void aetherZoom(float partialTicks, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
        float target = ZoomMath.clamp(MixinFeatures.EntityRenderer.zoomTargetScale, 0.05F, ZoomMath.NO_ZOOM);
        float current = MixinFeatures.EntityRenderer.zoomCurrentScale;
        if (ZoomMath.settled(current, target) && ZoomMath.settled(target, ZoomMath.NO_ZOOM)) {
            MixinFeatures.EntityRenderer.zoomCurrentScale = ZoomMath.NO_ZOOM;
            MixinFeatures.EntityRenderer.zoomLastFrameNanos = 0L;
            return;
        }
        long now = System.nanoTime();
        float deltaMillis = MixinFeatures.EntityRenderer.zoomLastFrameNanos == 0L
            ? 0.0F
            : (now - MixinFeatures.EntityRenderer.zoomLastFrameNanos) / 1000000.0F;
        MixinFeatures.EntityRenderer.zoomLastFrameNanos = now;
        if (ZoomMath.settled(current, target)) {
            current = target;
        } else {
            current = ZoomMath.smooth(current, target, deltaMillis, ZOOM_SETTLE_MILLIS);
        }
        MixinFeatures.EntityRenderer.zoomCurrentScale = current;
        if (current < ZoomMath.NO_ZOOM) {
            // Primitive return overload: no boxing on a path that runs every frame while zooming.
            cir.setReturnValue(cir.getReturnValueF() * current);
        }
    }
}
