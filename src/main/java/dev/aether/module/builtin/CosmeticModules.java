package dev.aether.module.builtin;

import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ModuleRegistry;

import static dev.aether.module.builtin.BuiltInModuleFactory.module;

final class CosmeticModules {
    private CosmeticModules() {
    }

    static void register(ModuleRegistry modules) {
        module(modules, "cosmetics.manager", "Cosmetic Manager", ModuleCategory.COSMETICS, "Opens cape and cosmetic import tools.");
        module(modules, "cosmetics.player_preview", "Player Preview", ModuleCategory.COSMETICS, "Shows a live player cosmetic preview slot.");
        module(modules, "cosmetics.current_cape", "Current Cape", ModuleCategory.COSMETICS, "Shows and manages the selected cape.");
        module(modules, "cosmetics.current_wings", "Current Wings", ModuleCategory.COSMETICS, "Shows and manages the selected wings.");
        module(modules, "cosmetics.current_halo", "Current Halo", ModuleCategory.COSMETICS, "Shows and manages the selected halo.");
        module(modules, "cosmetics.current_hat", "Current Hat", ModuleCategory.COSMETICS, "Shows and manages the selected hat.");
        module(modules, "cosmetics.current_trail", "Current Trail", ModuleCategory.COSMETICS, "Shows and manages the selected trail.");
        module(modules, "cosmetics.cape_preview", "Cape Preview", ModuleCategory.COSMETICS, "Shows the currently selected cape in the cosmetics screen.");
        module(modules, "cosmetics.trails", "Trail Cosmetics", ModuleCategory.COSMETICS, "Adds a reserved trail cosmetic slot.");
    }
}
