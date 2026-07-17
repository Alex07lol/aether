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
        addNumber("box_size", "Movement Key Size", 18);
        addNumber("click_size", "Click Key Size", 18);
        addNumber("spacebar_height", "Spacebar Height", 15);
        addNumber("gap", "Gap", 1);
        addNumber("fade_time", "Fade Time", 75);
        addColor("text_color", "Text Color", 0xFFFFFFFF);
        addColor("background_color", "Background Color", 0x6F000000);
        addColor("pressed_color", "Pressed Color", 0xCC52BEEB);
    }
}
