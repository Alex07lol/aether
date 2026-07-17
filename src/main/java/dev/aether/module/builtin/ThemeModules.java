package dev.aether.module.builtin;

import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ModuleRegistry;

import static dev.aether.module.builtin.BuiltInModuleFactory.module;

final class ThemeModules {
    private ThemeModules() {
    }

    static void register(ModuleRegistry modules) {
        module(modules, "theme.aether_blue", "Aether Blue", ModuleCategory.THEMES, "Switches the Click GUI to the Aether Blue theme.");
        module(modules, "theme.midnight", "Midnight", ModuleCategory.THEMES, "Switches the Click GUI to a dark midnight theme.");
        module(modules, "theme.aurora", "Aurora", ModuleCategory.THEMES, "Switches the Click GUI to an aurora accent theme.");
        module(modules, "theme.frost", "Frost", ModuleCategory.THEMES, "Switches the Click GUI to the frost theme.");
        module(modules, "theme.light", "Light", ModuleCategory.THEMES, "Switches the Click GUI to a bright light theme.");
    }
}
