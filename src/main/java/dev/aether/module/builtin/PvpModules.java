package dev.aether.module.builtin;

import dev.aether.module.ModuleRegistry;
import dev.aether.module.impl.pvp.BlockOverlayModule;
import dev.aether.module.impl.pvp.FreelookModule;
import dev.aether.module.impl.pvp.SnaplookModule;
import dev.aether.module.impl.pvp.ToggleSneakModule;
import dev.aether.module.impl.pvp.ToggleSprintModule;
import dev.aether.module.impl.pvp.ZoomModule;

final class PvpModules {
    private PvpModules() {
    }

    static void register(ModuleRegistry modules) {
        modules.register(new ToggleSprintModule());
        modules.register(new ToggleSneakModule());
        modules.register(new FreelookModule());
        modules.register(new SnaplookModule());
        modules.register(new ZoomModule());
        modules.register(new BlockOverlayModule());
    }
}
