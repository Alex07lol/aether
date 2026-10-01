package dev.aether.hud;

import dev.aether.AetherClient;
import dev.aether.TestSupport;
import dev.aether.module.ClientModule;
import dev.aether.module.setting.Setting;

import java.nio.file.Paths;

/**
 * Pins the keystrokes proportions the brief asks for, as the shipped defaults rather than as comments:
 * 28-unit keys with a 4-unit gap give a 92-wide, 60-tall movement pad, and a 92x22 spacebar under it.
 * <p>
 * It also checks the arithmetic the renderer uses, because a pad whose parts are computed by three
 * different sums is exactly how a widget ends up a few units narrower than the box the HUD editor
 * measures for it.
 */
public final class KeystrokesLayoutTest {

    private KeystrokesLayoutTest() {
    }

    public static void main(String[] args) {
        defaultProportions();
        arithmetic();
        System.out.println("KeystrokesLayoutTest passed");
    }

    private static void defaultProportions() {
        AetherClient client = new AetherClient(Paths.get("build", "test-config", "keystrokes.json"));
        ClientModule keystrokes = null;
        for (ClientModule module : client.modules().all()) {
            if (module.metadata().id().equals("hud.keystrokes")) {
                keystrokes = module;
                break;
            }
        }
        TestSupport.assertTrue(keystrokes != null, "the keystrokes module is registered");

        int boxSize = number(keystrokes, "box_size", 0);
        int gap = number(keystrokes, "gap", 0);
        int clickSize = number(keystrokes, "click_size", 0);
        int spacebarHeight = number(keystrokes, "spacebar_height", 0);
        int radius = number(keystrokes, "corner_radius", -1);

        TestSupport.assertEquals(28, boxSize, "keys ship at the reference size");
        TestSupport.assertEquals(4, gap, "the reference gap");
        TestSupport.assertEquals(22, clickSize, "the click keys match the reference height");
        TestSupport.assertEquals(22, spacebarHeight, "the spacebar is the reference height");
        TestSupport.assertEquals(6, radius, "the keys ship with the reference corner radius");

        TestSupport.assertEquals(92, KeystrokesLayout.movementWidth(boxSize, gap), "the pad is 92 wide");
        TestSupport.assertEquals(60, KeystrokesLayout.movementHeight(boxSize, gap), "the movement block is 60 tall");
        TestSupport.assertEquals(92, KeystrokesLayout.totalWidth(boxSize, gap), "the spacebar spans the pad");
        TestSupport.assertEquals(22, spacebarHeight, "so the spacebar is 92x22");
        TestSupport.assertEquals(60, KeystrokesLayout.totalHeight(boxSize, gap, clickSize, spacebarHeight, true, false, false),
            "the movement-only widget is exactly its movement block");
    }

    private static void arithmetic() {
        // A different size keeps the same relationships: three keys and two gaps, two rows and one gap.
        TestSupport.assertEquals(3 * 20 + 2 * 6, KeystrokesLayout.movementWidth(20, 6), "width scales with the keys");
        TestSupport.assertEquals(2 * 20 + 6, KeystrokesLayout.movementHeight(20, 6), "height scales with the keys");
        // Two click keys and the gap they share add up to the pad, so each is half the pad less half a gap.
        TestSupport.assertEquals((3 * 20 + 2 * 6 - 6) / 2, KeystrokesLayout.clickWidth(20, 6), "click keys split the pad");
        TestSupport.assertEquals((92 - 4) / 2, KeystrokesLayout.clickWidth(28, 4), "and they share the pad's width");
        TestSupport.assertEquals(KeystrokesLayout.movementWidth(28, 4),
            2 * KeystrokesLayout.clickWidth(28, 4) + 4, "two click keys and their gap fill the pad");
        // Movement, clicks and spacebar stack, with a gap between the rows and none after the last.
        TestSupport.assertEquals(60 + 4 + 22 + 4 + 22,
            KeystrokesLayout.totalHeight(28, 4, 22, 22, true, true, true), "all three blocks stack");
        TestSupport.assertEquals(0, KeystrokesLayout.totalHeight(28, 4, 22, 22, false, false, false),
            "nothing shown is nothing tall");
    }

    private static int number(ClientModule module, String id, int fallback) {
        for (Setting<?> setting : module.settings()) {
            if (setting.id().equals(id) && setting.value() instanceof Number) {
                return ((Number) setting.value()).intValue();
            }
        }
        return fallback;
    }
}
