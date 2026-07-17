package dev.aether.module.impl.interface_;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class GuiTweaksModule extends AbstractModule {
    public GuiTweaksModule() {
        super(ModuleMetadata.builder("interface.gui_tweaks", "GUI Tweaks")
            .category(ModuleCategory.INTERFACE)
            .description("Provides interface and container GUI tweaks.")
            .build());
    }
}
