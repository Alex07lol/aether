package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class SpeedIndicatorModule extends AbstractModule {
    public SpeedIndicatorModule() {
        super(ModuleMetadata.builder("hud.speed_indicator", "Speed Indicator")
            .category(ModuleCategory.HUD)
            .description("Shows movement speed in blocks per second on the HUD.")
            .build());

        addChoice("mode", "Mode", "Modern");
        addBool("show_background", "Show Background", true);
        addColor("text_color", "Text Color", 0xFFFFFFFF);
        addColor("background_color", "Background Color", 0x6F000000);
    }
}
