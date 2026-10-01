package dev.aether.gui.core;

/**
 * Marks a screen as Aether's own.
 * <p>
 * The input router has to know which screens belong to the client: a module key must never act
 * while one of them owns the keyboard, and the router reports that separately from "some screen is
 * open" so the GUI can reason about itself (the keybind capture, and the menu key not re-opening a
 * menu that is already up). A marker interface is what keeps that working for screens that do not
 * exist yet - a new Aether screen implements this and is covered by the rule, with no list of class
 * names to update anywhere.
 */
public interface AetherUiScreen {
}
