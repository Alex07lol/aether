package dev.aether.forge189.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.aether.forge189.Mc189Compat;
import dev.aether.forge189.MixinFeatures;
import net.minecraft.entity.Entity;

/**
 * Freelook's guarantee that the player's body and head never move while the camera is free.
 * <p>
 * {@code setAngles} — the method every mouse-driven rotation update flows through — is declared on
 * {@link Entity}, not on {@code EntityPlayerSP}: a mixin targeting the subclass silently never
 * applies, because injection works on the declaring class's bytecode. Targeting {@link Entity}
 * directly reaches the one call site that matters ({@code EntityPlayerSP.setAngles} inherits it),
 * and the local-player check below makes the freeze a no-op for every other entity.
 * <p>
 * The module keeps its own yaw and pitch and cancels the mouse delta, which is enough on its own -
 * but only if the delta is actually consumed there. Dropping the {@code setAngles} call as well
 * makes the guarantee unconditional and keeps head yaw, body yaw and the previous-yaw smoothing in
 * step: the player is frozen mid-pose instead of being rotated by whichever code path runs first.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "setAngles(FF)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void freezeLocalPlayerRotation(float yawDelta, float pitchDelta, CallbackInfo ci) {
        if (MixinFeatures.Entity.freelookFreezesRotation && Mc189Compat.isLocalPlayer(this)) {
            ci.cancel();
        }
    }
}
