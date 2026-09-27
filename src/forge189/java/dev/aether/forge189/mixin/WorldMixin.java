package dev.aether.forge189.mixin;

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
 * skipped hook cannot leave state behind either.
 */
@Mixin(World.class)
public abstract class WorldMixin {

    /** Set by ForgeClientEventBridge while graphics.time_changer is on in the overworld. */
    public static boolean visualTimeActive = false;

    /** The real world time sampled on the last tick; never written back. */
    public static long visualTimeWorldSnapshot = 0L;

    /** The offset in ticks the sky is drawn with. */
    public static int visualTimeOffset = 0;

    /** Set by ForgeClientEventBridge while graphics.weather_toggle is overriding the weather. */
    public static boolean weatherOverrideActive = false;

    /** The strengths to report while the override is active, as 0-1 values. */
    public static float weatherRainStrength = 0.0F;
    public static float weatherThunderStrength = 0.0F;

    @Inject(method = "getCelestialAngle", at = @At("RETURN"), cancellable = true, require = 0)
    private void aetherVisualTime(float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (!visualTimeActive) {
            return;
        }
        // Primitive return overload: this hook runs several times per frame, so it does not box.
        cir.setReturnValue(TimeChangerMath.celestialAngle(
            TimeChangerMath.visualTime(visualTimeWorldSnapshot, visualTimeOffset), partialTicks));
    }

    @Inject(method = "getRainStrength", at = @At("RETURN"), cancellable = true, require = 0)
    private void aetherRainStrength(float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (weatherOverrideActive) {
            cir.setReturnValue(weatherRainStrength);
        }
    }

    @Inject(method = "getThunderStrength", at = @At("RETURN"), cancellable = true, require = 0)
    private void aetherThunderStrength(float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (weatherOverrideActive) {
            cir.setReturnValue(weatherThunderStrength);
        }
    }
}
