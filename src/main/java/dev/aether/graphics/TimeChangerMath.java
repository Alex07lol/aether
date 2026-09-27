package dev.aether.graphics;

/**
 * Visual time of day for the time changer module.
 * <p>
 * The module must not write {@code World.setWorldTime}: the world's own clock is shared with the
 * server, the scoreboard, redstone-ish client logic and every other mod, and freezing it is a
 * side effect a cosmetic feature has no business having. Instead the celestial angle - the one
 * value the sky, sun, moon, fog colours and ambient light are derived from - is recomputed from
 * the <em>real</em> world time plus the configured offset at render time. The formula below is a
 * copy of vanilla's {@code WorldProvider.calculateCelestialAngle}, so an offset of 0 draws
 * exactly what vanilla draws.
 */
public final class TimeChangerMath {
    /** One Minecraft day, in ticks. */
    public static final long DAY_LENGTH = 24000L;

    private TimeChangerMath() {
    }

    /** Normalises an offset into {@code [0, 24000)} so negative or huge values cannot drift. */
    public static int wrapOffset(int offset) {
        int wrapped = offset % (int) DAY_LENGTH;
        return wrapped < 0 ? wrapped + (int) DAY_LENGTH : wrapped;
    }

    /**
     * The time the sky is drawn for. Adding the offset moves the sun without touching the world
     * clock; the day counter the server owns stays authoritative.
     */
    public static long visualTime(long worldTime, int offset) {
        return worldTime + wrapOffset(offset);
    }

    /**
     * Vanilla's celestial angle for a given world time: 0 at noon, 0.5 at midnight, wrapping
     * forward through the day. Kept bit-for-bit equivalent to {@code calculateCelestialAngle} so
     * the module is invisible at offset 0.
     */
    public static float celestialAngle(long visualTime, float partialTicks) {
        int dayTime = (int) (visualTime % DAY_LENGTH);
        float angle = ((float) dayTime + partialTicks) / (float) DAY_LENGTH - 0.25F;
        if (angle < 0.0F) {
            angle += 1.0F;
        }
        if (angle > 1.0F) {
            angle -= 1.0F;
        }
        float curve = 1.0F - (float) ((Math.cos((double) angle * Math.PI) + 1.0D) / 2.0D);
        return angle + (curve - angle) / 3.0F;
    }

    /** Tick within the day that {@link #celestialAngle} is drawing. */
    public static long visualDayTime(long worldTime, int offset) {
        long visual = visualTime(worldTime, offset) % DAY_LENGTH;
        return visual < 0 ? visual + DAY_LENGTH : visual;
    }
}
