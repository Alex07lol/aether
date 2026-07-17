package dev.aether.module;

import dev.aether.TestSupport;
import dev.aether.config.ConfigDocument;
import dev.aether.fairplay.FairPlayPolicy;
import dev.aether.fairplay.FairPlayViolationException;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.module.setting.Setting;
import dev.aether.module.setting.Setting.SettingType;

public final class ModuleRegistryTest {
    public static void main(String[] args) {
        ModuleRegistry registry = new ModuleRegistry(FairPlayPolicy.standard());
        registry.register(new TestModule("hud.fps", "FPS Counter"));
        registry.register(new TestModule("hud.reach_stats", "Reach Statistics"));
        registry.setEnabled("hud.fps", true);

        TestSupport.assertEquals(ModuleState.ENABLED, registry.get("hud.fps").state(), "Module should enable.");
        ConfigDocument config = registry.toConfig();
        TestSupport.assertTrue(config.getBoolean("module.hud.fps.enabled", false), "Module state should serialize.");
        TestSupport.assertEquals("true", config.get("module.hud.fps.setting.show_background", ""), "Module settings should serialize.");

        ModuleRegistry restored = new ModuleRegistry(FairPlayPolicy.standard());
        restored.register(new TestModule("hud.fps", "FPS Counter"));
        restored.applyConfig(config);
        TestSupport.assertEquals(ModuleState.ENABLED, restored.get("hud.fps").state(), "Module state should restore.");
        TestSupport.assertEquals(Boolean.TRUE, restored.get("hud.fps").settings().get(0).value(), "Module settings should restore.");

        boolean rejected = false;
        try {
            registry.register(new TestModule("combat.killaura", "Kill Aura"));
        } catch (FairPlayViolationException expected) {
            rejected = true;
        }
        TestSupport.assertTrue(rejected, "Prohibited modules should be rejected.");
    }

    private static final class TestModule extends AbstractModule {
        TestModule(String id, String name) {
            super(ModuleMetadata.builder(id, name)
                .category(ModuleCategory.HUD)
                .description("Test module")
                .build());
            addSetting(new Setting<Boolean>("show_background", "Show Background", SettingType.BOOLEAN, Boolean.TRUE));
        }
    }
}
