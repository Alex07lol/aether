package dev.aether.module.impl.graphics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class UiBlurModule extends AbstractModule {
    public UiBlurModule() {
        super(ModuleMetadata.builder("graphics.ui_blur", "UI Blur")
            .category(ModuleCategory.GRAPHICS)
            .description("Controls the Click GUI overlay dim and blur effect.")
            .build());

        addNumber("amount", "Amount", 52);
    }
}
