package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class PingModule extends AbstractModule {
    public PingModule() {
        super(ModuleMetadata.builder("hud.ping", "Ping")
            .category(ModuleCategory.HUD)
            .description("Shows your latency ping on the HUD.")
            .build());

        addChoice("mode", "Mode", "Modern");
        addBool("show_background", "Show Background", true);
        addColor("text_color", "Text Color", 0xFFFFFFFF);
        addColor("background_color", "Background Color", 0x6F000000);
    }
}
