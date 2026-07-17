package dev.aether.ui.theme;

import java.util.Objects;

public final class AetherUiTheme {
    private final String name;
    private final UiColor background;
    private final UiColor panel;
    private final UiColor card;
    private final UiColor cardHover;
    private final UiColor accent;
    private final UiColor accentGlow;
    private final UiColor border;
    private final UiColor shadow;
    private final UiColor textPrimary;
    private final UiColor textSecondary;
    private final UiColor textDisabled;
    private final UiColor scrollbar;
    private final UiColor toggle;
    private final UiColor selection;
    private final UiSpacing spacing;
    private final UiRadii radii;
    private final UiDurations durations;

    public AetherUiTheme(
        String name,
        UiColor background,
        UiColor panel,
        UiColor card,
        UiColor cardHover,
        UiColor accent,
        UiColor accentGlow,
        UiColor border,
        UiColor shadow,
        UiColor textPrimary,
        UiColor textSecondary,
        UiColor textDisabled,
        UiColor scrollbar,
        UiColor toggle,
        UiColor selection,
        UiSpacing spacing,
        UiRadii radii,
        UiDurations durations
    ) {
        this.name = require(name, "name");
        this.background = require(background, "background");
        this.panel = require(panel, "panel");
        this.card = require(card, "card");
        this.cardHover = require(cardHover, "cardHover");
        this.accent = require(accent, "accent");
        this.accentGlow = require(accentGlow, "accentGlow");
        this.border = require(border, "border");
        this.shadow = require(shadow, "shadow");
        this.textPrimary = require(textPrimary, "textPrimary");
        this.textSecondary = require(textSecondary, "textSecondary");
        this.textDisabled = require(textDisabled, "textDisabled");
        this.scrollbar = require(scrollbar, "scrollbar");
        this.toggle = require(toggle, "toggle");
        this.selection = require(selection, "selection");
        this.spacing = require(spacing, "spacing");
        this.radii = require(radii, "radii");
        this.durations = require(durations, "durations");
    }

    public String name() {
        return name;
    }

    public UiColor background() {
        return background;
    }

    public UiColor panel() {
        return panel;
    }

    public UiColor card() {
        return card;
    }

    public UiColor cardHover() {
        return cardHover;
    }

    public UiColor accent() {
        return accent;
    }

    public UiColor accentGlow() {
        return accentGlow;
    }

    public UiColor border() {
        return border;
    }

    public UiColor shadow() {
        return shadow;
    }

    public UiColor textPrimary() {
        return textPrimary;
    }

    public UiColor textSecondary() {
        return textSecondary;
    }

    public UiColor textDisabled() {
        return textDisabled;
    }

    public UiColor scrollbar() {
        return scrollbar;
    }

    public UiColor toggle() {
        return toggle;
    }

    public UiColor selection() {
        return selection;
    }

    public UiSpacing spacing() {
        return spacing;
    }

    public UiRadii radii() {
        return radii;
    }

    public UiDurations durations() {
        return durations;
    }

    private static <T> T require(T value, String name) {
        return Objects.requireNonNull(value, "Theme " + name + " cannot be null.");
    }
}
