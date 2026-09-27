package dev.aether.module.impl.cosmetics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

/**
 * Particle trail behind the player: vanilla particles are spawned through the world, so the
 * trail is a real in-world effect rather than a HUD overlay.
 */
public class CurrentTrailModule extends AbstractModule {
    public static final String ID = "cosmetics.current_trail";

    public CurrentTrailModule() {
        super(ModuleMetadata.builder(ID, "Particle Trail")
            .category(ModuleCategory.COSMETICS)
            .description("Spawns a vanilla particle trail behind the player.")
            .build());
        addChoice("particle", "Particle", "Cloud",
            "Cloud", "Flame", "Crit", "Magic Crit", "Smoke", "Heart", "Spark");
        addNumber("rate", "Rate", 2, 1, 10, 1);
        addNumber("spread", "Spread", 30, 0, 100, 5);
        addNumber("speed_threshold", "Only While Moving", 0, 0, 100, 5);
        addNumber("size", "Velocity", 100, 50, 200, 5);
    }
}
