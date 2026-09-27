package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class PotionStatusModule extends AbstractModule {
    public PotionStatusModule() {
        super(ModuleMetadata.builder("hud.potions", "Potion Effects")
            .category(ModuleCategory.HUD)
            .description("Lists active potion effects with their remaining duration.")
            .build());

        addChoice("mode", "Mode", "Compact").choices("Compact", "Detailed");
        addBool("show_background", "Show Background", false);
        addBool("hide_ambient", "Hide Ambient Effects", false);
        addColor("text_color", "Text Color", 0xFFFFFFFF);
        addColor("background_color", "Background Color", 0x6F000000);
    }
}
