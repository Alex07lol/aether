package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class CoordinatesModule extends AbstractModule {
    public CoordinatesModule() {
        super(ModuleMetadata.builder("hud.coordinates", "Coordinates")
            .category(ModuleCategory.HUD)
            .description("Displays the player's current X, Y, and Z position.")
            .favoriteByDefault(true)
            .build());

        addChoice("mode", "Mode", "Horizontal");
        addBool("show_coordinates", "Show Coordinates", true);
        addBool("hide_y", "Hide Y Coordinate", false);
        addBool("show_direction", "Direction", true);
        addBool("show_background", "Show Background", false);
        addText("custom_line", "Custom Line", "");
        addColor("coordinates_color", "Coordinates Color", 0xFFFFFFFF);
        addColor("direction_color", "Direction Color", 0xFFFFFFFF);
        addColor("background_color", "Background Color", 0x6F000000);
    }
}
