package dev.aether.animation;

import dev.aether.TestSupport;

/**
 * Guards the shared animation layer.
 * <p>
 * The two properties worth pinning down are (a) an animation takes the same wall-clock time
 * whatever the frame rate is, and (b) a value can never be advanced twice inside one frame, which
 * is the bug that turns a shared clock into a double-speed animation the moment two consumers read
 * it. Both are pure maths, so they are tested here instead of by watching the HUD.
 */
public final class AnimationTest {
    private static final float EPSILON = 0.0005F;

    private AnimationTest() {
    }

    public static void main(String[] args) {
        easingEndpoints();
        easingMonotonicForIn();
        colourMaths();
        approachIsTimeBased();
        animReachesTarget();
        animIsFrameRateIndependent();
        animDoesNotDoubleStep();
        animDurationZeroSnaps();
        animInterruptsFromCurrentValue();
        animRestartsOnRetarget();
        System.out.println("AnimationTest passed");
    }

    private static void easingEndpoints() {
        for (Easing easing : Easing.values()) {
            TestSupport.assertTrue(nearly(0.0F, easing.apply(0.0F)), easing + " starts at 0");
            TestSupport.assertTrue(nearly(1.0F, easing.apply(1.0F)), easing + " ends at 1");
            TestSupport.assertTrue(nearly(easing.apply(0.0F), easing.apply(-2.0F)), easing + " clamps below 0");
            TestSupport.assertTrue(nearly(easing.apply(1.0F), easing.apply(3.0F)), easing + " clamps above 1");
        }
        // Deceleration is the point of the OUT curves: they are ahead of linear halfway through.
        TestSupport.assertTrue(Easing.EASE_OUT_QUAD.apply(0.5F) > 0.5F, "ease-out is ahead at the midpoint");
        TestSupport.assertTrue(Easing.EASE_IN_QUAD.apply(0.5F) < 0.5F, "ease-in is behind at the midpoint");
        TestSupport.assertTrue(nearly(0.5F, Easing.EASE_IN_OUT_CUBIC.apply(0.5F)), "ease-in-out is symmetric");
        TestSupport.assertTrue(Easing.EASE_OUT_BACK.overshoots(), "the out-back curve overshoots");
        TestSupport.assertTrue(nearly(1.0F, Easing.EASE_OUT_BACK.apply(1.0F)), "out-back still lands on 1");
    }

    private static void easingMonotonicForIn() {
        float previous = -1.0F;
        for (int step = 0; step <= 20; step++) {
            float value = Easing.EASE_OUT_CUBIC.apply(step / 20.0F);
            TestSupport.assertTrue(value >= previous, "ease-out-cubic never goes backwards");
            previous = value;
        }
    }

    private static void colourMaths() {
        TestSupport.assertEquals(Integer.valueOf(0xFF112233), Integer.valueOf(AnimationMath.lerpColor(0xFF112233, 0xFFAABBCC, 0.0F)),
            "a blend at 0 keeps the source colour");
        TestSupport.assertEquals(Integer.valueOf(0xFFAABBCC), Integer.valueOf(AnimationMath.lerpColor(0xFF112233, 0xFFAABBCC, 1.0F)),
            "a blend at 1 lands on the destination colour");
        int middle = AnimationMath.lerpColor(0xFF000000, 0xFFFFFFFF, 0.5F);
        TestSupport.assertEquals(Integer.valueOf(0xFF808080), Integer.valueOf(AnimationMath.withAlpha(middle, 1.0F)),
            "alpha can be forced to fully opaque without touching the colour");
        TestSupport.assertEquals(Integer.valueOf(0x00FFFFFF), Integer.valueOf(AnimationMath.withAlpha(0xFFFFFFFF, 0.0F)),
            "alpha can be forced to fully transparent");
        TestSupport.assertEquals(Integer.valueOf(0x80FFFFFF), Integer.valueOf(AnimationMath.scaleAlpha(0xFFFFFFFF, 0.5F)),
            "scaling alpha halves an opaque colour");
        TestSupport.assertEquals(Integer.valueOf(0x40FF0000), Integer.valueOf(AnimationMath.scaleAlpha(0x80FF0000, 0.5F)),
            "scaling alpha is relative to the colour's own alpha");
    }

    private static void approachIsTimeBased() {
        TestSupport.assertTrue(nearly(1.0F, AnimationMath.approach(1.0F, 0.0F, 0.0F, 120.0F)),
            "a frame with no elapsed time makes no progress");
        TestSupport.assertTrue(nearly(0.0F, AnimationMath.approach(1.0F, 0.0F, 16.0F, 0.0F)),
            "a zero rate snaps to the target");
        float once = AnimationMath.approach(1.0F, 0.0F, 32.0F, 100.0F);
        float twice = AnimationMath.approach(AnimationMath.approach(1.0F, 0.0F, 16.0F, 100.0F), 0.0F, 16.0F, 100.0F);
        TestSupport.assertTrue(Math.abs(once - twice) < 0.002F,
            "two 16ms steps land where one 32ms step lands (exponential form)");
    }

    private static void animReachesTarget() {
        Anim anim = new Anim(0.0F, 100.0F, Easing.LINEAR);
        TestSupport.assertTrue(nearly(0.0F, anim.value()), "an animation starts at its initial value");
        TestSupport.assertTrue(anim.settled(), "an animation with no target is settled");
        anim.target(1.0F);
        TestSupport.assertTrue(!anim.settled(), "a fresh animation is not settled");
        for (int i = 0; i < 3; i++) {
            anim.update(50.0F);
        }
        TestSupport.assertTrue(nearly(1.0F, anim.value()), "50ms of a 100ms animation is half way, twice is all the way");
        TestSupport.assertTrue(anim.settled(), "the animation reports settled at its target");
        TestSupport.assertTrue(nearly(1.0F, anim.progress()), "progress reaches 1");
    }

    private static void animIsFrameRateIndependent() {
        // The same 200ms animation sampled at 30 and at 240 FPS must agree at the same wall-clock
        // time. This is the property that a frame-count or per-frame-step implementation fails.
        Anim slow = new Anim(0.0F, 200.0F, Easing.EASE_OUT_CUBIC).duration(200.0F);
        Anim fast = new Anim(0.0F, 200.0F, Easing.EASE_OUT_CUBIC).duration(200.0F);
        slow.target(1.0F);
        fast.target(1.0F);

        for (int frame = 0; frame < 6; frame++) {
            slow.update(1000.0F / 30.0F);
            for (int inner = 0; inner < 8; inner++) {
                fast.update(1000.0F / 240.0F);
            }
        }
        TestSupport.assertTrue(Math.abs(slow.value() - fast.value()) < 0.005F,
            "a 30 FPS sample and a 240 FPS sample agree after the same 200ms");

        // And the end state is exact at every rate.
        slow.update(50.0F);
        fast.update(50.0F);
        TestSupport.assertTrue(nearly(1.0F, slow.value()), "30 FPS lands exactly on the target");
        TestSupport.assertTrue(nearly(1.0F, fast.value()), "240 FPS lands exactly on the target");
    }

    private static void animDoesNotDoubleStep() {
        FrameClock.reset();
        FrameClock.beginFrame(16.0F);
        Anim anim = new Anim(0.0F, 100.0F, Easing.LINEAR);
        anim.target(1.0F);
        float first = anim.update();
        float second = anim.update();
        TestSupport.assertTrue(nearly(first, second), "a second update in the same frame changes nothing");
        FrameClock.beginFrame(16.0F);
        TestSupport.assertTrue(anim.update() > first, "the next frame advances again");
        FrameClock.reset();
    }

    private static void animDurationZeroSnaps() {
        Anim up = new Anim(0.0F, 0.0F, Easing.EASE_OUT_CUBIC);
        up.target(1.0F);
        TestSupport.assertTrue(nearly(1.0F, up.value()), "a zero duration snaps up instead of animating");
        TestSupport.assertTrue(up.settled(), "a snapped animation is settled");

        Anim down = new Anim(1.0F, 0.0F, Easing.LINEAR);
        down.target(0.0F);
        TestSupport.assertTrue(nearly(0.0F, down.value()), "a zero duration snaps down instead of animating");

        // Declaring a duration again makes the next move animate: this is how a module's own
        // "Fade Time" setting turns the snap into a real fade without a second code path.
        down.duration(100.0F).target(1.0F);
        down.update(50.0F);
        TestSupport.assertTrue(down.value() > 0.0F && down.value() < 1.0F, "a duration makes the next move animate");
        down.update(100.0F);
        TestSupport.assertTrue(nearly(1.0F, down.value()), "and it still lands exactly on the target");
    }

    private static void animInterruptsFromCurrentValue() {
        Anim anim = new Anim(0.0F, 100.0F, Easing.LINEAR);
        anim.target(1.0F);
        anim.update(40.0F);
        float partway = anim.value();
        TestSupport.assertTrue(partway > 0.0F && partway < 1.0F, "the press is part way through");
        anim.target(0.0F);
        TestSupport.assertTrue(nearly(partway, anim.value()), "releasing continues from the pressed value");
        float midpoint = anim.update(20.0F);
        TestSupport.assertTrue(midpoint < partway, "the release travels back down");
        anim.update(200.0F);
        TestSupport.assertTrue(nearly(0.0F, anim.value()), "the release lands on idle");
    }

    private static void animRestartsOnRetarget() {
        Anim anim = new Anim(0.0F, 100.0F, Easing.LINEAR);
        anim.target(1.0F);
        TestSupport.assertTrue(nearly(0.0F, anim.progress()), "targeting restarts progress");
        anim.update(100.0F);
        anim.target(1.0F);
        TestSupport.assertTrue(nearly(1.0F, anim.progress()), "targeting the value it already has is a no-op");
        anim.target(0.5F);
        TestSupport.assertTrue(nearly(0.0F, anim.progress()), "a different target restarts progress");
        anim.reverse();
        TestSupport.assertTrue(nearly(1.0F, anim.target()), "reverse travels back to the origin");
        anim.update(200.0F);
        TestSupport.assertTrue(nearly(1.0F, anim.value()), "the reverse finishes at the origin");
    }

    private static boolean nearly(float expected, float actual) {
        return Math.abs(expected - actual) <= EPSILON;
    }
}
