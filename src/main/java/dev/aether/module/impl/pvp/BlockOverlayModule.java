package dev.aether.module.impl.pvp;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class BlockOverlayModule extends AbstractModule {
    public BlockOverlayModule() {
        super(ModuleMetadata.builder("pvp.block_overlay", "Block Overlay")
            .category(ModuleCategory.PVP)
            .description("Adds a configurable block outline utility slot.")
            .build());

        addBool("outline", "Outline", true);
        addBool("fill", "Fill", true);
        addNumber("thickness", "Thickness", 2);
        addColor("outline_color", "Outline Color", 0x8852BEEB);
        addColor("fill_color", "Fill Color", 0x4452BEEB);
    }
}
