package dev.aether.runtime;

import dev.aether.TestSupport;

/**
 * Guards {@link FpsLimiter}: unlimited handling, the unfocused cap, and the fact that a value never
 * escapes vanilla's own slider range.
 */
public final class FpsLimiterTest {
    private FpsLimiterTest() {
    }

    public static void main(String[] args) {
        unlimited();
        focusedAndBackground();
        clamping();
        System.out.println("FpsLimiterTest passed");
    }

    private static void unlimited() {
        TestSupport.assertEquals(Integer.valueOf(FpsLimiter.UNLIMITED),
            Integer.valueOf(FpsLimiter.resolve(0, 0, true)), "0 asks for no cap at all");
        TestSupport.assertTrue(FpsLimiter.isUnlimited(FpsLimiter.resolve(0, 0, true)),
            "and vanilla is told that with its own unlimited value");
        TestSupport.assertTrue(!FpsLimiter.isUnlimited(FpsLimiter.resolve(120, 0, true)),
            "a real cap is not unlimited");
    }

    private static void focusedAndBackground() {
        TestSupport.assertEquals(Integer.valueOf(120), Integer.valueOf(FpsLimiter.resolve(120, 30, true)),
            "a focused window uses the gameplay cap");
        TestSupport.assertEquals(Integer.valueOf(30), Integer.valueOf(FpsLimiter.resolve(120, 30, false)),
            "an unfocused window uses its own cap");
        TestSupport.assertEquals(Integer.valueOf(120), Integer.valueOf(FpsLimiter.resolve(120, 0, false)),
            "0 for the unfocused cap means follow the gameplay cap");
        TestSupport.assertEquals(Integer.valueOf(30),
            Integer.valueOf(FpsLimiter.resolve(0, 30, false)), "an unfocused cap applies even when uncapped");
        TestSupport.assertEquals(Integer.valueOf(FpsLimiter.UNLIMITED),
            Integer.valueOf(FpsLimiter.resolve(0, 0, false)), "with both caps off nothing is limited");
    }

    private static void clamping() {
        TestSupport.assertEquals(Integer.valueOf(FpsLimiter.MINIMUM),
            Integer.valueOf(FpsLimiter.resolve(1, 0, true)), "a tiny cap is raised to vanilla's floor");
        TestSupport.assertEquals(Integer.valueOf(FpsLimiter.UNLIMITED),
            Integer.valueOf(FpsLimiter.resolve(600, 0, true)), "an oversized cap becomes unlimited");
        TestSupport.assertEquals(Integer.valueOf(240), Integer.valueOf(FpsLimiter.resolve(-5, 240, false)),
            "a negative gameplay cap falls back to the unfocused one when it is the active value");
    }
}
