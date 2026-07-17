package dev.aether.module.impl.performance;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class FpsOptimizerModule extends AbstractModule {
    public FpsOptimizerModule() {
        super(ModuleMetadata.builder("performance.fps_optimizer", "FPS Optimizer")
            .category(ModuleCategory.PERFORMANCE)
            .description("Applies conservative local video settings while enabled.")
            .favoriteByDefault(true)
            .build());

        addBool("limit_particles", "Limit Particles", true);
        addBool("fast_graphics", "Fast Graphics", true);
        addBool("use_vbo", "Use VBO", true);
        addBool("memory_cleanup", "Memory Cleanup", false);
        addNumber("memory_threshold", "Memory Threshold", 72);
        addNumber("max_render_distance", "Max Render Distance", 8);
        addNumber("fps_limit", "FPS Limit", 260);
    }
}
