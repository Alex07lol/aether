package dev.aether.gui;

import dev.aether.TestSupport;

/**
 * Guards {@link GuiScale}: the design canvas keeps its height across window sizes,
 * follows the aspect ratio with its width, and every conversion round-trips.
 */
public final class GuiScaleTest {
    private GuiScaleTest() {
    }

    public static void main(String[] args) {
        sixteenByNine();
        fourByThree();
        ultrawide();
        tinyWindow();
        roundTrip();
        System.out.println("GuiScaleTest passed");
    }

    /** At 16:9 the canvas is exactly 1920x1080 and one design unit is one GUI pixel. */
    private static void sixteenByNine() {
        GuiScale.update(1920, 1080);
        TestSupport.assertEquals(Double.valueOf(1.0D), Double.valueOf(GuiScale.unit()),
            "a 1080p 16:9 frame maps one design unit to one GUI pixel");
        TestSupport.assertEquals(Double.valueOf(1920.0D), Double.valueOf(GuiScale.designWidth()),
            "the design width is 1920 at 16:9");
        TestSupport.assertEquals(100, GuiScale.x(100), "x() is the identity at unit scale");
        TestSupport.assertEquals(1080, GuiScale.y(1080), "the full design height spans the screen");
    }

    /** At 4:3 the canvas narrows but keeps 1080 units of height. */
    private static void fourByThree() {
        GuiScale.update(1024, 768);
        TestSupport.assertEquals(Double.valueOf(1440.0D), Double.valueOf(GuiScale.designWidth()),
            "a 4:3 window narrows the canvas to 1440 units");
        TestSupport.assertEquals(768, GuiScale.y(1080), "the design height still fills the window");
        TestSupport.assertEquals(512, GuiScale.x(720), "half the canvas covers half the window");
    }

    /** Ultrawide keeps the vertical rhythm and gains horizontal room. */
    private static void ultrawide() {
        GuiScale.update(2560, 1080);
        TestSupport.assertEquals(Double.valueOf(2560.0D), Double.valueOf(GuiScale.designWidth()),
            "a 21:9 frame widens the canvas to 2560 units");
        TestSupport.assertEquals(Double.valueOf(1.0D), Double.valueOf(GuiScale.unit()),
            "1080 scaled pixels still mean unit scale");
    }

    /** Small windows must keep converting without a zero or negative unit. */
    private static void tinyWindow() {
        GuiScale.update(320, 240);
        TestSupport.assertTrue(GuiScale.unit() > 0.0D, "a tiny window still has a positive unit");
        TestSupport.assertEquals(240, GuiScale.y(1080), "the canvas shrinks to the window");
        TestSupport.assertTrue(GuiScale.designWidth() < 1920.0D, "the canvas narrows below 1920 units");
    }

    /** Mouse coordinates and layout coordinates must agree in both directions. */
    private static void roundTrip() {
        GuiScale.update(1600, 900);
        for (int guiX = 0; guiX <= 1600; guiX += 137) {
            int back = GuiScale.x(GuiScale.mouseX(guiX));
            TestSupport.assertEquals(guiX, back, "x round-trip stays on the same pixel at " + guiX);
        }
        GuiScale.update(3840, 2160);
        int guiY = 1337;
        int backY = GuiScale.y(GuiScale.mouseY(guiY));
        TestSupport.assertTrue(Math.abs(backY - guiY) <= 1,
            "y round-trip stays within a pixel at high scale, got " + backY);
    }

    private static void assertEquals(int expected, int actual, String message) {
        TestSupport.assertEquals(Integer.valueOf(expected), Integer.valueOf(actual), message);
    }
}
