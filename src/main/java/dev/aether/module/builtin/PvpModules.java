package dev.aether.module.builtin;

import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ModuleRegistry;

import static dev.aether.module.builtin.BuiltInModuleFactory.bool;
import static dev.aether.module.builtin.BuiltInModuleFactory.choice;
import static dev.aether.module.builtin.BuiltInModuleFactory.color;
import static dev.aether.module.builtin.BuiltInModuleFactory.keybind;
import static dev.aether.module.builtin.BuiltInModuleFactory.module;
import static dev.aether.module.builtin.BuiltInModuleFactory.number;

final class PvpModules {
    private PvpModules() {
    }

    static void register(ModuleRegistry modules) {
        module(modules, "pvp.toggle_sprint", "Toggle Sprint", ModuleCategory.PVP, "Allows sprint to remain active after a player-controlled key toggle.",
            keybind("keybind", "Keybind", 29),
            choice("mode", "Mode", "Modern"),
            bool("show_status", "Show Status", true),
            bool("show_background", "Show Background", true),
            color("text_color", "Text Color", 0xFFFFFFFF),
            color("background_color", "Background Color", 0x6F000000));
        module(modules, "pvp.freelook", "Freelook", ModuleCategory.PVP, "Lets the camera rotate around the player while a key is held.",
            keybind("keybind", "Keybind", 56),
            number("sensitivity", "Sensitivity", 100),
            bool("invert_y", "Invert Y", false));
        module(modules, "pvp.zoom", "Zoom", ModuleCategory.PVP, "Adds a client-side zoom utility slot.",
            keybind("keybind", "Keybind", 46),
            number("zoom_percent", "Zoom Percent", 40),
            bool("scroll_to_zoom", "Scroll To Zoom", true),
            number("min_zoom_percent", "Minimum Zoom", 15),
            number("max_zoom_percent", "Maximum Zoom", 90),
            number("scroll_step", "Scroll Step", 5));
        module(modules, "pvp.block_overlay", "Block Overlay", ModuleCategory.PVP, "Adds a configurable block outline utility slot.",
            bool("outline", "Outline", true),
            bool("fill", "Fill", true),
            number("thickness", "Thickness", 2),
            color("outline_color", "Outline Color", 0x8852BEEB),
            color("fill_color", "Fill Color", 0x4452BEEB));
    }
}
