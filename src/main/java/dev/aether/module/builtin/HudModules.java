package dev.aether.module.builtin;

import dev.aether.module.ModuleRegistry;
import dev.aether.module.impl.hud.ArmorStatusModule;
import dev.aether.module.impl.hud.BlockInfoModule;
import dev.aether.module.impl.hud.ClockModule;
import dev.aether.module.impl.hud.ComboModule;
import dev.aether.module.impl.hud.CoordinatesModule;
import dev.aether.module.impl.hud.CpsModule;
import dev.aether.module.impl.hud.DayCounterModule;
import dev.aether.module.impl.hud.DirectionModule;
import dev.aether.module.impl.hud.FpsModule;
import dev.aether.module.impl.hud.KeystrokesModule;
import dev.aether.module.impl.hud.MemoryModule;
import dev.aether.module.impl.hud.PingModule;
import dev.aether.module.impl.hud.PotionStatusModule;
import dev.aether.module.impl.hud.ReachDisplayModule;
import dev.aether.module.impl.hud.ServerAddressModule;
import dev.aether.module.impl.hud.SessionTimerModule;
import dev.aether.module.impl.hud.SpeedIndicatorModule;

final class HudModules {
    private HudModules() {
    }

    static void register(ModuleRegistry modules) {
        modules.register(new FpsModule());
        modules.register(new CoordinatesModule());
        modules.register(new KeystrokesModule());
        modules.register(new CpsModule());
        modules.register(new DirectionModule());
        modules.register(new ArmorStatusModule());
        modules.register(new PotionStatusModule());
        modules.register(new ClockModule());
        modules.register(new ComboModule());
        modules.register(new SessionTimerModule());
        modules.register(new MemoryModule());
        modules.register(new DayCounterModule());
        modules.register(new PingModule());
        modules.register(new ReachDisplayModule());
        modules.register(new SpeedIndicatorModule());
        modules.register(new ServerAddressModule());
    }

    static void registerBlockInfo(ModuleRegistry modules) {
        modules.register(new BlockInfoModule());
    }
}
