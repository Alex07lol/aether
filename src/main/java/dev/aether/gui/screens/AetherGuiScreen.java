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
 * (see docs/GUI_REBUILD.md): Leaf's fullscreen backdrop art with the four navigation tiles
 * at Leaf's exact positions (170x106 at x = 430, 650, 1100, 1320, y = 250), each drawn from
 * its own {@code button/<name>.png} art, with the screen's content underneath.
 * <p>
 * The art itself carries the wording, the panel outline and the shadows, so this class no
 * longer draws a logo or a gradient: the backdrop texture is Leaf's own screen art, recoloured
 * to translucent black and white (see {@link dev.aether.gui.leaf.LeafArt}).
 * <p>
 * Kept from Aether's previous layer because they are objectively better than Leaf's
 * equivalents: the centralized {@link GuiScale} conversion, the GL-state discipline of
 * {@code Mc189Compat}, and screens as pure presentation over the client's managers.
 */
public abstract class AetherGuiScreen extends GuiScreen {

    /** Leaf's navigation tile geometry, in design units. */
    protected static final int[] NAV_X = {430, 650, 1100, 1320};
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
     * player moves their on-screen elements - so the mapping is one to one.
     */
    private static String navArt(GuiSection section) {
        switch (section) {
            case COSMETICS:
                return LeafArt.NAV_COSMETICS;
            case HUD:
                return LeafArt.NAV_HUD;
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
        if (showsNav()) {
            for (int i = 0; i < nav.length; i++) {
                nav[i].setActive(GuiSection.ordered()[i] == section());
                nav[i].render();
            }
        }
        renderContent(mouseX, mouseY);
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
            int delta = -wheel / 24;
            if (delta == 0) {
                delta = wheel > 0 ? 1 : -1;
            }
            wheelContent(mouseX, mouseY, delta);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { // ESC
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
