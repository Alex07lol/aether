package dev.aether.module.impl.graphics;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class SkyCustomizationModule extends AbstractModule {
    public SkyCustomizationModule() {
        super(ModuleMetadata.builder("graphics.sky_customization", "Sky Customization")
            .category(ModuleCategory.GRAPHICS)
            .description("Overrides the cloud style and clears the distance fog.")
            .build());

        addChoice("clouds", "Clouds", "Vanilla").choices("Vanilla", "Off", "Fast", "Fancy");
        addBool("hide_fog", "Hide Fog", false);
    }
}
