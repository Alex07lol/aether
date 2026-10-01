package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class KeystrokesModule extends AbstractModule {
    public KeystrokesModule() {
        super(ModuleMetadata.builder("hud.keystrokes", "Keystrokes")
            .category(ModuleCategory.HUD)
            .description("Displays local movement and mouse input state.")
            .favoriteByDefault(true)
            .build());

        addBool("show_background", "Show Background", true);
        addBool("show_clicks", "Show Clicks", true);
        addBool("show_movement_keys", "Show Movement Keys", true);
        addBool("show_spacebar", "Show Spacebar", false);
        addBool("arrows", "Replace Names With Arrows", false);
        // The defaults are the proportions the brief's reference layout uses: 28-unit keys, a 4-unit
        // gap, so the movement pad is 3*28 + 2*4 = 92 wide and 28 + 4 + 28 = 60 tall, with a 92x22
        // spacebar under it. Every one of them stays adjustable, and the ranges are unchanged except
        // for the new radius.
        addNumber("box_size", "Movement Key Size", 28, 14, 34, 1);
        addNumber("click_size", "Click Key Size", 22, 14, 34, 1);
        addNumber("spacebar_height", "Spacebar Height", 22, 8, 24, 1);
        addNumber("gap", "Gap", 4, 0, 12, 1);
        addNumber("corner_radius", "Corner Radius", 6, 0, 12, 1);
        addNumber("fade_time", "Fade Time", 75, 0, 500, 5);
        addColor("text_color", "Text Color", 0xFFFFFFFF);
        addColor("background_color", "Background Color", 0x6F000000);
        addColor("pressed_color", "Pressed Color", 0xCC52BEEB);
    }
}
