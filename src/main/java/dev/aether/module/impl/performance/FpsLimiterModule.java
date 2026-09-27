package dev.aether.module.impl.performance;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

/**
 * Caps the client's framerate.
 * <p>
 * Vanilla already sleeps the remainder of a frame against its "Max Framerate" setting, so the module
 * drives that setting instead of inventing a second frame loop: the player's own value is captured
 * once and put back when the module is disabled. A separate cap applies while the window is not
 * focused, which is the usual reason to want a limiter at all.
 */
public class FpsLimiterModule extends AbstractModule {
    public FpsLimiterModule() {
        super(ModuleMetadata.builder("performance.fps_limiter", "FPS Limiter")
            .category(ModuleCategory.PERFORMANCE)
            .description("Caps the framerate, with a separate cap while the window is unfocused.")
            .build());

        // 0 means "no cap"; anything else is clamped into vanilla's own slider range.
        addNumber("target_fps", "Target FPS", 120, 0, 300, 5);
        addNumber("unfocused_fps", "Unfocused FPS", 30, 0, 300, 5);
    }
}
