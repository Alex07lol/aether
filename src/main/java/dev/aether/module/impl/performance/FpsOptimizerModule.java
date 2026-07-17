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

        addBool("limit_particles", "Limit Particles", true);
        addBool("fast_graphics", "Fast Graphics", true);
        addBool("use_vbo", "Use VBO", true);
        addBool("disable_entity_shadows", "Disable Entity Shadows", true);
        addBool("disable_clouds", "Disable Clouds", true);
        addBool("fast_lighting", "Fast Smooth Lighting", true);
        addBool("memory_cleanup", "Memory Cleanup", true);
        addNumber("memory_threshold", "Memory Threshold", 70);
        addNumber("max_render_distance", "Max Render Distance", 8);
        addNumber("fps_limit", "FPS Limit", 260);
    }
}
