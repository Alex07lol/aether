package dev.aether.graphics;

import dev.aether.TestSupport;

/**
 * Guards {@link FreelookView}: the camera seeds from the player, moves on vanilla's curve, honours
 * both inversions, clamps, and drops everything on release so a re-enable starts fresh.
 */
public final class FreelookViewTest {
    private static final float EPSILON = 0.0001F;

    private FreelookViewTest() {
    }

    public static void main(String[] args) {
        seeding();
        movement();
        inversions();
        clamping();
        releaseForgets();
        System.out.println("FreelookViewTest passed");
    }

    private static void seeding() {
        FreelookView view = new FreelookView();
        TestSupport.assertTrue(!view.isActive(), "a fresh view is inactive");
        view.start(90.0F, 12.0F);
        TestSupport.assertTrue(view.isActive(), "starting activates the view");
        TestSupport.assertTrue(nearly(-90.0F, view.yaw()),
            "the camera starts on vanilla's third-person base for the player's yaw");
        TestSupport.assertTrue(nearly(12.0F, view.pitch()), "and on the player's own pitch");
    }

    private static void movement() {
        FreelookView view = new FreelookView();
        view.start(0.0F, 0.0F);
        view.look(10, 0, 0.5F, 1.0F, false, false);
        TestSupport.assertTrue(nearly(-180.0F + 1.5F, view.yaw()) || nearly(181.5F, view.yaw()),
            "ten mouse units turn the camera vanilla's 1.5 degrees");
        float before = view.yaw();
        view.look(0, 0, 0.5F, 1.0F, false, false);
        TestSupport.assertTrue(nearly(before, view.yaw()), "a zero delta changes nothing");
    }

    private static void inversions() {
        FreelookView normal = new FreelookView();
        normal.start(0.0F, 0.0F);
        normal.look(10, 10, 0.5F, 1.0F, false, false);

        FreelookView inverted = new FreelookView();
        inverted.start(0.0F, 0.0F);
        inverted.look(10, 10, 0.5F, 1.0F, true, true);

        TestSupport.assertTrue(nearly(-normal.pitch(), inverted.pitch()), "invert Y flips the pitch");
        TestSupport.assertTrue(nearly(-normal.yaw(), inverted.yaw()), "invert X flips the yaw");
    }

    private static void clamping() {
        FreelookView view = new FreelookView();
        view.start(0.0F, 0.0F);
        view.look(0, 100000, 1.0F, 10.0F, false, false);
        TestSupport.assertTrue(nearly(-90.0F, view.pitch()), "the pitch clamps at straight up");
        view.look(0, -200000, 1.0F, 10.0F, false, false);
        TestSupport.assertTrue(nearly(90.0F, view.pitch()), "and at straight down");
    }

    private static void releaseForgets() {
        FreelookView view = new FreelookView();
        view.start(45.0F, 20.0F);
        view.look(40, 5, 0.5F, 1.0F, false, false);
        view.stop();
        TestSupport.assertTrue(!view.isActive(), "stopping deactivates the view");
        TestSupport.assertTrue(nearly(0.0F, view.yaw()), "the yaw is forgotten");
        TestSupport.assertTrue(nearly(0.0F, view.pitch()), "the pitch is forgotten");
        view.start(45.0F, 20.0F);
        TestSupport.assertTrue(nearly(-135.0F, view.yaw()), "a re-enable reseeds from the player again");
    }

    private static boolean nearly(float expected, float actual) {
        return Math.abs(expected - actual) <= EPSILON;
    }
}
