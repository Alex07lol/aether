package dev.aether.forge189.mixin;

import dev.aether.forge189.MixinFeatures;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.FloatBuffer;

/**
 * Damage overlay colour for {@code graphics.hit_color}.
 * <p>
 * {@code setBrightness} fills the brightness buffer with the four channels of the entity's
 * damage overlay right before the model is drawn, so replacing the values tints the hit flash
 * without touching the hurt timer or the entity's own colour.
 * <p>
 * The redirects are marked optional ({@code require = 0}) because they depend on the order of
 * the buffer writes in the mapped method. On a runtime where that order differs the tint is
 * skipped and logged instead of failing the whole mixin; the module's hit outline (drawn by the
 * bridge) still works. The published state lives on
 * {@link MixinFeatures.RendererLivingEntity} - a mixin class may not carry non-private static
 * fields.
 */
@Mixin(RendererLivingEntity.class)
public abstract class RendererLivingEntityMixin {

    @Redirect(method = "setBrightness", require = 0,
        at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 0))
    private FloatBuffer setRed(FloatBuffer instance, float value) {
        return instance.put(MixinFeatures.RendererLivingEntity.customHitColorEnabled
            ? MixinFeatures.RendererLivingEntity.hitColorRed : value);
    }

    @Redirect(method = "setBrightness", require = 0,
        at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 1))
    private FloatBuffer setGreen(FloatBuffer instance, float value) {
        return instance.put(MixinFeatures.RendererLivingEntity.customHitColorEnabled
            ? MixinFeatures.RendererLivingEntity.hitColorGreen : value);
    }

    @Redirect(method = "setBrightness", require = 0,
        at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 2))
    private FloatBuffer setBlue(FloatBuffer instance, float value) {
        return instance.put(MixinFeatures.RendererLivingEntity.customHitColorEnabled
            ? MixinFeatures.RendererLivingEntity.hitColorBlue : value);
    }

    @Redirect(method = "setBrightness", require = 0,
        at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 3))
    private FloatBuffer setAlpha(FloatBuffer instance, float value) {
        return instance.put(MixinFeatures.RendererLivingEntity.customHitColorEnabled
            ? MixinFeatures.RendererLivingEntity.hitColorAlpha : value);
    }
}
