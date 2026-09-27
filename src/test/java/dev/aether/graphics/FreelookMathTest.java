package dev.aether.graphics;

import dev.aether.TestSupport;

/**
 * Guards {@link FreelookMath}: the vanilla sensitivity curve, the inversion and clamping of the
 * pitch, yaw wrapping and the third-person yaw base freelook starts from.
 */
public final class FreelookMathTest {
    private static final float EPSILON = 0.0001F;

    private FreelookMathTest() {
    }

    public static void main(String[] args) {
        vanillaSensitivity();
        sensitivityDial();
        yawMovement();
        pitchMovement();
        pitchLimit();
        wrapping();
        System.out.println("FreelookMathTest passed");
    }

    private static void vanillaSensitivity() {
        TestSupport.assertTrue(nearly(0.15F, FreelookMath.mouseScale(0.5F)),
            "the default mouse slider moves the camera like vanilla's own look code");
        TestSupport.assertTrue(FreelookMath.mouseScale(1.0F) > FreelookMath.mouseScale(0.5F),
            "a higher slider value is faster");
        TestSupport.assertTrue(FreelookMath.mouseScale(0.0F) > 0.0F, "the slider cannot stop the camera");
    }

    private static void sensitivityDial() {
        TestSupport.assertTrue(nearly(1.0F, FreelookMath.moduleScale(100)),
            "a module sensitivity of 100 is exactly vanilla");
        TestSupport.assertTrue(nearly(0.5F, FreelookMath.moduleScale(50)), "the dial halves the speed");
        TestSupport.assertTrue(nearly(3.0F, FreelookMath.moduleScale(300)),
            "the dial can go three times vanilla, matching the setting's own ceiling");
    }

    private static void yawMovement() {
        // 10 mouse units at the default slider and module sensitivity: 1.5 degrees, like vanilla.
        TestSupport.assertTrue(nearly(1.5F, FreelookMath.yawAfter(0.0F, 10, 0.5F, 1.0F)),
            "yaw follows vanilla's degrees-per-unit");
        TestSupport.assertTrue(nearly(-1.5F, FreelookMath.yawAfter(0.0F, -10, 0.5F, 1.0F)),
            "yaw moves the other way for a negative delta");
        TestSupport.assertTrue(nearly(180.0F, Math.abs(FreelookMath.thirdPersonCameraYaw(0.0F))),
            "the camera base is the player's yaw plus 180");
        TestSupport.assertTrue(nearly(-90.0F, FreelookMath.thirdPersonCameraYaw(90.0F)),
            "the camera yaw wraps into [-180, 180)");
    }

    private static void pitchMovement() {
        // Vanilla subtracts the delta: pushing the mouse up (positive dy) looks up.
        TestSupport.assertTrue(nearly(-1.5F, FreelookMath.pitchAfter(0.0F, 10, 0.5F, 1.0F, false)),
            "an upward delta raises the camera like vanilla");
        TestSupport.assertTrue(nearly(1.5F, FreelookMath.pitchAfter(0.0F, 10, 0.5F, 1.0F, true)),
            "invert-Y flips the vertical axis");
        TestSupport.assertTrue(nearly(0.0F, FreelookMath.pitchAfter(0.0F, 0, 0.5F, 1.0F, false)),
            "a frame without movement leaves the pitch alone");
    }

    private static void pitchLimit() {
        TestSupport.assertTrue(nearly(90.0F, FreelookMath.pitchAfter(0.0F, 100000, 1.0F, 10.0F, true)),
            "the pitch clamps at straight down");
        TestSupport.assertTrue(nearly(-90.0F, FreelookMath.pitchAfter(0.0F, 100000, 1.0F, 10.0F, false)),
            "the pitch clamps at straight up");
    }

    private static void wrapping() {
        TestSupport.assertTrue(nearly(0.0F, FreelookMath.wrapDegrees(360.0F)), "360 wraps to 0");
        TestSupport.assertTrue(nearly(-179.0F, FreelookMath.wrapDegrees(181.0F)), "181 wraps to -179");
        TestSupport.assertTrue(nearly(179.0F, FreelookMath.wrapDegrees(-181.0F)), "-181 wraps to 179");
        TestSupport.assertTrue(nearly(0.0F, FreelookMath.wrapDegrees(720.0F)), "a full extra turn wraps to 0");
    }

    private static boolean nearly(float expected, float actual) {
        return Math.abs(expected - actual) <= EPSILON;
    }
}
