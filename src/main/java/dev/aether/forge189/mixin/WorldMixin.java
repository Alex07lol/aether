package dev.aether.forge189.mixin;

import dev.aether.forge189.MixinFeatures;
import dev.aether.graphics.TimeChangerMath;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Rendering-only world values for {@code graphics.time_changer} and {@code graphics.weather_toggle}.
 * <p>
 * Both modules used to write the client world itself - {@code setWorldTime} froze the clock and
 * {@code setWorldSetRain} cleared the weather - which is state the server, the scoreboard and every
 * other mod read. This mixin replaces those writes with two read-side hooks:
 * <ul>
 *   <li>{@code getCelestialAngle} is what the sky, sun, moon, fog colours and ambient light are all
 *       derived from, so recomputing it from the real world time plus the module's offset moves the
 *       time of day without touching the clock. An offset of 0 reproduces vanilla exactly.</li>
 *   <li>{@code getRainStrength} is what the rain renderer, the sky colour, the fog and the rain
 *       sounds read, so reporting zero hides the weather while the world's own rain state stays
 *       exactly as the server left it - there is nothing to restore on disable.</li>
 * </ul>
 * Both injections are soft ({@code require = 0}): if a runtime's mappings disagree, the module
 * becomes a no-op instead of crashing the client, and because neither hook writes anything, a
 * skipped hook cannot leave state behind either. The published state lives on
 * {@link MixinFeatures.World} - a mixin class may not carry non-private static fields.
 */
@Mixin(World.class)
public abstract class WorldMixin {

    @Inject(method = "getCelestialAngle", at = @At("RETURN"), cancellable = true, require = 0)
    private void aetherVisualTime(float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (!MixinFeatures.World.visualTimeActive) {
            return;
        }
        // Primitive return overload: this hook runs several times per frame, so it does not box.
        cir.setReturnValue(TimeChangerMath.celestialAngle(
            TimeChangerMath.visualTime(MixinFeatures.World.visualTimeWorldSnapshot,
                MixinFeatures.World.visualTimeOffset),
            partialTicks));
    }

    @Inject(method = "getRainStrength", at = @At("RETURN"), cancellable = true, require = 0)
    private void aetherRainStrength(float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (MixinFeatures.World.weatherOverrideActive) {
            cir.setReturnValue(MixinFeatures.World.weatherRainStrength);
        }
    }

    @Inject(method = "getThunderStrength", at = @At("RETURN"), cancellable = true, require = 0)
    private void aetherThunderStrength(float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (MixinFeatures.World.weatherOverrideActive) {
            cir.setReturnValue(MixinFeatures.World.weatherThunderStrength);
        }
    }
}
