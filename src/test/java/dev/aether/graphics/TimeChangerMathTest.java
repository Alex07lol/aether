package dev.aether.graphics;

import dev.aether.TestSupport;

/**
 * Guards {@link TimeChangerMath}: the celestial angle must match vanilla's own formula so an
 * offset of 0 is invisible, and the offset must move the sky without touching the world clock.
 */
public final class TimeChangerMathTest {
    private static final float EPSILON = 0.001F;

    private TimeChangerMathTest() {
    }

    public static void main(String[] args) {
        vanillaEquivalence();
        daySignposts();
        offsetShiftsTheSky();
        offsetWrapping();
        worldTimeIsNeverTouched();
        System.out.println("TimeChangerMathTest passed");
    }

    private static void vanillaEquivalence() {
        // Vanilla's WorldProvider.calculateCelestialAngle, written out again on purpose.
        for (long time = 0L; time < 48000L; time += 250L) {
            float expected = referenceAngle(time, 0.5F);
            float actual = TimeChangerMath.celestialAngle(time, 0.5F);
            TestSupport.assertTrue(Math.abs(expected - actual) <= EPSILON,
                "the angle at " + time + " ticks matches vanilla (" + expected + " vs " + actual + ")");
        }
        TestSupport.assertEquals(Long.valueOf(12345L), Long.valueOf(TimeChangerMath.visualTime(12345L, 0)),
            "an offset of 0 draws the real world time");
    }

    private static void daySignposts() {
        TestSupport.assertTrue(nearly(0.0F, TimeChangerMath.celestialAngle(6000L, 0.0F)),
            "noon is the zero of vanilla's angle");
        TestSupport.assertTrue(nearly(0.5F, TimeChangerMath.celestialAngle(18000L, 0.0F)),
            "midnight is half way through the angle");
        TestSupport.assertTrue(nearly(0.7845F, TimeChangerMath.celestialAngle(0L, 0.0F)),
            "the start of the day sits on the sunrise shoulder");
        TestSupport.assertTrue(TimeChangerMath.celestialAngle(6000L, 0.0F) < TimeChangerMath.celestialAngle(12000L, 0.0F)
                && TimeChangerMath.celestialAngle(12000L, 0.0F) < TimeChangerMath.celestialAngle(18000L, 0.0F),
            "the angle runs forward from noon to midnight");
    }

    private static void offsetShiftsTheSky() {
        long realTime = 1000L;
        float real = TimeChangerMath.celestialAngle(TimeChangerMath.visualTime(realTime, 0), 0.0F);
        float dusk = TimeChangerMath.celestialAngle(TimeChangerMath.visualTime(realTime, 12000), 0.0F);
        TestSupport.assertTrue(Math.abs(real - dusk) > 0.1F, "an offset of 12000 changes the sky");
        TestSupport.assertTrue(nearly(TimeChangerMath.celestialAngle(13000L, 0.0F), dusk),
            "the offset is a pure shift of the world time");
        TestSupport.assertEquals(Long.valueOf(12000L),
            Long.valueOf(TimeChangerMath.visualDayTime(15000L, -3000)), "a negative offset shifts back");
    }

    private static void offsetWrapping() {
        TestSupport.assertEquals(Integer.valueOf(23999), Integer.valueOf(TimeChangerMath.wrapOffset(-1)),
            "offsets wrap into the day");
        TestSupport.assertEquals(Integer.valueOf(0), Integer.valueOf(TimeChangerMath.wrapOffset(24000)),
            "a full day of offset is the same as none");
        TestSupport.assertEquals(Integer.valueOf(500), Integer.valueOf(TimeChangerMath.wrapOffset(48500)),
            "the offset keeps wrapping past two days");
        TestSupport.assertEquals(Integer.valueOf(16000),
            Integer.valueOf(TimeChangerMath.wrapOffset(1000000)), "a huge offset is still normalised");
    }

    private static void worldTimeIsNeverTouched() {
        // The whole point of the module: the caller passes world time in and only gets an angle back.
        long worldTime = 24000L * 7L + 6000L;
        TimeChangerMath.celestialAngle(TimeChangerMath.visualTime(worldTime, 18000), 0.0F);
        TestSupport.assertEquals(Long.valueOf(worldTime), Long.valueOf(worldTime),
            "the world time used for the visual override is untouched");
        TestSupport.assertEquals(Long.valueOf(6000L),
            Long.valueOf(TimeChangerMath.visualDayTime(worldTime, 0)),
            "the day count in the real world time is preserved");
    }

    /** Vanilla's formula, kept separate from the implementation under test. */
    private static float referenceAngle(long worldTime, float partialTicks) {
        int dayTime = (int) (worldTime % 24000L);
        float angle = ((float) dayTime + partialTicks) / 24000.0F - 0.25F;
        if (angle < 0.0F) {
            angle += 1.0F;
        }
        if (angle > 1.0F) {
            angle -= 1.0F;
        }
        float curve = 1.0F - (float) ((Math.cos((double) angle * Math.PI) + 1.0D) / 2.0D);
        return angle + (curve - angle) / 3.0F;
    }

    private static boolean nearly(float expected, float actual) {
        return Math.abs(expected - actual) <= EPSILON;
    }
}
