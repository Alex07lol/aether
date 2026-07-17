package dev.aether.module.impl.interface_;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class ThemeSelectorModule extends AbstractModule {
    public ThemeSelectorModule() {
        super(ModuleMetadata.builder("interface.theme_selector", "Theme Selector")
            .category(ModuleCategory.INTERFACE)
            .description("Adds a theme switching utility slot.")
            .build());
    }
}
