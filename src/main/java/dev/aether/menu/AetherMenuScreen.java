package dev.aether.menu;

import dev.aether.AetherClient;
import dev.aether.animation.Anim;
import dev.aether.animation.Easing;
import dev.aether.animation.FrameClock;
import dev.aether.gui.AetherFont;
import dev.aether.gui.core.AetherUiScreen;
import dev.aether.menu.impl.AppearanceCategory;
import dev.aether.menu.impl.CosmeticsCategory;
import dev.aether.menu.impl.HomeCategory;
import dev.aether.menu.impl.ModsCategory;
import dev.aether.menu.impl.ProfilesCategory;
import dev.aether.menu.impl.SettingsCategory;
import dev.aether.theme.ThemePalette;
import dev.aether.ui.GuiSection;
import dev.aether.ui.UiCanvas;
import dev.aether.ui.UiIcon;
import dev.aether.ui.UiMotion;
import dev.aether.ui.UiScale;
import dev.aether.ui.UiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

import java.io.IOException;

/**
 * The Aether menu: one window, one rail, routed categories - the structure and
 * geometry of the reference grammar rebuilt over Aether's own backend.
 * <p>
 * The window is a centered 450x280 sheet (GUI-scale pixels, radius 12, soft shadow)
 * floating over a dimmed game; the 32px icon rail selects among Home, Mods,
 * Cosmetics, Appearance, Profiles and Settings, with an accent pill that glides
 * between slots, and an Edit HUD button at the rail bottom that opens the HUD
 * editor. Category content is clipped to the area right of the rail; a detail view
 * (a module's settings, a new profile) slides in horizontally and Esc or a right
 * click steps back out. Clicking outside the window closes the menu.
 * <p>
 * The screen is pure presentation over the client's managers and implements the
 * {@link AetherUiScreen} marker so {@code ModuleInputRouter} suppresses module
 * keybinds while it is open.
 */
public final class AetherMenuScreen extends GuiScreen implements AetherUiScreen {

    public static final float WINDOW_W = 450.0F;
    public static final float WINDOW_H = 280.0F;
    public static final float RADIUS = 12.0F;
    public static final float RAIL_W = 32.0F;
    public static final float HEADER_H = 31.0F;
    public static final float NAV_ITEM = 21.0F;
    public static final float NAV_PITCH = 30.0F;

    private static final int SHADOW_STRENGTH = 7;

    private final AetherClient client;
    private final GuiScreen parent;

    private final Anim intro = new Anim(0.0F, 320.0F, Easing.EASE_OUT_BACK);
    private final UiMotion railGlide = new UiMotion(0.0F, 18.0F);
    private final Anim backdrop = new Anim(0.0F, 220.0F, Easing.EASE_OUT_QUAD);

    private MenuCategory[] categories;
    private int activeIndex;

    /** True while this screen instance has been laid out once. */
    private boolean opened;

    public AetherMenuScreen(AetherClient client) {
        this(client, null, GuiSection.MODULES);
    }

    public AetherMenuScreen(AetherClient client, GuiSection section) {
        this(client, null, section);
    }

    public AetherMenuScreen(AetherClient client, GuiScreen parent, GuiSection section) {
        this.client = client;
        this.parent = parent;
        this.activeIndex = indexOf(section);
    }

    private static int indexOf(GuiSection section) {
        // The rail routes every section except HUD, which is a fullscreen editor
        // behind its own button; the category array skips it accordingly.
        if (section == GuiSection.HUD) {
            return indexOf(GuiSection.HOME);
        }
        int index = section.ordinal();
        return index > GuiSection.HUD.ordinal() ? index - 1 : index;
    }

    /** The section the menu is currently showing (debug and tests). */
    public GuiSection visibleSection() {
        GuiSection[] ordered = GuiSection.ordered();
        int index = activeIndex;
        return index >= GuiSection.HUD.ordinal() ? ordered[index + 1] : ordered[index];
    }

    /** Opens a specific module category in the Mods list (one-shot entry points). */
    public void focusCategory(dev.aether.module.ClientModule.ModuleCategory category) {
        showCategory(indexOf(GuiSection.MODULES));
        ModsCategory mods = category(ModsCategory.class);
        if (mods != null) {
            mods.focusCategory(category);
        }
    }

    /* ── geometry ───────────────────────────────────────────────────────── */

    public float windowX() {
        return (UiScale.menuWidth() - WINDOW_W) / 2.0F;
    }

    public float windowY() {
        return (UiScale.menuHeight() - WINDOW_H) / 2.0F;
    }

    public float windowW() {
        return WINDOW_W;
    }

    public float windowH() {
        return WINDOW_H;
    }

    /** Last cursor position in GUI pixels, for categories that need it outside clicks. */
    public float mouseX() {
        return lastMouseX;
    }

    public float mouseY() {
        return lastMouseY;
    }

    /** True while the left button is held, for slider drags. */
    public boolean mouseDown() {
        return org.lwjgl.input.Mouse.isButtonDown(0);
    }

    private float lastMouseX;
    private float lastMouseY;

    /* ── lifecycle ──────────────────────────────────────────────────────── */

    @Override
    public void initGui() {
        if (categories == null) {
            categories = new MenuCategory[] {
                new HomeCategory(this, client),
                new ModsCategory(this, client),
                new CosmeticsCategory(this, client),
                new AppearanceCategory(this, client),
                new ProfilesCategory(this, client),
                new SettingsCategory(this, client),
            };
        }
        if (!opened) {
            opened = true;
            intro.set(0.0F);
            intro.target(1.0F);
            backdrop.set(0.0F);
            backdrop.target(1.0F);
            showCategory(activeIndex);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void showCategory(int index) {
        this.activeIndex = Math.max(0, Math.min(categories.length - 1, index));
        railGlide.target(activeIndex);
        categories[activeIndex].onShow();
    }

    public MenuCategory active() {
        return categories[activeIndex];
    }

    /* ── headless test / visual-debug hooks ─────────────────────────────── */

    /** Navigates to a section without going through the rail. */
    public void debugNavigate(GuiSection section) {
        showCategory(indexOf(section));
    }

    /** @return the category instance of the given class, or null. */
    @SuppressWarnings("unchecked")
    public <T extends MenuCategory> T category(Class<T> type) {
        for (MenuCategory category : categories) {
            if (type.isInstance(category)) {
                return (T) category;
            }
        }
        return null;
    }

    /** Opens a category by section (used by the factory and one-shot modules). */
    public void navigateTo(GuiSection section) {
        showCategory(indexOf(section));
    }

    @Override
    public void onGuiClosed() {
        for (MenuCategory category : categories) {
            category.dispose();
        }
    }

    /* ── rendering ──────────────────────────────────────────────────────── */

    @Override
    public void drawScreen(int rawMouseX, int rawMouseY, float partialTicks) {
        // Mouse arrives in Minecraft's GUI-scale space; the menu works in menu units.
        float mx = UiScale.menuFromGui(rawMouseX);
        float my = UiScale.menuFromGui(rawMouseY);
        this.lastMouseX = mx;
        this.lastMouseY = my;
        UiTheme.apply(client.theme().palette());
        // FrameClock is published once per client tick by the Forge bridge; the menu
        // only consumes the delta, so its animations advance in step with the rest.
        intro.update();
        backdrop.update();
        railGlide.update();
        active().update();

        drawBackdrop();
        UiCanvas.begin();
        try {
            drawWindow(mx, my);
        } finally {
            UiCanvas.end();
        }
    }

    /* (update calls live in drawScreen; see above) */

    private void drawBackdrop() {
        // An opaque near-black base: with a screen open Minecraft never clears the
        // framebuffer, so the previous screen's frame would bleed through a plain dim.
        // In-world this reads as the heavy-darkened background the grammar allows.
        UiCanvas.begin();
        try {
            int alpha = 255; // opaque: no frame bleed, heavy-dark backdrop per the grammar
            UiCanvas.roundRect(0, 0, UiScale.menuWidth(), UiScale.menuHeight(), 0.0F,
                UiTheme.withAlpha(0x05060A, alpha));
        } finally {
            UiCanvas.end();
        }
    }

    private void drawWindow(float mx, float my) {
        float t = intro.value();
        // EaseBackIn overshoots past 1; scale the window from slightly small.
        float scale = 0.90F + 0.10F * t;
        float alpha = Math.min(1.0F, t * 1.6F);
        float w = WINDOW_W;
        float h = WINDOW_H;
        float cx = windowX() + w / 2.0F;
        float cy = windowY() + h / 2.0F;
        float x = cx - w * scale / 2.0F;
        float y = cy - h * scale / 2.0F;

        int sheet = UiTheme.sheet();
        UiCanvas.softShadow(x, y, w, h, RADIUS, UiTheme.withAlpha(0x000000, Math.round(150 * alpha)), SHADOW_STRENGTH);
        UiCanvas.roundRect(x, y, w, h, RADIUS, sheet);

        // Content and header are drawn in window space; clip to the window while the
        // intro scales it, so nothing draws outside the sheet.
        UiCanvas.scissor(x, y, w, h);
        try {
            MenuCategory category = active();

            // rail background: card colour, rounded only on the outer corners
            UiCanvas.roundRectVarying(x, y, RAIL_W, h, RADIUS, 0.0F, 0.0F, RADIUS,
                UiTheme.card());

            // rail nav items
            GuiSection[] sections = GuiSection.ordered();
            float pillY = windowY() + 38.5F + railGlide.value() * NAV_PITCH;
            float pillSize = NAV_ITEM;
            // The accent pill only reads well after the intro settles in.
            if (t > 0.5F) {
                float pillAlpha = Math.min(1.0F, (t - 0.5F) * 2.0F);
                UiCanvas.gradientRoundRect(windowX() + 5.5F, pillY, pillSize, pillSize, 5.0F,
                    UiTheme.withAlpha(UiTheme.accent(), Math.round(230 * pillAlpha)),
                    UiTheme.withAlpha(UiTheme.accentDeep(), Math.round(230 * pillAlpha)));
            }
            for (int i = 0; i < sections.length; i++) {
                float ix = windowX() + 5.5F;
                float iy = windowY() + 38.5F + i * NAV_PITCH;
                boolean activeCategory = i == activeIndex;
                boolean hot = !activeCategory && mx >= ix && mx < ix + NAV_ITEM && my >= iy && my < iy + NAV_ITEM;
                int glyphColor = activeCategory ? UiTheme.readableOn(UiTheme.accent())
                    : hot ? UiTheme.text() : UiTheme.textSoft();
                char glyph = railGlyph(sections[i]);
                float glyphSize = 13.0F;
                AetherFont.drawIcon(glyph, glyphSize, ix + (NAV_ITEM - AetherFont.iconWidth(glyph, glyphSize)) / 2.0F,
                    iy + (NAV_ITEM - AetherFont.height(glyphSize)) / 2.0F, glyphColor);
            }

            // logo chip
            float logoX = windowX() + 5.0F;
            float logoY = windowY() + 7.0F;
            UiCanvas.gradientRoundRect(logoX, logoY, 22.0F, 22.0F, 11.0F,
                UiTheme.accent(), UiTheme.accentDeep());
            mc.getTextureManager().bindTexture(new ResourceLocation("aether", "aetherlogo.png"));
            float logoAlpha = Math.min(1.0F, alpha);
            GL11Texture.enable();
            net.minecraft.client.renderer.GlStateManager.enableBlend();
            net.minecraft.client.renderer.GlStateManager.color(1, 1, 1, logoAlpha);
            net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(
                Math.round(logoX + 4.5F), Math.round(logoY + 4.5F), 0.0F, 0.0F, 13, 13, 13, 13);
            GL11Texture.disable();
            net.minecraft.client.renderer.GlStateManager.color(1, 1, 1, 1);

            // Edit HUD button at the rail bottom
            float hudX = windowX() + 5.5F;
            float hudY = windowY() + h - 30.0F;
            boolean hudHot = mx >= hudX && mx < hudX + NAV_ITEM && my >= hudY && my < hudY + NAV_ITEM;
            UiCanvas.gradientRoundRect(hudX, hudY, NAV_ITEM, NAV_ITEM, 6.0F,
                UiTheme.withAlpha(UiTheme.accent(), hudHot ? 255 : 225),
                UiTheme.withAlpha(UiTheme.accentDeep(), hudHot ? 255 : 225));
            char layoutGlyph = UiIcon.EDIT_HUD;
            AetherFont.drawIcon(layoutGlyph, 13.0F,
                hudX + (NAV_ITEM - AetherFont.iconWidth(layoutGlyph, 13.0F)) / 2.0F,
                hudY + (NAV_ITEM - AetherFont.height(13.0F)) / 2.0F,
                UiTheme.readableOn(UiTheme.accent()));

            // header title
            float titleAlpha = Math.min(1.0F, Math.max(0.0F, (t - 0.2F) * 1.8F));
            AetherFont.draw(15.0F, dev.aether.forge189.font.AetherFontManager.Face.SEMIBOLD, category.title(),
                windowX() + RAIL_W + 13.0F, windowY() + 10.0F + (1.0F - titleAlpha) * 4.0F,
                UiTheme.withAlpha(UiTheme.text(), Math.round(255 * titleAlpha)));

            // header extras (search / folder button)
            category.drawHeaderExtras(mx, my);

            // content, clipped, with the category rise transition
            float rise = (1.0F - category.transition) * 50.0F;
            UiCanvas.scissor(windowX() + RAIL_W, windowY() + HEADER_H, w - RAIL_W, h - HEADER_H);
            org.lwjgl.opengl.GL11.glPushMatrix();
            org.lwjgl.opengl.GL11.glTranslatef(0.0F, rise, 0.0F);
            try {
                category.draw(mx, my - rise);
            } finally {
                org.lwjgl.opengl.GL11.glPopMatrix();
                UiCanvas.clearScissor();
            }
        } finally {
            UiCanvas.clearScissor();
        }
    }

    private char railGlyph(GuiSection section) {
        switch (section) {
            case MODULES: return UiIcon.MODULES;
            case COSMETICS: return UiIcon.COSMETICS;
            case APPEARANCE: return UiIcon.APPEARANCE;
            case PROFILES: return UiIcon.PROFILES;
            case SETTINGS: return UiIcon.SETTINGS;
            case HUD:
            default: return UiIcon.HOME;
        }
    }

    /* ── input ──────────────────────────────────────────────────────────── */

    @Override
    protected void mouseClicked(int rawX, int rawY, int button) throws IOException {
        // Same conversion as the draw path: events arrive in GUI-scale space, the
        // menu works in menu units - without it every hit-test reads the wrong spot.
        double mx = UiScale.menuFromGui(rawX);
        double my = UiScale.menuFromGui(rawY);

        // Click outside the window closes, with a small tolerance like the reference.
        if (mx < windowX() - 5.0F || mx > windowX() + WINDOW_W + 5.0F
            || my < windowY() - 5.0F || my > windowY() + WINDOW_H + 5.0F) {
            close();
            return;
        }

        // Rail navigation
        GuiSection[] sections = GuiSection.ordered();
        for (int i = 0; i < sections.length; i++) {
            float ix = windowX() + 5.5F;
            float iy = windowY() + 38.5F + i * NAV_PITCH;
            if (mx >= ix && mx < ix + NAV_ITEM && my >= iy && my < iy + NAV_ITEM) {
                if (i != activeIndex) {
                    showCategory(i);
                }
                return;
            }
        }

        // Edit HUD button
        float hudX = windowX() + 5.5F;
        float hudY = windowY() + WINDOW_H - 30.0F;
        if (mx >= hudX && mx < hudX + NAV_ITEM && my >= hudY && my < hudY + NAV_ITEM) {
            saveQuietly();
            dev.aether.gui.AetherGui.openHudEditor(client);
            return;
        }

        // Header extras (search field and friends)
        if (active().clickHeaderExtras(mx, my, button)) {
            return;
        }

        // Content
        if (active().insideContent(mx, my)) {
            active().click(mx, my, button);
        }
    }

    @Override
    protected void mouseReleased(int rawX, int rawY, int button) {
        active().release(UiScale.menuFromGui(rawX), UiScale.menuFromGui(rawY), button);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = McWheel.delta();
        if (wheel != 0) {
            int delta = -wheel / 24;
            if (delta == 0) {
                delta = wheel > 0 ? 1 : -1;
            }
            active().onWheel(lastMouseX, lastMouseY, delta);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { // ESC: step back a detail view first, then close
            if (active().stepBack()) {
                return;
            }
            close();
            return;
        }
        if (active().keyHeaderExtras(typedChar, keyCode)) {
            return;
        }
        if (active().key(typedChar, keyCode)) {
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private void close() {
        saveQuietly();
        Mc189CompatFacade.display(parent);
    }

    private void saveQuietly() {
        try {
            client.save();
        } catch (Exception ignored) {
        }
    }

    /* ── shims so this file does not drag static imports around ─────────── */

    /** Texture mode inside the vector batch (the batch itself runs untextured). */
    private static final class GL11Texture {
        static void enable() {
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
        }

        static void disable() {
            org.lwjgl.opengl.GL11.glDisable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
        }
    }

    private static final class McWheel {
        static int delta() {
            return org.lwjgl.input.Mouse.getEventDWheel();
        }
    }

    private static final class Mc189CompatFacade {
        static void display(net.minecraft.client.gui.GuiScreen screen) {
            dev.aether.forge189.Mc189Compat.displayGuiScreen(screen);
        }
    }

    private static final class AetherFontManagerFace {
        static dev.aether.forge189.font.AetherFontManager.Face SEMIBOLD =
            dev.aether.forge189.font.AetherFontManager.Face.SEMIBOLD;
    }
}
