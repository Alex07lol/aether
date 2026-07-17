package dev.aether.module.impl.hud;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class SessionTimerModule extends AbstractModule {
    public SessionTimerModule() {
        super(ModuleMetadata.builder("hud.session_time", "Session Timer")
            .category(ModuleCategory.HUD)
            .description("Shows how long the current client session has been running.")
            .build());
    }
}
