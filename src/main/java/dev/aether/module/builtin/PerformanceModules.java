package dev.aether.module.builtin;

import dev.aether.module.ModuleRegistry;
import dev.aether.module.impl.performance.DeveloperOverlayModule;
import dev.aether.module.impl.performance.FpsOptimizerModule;

final class PerformanceModules {
    private PerformanceModules() {
    }

    static void register(ModuleRegistry modules) {
        modules.register(new FpsOptimizerModule());
        modules.register(new DeveloperOverlayModule());
    }
}
