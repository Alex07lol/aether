package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class FpsModule extends AbstractModule {
    public FpsModule() {
        super(ModuleMetadata.builder("hud.fps", "FPS Counter")
            .category(ModuleCategory.HUD)
            .description("Displays the current frames per second.")
            .favoriteByDefault(true)
            .build());

        addBool("show_background", "Show Background", true);
        addColor("text_color", "Text Color", 0xFFFFFFFF);
        addColor("background_color", "Background Color", 0x6F000000);
    }
}
