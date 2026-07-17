package dev.aether.module.builtin;

import dev.aether.hud.HudLayout;
import dev.aether.module.ModuleRegistry;

public final class BuiltInModules {
    private BuiltInModules() {
    }

    public static void registerAll(ModuleRegistry modules, HudLayout hudLayout) {
        HudModules.register(modules);
        PvpModules.register(modules);
        PerformanceModules.register(modules);
        GraphicsModules.register(modules);
        InterfaceModules.register(modules);
        CosmeticModules.register(modules);
        ThemeModules.register(modules);
        HudModules.registerBlockInfo(modules);
        registerHudLayout(hudLayout);
    }

    private static void registerHudLayout(HudLayout hudLayout) {
        hudLayout.add("hud.fps", 8, 8);
        hudLayout.add("hud.coordinates", 8, 20);
        hudLayout.add("hud.keystrokes", 8, 44);
        hudLayout.add("hud.direction", 8, 68);
        hudLayout.add("hud.cps", 8, 80);
        hudLayout.add("pvp.toggle_sprint", 8, 92);
        hudLayout.add("hud.memory", 8, 104);
        hudLayout.add("hud.session_time", 8, 116);
        hudLayout.add("hud.clock", 8, 128);
        hudLayout.add("developer.overlay", 8, 148);
        hudLayout.add("hud.block_info", 8, 160);
        hudLayout.add("hud.armor", 8, 172);
        hudLayout.add("hud.potions", 260, 8);
    }
}
