package dev.aether.module.impl.interface_;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class NickHiderModule extends AbstractModule {
    public NickHiderModule() {
        super(ModuleMetadata.builder("interface.nick_hider", "Nick Hider")
            .category(ModuleCategory.INTERFACE)
            .description("Hides your username in game by replacing it.")
            .build());

        addText("nickname", "Nickname", "You");
    }
}
