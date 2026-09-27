package dev.aether.module.state;

import dev.aether.TestSupport;

/**
 * Guards {@link ToggleKey}: press edges, held keys, clean resets and the
 * enable / disable / re-enable sequence the modules rely on.
 */
public final class ToggleKeyTest {
    private ToggleKeyTest() {
    }

    public static void main(String[] args) {
        pressEdges();
        heldKeyDoesNotRepeat();
        disableResetsEverything();
        reEnableStartsClean();
        latchOnlyClear();
        System.out.println("ToggleKeyTest passed");
    }

    private static void pressEdges() {
        ToggleKey key = new ToggleKey();
        TestSupport.assertTrue(!key.active(), "a fresh toggle starts off");
        TestSupport.assertTrue(key.update(true), "the first press is an edge");
        TestSupport.assertTrue(key.active(), "the press latched the toggle on");
        TestSupport.assertTrue(!key.update(true), "a held key is not a new edge");
        TestSupport.assertTrue(key.active(), "a held key keeps the latch");
        key.update(false);
        TestSupport.assertTrue(key.update(true), "releasing and pressing is a new edge");
        TestSupport.assertTrue(!key.active(), "the second press toggled back off");
    }

    private static void heldKeyDoesNotRepeat() {
        ToggleKey key = new ToggleKey();
        key.update(true);
        for (int tick = 0; tick < 40; tick++) {
            key.update(true);
        }
        TestSupport.assertTrue(key.active(), "holding the key for two seconds stays toggled on");
    }

    private static void disableResetsEverything() {
        ToggleKey key = new ToggleKey();
        key.update(false);
        key.update(true);
        TestSupport.assertTrue(key.active(), "the toggle is on before the module is disabled");
        key.reset();
        TestSupport.assertTrue(!key.active(), "disabling drops the forced state");
        TestSupport.assertTrue(!key.keyDown(), "disabling drops the stale key latch");
        TestSupport.assertTrue(key.update(true), "the next press after a reset is a clean edge");
        TestSupport.assertTrue(key.active(), "the toggle comes back on");
    }

    private static void reEnableStartsClean() {
        ToggleKey key = new ToggleKey();
        key.update(true);
        boolean firstEnable = key.active();
        key.reset();
        boolean afterDisable = key.active();
        key.update(true);
        boolean secondEnable = key.active();
        TestSupport.assertTrue(firstEnable == secondEnable,
            "enable / disable / enable lands on the same state");
        TestSupport.assertTrue(!afterDisable, "the disabled module is off in between");
    }

    private static void latchOnlyClear() {
        ToggleKey key = new ToggleKey();
        key.update(true);
        TestSupport.assertTrue(key.keyDown(), "the key latch is remembered");
        key.clearActive();
        TestSupport.assertTrue(!key.active(), "clearing the latch turns the feature off");
        TestSupport.assertTrue(key.keyDown(), "the physical key latch survives");
        TestSupport.assertTrue(!key.update(true), "a still-held key is not a new edge");
    }
}
