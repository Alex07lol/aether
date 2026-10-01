package dev.aether.gui.screens;

import dev.aether.AetherClient;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;
import dev.aether.gui.GuiScale;
import dev.aether.gui.leaf.LeafArt;
import dev.aether.gui.leaf.NavButton;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.module.setting.Setting;
import dev.aether.ui.GuiSection;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

/**
 * Base class of the Aether GUI screens, carrying the shared Leaf 1.8.9 composition
 * (see docs/GUI_REBUILD.md): Leaf's fullscreen backdrop art with five navigation tiles at
 * Leaf's exact rectangles (170x106 at y = 250; Leaf's own x = 430, 650, 1100, 1320 pitched 220,
 * plus Aether's Themes tile at the free x = 860), each drawn from its own
 * {@code button/<name>.png} art, with the screen's content underneath.
 * <p>
 * The art itself carries the wording, the panel outline and the shadows, so this class no
 * longer draws a logo or a gradient: the backdrop texture is Leaf's own screen art, recoloured
 * to translucent black and white (see {@link dev.aether.gui.leaf.LeafArt}). The one piece of
 * Aether branding the port kept is the wordmark on the entry screen, drawn by
 * {@code AetherModScreen} into the panel corner Leaf leaves empty.
 * <p>
 * Kept from Aether's previous layer because they are objectively better than Leaf's
 * equivalents: the centralized {@link GuiScale} conversion, the GL-state discipline of
 * {@code Mc189Compat}, and screens as pure presentation over the client's managers.
 */
public abstract class AetherGuiScreen extends GuiScreen implements dev.aether.gui.core.AetherUiScreen {

    /** Leaf's navigation tile geometry, in design units. Five tiles share the row between
     * Leaf's outer pairs (430/1490 outer, 650/1100 inner), pitched 220 - Leaf's own pitch. */
    protected static final int[] NAV_X = {430, 650, 860, 1100, 1320};
    protected static final int NAV_Y = 250;
    protected static final int NAV_W = 170;
    protected static final int NAV_H = 106;

    protected final AetherClient client;
    protected final GuiScreen parent;

    /** Mouse position in design units, refreshed at the top of every frame. */
    protected double mouseX;
    protected double mouseY;

    private boolean blurLoaded;
    private NavButton[] nav;

    /**
     * The screen's own entrance: the whole composition eases up a few units when a section is
     * opened. One animation here rather than one per screen is what makes switching sections feel
     * like the same client, and it is driven from the frame clock, so it is the same 160 ms at 30
     * and at 240 FPS.
     */
    private final dev.aether.animation.Anim openTransition =
        new dev.aether.animation.Anim(0.0F, 160.0F, dev.aether.animation.Easing.EASE_OUT_CUBIC);
    private static final int OPEN_SLIDE = 8;

    protected AetherGuiScreen(AetherClient client) {
        this(client, null);
    }

    protected AetherGuiScreen(AetherClient client, GuiScreen parent) {
        this.client = client;
        this.parent = parent;
        buildNav();
    }

    /* ── screen contract ────────────────────────────────────────────────── */

    /** The section this screen shows; drives the highlighted navigation tile. */
    protected abstract GuiSection section();

    /** Draws the screen's content under the chrome. Mouse is in design units. */
    protected abstract void renderContent(double mx, double my);

    /**
     * A click the navigation did not consume.
     *
     * @return true when the content consumed the click
     */
    protected abstract boolean clickContent(double mx, double my, int button);

    /** A mouse button was released. */
    protected abstract void releaseContent(double mx, double my, int button);

    /** A key the screen may consume; ESC never reaches here. */
    protected boolean keyContent(char typedChar, int keyCode) {
        return false;
    }

    /** The wheel turned. */
    protected boolean wheelContent(double mx, double my, int delta) {
        return false;
    }

    /** Screens without the standard navigation row (the module settings screen). */
    protected boolean showsNav() {
        return true;
    }

    protected void disposeContent() {
    }

    /* ── lifecycle ──────────────────────────────────────────────────────── */

    @Override
    public void onGuiClosed() {
        disposeContent();
        if (blurLoaded) {
            blurLoaded = false;
            try {
                Mc189Compat.stopShader();
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void buildNav() {
        GuiSection[] sections = GuiSection.ordered();
        nav = new NavButton[sections.length];
        for (int i = 0; i < sections.length; i++) {
            final GuiSection target = sections[i];
            // Leaf's tiles carry their own wording in the art, so no label is drawn over them.
            nav[i] = new NavButton(navArt(target), NAV_X[i], NAV_Y, NAV_W, NAV_H, new Runnable() {
                public void run() {
                    if (target != section()) {
                        switchSection(target);
                    }
                }
            });
        }
    }

    /**
     * Leaf's navigation tile for a section. Leaf names its tiles mod / cosmetic / location /
     * setting, and "location" is the same screen Aether calls the HUD editor - the place a
     * player moves their on-screen elements - so the mapping is one to one. The Themes tile is
     * Aether's own (Leaf's system tab recoloured), because Leaf has no theme picker.
     */
    private static String navArt(GuiSection section) {
        switch (section) {
            case COSMETICS:
                return LeafArt.NAV_COSMETICS;
            case HUD:
                return LeafArt.NAV_HUD;
            case THEMES:
                return LeafArt.NAV_THEMES;
            case SETTINGS:
                return LeafArt.NAV_SETTINGS;
            case MODULES:
            default:
                return LeafArt.NAV_MODULES;
        }
    }

    /* ── rendering ──────────────────────────────────────────────────────── */

    @Override
    public void drawScreen(int rawMouseX, int rawMouseY, float partialTicks) {
        GuiScale.update(width, height);
        syncTheme();
        syncBlur();
        mouseX = GuiScale.mouseX(rawMouseX);
        mouseY = GuiScale.mouseY(rawMouseY);

        drawBackdrop();

        this.openTransition.target(1.0F);
        this.openTransition.update();
        float entrance = this.openTransition.value();
        boolean sliding = entrance < 1.0F;
        if (sliding) {
            Mc189Compat.translate(0.0F, GuiScale.h((1.0F - entrance) * OPEN_SLIDE), 0.0F);
        }
        if (showsNav()) {
            for (int i = 0; i < nav.length; i++) {
                nav[i].setActive(GuiSection.ordered()[i] == section());
                nav[i].render();
            }
        }
        renderContent(mouseX, mouseY);
        if (sliding) {
            Mc189Compat.translate(0.0F, -GuiScale.h((1.0F - entrance) * OPEN_SLIDE), 0.0F);
        }
        Mc189Compat.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Leaf's backdrop: one fullscreen texture that already contains the scrim and the panel the
     * screen's controls sit on. Screens that Leaf gave its own backdrop override this.
     */
    protected String backdropArt() {
        return LeafArt.BACKDROP_MAIN;
    }

    private void drawBackdrop() {
        LeafArt.draw(backdropArt(), 0, 0, GuiScale.guiWidth(), GuiScale.guiHeight());
    }

    private void syncTheme() {
        AetherUi.syncTheme();
    }

    private void syncBlur() {
        boolean wanted = moduleEnabled("graphics.ui_blur");
        if (wanted == blurLoaded) {
            return;
        }
        blurLoaded = wanted;
        try {
            if (wanted) {
                Mc189Compat.loadBlurShader();
            } else {
                Mc189Compat.stopShader();
            }
        } catch (Throwable ignored) {
        }
    }

    /* ── input ──────────────────────────────────────────────────────────── */

    @Override
    protected void mouseClicked(int rawX, int rawY, int button) throws IOException {
        double mx = GuiScale.mouseX(rawX);
        double my = GuiScale.mouseY(rawY);
        if (showsNav()) {
            for (NavButton tile : nav) {
                if (tile.onMouseClick(mx, my, button)) {
                    return;
                }
            }
        }
        clickContent(mx, my, button);
    }

    @Override
    protected void mouseReleased(int rawX, int rawY, int button) {
        releaseContent(GuiScale.mouseX(rawX), GuiScale.mouseY(rawY), button);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mc189Compat.mouseWheelDelta();
        if (wheel != 0) {
            wheelContent(mouseX, mouseY, scrollDelta(-wheel / 24));
        }
    }

    /**
     * Applies the user's scroll direction to one wheel step.
     * <p>
     * Convention: positive = scroll down / forward. Vanilla's raw wheel is inverted on the way in
     * ({@code -wheel / 24}), and the ported Leaf screens then inverted it again on the way to the
     * {@code PageBar}, which made the wheel run backwards on every paged screen. The sign is now
     * normalised once, here, and the "Invert Scroll" preference flips it for everyone - screens
     * must not negate it again. The raw value (before this flip) also decides the PageBar direction
     * through {@link #scrollDelta(int)}.
     */
    protected int scrollDelta(int raw) {
        return client.preferences().invertScroll() ? -raw : raw;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { // ESC
            // ESC belongs to the screen first: a keybind capture cancels with it, everything else
            // leaves for the screen this one was opened from.
            if (keyContent(typedChar, keyCode)) {
                return;
            }
            Mc189Compat.displayGuiScreen(escapeScreen());
            return;
        }
        if (keyContent(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    /** The screen ESC returns to; screens with a home flow override this. */
    protected GuiScreen escapeScreen() {
        return parent;
    }

    protected void switchSection(GuiSection target) {
        saveQuietly();
        dev.aether.gui.AetherGui.open(client, target);
    }

    /* ── shared helpers ─────────────────────────────────────────────────── */

    protected boolean moduleEnabled(String id) {
        try {
            return client.modules().get(id).state() == ModuleState.ENABLED;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    protected void saveQuietly() {
        try {
            client.save();
        } catch (IOException ignored) {
            // The shutdown hook and the next change both retry the write.
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected static void setValue(Setting<?> setting, Object value) {
        ((Setting) setting).setValue(value);
    }
}
