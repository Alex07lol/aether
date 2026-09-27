package dev.aether.module.state;

import dev.aether.TestSupport;

/**
 * Guards {@link ValueHold}: capture once, restore exactly once, and behave identically on a
 * second enable after a disable.
 */
public final class ValueHoldTest {
    private ValueHoldTest() {
    }

    public static void main(String[] args) {
        capturesOnce();
        releaseRestoresExactlyOnce();
        repeatedCycleIsStable();
        forgetDropsWithoutRestoring();
        System.out.println("ValueHoldTest passed");
    }

    private static void capturesOnce() {
        ValueHold<Float> gamma = new ValueHold<Float>("gamma");
        TestSupport.assertTrue(!gamma.isHolding(), "nothing is held before the module turns on");
        TestSupport.assertTrue(gamma.capture(Float.valueOf(0.35F)), "the first capture takes the value");
        TestSupport.assertTrue(!gamma.capture(Float.valueOf(100.0F)),
            "capturing again while active is ignored, so the module cannot capture its own override");
        TestSupport.assertEquals(Float.valueOf(0.35F), gamma.held(),
            "the module's own override never replaces the user's gamma");
        TestSupport.assertEquals("gamma", gamma.name(), "the hold keeps the tag it was built with");
    }

    private static void releaseRestoresExactlyOnce() {
        ValueHold<Integer> perspective = new ValueHold<Integer>("perspective");
        TestSupport.assertEquals(null, perspective.release(),
            "releasing without a capture restores nothing at all");
        perspective.capture(Integer.valueOf(2));
        TestSupport.assertEquals(Integer.valueOf(2), perspective.release(),
            "the captured perspective comes back on release");
        TestSupport.assertEquals(null, perspective.release(), "a second release restores nothing");
        TestSupport.assertTrue(!perspective.isHolding(), "the hold is cleared after the release");
    }

    private static void repeatedCycleIsStable() {
        ValueHold<Boolean> fancy = new ValueHold<Boolean>("fancy graphics");
        for (int cycle = 0; cycle < 3; cycle++) {
            fancy.capture(Boolean.TRUE);
            TestSupport.assertEquals(Boolean.TRUE, fancy.release(),
                "cycle " + cycle + " restores the user's own value");
        }
    }

    private static void forgetDropsWithoutRestoring() {
        ValueHold<Float> hold = new ValueHold<Float>("fov");
        hold.capture(Float.valueOf(70.0F));
        hold.forget();
        TestSupport.assertTrue(!hold.isHolding(), "forget ends the hold");
        TestSupport.assertEquals(null, hold.release(), "forget never hands the value back");
    }
}
