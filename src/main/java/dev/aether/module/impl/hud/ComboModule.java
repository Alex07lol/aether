package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class ComboModule extends AbstractModule {
    public ComboModule() {
        super(ModuleMetadata.builder("hud.combo", "Combo Counter")
            .category(ModuleCategory.HUD)
            .description("Counts consecutive hits and shows the combo on the HUD.")
            .build());

        addChoice("mode", "Mode", "Modern").choices("Modern", "Legacy");
        addBool("show_background", "Show Background", true);
        addBool("show_label", "Show Label", true);
        addNumber("reset_time", "Reset After (ms)", 2000, 250, 10000, 250);
        addColor("text_color", "Text Color", 0xFFFFFFFF);
        addColor("background_color", "Background Color", 0x6F000000);
    }
}
