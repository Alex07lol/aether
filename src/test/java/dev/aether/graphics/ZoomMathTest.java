package dev.aether.graphics;

import dev.aether.TestSupport;

/**
 * Guards {@link ZoomMath}: percent-to-scale conversion inside the setting's own range, scroll
 * bounds, and the smoothing that makes a released zoom glide back to normal.
 */
public final class ZoomMathTest {
    private static final float EPSILON = 0.0001F;

    private ZoomMathTest() {
    }

    public static void main(String[] args) {
        scaleFromPercent();
        scrollBounds();
        smoothing();
        settles();
        System.out.println("ZoomMathTest passed");
    }

    private static void scaleFromPercent() {
        TestSupport.assertTrue(nearly(1.0F, ZoomMath.scaleFromPercent(100, 5)),
            "100% is the unzoomed field of view");
        TestSupport.assertTrue(nearly(0.4F, ZoomMath.scaleFromPercent(40, 5)), "40% is a 2.5x zoom");
        TestSupport.assertTrue(nearly(0.05F, ZoomMath.scaleFromPercent(1, 5)),
            "a value under the setting's floor is clamped to that floor, not to a hard-coded 10%");
        TestSupport.assertTrue(nearly(1.0F, ZoomMath.scaleFromPercent(400, 5)),
            "a value over 100% cannot widen the field of view");
        TestSupport.assertTrue(ZoomMath.scaleFromPercent(40, 15) >= 0.15F,
            "the declared minimum of the setting is respected");
    }

    private static void scrollBounds() {
        TestSupport.assertEquals(Integer.valueOf(45), Integer.valueOf(ZoomMath.scrollTarget(40, 1, 5, 15, 90)),
            "scrolling up steps by the configured amount");
        TestSupport.assertEquals(Integer.valueOf(35), Integer.valueOf(ZoomMath.scrollTarget(40, -1, 5, 15, 90)),
            "scrolling down steps back");
        TestSupport.assertEquals(Integer.valueOf(90), Integer.valueOf(ZoomMath.scrollTarget(88, 1, 5, 15, 90)),
            "scrolling cannot pass the maximum");
        TestSupport.assertEquals(Integer.valueOf(15), Integer.valueOf(ZoomMath.scrollTarget(17, -1, 5, 15, 90)),
            "scrolling cannot pass the minimum");
        TestSupport.assertEquals(Integer.valueOf(15), Integer.valueOf(ZoomMath.scrollTarget(15, -1, 0, 15, 90)),
            "a zero step still cannot escape the bounds");
        TestSupport.assertEquals(Integer.valueOf(15), Integer.valueOf(ZoomMath.scrollTarget(15, -1, 5, 90, 15)),
            "reversed bounds do not produce a value outside the range");
    }

    private static void smoothing() {
        TestSupport.assertTrue(nearly(1.0F, ZoomMath.smooth(1.0F, 0.4F, 0.0F, 120.0F)),
            "a frame with no elapsed time makes no progress");
        TestSupport.assertTrue(nearly(0.4F, ZoomMath.smooth(1.0F, 0.4F, 1000.0F, 120.0F)),
            "a long frame arrives at the target");
        float step = ZoomMath.smooth(1.0F, 0.4F, 16.0F, 120.0F);
        TestSupport.assertTrue(step < 1.0F && step > 0.4F, "a normal frame moves partway");
        TestSupport.assertTrue(nearly(0.4F, ZoomMath.smooth(1.0F, 0.4F, 16.0F, 0.0F)),
            "a zero settle time snaps instead of animating");
        float released = ZoomMath.smooth(0.4F, 1.0F, 16.0F, 120.0F);
        TestSupport.assertTrue(released > 0.4F && released < 1.0F, "releasing animates back up");
    }

    private static void settles() {
        TestSupport.assertTrue(ZoomMath.settled(1.0F, 1.0F), "an idle zoom counts as settled");
        TestSupport.assertTrue(ZoomMath.settled(0.40001F, 0.4F), "a hair away is settled too");
        TestSupport.assertTrue(!ZoomMath.settled(0.4F, 1.0F), "a mid-animation zoom is not settled");
    }

    private static boolean nearly(float expected, float actual) {
        return Math.abs(expected - actual) <= EPSILON;
    }
}
