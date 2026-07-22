package dev.aether.module.impl.interface_;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class NickHiderModule extends AbstractModule {
    public NickHiderModule() {
        super(ModuleMetadata.builder("interface.nick_hider", "Nick Hider")
            .category(ModuleCategory.INTERFACE)
            .description("Replaces your in-game username with a custom nickname for privacy.")
            .build());

        addText("nickname", "Nickname", "You");
    }
}
