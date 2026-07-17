package dev.aether.module.builtin;

import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ModuleRegistry;

import static dev.aether.module.builtin.BuiltInModuleFactory.bool;
import static dev.aether.module.builtin.BuiltInModuleFactory.choice;
import static dev.aether.module.builtin.BuiltInModuleFactory.color;
import static dev.aether.module.builtin.BuiltInModuleFactory.module;
import static dev.aether.module.builtin.BuiltInModuleFactory.number;

final class GraphicsModules {
    private GraphicsModules() {
    }

    static void register(ModuleRegistry modules) {
        module(modules, "graphics.fullbright", "Fullbright", ModuleCategory.GRAPHICS, "Raises local brightness while enabled.", true,
            number("brightness", "Brightness", 100));
        module(modules, "graphics.weather_toggle", "Weather Toggle", ModuleCategory.GRAPHICS, "Hides local rain and thunder visuals while enabled.");
        module(modules, "graphics.motion_blur", "Motion Blur", ModuleCategory.GRAPHICS, "Adds a reserved graphics-effect slot.",
            number("strength", "Strength", 40));
        module(modules, "graphics.item_physics", "Item Physics", ModuleCategory.GRAPHICS, "Adds a reserved item-rendering effect slot.");
        module(modules, "graphics.custom_crosshair", "Custom Crosshair", ModuleCategory.GRAPHICS, "Adds a configurable crosshair rendering slot.",
            choice("shape", "Shape", "Cross"),
            color("color", "Color", 0xFFFFFFFF),
            number("size", "Size", 8),
            number("gap", "Gap", 4),
            number("thickness", "Thickness", 1),
            bool("show_dot", "Show Dot", false));
        module(modules, "graphics.hit_color", "Hit Color", ModuleCategory.GRAPHICS, "Adds a local hit feedback color slot.",
            color("color", "Color", 0xFFFF5555));
        module(modules, "graphics.no_hurt_cam", "No Hurt Cam", ModuleCategory.GRAPHICS, "Controls how much damage shakes the camera.",
            number("shake_amount", "Shake Amount", 0));
        module(modules, "graphics.particles", "Particles", ModuleCategory.GRAPHICS, "Customizes local hit particle feedback.",
            number("particle_amount", "Particle Amount", 5),
            choice("show_criticals", "Show Criticals", "Vanilla"),
            choice("show_sharpness", "Show Sharpness", "Vanilla"));
        module(modules, "graphics.ui_blur", "UI Blur", ModuleCategory.GRAPHICS, "Controls the Click GUI overlay dim and blur effect.",
            number("amount", "Amount", 52));
        module(modules, "graphics.sky_customization", "Sky Customization", ModuleCategory.GRAPHICS, "Adds a reserved sky styling slot.");
    }
}
