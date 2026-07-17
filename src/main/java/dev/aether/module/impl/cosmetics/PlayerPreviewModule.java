package dev.aether.module.impl.cosmetics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class PlayerPreviewModule extends AbstractModule {
    public PlayerPreviewModule() {
        super(ModuleMetadata.builder("cosmetics.player_preview", "Player Preview")
            .category(ModuleCategory.COSMETICS)
            .description("Shows a live player cosmetic preview slot.")
            .build());
    }
}
