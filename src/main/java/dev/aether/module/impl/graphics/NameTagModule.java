package dev.aether.module.impl.graphics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class NameTagModule extends AbstractModule {
    public static final String ID = "graphics.nametag";

    public NameTagModule() {
        super(ModuleMetadata.builder(ID, "Name Tags")
            .category(ModuleCategory.GRAPHICS)
            .description("Customizes player name tag rendering with configurable scale, colors, and background.")
            .build());

        addNumber("scale", "Scale", 100, 50, 150, 5);
        addBool("show_background", "Show Background", true);
        addColor("text_color", "Text Color", 0xFFFFFFFF);
        addColor("background_color", "Background Color", 0x6F000000);
        addBool("show_armor", "Show Armor Value", false);
        addBool("hide_sneaking", "Hide While Sneaking", true);
    }
}
