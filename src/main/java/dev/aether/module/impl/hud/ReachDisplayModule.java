package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class ReachDisplayModule extends AbstractModule {
    public ReachDisplayModule() {
        super(ModuleMetadata.builder("hud.reach_display", "Reach Display")
            .category(ModuleCategory.HUD)
            .description("Shows the distance to attacked entities.")
            .build());

        addChoice("mode", "Mode", "Modern");
        addBool("show_background", "Show Background", true);
        addColor("text_color", "Text Color", 0xFFFFFFFF);
        addColor("background_color", "Background Color", 0x6F000000);
    }
}
