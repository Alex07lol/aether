package dev.aether.config;

import dev.aether.AetherClient;
import dev.aether.TestSupport;
import dev.aether.module.setting.Setting;

import java.nio.file.Paths;

/**
 * Guards {@link ProfileStore}: capture, apply, delete, persistence and the promise that a profile can
 * never restore a value the setting metadata would reject.
 */
public final class ProfileStoreTest {
    private ProfileStoreTest() {
    }

    public static void main(String[] args) {
        AetherClient client = new AetherClient(Paths.get("build", "test-config", "profiles.json"));

        nameSanitising();
        captureAndApply(client);
        unknownModulesAreTolerated(client);
        persistence(client);
        invalidValuesAreNormalised(client);
        System.out.println("ProfileStoreTest passed");
    }

    private static void nameSanitising() {
        TestSupport.assertEquals("", ProfileStore.sanitize(null), "a missing name sanitises to nothing");
        TestSupport.assertEquals("", ProfileStore.sanitize("   "), "a blank name sanitises to nothing");
        TestSupport.assertEquals("My PvP Setup", ProfileStore.sanitize("  My PvP. Setup  "),
            "dots are dropped and whitespace runs collapse");
        TestSupport.assertEquals("no dots here", ProfileStore.sanitize("no.dots.here"),
            "every dot goes, because the key layout uses them as separators");
        TestSupport.assertTrue(ProfileStore.sanitize("x".concat(repeat('y', 200))).length() <= 32,
            "a long paste is capped to a readable key");
    }

    private static void captureAndApply(AetherClient client) {
        ProfileStore profiles = new ProfileStore();
        client.modules().setEnabled("graphics.fullbright", true);
        client.modules().setEnabled("pvp.zoom", true);
        TestSupport.assertTrue(profiles.save("PvP", client.modules()), "a profile can be saved");
        TestSupport.assertTrue(profiles.exists("PvP"), "and is found again");
        TestSupport.assertTrue(profiles.exists("pvp"), "names are matched case-insensitively");
        TestSupport.assertEquals(Integer.valueOf(2), Integer.valueOf(profiles.enabledCount("PvP")),
            "the profile counts the modules that were on");

        client.modules().setEnabled("graphics.fullbright", false);
        client.modules().setEnabled("pvp.zoom", false);
        TestSupport.assertTrue(profiles.apply("PvP", client.modules()), "the profile applies");
        TestSupport.assertTrue(isEnabled(client, "graphics.fullbright"), "fullbright comes back on");
        TestSupport.assertTrue(isEnabled(client, "pvp.zoom"), "and so does zoom");
        TestSupport.assertTrue(profiles.apply("missing", client.modules()) == false,
            "applying an unknown profile changes nothing");

        TestSupport.assertTrue(profiles.delete("PvP"), "the profile can be deleted");
        TestSupport.assertTrue(!profiles.exists("PvP"), "and is gone");
        TestSupport.assertTrue(!profiles.delete("PvP"), "deleting it twice reports nothing");
        TestSupport.assertEquals(Integer.valueOf(0), Integer.valueOf(profiles.size()), "the store is empty again");
    }

    private static void unknownModulesAreTolerated(AetherClient client) {
        ConfigDocument document = ConfigDocument.builder()
            .put("profile.Future.module.unknown.module.enabled", "true")
            .put("profile.Future.module.graphics.fullbright.enabled", "true")
            .build();
        ProfileStore profiles = new ProfileStore();
        profiles.applyConfig(document);
        TestSupport.assertTrue(profiles.exists("Future"), "a profile from a newer build still loads");
        TestSupport.assertTrue(profiles.apply("Future", client.modules()), "and still applies");
        TestSupport.assertTrue(isEnabled(client, "graphics.fullbright"), "the known part of it takes effect");
        client.modules().setEnabled("graphics.fullbright", false);
    }

    private static void persistence(AetherClient client) {
        ProfileStore profiles = new ProfileStore();
        client.modules().setEnabled("hud.fps", true);
        profiles.save("HUD", client.modules());

        ConfigDocument.Builder builder = ConfigDocument.builder();
        profiles.writeConfig(builder);
        ConfigDocument document = builder.build();
        TestSupport.assertTrue(document.values().containsKey("profile.HUD.module.hud.fps.enabled"),
            "the profile is written under its own key prefix");

        ProfileStore restored = new ProfileStore();
        restored.applyConfig(document);
        TestSupport.assertEquals(profiles.names(), restored.names(), "the profile names survive a save and load");
        TestSupport.assertEquals(profiles.enabledCount("HUD"), restored.enabledCount("HUD"),
            "and so does what they switch on");
        TestSupport.assertTrue(restored.apply("HUD", client.modules()), "the restored profile applies");
        TestSupport.assertTrue(isEnabled(client, "hud.fps"), "with the same effect");
        client.modules().setEnabled("hud.fps", false);
    }

    private static void invalidValuesAreNormalised(AetherClient client) {
        // A hand-edited or old profile must go through the same normalisation as the config file.
        ConfigDocument document = ConfigDocument.builder()
            .put("profile.Wild.module.graphics.fullbright.enabled", "true")
            .put("profile.Wild.module.graphics.fullbright.setting.brightness", "99999")
            .build();
        ProfileStore profiles = new ProfileStore();
        profiles.applyConfig(document);
        TestSupport.assertTrue(profiles.apply("Wild", client.modules()), "the wild profile applies");
        TestSupport.assertEquals(Integer.valueOf(100), Integer.valueOf(settingValue(client, "graphics.fullbright", "brightness")),
            "an out-of-range profile value is clamped to the setting's own range");
        client.modules().setEnabled("graphics.fullbright", false);
    }

    private static boolean isEnabled(AetherClient client, String id) {
        return client.modules().get(id).state() == dev.aether.module.ClientModule.ModuleState.ENABLED;
    }

    private static int settingValue(AetherClient client, String moduleId, String settingId) {
        for (Setting<?> setting : client.modules().get(moduleId).settings()) {
            if (setting.id().equals(settingId) && setting.value() instanceof Number) {
                return ((Number) setting.value()).intValue();
            }
        }
        throw new AssertionError("missing setting " + moduleId + "." + settingId);
    }

    private static String repeat(char c, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(c);
        }
        return builder.toString();
    }
}
