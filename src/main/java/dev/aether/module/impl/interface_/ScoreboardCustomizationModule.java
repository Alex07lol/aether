package dev.aether.module.impl.interface_;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class ScoreboardCustomizationModule extends AbstractModule {
    public ScoreboardCustomizationModule() {
        super(ModuleMetadata.builder("interface.scoreboard_customization", "Scoreboard Customization")
            .category(ModuleCategory.INTERFACE)
            .description("Customizes the vanilla scoreboard presentation.")
            .build());

        addBool("show_background", "Show Background", true);
        addBool("text_shadow", "Text Shadow", true);
        addBool("hide_red_numbers", "Hide Red Numbers", false);
        addNumber("scale", "Scale", 100);
        addColor("title_color", "Title Color", 0xFFFFFFFF);
        addColor("text_color", "Text Color", 0xFFFFFFFF);
        addColor("background_color", "Background Color", 0x6F000000);
    }
}
