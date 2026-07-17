package dev.aether.feature;

import dev.aether.module.AbstractModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;
import dev.aether.module.setting.Setting;

public final class SimpleModule extends AbstractModule {
    public SimpleModule(String id, String name, ModuleCategory category, String description, boolean favoriteByDefault, Setting<?>... settings) {
        super(ModuleMetadata.builder(id, name)
            .category(category)
            .description(description)
            .favoriteByDefault(favoriteByDefault)
            .build());
        for (int i = 0; i < settings.length; i++) {
            addSetting(settings[i]);
        }
    }
}
