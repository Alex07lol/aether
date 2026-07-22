package dev.aether.forge189.mixin;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.FloatBuffer;

@Mixin(RendererLivingEntity.class)
public abstract class RendererLivingEntityMixin<T extends EntityLivingBase> extends Render<T> {

    public static boolean customHitColorEnabled = false;
    public static float hitColorRed = 1.0F;
    public static float hitColorGreen = 0.0F;
    public static float hitColorBlue = 0.0F;
    public static float hitColorAlpha = 0.3F;

    protected RendererLivingEntityMixin(RenderManager renderManager) {
        super(renderManager);
    }

    @Redirect(method = "setBrightness", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 0))
    public FloatBuffer setRed(FloatBuffer instance, float v) {
        if (customHitColorEnabled) {
            instance.put(hitColorRed);
        } else {
            instance.put(v);
        }
        return instance;
    }

    @Redirect(method = "setBrightness", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 1))
    public FloatBuffer setGreen(FloatBuffer instance, float v) {
        if (customHitColorEnabled) {
            instance.put(hitColorGreen);
        } else {
            instance.put(v);
        }
        return instance;
    }

    @Redirect(method = "setBrightness", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 2))
    public FloatBuffer setBlue(FloatBuffer instance, float v) {
        if (customHitColorEnabled) {
            instance.put(hitColorBlue);
        } else {
            instance.put(v);
        }
        return instance;
    }

    @Redirect(method = "setBrightness", at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 3))
    public FloatBuffer setAlpha(FloatBuffer instance, float v) {
        if (customHitColorEnabled) {
            instance.put(hitColorAlpha);
        } else {
            instance.put(v);
        }
        return instance;
    }
}
