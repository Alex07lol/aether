package dev.aether.forge189.ui.pages;

import dev.aether.forge189.AetherClickGuiScreen;
import dev.aether.forge189.ui.Layout;

/**
 * Abstract base for all Control Center pages. Each page is responsible for drawing its
 * own content and handling clicks within the page region.
 */
public abstract class Page {
    public abstract void render(AetherClickGuiScreen screen,
                                Object font,
                                int mouseX,
                                int mouseY,
                                Layout layout);

    public abstract void handleClick(AetherClickGuiScreen screen,
                                     Layout layout,
                                     int mouseX,
                                     int mouseY,
                                     int button);
}