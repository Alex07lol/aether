package dev.aether.hud;

import dev.aether.TestSupport;

/**
 * Guards the Target Info widget's two pure rules: the health line for each {@code health_mode}, and
 * the bar's fill fraction. Both are on the render path of a HUD element, so they are pinned here
 * rather than checked by eye every time the widget changes.
 */
public final class TargetHealthTextTest {
    private static final float EPSILON = 0.0005F;

    private TargetHealthTextTest() {
    }

    public static void main(String[] args) {
        valueMode();
        percentMode();
        bothMode();
        unknownHealth();
        fractionRules();
        System.out.println("TargetHealthTextTest passed");
    }

    private static void valueMode() {
        TestSupport.assertEquals("13.5", TargetHealthText.format(13.5F, 20.0F, TargetHealthText.VALUE),
            "value mode shows one decimal");
        TestSupport.assertEquals("20.0", TargetHealthText.format(20.0F, 20.0F, TargetHealthText.VALUE),
            "a full bar still reads as a value");
        TestSupport.assertEquals("13.5", TargetHealthText.format(13.5F, 20.0F, "value"),
            "the mode is case insensitive, so a hand-edited config cannot break the widget");
        TestSupport.assertEquals("13.5", TargetHealthText.format(13.5F, 20.0F, null),
            "an unreadable mode falls back to the value form");
    }

    private static void percentMode() {
        TestSupport.assertEquals("68%", TargetHealthText.format(13.5F, 20.0F, TargetHealthText.PERCENT),
            "percent mode rounds to the nearest percent");
        TestSupport.assertEquals("0%", TargetHealthText.format(0.0F, 20.0F, TargetHealthText.PERCENT),
            "an empty bar is zero percent");
        TestSupport.assertEquals("100%", TargetHealthText.format(20.0F, 20.0F, TargetHealthText.PERCENT),
            "a full bar is one hundred percent");
    }

    private static void bothMode() {
        TestSupport.assertEquals("14 / 20 (68%)", TargetHealthText.format(13.5F, 20.0F, TargetHealthText.BOTH),
            "both mode shows the rounded value, the maximum and the percentage");
        TestSupport.assertEquals("20 / 20 (100%)", TargetHealthText.format(20.0F, 20.0F, TargetHealthText.BOTH),
            "both mode reads correctly at full health");
    }

    private static void unknownHealth() {
        TestSupport.assertEquals("", TargetHealthText.format(-1.0F, 20.0F, TargetHealthText.VALUE),
            "an unreadable health draws no line at all");
        TestSupport.assertEquals("", TargetHealthText.format(-1.0F, 20.0F, TargetHealthText.BOTH),
            "an unreadable health draws no line in any mode");
        TestSupport.assertEquals(0, TargetHealthText.percent(10.0F, 0.0F),
            "an unknown maximum cannot produce a percentage");
    }

    private static void fractionRules() {
        TestSupport.assertTrue(nearly(0.5F, TargetHealthText.fraction(10.0F, 20.0F)), "half health is half a bar");
        TestSupport.assertTrue(nearly(1.0F, TargetHealthText.fraction(30.0F, 20.0F)), "an over-full bar clamps to 1");
        TestSupport.assertTrue(nearly(0.0F, TargetHealthText.fraction(0.0F, 20.0F)), "zero health is an empty bar");
        TestSupport.assertTrue(nearly(1.0F, TargetHealthText.fraction(-1.0F, 20.0F)),
            "unknown health draws a full bar rather than an empty one");
        TestSupport.assertTrue(nearly(1.0F, TargetHealthText.fraction(10.0F, 0.0F)),
            "an unknown maximum draws a full bar rather than a divide-by-zero");
    }

    private static boolean nearly(float expected, float actual) {
        return Math.abs(expected - actual) <= EPSILON;
    }
}
