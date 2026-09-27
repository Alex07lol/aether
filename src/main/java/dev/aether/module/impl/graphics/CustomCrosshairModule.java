package dev.aether.module.impl.graphics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class CustomCrosshairModule extends AbstractModule {
    public CustomCrosshairModule() {
        super(ModuleMetadata.builder("graphics.custom_crosshair", "Custom Crosshair")
            .category(ModuleCategory.GRAPHICS)
            .description("Adds a configurable crosshair rendering slot.")
            .build());

        addChoice("shape", "Shape", "Cross").choices("Cross", "Dot", "Circle");
        addColor("color", "Color", 0xFFFFFFFF);
        addNumber("size", "Size", 8, 1, 24, 1);
        addNumber("gap", "Gap", 4, 0, 12, 1);
        addNumber("thickness", "Thickness", 1, 1, 8, 1);
        addBool("show_dot", "Show Dot", false);
    }
}
