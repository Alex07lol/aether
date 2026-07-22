package dev.aether.module.builtin;

import dev.aether.module.ModuleRegistry;
import dev.aether.module.impl.graphics.AnimationModule;
import dev.aether.module.impl.graphics.CustomCrosshairModule;
import dev.aether.module.impl.graphics.FullbrightModule;
import dev.aether.module.impl.graphics.HitColorModule;
import dev.aether.module.impl.graphics.ItemPhysicsModule;
import dev.aether.module.impl.graphics.MotionBlurModule;
import dev.aether.module.impl.graphics.NameTagModule;
import dev.aether.module.impl.graphics.NoHurtCamModule;
import dev.aether.module.impl.graphics.ParticlesModule;
import dev.aether.module.impl.graphics.TimeChangerModule;
import dev.aether.module.impl.graphics.UiBlurModule;
import dev.aether.module.impl.graphics.WeatherToggleModule;

final class GraphicsModules {
    private GraphicsModules() {
    }

    static void register(ModuleRegistry modules) {
        modules.register(new FullbrightModule());
        modules.register(new WeatherToggleModule());
        modules.register(new MotionBlurModule());
        modules.register(new ItemPhysicsModule());
        modules.register(new CustomCrosshairModule());
        modules.register(new HitColorModule());
        modules.register(new NoHurtCamModule());
        modules.register(new ParticlesModule());
        modules.register(new UiBlurModule());
        modules.register(new AnimationModule());
        modules.register(new NameTagModule());
        modules.register(new TimeChangerModule());
    }
}
