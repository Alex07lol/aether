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
        ThemeModules.register(modules);
        registerHudLayout(hudLayout);
    }

    private static void registerHudLayout(HudLayout hudLayout) {
        hudLayout.add("hud.fps", 8, 8);
        hudLayout.add("hud.coordinates", 8, 20);
        hudLayout.add("hud.keystrokes", 8, 44);
        hudLayout.add("hud.cps", 8, 68);
        hudLayout.add("pvp.toggle_sprint", 8, 80);
        hudLayout.add("hud.memory", 8, 92);

        hudLayout.add("hud.clock", 8, 116);
        hudLayout.add("developer.overlay", 8, 136);
        hudLayout.add("hud.potions", 260, 8);
        hudLayout.add("hud.ping", 8, 148);
        hudLayout.add("hud.reach_display", 8, 160);
        hudLayout.add("hud.speed_indicator", 8, 172);
        hudLayout.add("hud.server_address", 8, 184);
        hudLayout.add("hud.direction", 8, 196);
        hudLayout.add("hud.block_info", 8, 208);
        hudLayout.add("hud.armor", 260, 80);
        hudLayout.add("hud.fps_graph", 8, 220);

    }
}
