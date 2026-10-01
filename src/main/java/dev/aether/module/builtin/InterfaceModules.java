package dev.aether.module.builtin;

import dev.aether.module.ModuleRegistry;
import dev.aether.module.impl.interface_.BossbarModule;
import dev.aether.module.impl.interface_.ChatCustomizationModule;
import dev.aether.module.impl.interface_.NickHiderModule;
import dev.aether.module.impl.interface_.NotificationsModule;
import dev.aether.module.impl.interface_.ScoreboardCustomizationModule;

final class InterfaceModules {
    private InterfaceModules() {
    }

    static void register(ModuleRegistry modules) {
        modules.register(new NotificationsModule());
        modules.register(new ScoreboardCustomizationModule());
        modules.register(new BossbarModule());
        modules.register(new ChatCustomizationModule());
        modules.register(new NickHiderModule());
    }
}
