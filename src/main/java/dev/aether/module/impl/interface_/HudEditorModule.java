package dev.aether.module.impl.interface_;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

/**
 * Enabling this module opens the HUD layout editor and immediately switches itself
 * back off, which is the same one-shot behaviour the client's menus use. It exists
 * so {@code interface.hud_editor} is a registered, discoverable entry instead of an
 * id the other screens filter against a module that never existed.
 */
public class HudEditorModule extends AbstractModule {
    public HudEditorModule() {
        super(ModuleMetadata.builder("interface.hud_editor", "HUD Editor")
            .category(ModuleCategory.INTERFACE)
            .description("Opens the on-screen HUD layout editor.")
            .build());
    }
}
