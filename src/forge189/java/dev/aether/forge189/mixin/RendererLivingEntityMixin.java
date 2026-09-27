package dev.aether.forge189.mixin;

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
 * bridge) still works.
 */
@Mixin(RendererLivingEntity.class)
public abstract class RendererLivingEntityMixin {

    /** Set by ForgeClientEventBridge while graphics.hit_color is on. */
    public static boolean customHitColorEnabled = false;

    public static float hitColorRed = 1.0F;
    public static float hitColorGreen = 0.0F;
    public static float hitColorBlue = 0.0F;
    public static float hitColorAlpha = 0.3F;

    @Redirect(method = "setBrightness", require = 0,
        at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 0))
    public FloatBuffer setRed(FloatBuffer instance, float value) {
        return instance.put(customHitColorEnabled ? hitColorRed : value);
    }

    @Redirect(method = "setBrightness", require = 0,
        at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 1))
    public FloatBuffer setGreen(FloatBuffer instance, float value) {
        return instance.put(customHitColorEnabled ? hitColorGreen : value);
    }

    @Redirect(method = "setBrightness", require = 0,
        at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 2))
    public FloatBuffer setBlue(FloatBuffer instance, float value) {
        return instance.put(customHitColorEnabled ? hitColorBlue : value);
    }

    @Redirect(method = "setBrightness", require = 0,
        at = @At(value = "INVOKE", target = "Ljava/nio/FloatBuffer;put(F)Ljava/nio/FloatBuffer;", ordinal = 3))
    public FloatBuffer setAlpha(FloatBuffer instance, float value) {
        return instance.put(customHitColorEnabled ? hitColorAlpha : value);
    }
}
