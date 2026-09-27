package dev.aether.module.impl.pvp;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

/**
 * Keeps sneak held for you, mirroring how {@code pvp.toggle_sprint} handles sprint.
 * Some servers treat forced sneak as a restricted client feature, which is why the
 * module is off by default and called out in {@code docs/FAIR_PLAY.md}.
 */
public class ToggleSneakModule extends AbstractModule {
    public ToggleSneakModule() {
        super(ModuleMetadata.builder("pvp.toggle_sneak", "Toggle Sneak")
            .category(ModuleCategory.PVP)
            .description("Toggles sneak on and off with a key instead of holding it.")
            .build());

        addKeybind("keybind", "Keybind", 42);
        addChoice("mode", "Mode", "Modern").choices("Modern", "Legacy");
        addBool("show_status", "Show Status", true);
        addBool("show_background", "Show Background", true);
        addColor("text_color", "Text Color", 0xFFFFFFFF);
        addColor("background_color", "Background Color", 0x6F000000);
    }
}
