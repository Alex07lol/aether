package dev.aether.module.builtin;

import dev.aether.module.ModuleRegistry;
import dev.aether.module.impl.performance.DeveloperOverlayModule;
import dev.aether.module.impl.performance.FpsLimiterModule;
import dev.aether.module.impl.performance.FpsOptimizerModule;
import dev.aether.module.impl.performance.ScreenshotModule;

final class PerformanceModules {
    private PerformanceModules() {
    }

    static void register(ModuleRegistry modules) {
        modules.register(new FpsOptimizerModule());
        modules.register(new FpsLimiterModule());
        modules.register(new ScreenshotModule());
        modules.register(new DeveloperOverlayModule());
    }
}
