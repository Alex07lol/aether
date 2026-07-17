package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class DirectionModule extends AbstractModule {
    public DirectionModule() {
        super(ModuleMetadata.builder("hud.direction", "Direction HUD")
            .category(ModuleCategory.HUD)
            .description("Shows the player's facing direction.")
            .favoriteByDefault(true)
            .build());

        addChoice("style", "Style", "Compass");
        addColor("text_color", "Text Color", 0xFFFFFFFF);
    }
}
