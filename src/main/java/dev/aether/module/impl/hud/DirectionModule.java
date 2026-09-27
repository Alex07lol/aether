package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class DirectionModule extends AbstractModule {
    public DirectionModule() {
        super(ModuleMetadata.builder("hud.direction", "Direction HUD")
            .category(ModuleCategory.HUD)
            .description("Shows the cardinal direction you are facing.")
            .favoriteByDefault(true)
            .build());

        addChoice("style", "Style", "Compass").choices("Compass", "Simple");
        addBool("show_background", "Show Background", false);
        addColor("text_color", "Text Color", 0xFFFFFFFF);
        addColor("background_color", "Background Color", 0x6F000000);
    }
}
