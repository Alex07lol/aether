package dev.aether.graphics;

import dev.aether.TestSupport;

/**
 * Guards {@link HurtCamMath}: 0 must mean "no shake at all", 100 must reproduce vanilla's own
 * 14 degree curve, and the values in between must scale it without changing its shape.
 */
public final class HurtCamMathTest {
    private static final float EPSILON = 0.001F;

    private HurtCamMathTest() {
    }

    public static void main(String[] args) {
        scaleDial();
        curveShape();
        endpointBehaviour();
        scalingIsProportional();
        System.out.println("HurtCamMathTest passed");
    }

    private static void scaleDial() {
        TestSupport.assertTrue(nearly(0.0F, HurtCamMath.scaleFromPercent(0)), "0% is a zero scale");
        TestSupport.assertTrue(nearly(1.0F, HurtCamMath.scaleFromPercent(100)), "100% is vanilla strength");
        TestSupport.assertTrue(nearly(0.25F, HurtCamMath.scaleFromPercent(25)), "the dial is a percentage");
        TestSupport.assertTrue(nearly(0.0F, HurtCamMath.scaleFromPercent(-50)), "negative dials clamp to 0");
        TestSupport.assertTrue(nearly(1.0F, HurtCamMath.scaleFromPercent(900)), "dials over 100 clamp to 1");
        TestSupport.assertTrue(HurtCamMath.applies(0.99F), "a scaled shake takes over from vanilla");
        TestSupport.assertTrue(!HurtCamMath.applies(1.0F), "full strength leaves vanilla's own draw alone");
    }

    private static void curveShape() {
        TestSupport.assertTrue(HurtCamMath.curve(10, 10, 0.0F) < 0.05F, "the shake starts near zero");
        TestSupport.assertTrue(HurtCamMath.curve(8, 10, 0.0F) > 0.9F, "the shake peaks in the middle");
        TestSupport.assertTrue(HurtCamMath.curve(5, 10, 0.0F) > 0.05F, "the shake is still running");
        TestSupport.assertTrue(HurtCamMath.curve(4, 10, 0.0F) > HurtCamMath.curve(2, 10, 0.0F),
            "the curve rises as the hit gets fresher");
        TestSupport.assertTrue(nearly(HurtCamMath.VANILLA_SHAKE_DEGREES * HurtCamMath.curve(8, 10, 0.0F),
            HurtCamMath.shakeDegrees(8, 10, 0.0F, 1.0F)), "full strength is vanilla's 14 degree curve");
    }

    private static void endpointBehaviour() {
        TestSupport.assertTrue(nearly(0.0F, HurtCamMath.curve(0, 10, 0.0F)), "no hurt timer means no shake");
        TestSupport.assertTrue(nearly(0.0F, HurtCamMath.curve(-2, 10, 0.0F)), "a stale timer means no shake");
        TestSupport.assertTrue(nearly(0.0F, HurtCamMath.curve(5, 0, 0.0F)), "an unreadable max timer means no shake");
        TestSupport.assertTrue(nearly(0.0F, HurtCamMath.curve(5, 10, 5.0F)),
            "subtracting the partial tick can end the shake early");
        TestSupport.assertTrue(HurtCamMath.curve(5, 10, -1.0F) > 0.0F, "a negative partial tick still shakes");
    }

    private static void scalingIsProportional() {
        float full = HurtCamMath.shakeDegrees(7, 10, 0.0F, 1.0F);
        TestSupport.assertTrue(nearly(0.0F, HurtCamMath.shakeDegrees(7, 10, 0.0F, 0.0F)),
            "a zero scale removes the shake completely");
        TestSupport.assertTrue(nearly(full / 2.0F, HurtCamMath.shakeDegrees(7, 10, 0.0F, 0.5F)),
            "half strength is half the vanilla rotation");
        TestSupport.assertTrue(nearly(0.0F, HurtCamMath.shakeDegrees(7, 10, 0.0F, -3.0F)),
            "a negative scale cannot invert the shake");
    }

    private static boolean nearly(float expected, float actual) {
        return Math.abs(expected - actual) <= EPSILON;
    }
}
