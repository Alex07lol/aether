package dev.aether.gui.screens;

import dev.aether.AetherClient;
import dev.aether.config.ClientPreferences;
import dev.aether.forge189.AetherFontDiagScreen;
import dev.aether.forge189.AetherUi;
import dev.aether.forge189.Mc189Compat;
import dev.aether.gui.AetherFont;
import dev.aether.gui.GuiScale;
import dev.aether.gui.components.TextField;
import dev.aether.gui.core.ScreenShell;
import dev.aether.ui.GuiSection;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.module.setting.Setting;
import net.minecraft.client.gui.GuiScreen;

import java.io.IOException;

/**
 * Base class of the four Aether GUI screens. It owns everything the screens share -
 * the shell chrome, the design-space mouse coordinates, the blur toggle, the save path
 * and the input routing order - so a screen only implements its content.
 * <p>
 * Input routing order (the hierarchy the brief demands):
 * <ol>
 *   <li>the shell: navigation tabs and the search field;</li>
 *   <li>the screen's content, which decides among its own components;</li>
 *   <li>nothing else - a click that no component wanted is dropped, never passed
 *       through to whatever happens to be underneath.</li>
 * </ol>
 * Keyboard input goes to the focused control first (search field), then to the
 * content, then to ESC. ESC closes a panel or defocuses a field before it closes the
 * screen.
 * <p>
 * Screens are swapped (not re-laid-out in place) when the section changes, and every
 * durable value lives in the client's managers, so a fresh screen instance always
 * shows correct state without carrying any over.
 */
public abstract class AetherGuiScreen extends GuiScreen {

    protected final AetherClient client;
    protected final GuiScreen parent;
    protected final ScreenShell shell = new ScreenShell();

    /** Mouse position in design units, refreshed at the top of every frame. */
    protected double mouseX;
    protected double mouseY;

    private boolean blurLoaded;
    private double lastLayoutWidth = -1.0D;

    protected AetherGuiScreen(AetherClient client) {
        this(client, null);
    }

    protected AetherGuiScreen(AetherClient client, GuiScreen parent) {
        this.client = client;
        this.parent = parent;
    }

    /* ── screen contract ────────────────────────────────────────────────── */

    /** The section this screen shows; drives the highlighted navigation tab. */
    protected abstract GuiSection section();

    /** Title shown next to the branding; null for no title. */
    protected abstract String title();

    /**
     * The search field shown in the header, or null. The shell renders it and consumes
     * clicks on it; the screen wires its change callback to its own filtering.
     */
    protected abstract TextField searchField();

    /**
     * (Re)builds the screen's content geometry. Called on open, on window resize and
     * whenever the design canvas width changes (aspect ratio change). Coordinates are
     * design units of the content area.
     */
    protected abstract void layout(double contentX, double contentY, double contentW, double contentH);

    /** Draws the content inside the panel. Mouse coordinates are design units. */
    protected abstract void renderContent(double mx, double my);

    /**
     * A click the shell did not consume.
     *
     * @return true when the content consumed the click
     */
    protected abstract boolean clickContent(double mx, double my, int button);

    /** A mouse button was released; delivered whether or not the press hit anything. */
    protected abstract void releaseContent(double mx, double my, int button);

    /**
     * A key the search field did not consume. Returning true consumes the key.
     * ESC never reaches here; the base screen handles it.
     */
    protected abstract boolean keyContent(char typedChar, int keyCode);

    /**
     * The wheel turned over the content.
     *
     * @return true when the content consumed the wheel delta
     */
    protected abstract boolean wheelContent(double mx, double my, int delta);

    /** Hint line for the footer's left side; null or empty for none. */
    protected String footerHint() {
        return "Right Shift: close";
    }

    /** Releases content resources before the screen object is dropped. */
    protected void disposeContent() {
    }

    /* ── lifecycle ──────────────────────────────────────────────────────── */

    @Override
    public void initGui() {
        relayout();
    }

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

    /** Rebuilds chrome geometry and asks the screen to lay its content out again. */
    protected void relayout() {
        relayout(true);
    }

    private void relayout(boolean force) {
        GuiScale.update(width, height);
        shell.layout();
        double designW = GuiScale.designWidth();
        if (force || designW != lastLayoutWidth) {
            lastLayoutWidth = designW;
            layout(shell.contentX, shell.contentY, shell.contentW, shell.contentH);
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
        if (Math.round(GuiScale.designWidth()) != Math.round(lastLayoutWidth)) {
            relayout();
        }

        shell.render(section(), title(), searchField(), mouseX, mouseY);
        renderContent(mouseX, mouseY);
        shell.drawHint(footerHint());
        Mc189Compat.color(1.0F, 1.0F, 1.0F, 1.0F);
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
            // A missing or unsupported shader must never take the GUI down with it.
        }
    }

    /* ── input ──────────────────────────────────────────────────────────── */

    @Override
    protected void mouseClicked(int rawX, int rawY, int button) throws IOException {
        double mx = GuiScale.mouseX(rawX);
        double my = GuiScale.mouseY(rawY);
        TextField search = searchField();
        if (shell.click(mx, my, button, section(), search, new ScreenShell.Navigator() {
            public void navigateTo(GuiSection target) {
                switchSection(target);
            }
        })) {
            return;
        }
        if (search != null) {
            search.clickOutside();
        }
        clickContent(mx, my, button);
    }

    @Override
    protected void mouseReleased(int rawX, int rawY, int button) {
        releaseContent(GuiScale.mouseX(rawX), GuiScale.mouseY(rawY), button);
    }

    @Override
    protected void mouseClickMove(int rawX, int rawY, int button, long timeSinceLastClick) {
        // Drags are driven by onMouseMove inside the components; nothing to do here
        // because the base screen already forwards motion every frame in drawScreen.
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
            if (handleEscape()) {
                return;
            }
            Mc189Compat.displayGuiScreen(parent);
            return;
        }
        TextField search = searchField();
        if (search != null && search.isFocused()) {
            if (search.onKeyTyped(typedChar, keyCode)) {
                return;
            }
        }
        if (keyCode == 67 && search != null && !search.isFocused()) { // F9: glyph diagnostic
            Mc189Compat.displayGuiScreen(new AetherFontDiagScreen(client, this));
            return;
        }
        if (keyContent(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    /**
     * ESC handling before the screen closes: defocus the search field first, then let
     * the content close an open panel.
     *
     * @return true when ESC was consumed
     */
    protected boolean handleEscape() {
        TextField search = searchField();
        if (search != null && search.isFocused()) {
            search.setFocused(false);
            return true;
        }
        return keyContent('\0', 1);
    }

    /** Switches to another section of the GUI. */
    protected void switchSection(GuiSection target) {
        if (target == section()) {
            return;
        }
        client.preferences().setOpenSection(target.label());
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

    protected int settingNumber(String moduleId, String settingId, int fallback) {
        try {
            for (Setting<?> setting : client.modules().get(moduleId).settings()) {
                if (settingId.equals(setting.id()) && setting.value() instanceof Number) {
                    return ((Number) setting.value()).intValue();
                }
            }
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
        return fallback;
    }

    /** Persists through the one save path; a locked file never breaks the GUI. */
    protected void saveQuietly() {
        try {
            client.save();
        } catch (IOException ignored) {
            // The shutdown hook and the next change both retry the write.
        }
    }

    /** The saved preference for which section opens first. */
    protected static GuiSection preferredSection(AetherClient client) {
        return GuiSection.fromLabel(client.preferences().openSection(), GuiSection.MODULES);
    }

    /** Small helper for screens that draw a section heading inside their content. */
    protected void drawSectionHeading(String text, double x, double y) {
        AetherFont.draw(AetherFont.Size.SECTION, text, GuiScale.x(x), GuiScale.y(y), AetherUi.TEXT_PRIMARY);
    }

    /** Readable accessor for the tooltip preference, used by rows with descriptions. */
    protected boolean tooltipsShown() {
        return client.preferences().showTooltips();
    }

    /** Readable accessor mirroring {@link ClientPreferences#saveOnClose()}. */
    protected boolean saveOnClose() {
        return client.preferences().saveOnClose();
    }
}
