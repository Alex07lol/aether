package dev.aether.module.impl.interface_;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class NickHiderModule extends AbstractModule {
    public static final String ID = "interface.nick_hider";

    public NickHiderModule() {
        super(ModuleMetadata.builder(ID, "Nick Hider")
            .category(ModuleCategory.INTERFACE)
            .description("Replaces your in-game username with a custom nickname for privacy.")
            .build());

        addText("nickname", "Nickname", "You");
    }
}
