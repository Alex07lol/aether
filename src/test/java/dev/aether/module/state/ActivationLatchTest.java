package dev.aether.module.state;

import dev.aether.TestSupport;

/**
 * Guards {@link ActivationLatch}: hold and toggle activation, the press edge, mode switches and the
 * reset that a disable or a GUI taking the keyboard depends on.
 */
public final class ActivationLatchTest {
    private ActivationLatchTest() {
    }

    public static void main(String[] args) {
        holdMode();
        toggleMode();
        modeSwitchEndsActivation();
        resetIsClean();
        System.out.println("ActivationLatchTest passed");
    }

    private static void holdMode() {
        ActivationLatch latch = new ActivationLatch(ActivationMode.HOLD);
        TestSupport.assertTrue(!latch.update(false), "holding nothing keeps the module off");
        TestSupport.assertTrue(latch.update(true), "holding the key activates the module");
        TestSupport.assertTrue(latch.isActive(), "the latch reports the active state");
        TestSupport.assertTrue(!latch.update(false), "releasing the key deactivates it");
        TestSupport.assertTrue(!latch.isActive(), "and the state is forgotten");
    }

    private static void toggleMode() {
        ActivationLatch latch = new ActivationLatch(ActivationMode.TOGGLE);
        TestSupport.assertTrue(latch.update(true), "a press turns it on");
        TestSupport.assertTrue(latch.changed(), "the change is reported once");
        for (int tick = 0; tick < 40; tick++) {
            latch.update(true);
        }
        TestSupport.assertTrue(latch.isActive(), "a held key does not toggle again");
        TestSupport.assertTrue(!latch.changed(), "a held key is not a change");
        latch.update(false);
        TestSupport.assertTrue(!latch.update(true), "releasing and pressing turns it off");
        TestSupport.assertTrue(!latch.isActive(), "the second press toggled it off");
        TestSupport.assertTrue(latch.changed(), "and that change is reported too");
    }

    private static void modeSwitchEndsActivation() {
        ActivationLatch latch = new ActivationLatch(ActivationMode.TOGGLE);
        latch.update(true);
        TestSupport.assertTrue(latch.isActive(), "toggled on before the mode change");
        latch.setMode(ActivationMode.HOLD);
        TestSupport.assertTrue(!latch.isActive(), "switching to hold drops the toggled state");
        latch.setMode(ActivationMode.HOLD);
        TestSupport.assertTrue(!latch.isActive(), "setting the same mode is a no-op");
        TestSupport.assertEquals(ActivationMode.HOLD, latch.mode(), "the mode is remembered");
    }

    private static void resetIsClean() {
        ActivationLatch latch = new ActivationLatch(ActivationMode.TOGGLE);
        latch.update(true);
        TestSupport.assertTrue(latch.reset(), "resetting an active latch reports the release");
        TestSupport.assertTrue(!latch.isActive(), "the latch is off after the reset");
        TestSupport.assertTrue(!latch.reset(), "resetting an inactive latch reports nothing");
        TestSupport.assertTrue(latch.update(true), "the next press is a fresh edge, not a stale one");
        TestSupport.assertTrue(latch.isActive(), "and it activates again");
    }
}
