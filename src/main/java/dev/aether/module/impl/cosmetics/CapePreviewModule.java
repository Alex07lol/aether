package dev.aether.module.impl.cosmetics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

/**
 * Flat cape preview: renders the worn cape without the wave and walk swing so an imported
 * PNG can be inspected in world at a readable angle.
 */
public class CapePreviewModule extends AbstractModule {
    public static final String ID = "cosmetics.cape_preview";

    public CapePreviewModule() {
        super(ModuleMetadata.builder(ID, "Cape Preview")
            .category(ModuleCategory.COSMETICS)
            .description("Draws the cape flat so imported textures are easy to check.")
            .build());
        addNumber("scale", "Preview Scale", 100, 50, 200, 5);
    }
}
