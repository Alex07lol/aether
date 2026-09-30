package dev.aether.hud;

import dev.aether.TestSupport;

/**
 * Guards the Mouse Display widget's pure offset maths: direction-preserving drift towards a
 * radius-clamped target, the spring back to centre, frame-rate independence, and the eight-way
 * direction words. All on the render path of a HUD element, so pinned here rather than checked by
 * eye every time the widget changes.
 */
public final class MouseIndicatorTest {
    private static final float EPSILON = 0.01F;

    private MouseIndicatorTest() {
    }

    public static void main(String[] args) {
        flickTowardsRim();
        diagonalClamp();
        springBack();
        frameRateIndependence();
        zeroDelta();
        directionWords();
        System.out.println("MouseIndicatorTest passed");
    }

    private static void flickTowardsRim() {
        // A flick towards +x targets the rim in +x...
        float[] step1 = MouseIndicator.next(0, 0, new int[] {500, 0}, 9.0F, 0.05F, 16.7F, 120.0F);
        TestSupport.assertTrue(step1[0] > 0.5F, "A flick towards +x has to move the indicator towards +x");
        TestSupport.assertTrue(Math.abs(step1[1]) < 1.0E-6F, "A horizontal flick must not produce vertical drift");
        TestSupport.assertTrue(Math.abs(step1[0]) <= 9.0F, "The indicator never leaves the pad");

        // ...and repeated frames converge on the rim (clamped target), never past it.
        float x = 0.0F;
        for (int i = 0; i < 200; i++) {
            x = MouseIndicator.next(x, 0.0F, new int[] {500, 0}, 9.0F, 0.05F, 16.7F, 120.0F)[0];
        }
        TestSupport.assertTrue(Math.abs(x - 9.0F) < EPSILON,
            "A continuous flick converges on the rim, got " + x);
    }

    private static void diagonalClamp() {
        float dx = 0.0F;
        float dy = 0.0F;
        for (int i = 0; i < 400; i++) {
            float[] next = MouseIndicator.next(dx, dy, new int[] {300, 300}, 9.0F, 0.05F, 16.7F, 120.0F);
            dx = next[0];
            dy = next[1];
        }
        TestSupport.assertTrue(Math.abs(dx - dy) < 0.05F, "Diagonal movement drifts diagonally");
        float out = (float) Math.sqrt(dx * dx + dy * dy);
        TestSupport.assertTrue(out <= 9.0F + 1.0E-4F, "The vector clamp keeps diagonals inside the radius");
    }

    private static void springBack() {
        float x = 8.0F;
        for (int i = 0; i < 400; i++) {
            x = MouseIndicator.next(x, 0.0F, new int[] {0, 0}, 9.0F, 0.05F, 16.7F, 120.0F)[0];
        }
        TestSupport.assertTrue(Math.abs(x) < EPSILON, "Stillness springs the indicator back to centre");
    }

    private static void frameRateIndependence() {
        // One 100 ms step lands near twenty 5 ms steps: the approach is exponential, so equal total
        // time means (nearly) equal travel regardless of how it was split into frames.
        float slow = MouseIndicator.next(0, 0, new int[] {400, 0}, 9.0F, 0.05F, 100.0F, 120.0F)[0];
        float fast = 0.0F;
        for (int i = 0; i < 20; i++) {
            fast = MouseIndicator.next(fast, 0.0F, new int[] {400, 0}, 9.0F, 0.05F, 5.0F, 120.0F)[0];
        }
        TestSupport.assertTrue(Math.abs(slow - fast) < 0.5F,
            "100 ms in one step should travel like 20x5 ms, got " + slow + " vs " + fast);
    }

    private static void zeroDelta() {
        float[] next = MouseIndicator.next(3.0F, -2.0F, new int[] {0, 0}, 9.0F, 0.05F, 16.7F, 120.0F);
        TestSupport.assertTrue(Float.isFinite(next[0]) && Float.isFinite(next[1]),
            "A zero-length delta must never produce NaN");
    }

    private static void directionWords() {
        TestSupport.assertEquals("North", MouseIndicator.direction(0.0F, -5.0F), "Up on screen is north");
        TestSupport.assertEquals("East", MouseIndicator.direction(5.0F, 0.0F), "Right on screen is east");
        TestSupport.assertEquals("South-West", MouseIndicator.direction(-4.0F, 4.0F), "Down-left is south-west");
        TestSupport.assertEquals("", MouseIndicator.direction(0.0F, 0.0F), "No offset has no direction word");
    }
}
