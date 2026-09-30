package dev.aether.hud;

import java.util.Locale;

/**
 * The two pure rules behind the Target Info widget: how the health line reads and how full the
 * health bar is.
 * <p>
 * They live in the core rather than in the renderer so they can be tested without Minecraft (see
 * {@code TargetHealthTextTest}) and so the three modes the module offers are defined in exactly one
 * place - a {@code health_mode} choice the renderer does not implement would be a dead setting.
 */
public final class TargetHealthText {

    /** The three {@code health_mode} options, in the order the module declares them. */
    public static final String VALUE = "Value";
    public static final String PERCENT = "Percent";
    public static final String BOTH = "Both";

    private TargetHealthText() {
    }

    /**
     * @param health    the entity's health, or a negative value when the runtime cannot read it
     * @param maxHealth the entity's maximum health, or a non-positive value when unknown
     * @param mode      the module's {@code health_mode}
     * @return the line to draw; empty when health is unknown, which is what makes the widget fall
     *         back to showing only the skin and the name instead of a made-up number
     */
    public static String format(float health, float maxHealth, String mode) {
        if (health < 0.0F) {
            return "";
        }
        int percent = percent(health, maxHealth);
        if (PERCENT.equalsIgnoreCase(mode)) {
            return percent + "%";
        }
        if (BOTH.equalsIgnoreCase(mode)) {
            return Math.round(health) + " / " + Math.round(maxHealth) + " (" + percent + "%)";
        }
        return String.format(Locale.ENGLISH, "%.1f", Float.valueOf(health));
    }

    /** @return the health bar's fill as 0-1, clamped; unknown health reads as a full bar. */
    public static float fraction(float health, float maxHealth) {
        if (health < 0.0F || maxHealth <= 0.0F) {
            return 1.0F;
        }
        return Math.min(1.0F, Math.max(0.0F, health / maxHealth));
    }

    /** @return the rounded percentage, or 0 when the maximum is unknown. */
    public static int percent(float health, float maxHealth) {
        if (health < 0.0F || maxHealth <= 0.0F) {
            return 0;
        }
        return Math.round(health / maxHealth * 100.0F);
    }
}
