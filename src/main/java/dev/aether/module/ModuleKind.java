package dev.aether.module;

/**
 * What a registered entry actually is, so the module browser can show the features a player can
 * switch on and nothing else.
 * <p>
 * Aether's registry is a single lookup table for everything the client can enable, which was fine
 * while every entry was a feature. It is not fine any more: a "current cape"
 * is the cosmetic slot the world renderer reads, and the screens themselves need switches that are
 * set by a button and immediately cleared. Exposing those as modules made the browser list things a
 * player cannot use, and left the classification to be rediscovered by every screen.
 * <p>
 * Themes are not classified here at all: a palette is configuration, owned by
 * {@code dev.aether.theme.ThemeManager} and chosen on the Appearance screen, never registered in
 * the module system in the first place.
 * <p>
 * The kind is declared once, next to the module's own metadata, and {@link ModuleRegistry} turns it
 * into the browser's list - so nothing in the GUI has to recognise a module by name.
 */
public enum ModuleKind {

    /**
     * A feature a player switches on: keystrokes, zoom, CPS, fullbright. The only kind the module
     * browser and the module search ever show.
     */
    USER_MODULE,

    /**
     * Part of how the client works rather than something to switch on: a rendering service a screen
     * or a bridge reads. Never listed.
     */
    INTERNAL,

    /**
     * A screen's doorbell: enabling it opens a screen and clears itself. The entry exists so the id
     * is registered and configurable; it is not a feature. Never listed.
     */
    UI_CONTROLLER,

    /**
     * One slot of the cosmetics system: the "current cape / wings / halo / hat / trail" switches
     * and the trail renderers. The in-world cosmetic renderer reads their state and settings, the
     * Cosmetics screen owns the choices. Never listed.
     */
    COSMETIC_SERVICE;

    /** @return true when a module of this kind belongs in the module browser. */
    public boolean isUserFacing() {
        return this == USER_MODULE;
    }
}
