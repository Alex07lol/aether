package dev.aether.module.impl.cosmetics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;
import dev.aether.module.ModuleKind;

/**
 * Ribbon trail: a continuous band drawn through the recent player positions, tinted from the
 * selected trail cosmetic. Independent of the particle trail module, so both can run at once.
 */
public class TrailCosmeticsModule extends AbstractModule {
    public static final String ID = "cosmetics.trails";

    public TrailCosmeticsModule() {
        super(ModuleMetadata.builder(ID, "Ribbon Trail")
            .category(ModuleCategory.COSMETICS)
            .kind(ModuleKind.COSMETIC_SERVICE)
            .description("Draws a fading ribbon through the player's recent path.")
            .build());
        addNumber("length", "Length", 24, 5, 60, 1);
        addNumber("width", "Width", 100, 10, 150, 5);
        addNumber("opacity", "Opacity", 60, 0, 100, 5);
        addBool("glow", "Glow", false);
    }
}
