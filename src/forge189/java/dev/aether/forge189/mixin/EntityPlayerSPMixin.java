package dev.aether.forge189.mixin;

import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Freelook's guarantee that the player's body and head never move while the camera is free.
 * <p>
 * The module keeps its own yaw and pitch and cancels the mouse delta, which is enough on its own -
 * but only if the delta is actually consumed there. Dropping the call to {@code setAngles} as well
 * makes the guarantee unconditional and, more importantly, keeps head yaw, body yaw and the
 * previous-yaw smoothing in step: the player is frozen mid-pose instead of being rotated by
 * whichever code path happens to run first.
 * <p>
 * The hook is soft ({@code require = 0}), so a runtime whose mappings do not expose {@code setAngles}
 * simply falls back to the mouse-event cancellation.
 */
@Mixin(EntityPlayerSP.class)
public abstract class EntityPlayerSPMixin {

    /** Set by ForgeClientEventBridge while freelook is held. */
    public static boolean freelookFreezesRotation = false;

    @Inject(method = "setAngles", at = @At("HEAD"), cancellable = true, require = 0)
    private void freezePlayerRotation(float yawDelta, float pitchDelta, CallbackInfo ci) {
        if (freelookFreezesRotation) {
            ci.cancel();
        }
    }
}
