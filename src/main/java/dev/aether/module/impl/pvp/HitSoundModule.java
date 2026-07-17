package dev.aether.module.impl.pvp;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;

public class HitSoundModule extends AbstractModule {
    public HitSoundModule() {
        super(ModuleMetadata.builder("pvp.hit_sound", "Custom Hit Sounds")
            .category(ModuleCategory.PVP)
            .description("Plays custom hit sounds and particles when landing attacks on players.")
            .favoriteByDefault(true)
            .build());

        addChoice("sound_type", "Sound Type", "Ding");
        addNumber("pitch", "Pitch", 100);
        addNumber("volume", "Volume", 100);
        addBool("spawn_particles", "Spawn Hit Particles", true);
    }
}
