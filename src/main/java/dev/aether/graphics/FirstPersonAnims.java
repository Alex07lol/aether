package dev.aether.graphics;

/**
 * 1.7 first-person item pose rules, kept as pure math so they can be unit tested
 * without a Minecraft runtime.
 * <p>
 * Two things changed between 1.7 and 1.8 for the item in your hand:
 * <ul>
 *   <li>1.8 freezes the arm swing while an item is in use: every transform call in the
 *       using branch passes a swing progress of {@code 0}, so the blocking sword, the
 *       food and the bow no longer follow the arm. 1.7 passed the live swing progress,
 *       which is the "block hit" bob animation.</li>
 *   <li>1.8 eases the bow draw with a smoothstep ({@code (f^2 + 2f) / 3}); 1.7 used
 *       {@code f^2}, which draws slower for the first half and finishes sharply.</li>
 * </ul>
 * The adapter applies these rules by rewriting the arguments of vanilla's own render
 * methods, so no pose constants are duplicated here.
 */
public final class FirstPersonAnims {
    /** Ticks a 1.7/1.8 bow takes to reach full draw; both versions divide the draw by this. */
    public static final float BOW_DRAW_TICKS = 20.0F;

    private FirstPersonAnims() {
    }

    /** The action vanilla reports for the held stack, reduced to what the poses care about. */
    public enum Action {
        NONE,
        BLOCK,
        EAT_DRINK,
        BOW
    }

    /** @return the pose action for an item use action name ({@code EnumAction.toString()}). */
    public static Action action(String useActionName) {
        if ("BLOCK".equals(useActionName)) {
            return Action.BLOCK;
        }
        if ("EAT".equals(useActionName) || "DRINK".equals(useActionName)) {
            return Action.EAT_DRINK;
        }
        if ("BOW".equals(useActionName)) {
            return Action.BOW;
        }
        return Action.NONE;
    }

    /**
     * @param action the held stack's use action
     * @param vanillaSwing the swing progress vanilla passes to its own transform
     * @param swingProgress the live swing progress of the player's arm
     * @param blockAnimation whether the 1.7 block pose is enabled
     * @param eatDrinkAnimation whether the 1.7 eat/drink pose is enabled
     * @return the swing progress the item transform should use
     */
    public static float itemTransformSwing(Action action, float vanillaSwing, float swingProgress,
                                           boolean blockAnimation, boolean eatDrinkAnimation) {
        if (action == Action.BLOCK) {
            return blockAnimation ? swingProgress : vanillaSwing;
        }
        if (action == Action.EAT_DRINK) {
            return eatDrinkAnimation ? swingProgress : vanillaSwing;
        }
        return vanillaSwing;
    }

    /** 1.7 bow draw easing: the draw fraction squared. */
    public static float legacyBowEase(float drawFraction) {
        float fraction = clamp01(drawFraction);
        return fraction * fraction;
    }

    /** 1.8 bow draw easing: {@code (f^2 + 2f) / 3}, mirrored so the compensation can be tested. */
    public static float vanillaBowEase(float drawFraction) {
        float fraction = clamp01(drawFraction);
        return (fraction * fraction + fraction * 2.0F) / 3.0F;
    }

    /**
     * The draw fraction vanilla's easing must receive to reach the same value as the 1.7
     * easing at {@code drawFraction} (the positive root of {@code f^2 + 2f - 3ease = 0}).
     */
    public static float vanillaFractionForLegacyEase(float drawFraction) {
        float eased = legacyBowEase(drawFraction);
        if (eased <= 0.0F) {
            return 0.0F;
        }
        if (eased >= 1.0F) {
            return 1.0F;
        }
        return (float) (-1.0D + Math.sqrt(1.0D + 3.0D * eased));
    }

    /** @return how far the bow is drawn, from {@code 0} to {@code 1}. */
    public static float drawFraction(float remainingUseTicks, float partialTicks) {
        return clamp01((remainingUseTicks - partialTicks) / BOW_DRAW_TICKS);
    }

    /**
     * Vanilla reads the draw state as {@code remainingTicks - partialTicks} divided by
     * {@link #BOW_DRAW_TICKS}. Handing it a compensated partial tick makes its own bow pose
     * follow the 1.7 easing curve while keeping vanilla's rotations and bob shape.
     *
     * @return the partial tick to pass to the bow transform
     */
    public static float bowPartialTicks(float remainingUseTicks, float partialTicks) {
        float fraction = drawFraction(remainingUseTicks, partialTicks);
        float wanted = vanillaFractionForLegacyEase(fraction);
        return remainingUseTicks - BOW_DRAW_TICKS * wanted;
    }

    /** @return whether the two eases already agree closely enough to leave vanilla alone. */
    public static boolean bowEasesAgree(float remainingUseTicks, float partialTicks) {
        float fraction = drawFraction(remainingUseTicks, partialTicks);
        return Math.abs(legacyBowEase(fraction) - vanillaBowEase(fraction)) < 0.001F;
    }

    private static float clamp01(float value) {
        if (value < 0.0F) {
            return 0.0F;
        }
        return value > 1.0F ? 1.0F : value;
    }
}
