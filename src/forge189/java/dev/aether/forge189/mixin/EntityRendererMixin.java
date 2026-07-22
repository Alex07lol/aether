package dev.aether.forge189.mixin;

import dev.aether.forge189.Mc189Compat;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    public static boolean freelookActive = false;
    public static float cameraYaw = 0.0F;
    public static float cameraPitch = 0.0F;

    @Redirect(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;setAngles(FF)V"))
    private void lockPlayerLooking(EntityPlayerSP instance, float x, float y) {
        if (!freelookActive) {
            Mc189Compat.setAngles(instance, x, y);
        } else {
            cameraYaw += x * 0.15F;
            cameraPitch -= y * 0.15F;
            if (cameraPitch > 90.0F) cameraPitch = 90.0F;
            if (cameraPitch < -90.0F) cameraPitch = -90.0F;
        }
    }

    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
    private void hurtCameraEffect(float partialTicks, CallbackInfo ci) {
        ci.cancel();
    }
}
