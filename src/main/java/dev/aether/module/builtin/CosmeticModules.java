package dev.aether.module.builtin;

import dev.aether.module.ModuleRegistry;
import dev.aether.module.impl.cosmetics.CapePreviewModule;
import dev.aether.module.impl.cosmetics.CosmeticManagerModule;
import dev.aether.module.impl.cosmetics.CurrentCapeModule;
import dev.aether.module.impl.cosmetics.CurrentHaloModule;
import dev.aether.module.impl.cosmetics.CurrentHatModule;
import dev.aether.module.impl.cosmetics.CurrentTrailModule;
import dev.aether.module.impl.cosmetics.CurrentWingsModule;
import dev.aether.module.impl.cosmetics.PlayerPreviewModule;
import dev.aether.module.impl.cosmetics.TrailCosmeticsModule;

final class CosmeticModules {
    private CosmeticModules() {
    }

    static void register(ModuleRegistry modules) {
        modules.register(new CosmeticManagerModule());
        modules.register(new PlayerPreviewModule());
        modules.register(new CurrentCapeModule());
        modules.register(new CurrentWingsModule());
        modules.register(new CurrentHaloModule());
        modules.register(new CurrentHatModule());
        modules.register(new CurrentTrailModule());
        modules.register(new CapePreviewModule());
        modules.register(new TrailCosmeticsModule());
    }
}
