package dev.aether.module.builtin;

import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ModuleRegistry;

import static dev.aether.module.builtin.BuiltInModuleFactory.bool;
import static dev.aether.module.builtin.BuiltInModuleFactory.module;
import static dev.aether.module.builtin.BuiltInModuleFactory.number;

final class PerformanceModules {
    private PerformanceModules() {
    }

    static void register(ModuleRegistry modules) {
        module(modules, "performance.fps_optimizer", "FPS Optimizer", ModuleCategory.PERFORMANCE, "Applies conservative local video settings while enabled.", true,
            bool("limit_particles", "Limit Particles", true),
            bool("fast_graphics", "Fast Graphics", true),
            bool("use_vbo", "Use VBO", true),
            bool("memory_cleanup", "Memory Cleanup", false),
            number("memory_threshold", "Memory Threshold", 72),
            number("max_render_distance", "Max Render Distance", 8),
            number("fps_limit", "FPS Limit", 260));
        module(modules, "developer.overlay", "Developer Overlay", ModuleCategory.PERFORMANCE, "Shows local frame, memory, and runtime diagnostics for development.");
    }
}
