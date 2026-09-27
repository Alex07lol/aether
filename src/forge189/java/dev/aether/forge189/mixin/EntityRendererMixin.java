package dev.aether.forge189.mixin;

import dev.aether.forge189.Mc189Compat;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Camera shake for {@code graphics.no_hurt_cam}.
 * <p>
 * Vanilla shakes the camera while the player's hurt timer runs: it rolls and pitches by up to
 * fourteen degrees around the yaw the hit came from. The module's {@code shake_amount} is a
 * 0-100 dial, so this mixin takes over the shake when a scale is published and reproduces
 * vanilla's own rotations at that scale - at 100 the module's draw is unchanged.
 * <p>
 * Doing it on the camera rather than by rewriting the player's hurt timers keeps those timers
 * intact for everything else that reads them (the hurt overlay, other renderers, other mods).
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    /** Set by ForgeClientEventBridge while graphics.no_hurt_cam is on. */
    public static boolean hurtCameraScaled = false;

    /** The module's shake amount as a 0-1 scale; {@code 1.0} leaves vanilla's shake alone. */
    public static float hurtCameraScale = 1.0F;

    /** Degrees vanilla rolls and pitches the camera at full strength. */
    private static final float SHAKE_DEGREES = 14.0F;

    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
    private void scaledHurtCameraEffect(float partialTicks, CallbackInfo ci) {
        if (!hurtCameraScaled || hurtCameraScale >= 1.0F) {
            return;
        }
        Object player = Mc189Compat.localPlayer();
        if (player == null || Mc189Compat.hurtTime(player) <= 0) {
            return;
        }
        // Replace vanilla's shake with the scaled one, including the zero case.
        ci.cancel();
        if (hurtCameraScale <= 0.0F) {
            return;
        }
        int maxHurtTime = Mc189Compat.maxHurtTime(player);
        if (maxHurtTime <= 0) {
            return;
        }
        float remaining = Mc189Compat.hurtTime(player) - partialTicks;
        if (remaining < 0.0F) {
            return;
        }
        float progress = remaining / maxHurtTime;
        // Vanilla's curve: the shake ramps in and out over sin(progress^4 * pi).
        float strength = (float) Math.sin(progress * progress * progress * progress * Math.PI) * hurtCameraScale;
        float attackYaw = Mc189Compat.attackedAtYaw(player);
        Mc189Compat.rotate(-attackYaw, 0.0F, 1.0F, 0.0F);
        Mc189Compat.rotate(-strength * SHAKE_DEGREES, 0.0F, 0.0F, 1.0F);
        Mc189Compat.rotate(attackYaw, 0.0F, 0.0F, 1.0F);
        Mc189Compat.rotate(strength * SHAKE_DEGREES, 1.0F, 0.0F, 0.0F);
    }
}
