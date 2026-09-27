package dev.aether.module;

import dev.aether.AetherClient;
import dev.aether.TestSupport;
import dev.aether.module.setting.Setting;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Guards the promise that screens no longer guess: every registered setting carries the
 * metadata its control needs, so the Click Deck (and the legacy manager screen) never
 * falls back to an invented slider bound or an empty pill option list.
 */
public final class AetherSettingsMetadataTest {
    public static void main(String[] args) {
        AetherClient client = new AetherClient(Paths.get("build", "test-config", "metadata.json"));
        List<String> problems = new ArrayList<String>();
        int moduleCount = 0;
        int settingCount = 0;
        int ranged = 0;
        int choiceful = 0;

        for (ClientModule module : client.modules().all()) {
            moduleCount++;
            String moduleId = module.metadata().id();
            TestSupport.assertTrue(moduleId != null && !moduleId.trim().isEmpty(),
                "A registered module is missing its id.");
            TestSupport.assertTrue(module.metadata().name() != null && !module.metadata().name().trim().isEmpty(),
                moduleId + " is missing a display name.");

            Set<String> ids = new HashSet<String>();
            for (Setting<?> setting : module.settings()) {
                settingCount++;
                String where = moduleId + "." + setting.id();
                if (setting.id() == null || setting.id().trim().isEmpty()) {
                    problems.add(moduleId + " has a setting with a blank id.");
                    continue;
                }
                if (!ids.add(setting.id())) {
                    problems.add(moduleId + " declares the setting id " + setting.id() + " twice.");
                }
                if (setting.label() == null || setting.label().trim().isEmpty()) {
                    problems.add(where + " has no label to render.");
                }
                if (setting.value() == null) {
                    problems.add(where + " has a null value.");
                }

                switch (setting.type()) {
                    case NUMBER: {
                        Setting.Range range = setting.range();
                        if (range == null) {
                            problems.add(where + " is numeric but declares no Setting.Range; the GUI would guess.");
                            break;
                        }
                        ranged++;
                        if (range.max() <= range.min()) {
                            problems.add(where + " has an inverted range " + range + ".");
                        }
                        if (range.step() <= 0) {
                            problems.add(where + " has a non-positive step " + range + ".");
                        }
                        if (!(setting.defaultValue() instanceof Number)) {
                            problems.add(where + " is numeric but its default is not a number.");
                            break;
                        }
                        int def = ((Number) setting.defaultValue()).intValue();
                        if (def < range.min() || def > range.max()) {
                            problems.add(where + " defaults to " + def + ", outside " + range + ".");
                        }
                        break;
                    }
                    case CHOICE: {
                        if (!setting.hasChoices()) {
                            problems.add(where + " is a choice but declares no options; the GUI would guess.");
                            break;
                        }
                        choiceful++;
                        if (setting.choices().size() < 2) {
                            problems.add(where + " lists only one option, so the pill cannot change.");
                        }
                        if (!setting.choices().contains(String.valueOf(setting.defaultValue()))) {
                            problems.add(where + " defaults to '" + setting.defaultValue()
                                + "', which is not one of its options.");
                        }
                        break;
                    }
                    case BOOLEAN: {
                        if (!(setting.defaultValue() instanceof Boolean)) {
                            problems.add(where + " is a boolean setting with a non-boolean default.");
                        }
                        break;
                    }
                    case COLOR:
                    case KEYBIND: {
                        if (!(setting.defaultValue() instanceof Number)) {
                            problems.add(where + " needs an integer default for its " + setting.type() + " control.");
                        }
                        break;
                    }
                    case TEXT: {
                        if (!(setting.defaultValue() instanceof String)) {
                            problems.add(where + " is text with a non-text default.");
                        }
                        break;
                    }
                    default:
                        problems.add(where + " uses an unhandled setting type " + setting.type() + ".");
                }
            }
        }

        // A small synthetic check that declared metadata actually binds, not just exists.
        Setting<Integer> bounded = new Setting<Integer>("synthetic", "Synthetic", Setting.SettingType.NUMBER, 150)
            .range(0, 100, 10);
        TestSupport.assertEquals(Integer.valueOf(100), bounded.value(), "A range must clamp its default.");
        bounded.setValue(Integer.valueOf(-40));
        TestSupport.assertEquals(Integer.valueOf(0), bounded.value(), "A range must clamp assigned values.");
        TestSupport.assertEquals(20, bounded.range().snap(24), "Steps should snap to the nearest step from the minimum.");
        TestSupport.assertEquals(20, bounded.range().snap(16), "Snapping should round to the closer step.");

        Setting<String> listed = new Setting<String>("synthetic", "Synthetic", Setting.SettingType.CHOICE, "Nope")
            .choices("Alpha", "Beta");
        TestSupport.assertEquals("Alpha", listed.value(), "An unknown choice should fall back to the first option.");
        listed.setValue("Beta");
        TestSupport.assertEquals("Beta", listed.value(), "A listed choice should stick.");

        TestSupport.assertTrue(problems.isEmpty(), "Setting metadata problems:\n  - "
            + String.join("\n  - ", problems.size() > 24 ? problems.subList(0, 24) : problems));

        System.out.println("Metadata coverage: " + moduleCount + " modules, " + settingCount + " settings, "
            + ranged + " ranges, " + choiceful + " choice lists.");
        TestSupport.assertTrue(moduleCount >= 49, "Expected the full built-in module set, saw " + moduleCount + ".");
        TestSupport.assertTrue(ranged >= 25, "Expected declared slider ranges across the module set, saw " + ranged + ".");
        TestSupport.assertTrue(choiceful >= 18, "Expected declared option lists across the module set, saw " + choiceful + ".");
        System.out.println("AetherSettingsMetadataTest passed (" + moduleCount + " modules, " + settingCount
            + " settings, " + ranged + " ranges, " + choiceful + " choice lists).");
    }
}
