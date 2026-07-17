package dev.aether.module.impl.graphics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class WeatherToggleModule extends AbstractModule {
    public WeatherToggleModule() {
        super(ModuleMetadata.builder("graphics.weather_toggle", "Weather Toggle")
            .category(ModuleCategory.GRAPHICS)
            .description("Hides local rain and thunder visuals while enabled.")
            .build());
    }
}
