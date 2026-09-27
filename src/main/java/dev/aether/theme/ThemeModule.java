package dev.aether.theme;

import dev.aether.module.ClientModule;

/**
 * A module that selects the active client palette.
 * <p>
 * Theme modules all join {@link #GROUP}, so enabling one disables the others
 * through {@code ModuleRegistry}. Adapters read the winning palette from
 * {@code AetherClient.theme()} instead of hard-coding colours, which is what
 * makes the theme switches real instead of decorative.
 */
public interface ThemeModule extends ClientModule {
    /** Mutual-exclusion group shared by every theme module. */
    String GROUP = "theme";

    ThemePalette palette();
}
