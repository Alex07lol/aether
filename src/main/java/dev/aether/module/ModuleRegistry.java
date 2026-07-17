package dev.aether.module;

import dev.aether.config.ConfigDocument;
import dev.aether.fairplay.FairPlayPolicy;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.module.setting.Setting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ModuleRegistry {
    private final FairPlayPolicy fairPlayPolicy;
    private final Map<String, ClientModule> modules = new LinkedHashMap<String, ClientModule>();

    public ModuleRegistry(FairPlayPolicy fairPlayPolicy) {
        this.fairPlayPolicy = fairPlayPolicy;
    }

    public void register(ClientModule module) {
        fairPlayPolicy.validate(module.metadata());
        String id = module.metadata().id();
        if (modules.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate module id: " + id);
        }
        modules.put(id, module);
    }

    public ClientModule get(String id) {
        ClientModule module = modules.get(id);
        if (module == null) {
            throw new IllegalArgumentException("Unknown module: " + id);
        }
        return module;
    }

    public List<ClientModule> all() {
        return Collections.unmodifiableList(new ArrayList<ClientModule>(modules.values()));
    }

    public List<ClientModule> byCategory(ModuleCategory category) {
        List<ClientModule> result = new ArrayList<ClientModule>();
        for (ClientModule module : modules.values()) {
            if (module.metadata().category() == category) {
                result.add(module);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public void setEnabled(String id, boolean enabled) {
        if (enabled) {
            get(id).enable();
        } else {
            get(id).disable();
        }
    }

    public ConfigDocument toConfig() {
        ConfigDocument.Builder builder = ConfigDocument.builder();
        for (ClientModule module : modules.values()) {
            builder.putBoolean("module." + module.metadata().id() + ".enabled", module.state() == ModuleState.ENABLED);
            for (Setting<?> setting : module.settings()) {
                builder.put(settingKey(module, setting), setting.serializeValue());
            }
        }
        return builder.build();
    }

    public void applyConfig(ConfigDocument document) {
        for (ClientModule module : modules.values()) {
            boolean enabled = document.getBoolean("module." + module.metadata().id() + ".enabled", module.state() == ModuleState.ENABLED);
            setEnabled(module.metadata().id(), enabled);
            for (Setting<?> setting : module.settings()) {
                String key = settingKey(module, setting);
                if (document.values().containsKey(key)) {
                    setting.restoreValue(document.get(key, setting.serializeValue()));
                }
            }
        }
    }

    private static String settingKey(ClientModule module, Setting<?> setting) {
        return "module." + module.metadata().id() + ".setting." + setting.id();
    }
}
