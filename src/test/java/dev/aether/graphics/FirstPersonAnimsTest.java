package dev.aether.graphics;

import dev.aether.TestSupport;

/**
 * Guards the 1.7 first-person pose rules in {@link FirstPersonAnims}: the swing argument
 * the item transform receives, and the bow draw easing compensation.
 */
public final class FirstPersonAnimsTest {
    private static final float EPSILON = 0.0005F;

    private FirstPersonAnimsTest() {
    }

    public static void main(String[] args) {
        actionNames();
        swingArgument();
        bowEasing();
        bowCompensation();
        System.out.println("FirstPersonAnimsTest passed");
    }

    private static void actionNames() {
        TestSupport.assertEquals(FirstPersonAnims.Action.BLOCK, FirstPersonAnims.action("BLOCK"), "a sword blocks");
        TestSupport.assertEquals(FirstPersonAnims.Action.EAT_DRINK, FirstPersonAnims.action("EAT"), "food eats");
        TestSupport.assertEquals(FirstPersonAnims.Action.EAT_DRINK, FirstPersonAnims.action("DRINK"), "a potion drinks");
        TestSupport.assertEquals(FirstPersonAnims.Action.BOW, FirstPersonAnims.action("BOW"), "a bow draws");
        TestSupport.assertEquals(FirstPersonAnims.Action.NONE, FirstPersonAnims.action("NONE"), "a plain stack has no pose");
        TestSupport.assertEquals(FirstPersonAnims.Action.NONE, FirstPersonAnims.action(null), "an unreadable action has no pose");
    }

    private static void swingArgument() {
        FirstPersonAnims.Action block = FirstPersonAnims.Action.BLOCK;
        FirstPersonAnims.Action eat = FirstPersonAnims.Action.EAT_DRINK;

        // 1.7 passes the live arm swing for a blocking or eating sword/food; 1.8 passes 0.
        TestSupport.assertTrue(nearly(0.5F, FirstPersonAnims.itemTransformSwing(block, 0.0F, 0.5F, true, true)),
            "1.7 blocking uses the arm swing progress");
        TestSupport.assertTrue(nearly(0.5F, FirstPersonAnims.itemTransformSwing(eat, 0.0F, 0.5F, true, true)),
            "1.7 eating uses the arm swing progress");
        TestSupport.assertTrue(nearly(0.0F, FirstPersonAnims.itemTransformSwing(block, 0.0F, 0.5F, false, true)),
            "turning the block pose off keeps vanilla's frozen swing");
        TestSupport.assertTrue(nearly(0.0F, FirstPersonAnims.itemTransformSwing(eat, 0.0F, 0.5F, true, false)),
            "turning the eat/drink pose off keeps vanilla's frozen swing");

        // The normal swing path already passes the live swing, so every action must stay put.
        TestSupport.assertTrue(nearly(0.75F, FirstPersonAnims.itemTransformSwing(block, 0.75F, 0.75F, true, true)),
            "the world swing path is unchanged for a blocking stack");
        TestSupport.assertTrue(nearly(0.75F, FirstPersonAnims.itemTransformSwing(FirstPersonAnims.Action.NONE, 0.75F, 0.25F, true, true)),
            "an unused stack keeps vanilla's swing");
        TestSupport.assertTrue(nearly(0.0F, FirstPersonAnims.itemTransformSwing(FirstPersonAnims.Action.BOW, 0.0F, 0.5F, true, true)),
            "the bow pose leaves the item transform alone");
    }

    private static void bowEasing() {
        TestSupport.assertTrue(nearly(0.0F, FirstPersonAnims.legacyBowEase(0.0F)), "1.7 bow ease starts at 0");
        TestSupport.assertTrue(nearly(0.25F, FirstPersonAnims.legacyBowEase(0.5F)), "1.7 bow ease is the fraction squared");
        TestSupport.assertTrue(nearly(1.0F, FirstPersonAnims.legacyBowEase(1.0F)), "1.7 bow ease ends at 1");
        TestSupport.assertTrue(nearly(1.0F, FirstPersonAnims.legacyBowEase(1.4F)), "1.7 bow ease clamps past full draw");
        TestSupport.assertTrue(nearly(0.0F, FirstPersonAnims.legacyBowEase(-0.3F)), "1.7 bow ease clamps below 0");

        TestSupport.assertTrue(nearly(0.0F, FirstPersonAnims.vanillaBowEase(0.0F)), "1.8 bow ease starts at 0");
        TestSupport.assertTrue(nearly(1.0F, FirstPersonAnims.vanillaBowEase(1.0F)), "1.8 bow ease ends at 1");
        TestSupport.assertTrue(FirstPersonAnims.vanillaBowEase(0.5F) > FirstPersonAnims.legacyBowEase(0.5F),
            "1.8 draws ahead of 1.7 for the first half of the draw");

        TestSupport.assertTrue(nearly(0.0F, FirstPersonAnims.drawFraction(0.0F, 0.0F)), "an empty bow is not drawn");
        TestSupport.assertTrue(nearly(0.5F, FirstPersonAnims.drawFraction(20.0F, 10.0F)), "half the draw ticks is half the curve");
        TestSupport.assertTrue(nearly(1.0F, FirstPersonAnims.drawFraction(40.0F, 0.0F)), "a held draw clamps at full");
    }

    private static void bowCompensation() {
        // The compensated tick reproduces the 1.7 easing through vanilla's own smoothstep.
        for (int step = 0; step <= 20; step++) {
            float fraction = step / 20.0F;
            float remaining = 20.0F;
            float partialTicks = remaining - FirstPersonAnims.BOW_DRAW_TICKS * fraction;
            float compensated = FirstPersonAnims.bowPartialTicks(remaining, partialTicks);
            float vanillaFraction = FirstPersonAnims.drawFraction(remaining, compensated);
            TestSupport.assertTrue(nearly(FirstPersonAnims.legacyBowEase(fraction), FirstPersonAnims.vanillaBowEase(vanillaFraction)),
                "compensated draw fraction matches the 1.7 ease at " + fraction);
        }

        // 1.7 is slower early in the draw, so vanilla has to be told the bow is less drawn:
        // at half draw the 1.7 ease is 0.25, which vanilla reaches at fraction 0.3229.
        float adjusted = FirstPersonAnims.bowPartialTicks(20.0F, 10.0F);
        float halfDrawFraction = (float) (-1.0D + Math.sqrt(1.0D + 3.0D * 0.25D));
        TestSupport.assertTrue(nearly(20.0F - 20.0F * halfDrawFraction, adjusted),
            "mid draw is held back to the 1.7 curve");
        TestSupport.assertTrue(adjusted > 10.0F, "the 1.7 draw lags vanilla at half draw");

        // Endpoints already agree, so both curves are told the same draw state.
        TestSupport.assertTrue(nearly(20.0F, FirstPersonAnims.bowPartialTicks(20.0F, 20.0F)), "an undrawn bow keeps vanilla's input");
        TestSupport.assertTrue(nearly(0.0F, FirstPersonAnims.bowPartialTicks(20.0F, 0.0F)), "a full draw keeps vanilla's input");
        TestSupport.assertTrue(FirstPersonAnims.bowEasesAgree(20.0F, 20.0F), "both eases agree at a full draw");
        TestSupport.assertTrue(FirstPersonAnims.bowEasesAgree(20.0F, 0.0F), "both eases agree before the draw starts");
        TestSupport.assertTrue(!FirstPersonAnims.bowEasesAgree(20.0F, 14.0F), "both eases disagree mid draw");
    }

    private static boolean nearly(float expected, float actual) {
        return Math.abs(expected - actual) <= EPSILON;
    }
}
