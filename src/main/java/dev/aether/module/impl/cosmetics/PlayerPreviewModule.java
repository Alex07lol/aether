package dev.aether.module.impl.cosmetics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;
import dev.aether.module.ModuleKind;

/**
 * Renders your selected cosmetics on nearby players too, so a cape or wings can be seen on
 * a different skin or pose. Purely a local preview: nothing is sent to the server.
 */
public class PlayerPreviewModule extends AbstractModule {
    public static final String ID = "cosmetics.player_preview";

    public PlayerPreviewModule() {
        super(ModuleMetadata.builder(ID, "Player Preview")
            .category(ModuleCategory.COSMETICS)
            .kind(ModuleKind.COSMETIC_SERVICE)
            .description("Shows your cosmetics on nearby players as a local preview.")
            .build());
        addNumber("max_distance", "Max Distance", 12, 2, 32, 1);
        addBool("skip_invisible", "Skip Invisible", true);
    }
}
