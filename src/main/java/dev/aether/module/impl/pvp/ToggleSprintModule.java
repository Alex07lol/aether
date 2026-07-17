package dev.aether.module.impl.pvp;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class ToggleSprintModule extends AbstractModule {
    public ToggleSprintModule() {
        super(ModuleMetadata.builder("pvp.toggle_sprint", "Toggle Sprint")
            .category(ModuleCategory.PVP)
            .description("Allows sprint to remain active after a player-controlled key toggle.")
            .build());

        addKeybind("keybind", "Keybind", 29);
        addChoice("mode", "Mode", "Modern");
        addBool("show_status", "Show Status", true);
        addBool("show_background", "Show Background", true);
        addColor("text_color", "Text Color", 0xFFFFFFFF);
        addColor("background_color", "Background Color", 0x6F000000);
    }
}
