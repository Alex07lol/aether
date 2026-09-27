package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class MemoryModule extends AbstractModule {
    public MemoryModule() {
        super(ModuleMetadata.builder("hud.memory", "Memory Usage")
            .category(ModuleCategory.HUD)
            .description("Shows used and allocated JVM memory.")
            .build());

        addBool("show_background", "Show Background", false);
        addBool("show_percent", "Show Percent", true);
        addColor("text_color", "Text Color", 0xFFFFFFFF);
        addColor("background_color", "Background Color", 0x6F000000);
    }
}
