package dev.aether.module;

import dev.aether.module.ClientModule.ModuleMetadata;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.module.setting.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractModule implements ClientModule {
    private final ModuleMetadata metadata;
    private final List<Setting<?>> settings = new ArrayList<Setting<?>>();
    private ModuleState state = ModuleState.DISABLED;

    protected AbstractModule(ModuleMetadata metadata) {
        this.metadata = metadata;
    }

    public ModuleMetadata metadata() {
        return metadata;
    }

    public ModuleState state() {
        return state;
    }

    public void enable() {
        if (state != ModuleState.ENABLED) {
            state = ModuleState.ENABLED;
            onEnable();
        }
    }

    public void disable() {
        if (state != ModuleState.DISABLED) {
            state = ModuleState.DISABLED;
            onDisable();
        }
    }

    public List<Setting<?>> settings() {
        return Collections.unmodifiableList(settings);
    }

    protected final void addSetting(Setting<?> setting) {
        settings.add(setting);
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }
}
