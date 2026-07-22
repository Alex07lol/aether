package dev.aether.module.impl.performance;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class FpsOptimizerModule extends AbstractModule {
    public FpsOptimizerModule() {
        super(ModuleMetadata.builder("performance.fps_optimizer", "FPS Optimizer")
            .category(ModuleCategory.PERFORMANCE)
            .description("Applies aggressive performance optimizations and video settings for maximum FPS.")
            .favoriteByDefault(true)
            .build());

        addBool("fast_graphics", "Fast Graphics", true);
        addBool("use_vbo", "Use VBO", true);
        addBool("fast_lighting", "Fast Smooth Lighting", true);
        addBool("memory_cleanup", "Memory Cleanup", true);
    }
}
