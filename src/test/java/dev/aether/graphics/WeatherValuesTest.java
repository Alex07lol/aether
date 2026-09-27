package dev.aether.graphics;

import dev.aether.TestSupport;

/**
 * Guards {@link WeatherValues}: server weather is never overridden unless asked for, clear/rain/storm
 * differ in the way their names promise, and strengths clamp.
 */
public final class WeatherValuesTest {
    private WeatherValuesTest() {
    }

    public static void main(String[] args) {
        serverMode();
        clearMode();
        rainMode();
        stormMode();
        strengthClamping();
        System.out.println("WeatherValuesTest passed");
    }

    private static void serverMode() {
        TestSupport.assertEquals(null, WeatherValues.rainStrength(WeatherValues.Mode.SERVER, 100),
            "server mode reports no rain override");
        TestSupport.assertEquals(null, WeatherValues.thunderStrength(WeatherValues.Mode.SERVER, 100),
            "server mode reports no thunder override");
        TestSupport.assertEquals(null, WeatherValues.rainStrength(null, 100),
            "an unreadable mode falls back to the server's weather");
        TestSupport.assertTrue(!WeatherValues.overrides(WeatherValues.Mode.SERVER),
            "server mode overrides nothing");
    }

    private static void clearMode() {
        TestSupport.assertEquals(Float.valueOf(0.0F), WeatherValues.rainStrength(WeatherValues.Mode.CLEAR, 100),
            "clear skies report no rain");
        TestSupport.assertEquals(Float.valueOf(0.0F), WeatherValues.thunderStrength(WeatherValues.Mode.CLEAR, 100),
            "clear skies report no thunder");
        TestSupport.assertTrue(WeatherValues.overrides(WeatherValues.Mode.CLEAR),
            "clear skies still have to override the server");
    }

    private static void rainMode() {
        TestSupport.assertEquals(Float.valueOf(0.5F), WeatherValues.rainStrength(WeatherValues.Mode.RAIN, 50),
            "rain uses the configured strength");
        TestSupport.assertEquals(Float.valueOf(0.0F), WeatherValues.thunderStrength(WeatherValues.Mode.RAIN, 100),
            "plain rain never flashes");
    }

    private static void stormMode() {
        TestSupport.assertEquals(Float.valueOf(1.0F), WeatherValues.rainStrength(WeatherValues.Mode.STORM, 100),
            "a storm rains at full strength");
        TestSupport.assertEquals(Float.valueOf(0.8F), WeatherValues.thunderStrength(WeatherValues.Mode.STORM, 80),
            "and flashes at its own configured strength");
    }

    private static void strengthClamping() {
        TestSupport.assertEquals(Float.valueOf(0.0F), WeatherValues.rainStrength(WeatherValues.Mode.RAIN, -20),
            "a negative strength clamps to none");
        TestSupport.assertEquals(Float.valueOf(1.0F), WeatherValues.rainStrength(WeatherValues.Mode.RAIN, 400),
            "an oversized strength clamps to full");
        TestSupport.assertEquals(WeatherValues.Mode.STORM, WeatherValues.Mode.from("storm", WeatherValues.Mode.SERVER),
            "mode names are read case-insensitively");
        TestSupport.assertEquals(WeatherValues.Mode.SERVER, WeatherValues.Mode.from("blizzard", WeatherValues.Mode.SERVER),
            "an unknown mode falls back");
    }
}
