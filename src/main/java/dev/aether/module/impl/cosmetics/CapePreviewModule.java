package dev.aether.module.impl.cosmetics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class CapePreviewModule extends AbstractModule {
    public CapePreviewModule() {
        super(ModuleMetadata.builder("cosmetics.cape_preview", "Cape Preview")
            .category(ModuleCategory.COSMETICS)
            .description("Shows the currently selected cape in the cosmetics screen.")
            .build());
    }
}
