package dev.aether.module.impl.graphics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class ParticlesModule extends AbstractModule {
    public ParticlesModule() {
        super(ModuleMetadata.builder("graphics.particles", "Particles")
            .category(ModuleCategory.GRAPHICS)
            .description("Customizes local hit particle feedback.")
            .build());

        addNumber("particle_amount", "Particle Amount", 5);
        addChoice("show_criticals", "Show Criticals", "Vanilla");
        addChoice("show_sharpness", "Show Sharpness", "Vanilla");
    }
}
