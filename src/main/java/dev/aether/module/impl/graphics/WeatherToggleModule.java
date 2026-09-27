package dev.aether.module.impl.graphics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

/**
 * Controls the weather the client draws.
 * <p>
 * The module reports rain and thunder strengths to the render layer instead of writing them into the
 * world, so the server's weather is never touched and switching the module off restores normal
 * weather on the next frame. {@code Server} means "draw whatever the server says", which is the
 * behaviour of a player who only wants the module available without changing anything.
 */
public class WeatherToggleModule extends AbstractModule {
    public WeatherToggleModule() {
        super(ModuleMetadata.builder("graphics.weather_toggle", "Weather Changer")
            .category(ModuleCategory.GRAPHICS)
            .description("Draws clear, rainy or stormy weather locally without changing the world.")
            .build());

        addChoice("mode", "Weather", "Clear").choices("Server", "Clear", "Rain", "Storm");
        addNumber("rain_strength", "Rain Strength", 100, 0, 100, 5);
        addNumber("thunder_strength", "Thunder Strength", 100, 0, 100, 5);
    }
}
