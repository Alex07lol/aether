package dev.aether.module.builtin;

import dev.aether.feature.SimpleModule;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ModuleRegistry;
import dev.aether.module.setting.Setting;
import dev.aether.module.setting.Setting.SettingType;

final class BuiltInModuleFactory {
    private BuiltInModuleFactory() {
    }

    static void module(ModuleRegistry modules, String id, String name, ModuleCategory category, String description, Setting<?>... settings) {
        module(modules, id, name, category, description, false, settings);
    }

    static void module(ModuleRegistry modules, String id, String name, ModuleCategory category, String description, boolean favoriteByDefault, Setting<?>... settings) {
        modules.register(new SimpleModule(id, name, category, description, favoriteByDefault, settings));
    }

    static Setting<Boolean> bool(String id, String label, boolean defaultValue) {
        return new Setting<Boolean>(id, label, SettingType.BOOLEAN, Boolean.valueOf(defaultValue));
    }

    static Setting<Integer> number(String id, String label, int defaultValue) {
        return new Setting<Integer>(id, label, SettingType.NUMBER, Integer.valueOf(defaultValue));
    }

    static Setting<String> text(String id, String label, String defaultValue) {
        return new Setting<String>(id, label, SettingType.TEXT, defaultValue);
    }

    static Setting<String> choice(String id, String label, String defaultValue) {
        return new Setting<String>(id, label, SettingType.CHOICE, defaultValue);
    }

    static Setting<Integer> color(String id, String label, int defaultValue) {
        return new Setting<Integer>(id, label, SettingType.COLOR, Integer.valueOf(defaultValue));
    }

    static Setting<Integer> keybind(String id, String label, int defaultValue) {
        return new Setting<Integer>(id, label, SettingType.KEYBIND, Integer.valueOf(defaultValue));
    }
}
