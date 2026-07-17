package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class BlockInfoModule extends AbstractModule {
    public BlockInfoModule() {
        super(ModuleMetadata.builder("hud.block_info", "Block Info")
            .category(ModuleCategory.HUD)
            .description("Displays information about the block you are looking at.")
            .favoriteByDefault(true)
            .build());

        addBool("show_background", "Show Background", true);
        addColor("text_color", "Text Color", 0xFFFFFFFF);
        addColor("background_color", "Background Color", 0x6F000000);
    }
}
