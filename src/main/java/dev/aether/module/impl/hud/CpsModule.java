package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class CpsModule extends AbstractModule {
    public CpsModule() {
        super(ModuleMetadata.builder("hud.cps", "CPS Counter")
            .category(ModuleCategory.HUD)
            .description("Shows local mouse click speed.")
            .build());

        addChoice("mode", "Mode", "Modern");
        addBool("show_background", "Show Background", true);
        addBool("right_click", "Right Click", false);
        addColor("background_color", "Background Color", 0x6F000000);
        addColor("text_color", "Text Color", 0xFFFFFFFF);
    }
}
