package dev.aether.module.builtin;

import dev.aether.module.ModuleRegistry;
import dev.aether.module.impl.themes.AetherBlueThemeModule;
import dev.aether.module.impl.themes.AuroraThemeModule;
import dev.aether.module.impl.themes.FrostThemeModule;
import dev.aether.module.impl.themes.LightThemeModule;
import dev.aether.module.impl.themes.MidnightThemeModule;

final class ThemeModules {
    private ThemeModules() {
    }

    static void register(ModuleRegistry modules) {
        modules.register(new AetherBlueThemeModule());
        modules.register(new MidnightThemeModule());
        modules.register(new AuroraThemeModule());
        modules.register(new FrostThemeModule());
        modules.register(new LightThemeModule());
    }
}
