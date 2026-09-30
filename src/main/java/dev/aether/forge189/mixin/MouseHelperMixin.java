package dev.aether.forge189.mixin;

import dev.aether.forge189.MixinFeatures;
import net.minecraft.util.MouseHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Publishes the mouse delta of the frame currently being rendered.
 * <p>
 * {@code MouseHelper.mouseXYChange()} is the one place in 1.8.9 that reads the mouse, and
 * {@code EntityRenderer.updateCameraAndRender} calls it exactly once per frame, immediately before
 * the world is drawn. That makes it the only correct source for anything that has to move smoothly
 * with the camera.
 * <p>
 * The alternative - Forge's {@code MouseEvent} - fires from {@code Minecraft.runTick()}, and
 * {@code runGameLoop} calls {@code runTick()} once per <em>elapsed game tick</em>, not once per
 * frame. Driving the freelook camera from it moved the view in ~20 Hz steps while the world
 * rendered at the display's rate, which is the jitter this mixin removes. Cancelling that event is
 * also not a safe way to freeze the player: {@code runTick} does
 * {@code if (ForgeHooksClient.postMouseEvent()) continue;}, so cancelling it swallows vanilla's
 * attack/use/wheel handling for those events.
 * <p>
 * The injection is soft ({@code require = 0}): on a runtime whose mappings disagree with
 * {@code mouseXYChange} the hook is skipped and freelook falls back to the tick-rate path rather
 * than failing startup. {@code deltaX}/{@code deltaY} are public fields on the target, so no
 * {@code @Shadow} is needed.
 */
@Mixin(MouseHelper.class)
public abstract class MouseHelperMixin {

    @Inject(method = "mouseXYChange", at = @At("RETURN"), require = 0)
    private void aetherPublishFrameDelta(CallbackInfo ci) {
        MouseHelper helper = (MouseHelper) (Object) this;
        MixinFeatures.Mouse.capture(helper.deltaX, helper.deltaY);
    }
}
