package dev.aether.module.impl.pvp;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class ZoomModule extends AbstractModule {
    public ZoomModule() {
        super(ModuleMetadata.builder("pvp.zoom", "Zoom")
            .category(ModuleCategory.PVP)
            .description("Adds a client-side zoom utility slot.")
            .build());

        addKeybind("keybind", "Keybind", 46);
        addNumber("zoom_percent", "Zoom Percent", 40);
        addBool("scroll_to_zoom", "Scroll To Zoom", true);
        addNumber("min_zoom_percent", "Minimum Zoom", 15);
        addNumber("max_zoom_percent", "Maximum Zoom", 90);
        addNumber("scroll_step", "Scroll Step", 5);
    }
}
