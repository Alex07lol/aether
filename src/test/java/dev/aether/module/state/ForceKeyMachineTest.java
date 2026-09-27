package dev.aether.module.state;

import dev.aether.TestSupport;

/**
 * Guards {@link ForceKeyMachine}: the three modes, the publish-once rule that keeps vanilla's own key
 * handling alive, and the clean release a disable depends on.
 */
public final class ForceKeyMachineTest {
    private ForceKeyMachineTest() {
    }

    public static void main(String[] args) {
        vanillaMode();
        heldMode();
        toggledMode();
        modeSwitchReleases();
        resetReleasesOnce();
        System.out.println("ForceKeyMachineTest passed");
    }

    private static void vanillaMode() {
        ForceKeyMachine machine = new ForceKeyMachine();
        machine.setMode(ForceKeyMachine.Mode.VANILLA);
        TestSupport.assertEquals(null, machine.update(true), "vanilla mode never touches the key");
        TestSupport.assertEquals(null, machine.update(false), "in either direction");
        TestSupport.assertTrue(!machine.toggled(), "and never reports a toggled state");
        TestSupport.assertTrue(!machine.isForcing(), "and never claims the key");
    }

    private static void heldMode() {
        ForceKeyMachine machine = new ForceKeyMachine();
        machine.setMode(ForceKeyMachine.Mode.HELD);
        TestSupport.assertEquals(Boolean.TRUE, machine.update(true), "holding the key presses vanilla's key");
        TestSupport.assertEquals(null, machine.update(true), "holding it again writes nothing");
        TestSupport.assertEquals(Boolean.FALSE, machine.update(false), "releasing hands the key back");
        TestSupport.assertEquals(null, machine.update(false), "and does not write again");
        TestSupport.assertTrue(!machine.toggled(), "held mode has no latched toggle");
    }

    private static void toggledMode() {
        ForceKeyMachine machine = new ForceKeyMachine();
        TestSupport.assertEquals(ForceKeyMachine.Mode.TOGGLED, machine.mode(),
            "the classic toggle is the default");
        TestSupport.assertEquals(Boolean.TRUE, machine.update(true), "a press forces the key on");
        TestSupport.assertTrue(machine.toggled(), "the latch is on");
        TestSupport.assertTrue(machine.justToggled(), "the press is reported as a toggle");
        TestSupport.assertEquals(null, machine.update(false), "releasing the key keeps it forced");
        TestSupport.assertTrue(machine.isForcing(), "the module still owns the key");
        TestSupport.assertEquals(Boolean.FALSE, machine.update(true), "the next press forces it off");
        TestSupport.assertTrue(!machine.toggled(), "the latch is off");
        machine.update(true);
        TestSupport.assertTrue(!machine.justToggled(), "holding the key after that is not another toggle");
    }

    private static void modeSwitchReleases() {
        ForceKeyMachine machine = new ForceKeyMachine();
        machine.update(true);
        TestSupport.assertTrue(machine.isForcing(), "forcing before the switch");
        machine.setMode(ForceKeyMachine.Mode.VANILLA);
        TestSupport.assertEquals(Boolean.FALSE, machine.update(false),
            "switching to vanilla hands the key back exactly once");
        TestSupport.assertEquals(null, machine.update(false), "and then stops writing");
    }

    private static void resetReleasesOnce() {
        ForceKeyMachine machine = new ForceKeyMachine();
        machine.update(true);
        TestSupport.assertTrue(machine.reset(), "disabling reports that the key has to be released");
        TestSupport.assertTrue(!machine.isForcing(), "the machine no longer owns the key");
        TestSupport.assertTrue(!machine.toggled(), "the latch is cleared");
        TestSupport.assertTrue(!machine.reset(), "a second reset reports nothing");
        TestSupport.assertEquals(Boolean.TRUE, machine.update(true),
            "after a reset the next press is a fresh edge");
        TestSupport.assertTrue(machine.toggled(), "and the toggle is on");
    }
}
