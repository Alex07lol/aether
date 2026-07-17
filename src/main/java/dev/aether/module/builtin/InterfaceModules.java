package dev.aether.module.builtin;

import dev.aether.module.ModuleRegistry;
import dev.aether.module.impl.interface_.BossbarModule;
import dev.aether.module.impl.interface_.ChatCustomizationModule;
import dev.aether.module.impl.interface_.CrosshairEditorModule;
import dev.aether.module.impl.interface_.GuiTweaksModule;
import dev.aether.module.impl.interface_.NickHiderModule;
import dev.aether.module.impl.interface_.NotificationsModule;
import dev.aether.module.impl.interface_.ScoreboardCustomizationModule;
import dev.aether.module.impl.interface_.ScreenshotManagerModule;
import dev.aether.module.impl.interface_.ScrollTooltipsModule;
import dev.aether.module.impl.interface_.ThemeSelectorModule;

final class InterfaceModules {
    private InterfaceModules() {
    }

    static void register(ModuleRegistry modules) {
        modules.register(new CrosshairEditorModule());
        modules.register(new ThemeSelectorModule());
        modules.register(new NotificationsModule());
        modules.register(new ScoreboardCustomizationModule());
        modules.register(new ChatCustomizationModule());
        modules.register(new ScreenshotManagerModule());
        modules.register(new BossbarModule());
        modules.register(new GuiTweaksModule());
        modules.register(new NickHiderModule());
        modules.register(new ScrollTooltipsModule());
    }
}
