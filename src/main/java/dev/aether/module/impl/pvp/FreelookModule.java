package dev.aether.module.impl.pvp;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class FreelookModule extends AbstractModule {
    public FreelookModule() {
        super(ModuleMetadata.builder("pvp.freelook", "Freelook")
            .category(ModuleCategory.PVP)
            .description("Lets the camera rotate around the player while a key is held.")
            .build());

        addKeybind("keybind", "Keybind", 56);
        addNumber("sensitivity", "Sensitivity", 100, 10, 300, 10);
        addBool("invert_y", "Invert Y", false);
    }
}
