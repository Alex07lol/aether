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

    protected final Setting<Boolean> addBool(String id, String label, boolean defaultValue) {
        Setting<Boolean> s = new Setting<Boolean>(id, label, Setting.SettingType.BOOLEAN, Boolean.valueOf(defaultValue));
        addSetting(s);
        return s;
    }

    protected final Setting<Integer> addNumber(String id, String label, int defaultValue) {
        Setting<Integer> s = new Setting<Integer>(id, label, Setting.SettingType.NUMBER, Integer.valueOf(defaultValue));
        addSetting(s);
        return s;
    }

    protected final Setting<String> addText(String id, String label, String defaultValue) {
        Setting<String> s = new Setting<String>(id, label, Setting.SettingType.TEXT, defaultValue);
        addSetting(s);
        return s;
    }

    protected final Setting<String> addChoice(String id, String label, String defaultValue) {
        Setting<String> s = new Setting<String>(id, label, Setting.SettingType.CHOICE, defaultValue);
        addSetting(s);
        return s;
    }

    protected final Setting<Integer> addColor(String id, String label, int defaultValue) {
        Setting<Integer> s = new Setting<Integer>(id, label, Setting.SettingType.COLOR, Integer.valueOf(defaultValue));
        addSetting(s);
        return s;
    }

    protected final Setting<Integer> addKeybind(String id, String label, int defaultValue) {
        Setting<Integer> s = new Setting<Integer>(id, label, Setting.SettingType.KEYBIND, Integer.valueOf(defaultValue));
        addSetting(s);
        return s;
    }

    protected void onEnable() {
    }

    protected void onDisable() {
    }
}
