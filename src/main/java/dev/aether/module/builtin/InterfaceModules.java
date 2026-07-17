package dev.aether.module.builtin;

import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ModuleRegistry;

import static dev.aether.module.builtin.BuiltInModuleFactory.bool;
import static dev.aether.module.builtin.BuiltInModuleFactory.color;
import static dev.aether.module.builtin.BuiltInModuleFactory.module;
import static dev.aether.module.builtin.BuiltInModuleFactory.number;

final class InterfaceModules {
    private InterfaceModules() {
    }

    static void register(ModuleRegistry modules) {
        module(modules, "interface.crosshair_editor", "Crosshair Editor", ModuleCategory.INTERFACE, "Adds a configurable crosshair utility slot.");
        module(modules, "interface.theme_selector", "Theme Selector", ModuleCategory.INTERFACE, "Adds a theme switching utility slot.");
        module(modules, "interface.notifications", "Notifications", ModuleCategory.INTERFACE, "Adds a notification center utility slot.",
            bool("show_toasts", "Show Toasts", true));
        module(modules, "interface.scoreboard_customization", "Scoreboard Customization", ModuleCategory.INTERFACE, "Customizes the vanilla scoreboard presentation.",
            bool("show_background", "Show Background", true),
            bool("text_shadow", "Text Shadow", true),
            bool("hide_red_numbers", "Hide Red Numbers", false),
            number("scale", "Scale", 100),
            color("title_color", "Title Color", 0xFFFFFFFF),
            color("text_color", "Text Color", 0xFFFFFFFF),
            color("background_color", "Background Color", 0x6F000000));
        module(modules, "interface.chat_customization", "Chat Customization", ModuleCategory.INTERFACE, "Customizes chat readability and presentation.",
            bool("show_background", "Show Background", true),
            bool("text_shadow", "Text Shadow", true),
            bool("timestamps", "Timestamps", false),
            number("opacity", "Opacity", 70),
            color("text_color", "Text Color", 0xFFFFFFFF),
            color("background_color", "Background Color", 0x6F000000),
            color("timestamp_color", "Timestamp Color", 0xFF52BEEB));
        module(modules, "interface.screenshot_manager", "Screenshot Manager", ModuleCategory.INTERFACE, "Adds a screenshot organization utility slot.");
    }
}
