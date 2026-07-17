package dev.aether.module.builtin;

import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ModuleRegistry;

import static dev.aether.module.builtin.BuiltInModuleFactory.bool;
import static dev.aether.module.builtin.BuiltInModuleFactory.choice;
import static dev.aether.module.builtin.BuiltInModuleFactory.color;
import static dev.aether.module.builtin.BuiltInModuleFactory.module;
import static dev.aether.module.builtin.BuiltInModuleFactory.number;
import static dev.aether.module.builtin.BuiltInModuleFactory.text;

final class HudModules {
    private HudModules() {
    }

    static void register(ModuleRegistry modules) {
        module(modules, "hud.fps", "FPS Counter", ModuleCategory.HUD, "Displays the current frames per second.", true,
            bool("show_background", "Show Background", true),
            color("text_color", "Text Color", 0xFFFFFFFF),
            color("background_color", "Background Color", 0x6F000000));
        module(modules, "hud.coordinates", "Coordinates", ModuleCategory.HUD, "Displays the player's current X, Y, and Z position.", true,
            choice("mode", "Mode", "Horizontal"),
            bool("show_coordinates", "Show Coordinates", true),
            bool("hide_y", "Hide Y Coordinate", false),
            bool("show_direction", "Direction", true),
            text("custom_line", "Custom Line", ""),
            color("coordinates_color", "Coordinates Color", 0xFFFFFFFF),
            color("direction_color", "Direction Color", 0xFFFFFFFF));
        module(modules, "hud.keystrokes", "Keystrokes", ModuleCategory.HUD, "Displays local movement and mouse input state.", true,
            bool("show_background", "Show Background", true),
            bool("show_clicks", "Show Clicks", true),
            bool("show_movement_keys", "Show Movement Keys", true),
            bool("show_spacebar", "Show Spacebar", false),
            bool("arrows", "Replace Names With Arrows", false),
            number("box_size", "Movement Key Size", 18),
            number("click_size", "Click Key Size", 18),
            number("spacebar_height", "Spacebar Height", 15),
            number("gap", "Gap", 1),
            number("fade_time", "Fade Time", 75),
            color("text_color", "Text Color", 0xFFFFFFFF),
            color("background_color", "Background Color", 0x6F000000),
            color("pressed_color", "Pressed Color", 0xCC52BEEB));
        module(modules, "hud.cps", "CPS Counter", ModuleCategory.HUD, "Shows local mouse click speed.",
            choice("mode", "Mode", "Modern"),
            bool("show_background", "Show Background", true),
            bool("right_click", "Right Click", false),
            color("background_color", "Background Color", 0x6F000000),
            color("text_color", "Text Color", 0xFFFFFFFF));
        module(modules, "hud.direction", "Direction HUD", ModuleCategory.HUD, "Shows the player's facing direction.", true,
            choice("style", "Style", "Compass"),
            color("text_color", "Text Color", 0xFFFFFFFF));
        module(modules, "hud.armor", "Armor Status", ModuleCategory.HUD, "Provides a reserved HUD slot for armor durability.",
            bool("show_durability", "Show Durability", true),
            bool("show_damage", "Show Damage", true));
        module(modules, "hud.potions", "Potion Effects", ModuleCategory.HUD, "Provides a reserved HUD slot for active potion effects.",
            choice("mode", "Mode", "Compact"));
        module(modules, "hud.clock", "Clock", ModuleCategory.HUD, "Shows local time on the HUD.",
            choice("format", "Format", "24h"));
        module(modules, "hud.combo", "Combo Counter", ModuleCategory.HUD, "Provides a reserved HUD slot for fair-play combo stats.");
        module(modules, "hud.session_time", "Session Timer", ModuleCategory.HUD, "Shows how long the current client session has been running.");
        module(modules, "hud.memory", "Memory Usage", ModuleCategory.HUD, "Shows JVM memory usage on the HUD.");
    }

    static void registerBlockInfo(ModuleRegistry modules) {
        module(modules, "hud.block_info", "Block Info", ModuleCategory.HUD, "Displays information about the block you are looking at.", true,
            bool("show_background", "Show Background", true),
            color("text_color", "Text Color", 0xFFFFFFFF),
            color("background_color", "Background Color", 0x6F000000));
    }
}
