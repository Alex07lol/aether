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
        ClientModule module = get(id);
        if (enabled) {
            disableOtherMembersOfGroup(module);
            module.enable();
        } else {
            module.disable();
        }
    }

    /**
     * Enforces mutual exclusion for modules that declare the same group. Used by the
     * theme modules so exactly one palette can be active at a time; screens and the
     * click GUI never need to know the rule exists.
     */
    private void disableOtherMembersOfGroup(ClientModule module) {
        String group = module.metadata().group();
        if (group == null) {
            return;
        }
        for (ClientModule candidate : modules.values()) {
            if (candidate == module) {
                continue;
            }
            if (group.equals(candidate.metadata().group()) && candidate.state() == ModuleState.ENABLED) {
                candidate.disable();
            }
        }
    }

    /** @return every registered module that shares the given group, in registration order. */
    public List<ClientModule> byGroup(String group) {
        List<ClientModule> result = new ArrayList<ClientModule>();
        if (group == null) {
            return Collections.unmodifiableList(result);
        }
        for (ClientModule module : modules.values()) {
            if (group.equals(module.metadata().group())) {
                result.add(module);
            }
        }
        return Collections.unmodifiableList(result);
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
