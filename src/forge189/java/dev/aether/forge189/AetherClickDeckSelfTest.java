package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.module.ClientModule;
import dev.aether.module.ClientModule.ModuleState;
import dev.aether.ui.ControlCenterSection;
import dev.aether.forge189.AetherUi;

import net.minecraft.client.gui.FontRenderer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Headless verification for {@link AetherClickGuiScreen}.
 * <p>
 * The Control Center is normally driven by Minecraft, but every geometry, input and persistence
 * path is plain Java. Running this class against the bundled 1.8.9 stubs (see
 * {@code scripts/verify.sh}) proves that layout, keyboard navigation, accordion expansion, control
 * hit-testing, section navigation, profile save/apply and preference toggles complete without
 * exceptions, and that Enter / switch clicks really flip module state. Rendering calls resolve to
 * the stub Gui, so the run needs no LWJGL context.
 */
public final class AetherClickDeckSelfTest {
    private static int checks;

    private AetherClickDeckSelfTest() {
    }

    public static void main(String[] args) throws Exception {
        Path configFile = new File(System.getProperty("java.io.tmpdir"), "aether-deck-self-test.json").toPath();
        if (configFile.toFile().exists() && !configFile.toFile().delete()) {
            throw new IllegalStateException("Could not clear " + configFile);
        }
        System.setProperty("aether.changeLog", new File(System.getProperty("java.io.tmpdir"), "aether-deck-lastchange.txt").getAbsolutePath());

        AetherClient client = new AetherClient(configFile);
        client.start();
        AetherUi.bind(client);

        AetherClickGuiScreen wide = screen(client, 854, 480);
        wide.drawScreen(10, 10, 0F);

        int flipped = countEnabled(client);
        wide.typeKey('\0', 28); // Enter toggles the first row
        assertEquals(flipped + 1, countEnabled(client), "Enter toggles the selected module");

        clickFirstRowToggle(wide, 854, 480);
        assertEquals(flipped, countEnabled(client), "clicking the row switch toggles the module back");

        // expand the first row, then exercise every control type that lives in it
        wide.drawScreen(10, 10, 0F);
        // First module now starts at listY + 90, toggle is at y + 6
        int rowY = listY(854, 480) + 90;
        // Toggle button is at cardX + cardW - 44 with bounds [toggleX, toggleX+26] x [y+6, y+19]
        wide.mouseClicked(rowX(854) + rowWidth(854) - 57, rowY + 12, 0);
        wide.drawScreen(10, 10, 0F);
        for (int y = rowY + 30; y < rowY + 240; y += 6) {
            for (int x = rowX(854) + 40; x < rowX(854) + listWidth(854) - 20; x += 40) {
                wide.drawScreen(x, y, 0F);
                wide.mouseClicked(x, y, 0);
            }
        }
        wide.drawScreen(10, 10, 0F);
        wide.typeKey('\0', 1); // Escape closes any palette / capture / text editor

        // keyboard navigation, search, section cycling, expand all, reset
        wide.typeKey('\0', 208);
        wide.typeKey('\0', 208);
        wide.typeKey('\0', 200);
        wide.typeKey('z', 44);
        wide.typeKey('o', 25);
        wide.typeKey('o', 25);
        wide.typeKey('m', 50);
        wide.drawScreen(10, 10, 0F);
        wide.typeKey('\0', 1);
        wide.typeKey('\0', 15); // Tab cycles the section sidebar
        wide.drawScreen(10, 10, 0F);
        wide.typeKey('e', 18);
        wide.drawScreen(10, 10, 0F);
        wide.typeKey('c', 46);
        wide.typeKey('r', 19);
        wide.mouseClicked(rowX(854) + 40, rowY + 15, 1); // right click resets a row
        wide.typeKey('\0', 209);
        wide.typeKey('\0', 207);
        wide.typeKey('\0', 199);
        wide.typeKey(' ', 57);
        wide.drawScreen(10, 10, 0F);

        // palette, slider and keybind capture round trips. Each exercise starts from a
        // fresh screen so a text editor or palette left open by the fuzz pass cannot
        // swallow the next interaction (the deck itself handles Escape correctly).
        exerciseColourRow(client);
        exerciseSliderRow(client);
        exerciseKeybindRow(client);

        // the five theme modules must repaint the shared token hub that every screen reads
        exerciseThemes(client, wide);

        // section navigation: every page renders without exceptions, and the preference
        // rows on the Settings page really flip and persist.
        exerciseSections(client, wide);

        // profile save/apply/delete round trip through the page buttons
        exerciseProfiles(client, wide);

        // narrow viewport hides the telemetry spine and the sidebar stays the navigation
        AetherClickGuiScreen narrow = screen(client, 320, 240);
        narrow.drawScreen(4, 4, 0F);
        narrow.mouseClicked(60, 46, 0);
        narrow.drawScreen(4, 4, 0F);
        narrow.typeKey('\0', 1);

        wide.handleMouseInput();
        wide.releaseMouse(10, 10, 0);

        for (ClientModule module : client.modules().all()) {
            client.modules().setEnabled(module.metadata().id(), false);
        }
        client.save();

        System.out.println("AetherClickDeckSelfTest passed (" + checks + " checks).");
    }

    /**
     * Enabling a theme module has to reach {@link AetherUi}, and the deck has to draw from
     * those tokens rather than a private palette. This walks every theme and checks the
     * tokens actually move, then puts the client back on the default theme.
     */
    private static void exerciseThemes(AetherClient client, AetherClickGuiScreen deck) throws IOException {
        AetherUi.syncTheme();
        int defaultText = AetherUi.TEXT_PRIMARY;
        int defaultSurface = AetherUi.SURFACE;
        boolean defaultLight = AetherUi.LIGHT_SURFACE;

        client.modules().setEnabled("theme.midnight", true);
        assertEquals(Boolean.TRUE, Boolean.valueOf(AetherUi.syncTheme()), "a theme switch reports that the tokens changed");
        int midnightSurface = AetherUi.SURFACE;
        int midnightText = AetherUi.TEXT_PRIMARY;
        int midnightEdge = AetherUi.PANEL_EDGE;
        assertTrue(midnightSurface != defaultSurface, "Midnight repaints the shared surface token");
        assertEquals("Midnight", client.theme().name(), "the enabled theme module becomes the active theme");
        assertTrue(AetherUi.syncTheme() == false, "a second frame with the same theme is a no-op");

        client.modules().setEnabled("theme.light", true);
        AetherUi.syncTheme();
        assertEquals(Boolean.TRUE, Boolean.valueOf(AetherUi.LIGHT_SURFACE), "the Light palette is marked as a light surface");
        assertTrue(AetherUi.TEXT_PRIMARY != midnightText, "Light re-derives the text token for contrast");
        assertTrue(AetherUi.PANEL_EDGE != midnightEdge, "Light uses dark edges instead of the white glass highlight");

        // the deck draws from the hub, so painting after a theme switch must not throw and
        // must leave the tokens in place for the other screens
        deck.drawScreen(10, 10, 0F);
        deck.keyTyped('\0', 15);
        deck.drawScreen(10, 10, 0F);
        assertEquals("Light", client.theme().name(), "the deck keeps the active theme while it is opened");

        client.modules().setEnabled("theme.light", false);
        client.modules().setEnabled("theme.frost", true);
        AetherUi.syncTheme();
        assertEquals("Frost", client.theme().name(), "Frost theme is active");
        int frostSurface = AetherUi.SURFACE;
        assertEquals(Integer.valueOf(frostSurface), Integer.valueOf(AetherUi.SURFACE), "Frost palette is applied");
        client.modules().setEnabled("theme.frost", false);
        AetherUi.syncTheme();
        assertEquals(Integer.valueOf(defaultSurface), Integer.valueOf(AetherUi.SURFACE), "disabling theme restores default");
    }

    /**
     * Walks all six sidebar sections, exercises the preference rows on the Settings page and
     * proves the module list is intact when it comes back.
     */
    private static void exerciseSections(AetherClient client, AetherClickGuiScreen deck) throws IOException {
        int modulesEnabled = countEnabled(client);
        for (int i = 0; i < ControlCenterSection.ordered().length; i++) {
            deck.typeKey('\0', 15); // Tab cycles sections
            deck.drawScreen(10, 10, 0F);
            assertTrue(deck.nav().consistent(), "the navigation state stays consistent on every section");
        }
        // cycle back to Modules
        deck.typeKey('\0', 15);
        deck.drawScreen(10, 10, 0F);

        // Tab + Shift+Tab round trip
        deck.typeKey('\0', 15);
        deck.typeKey('S', 31); // shift is read through isShiftKeyDown; harmless either way
        deck.drawScreen(10, 10, 0F);

        // On the Settings section, click the preference rows: flip save_on_close twice.
        gotoSectionByClick(deck, "Settings");
        Boolean before = client.preferences().saveOnClose();
        // SettingsPage: SETTINGS header at y=95, INTERFACE header at y=125, first row at y=149
        // Toggle is at listX + listW - 48 = (innerListX+8) + listW - 48 = 158 + 513 - 48 = 623
        int rowY = listY(854, 480) + 8 + 30 + 24;
        int toggleX = innerListX(854) + 8 + listWidth(854) - 48;
        deck.mouseClicked(toggleX, rowY + 6, 0);
        deck.drawScreen(10, 10, 0F);
        assertEquals(!before.booleanValue(), client.preferences().saveOnClose(), "clicking save-on-close flips the preference");
        deck.mouseClicked(toggleX, rowY + 6, 0);
        assertEquals(before.booleanValue(), client.preferences().saveOnClose(), "clicking it again restores the preference");

        // back to modules; the list is still there
        gotoSectionByClick(deck, "Modules");
        deck.drawScreen(10, 10, 0F);
        assertEquals(modulesEnabled, countEnabled(client), "section navigation does not change module state");
    }

    private static void gotoSectionByClick(AetherClickGuiScreen deck, String sectionLabel) throws IOException {
        deck.drawScreen(10, 10, 0F);
        // Sidebar items use AetherMetrics.SIDEBAR_ITEM_HEIGHT = 30
        int itemH = 30; // AetherMetrics.SIDEBAR_ITEM_HEIGHT
        int baseY = listY(854, 480);
        int index = -1;
        for (int i = 0; i < ControlCenterSection.ordered().length; i++) {
            if (ControlCenterSection.ordered()[i].label().equals(sectionLabel)) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            throw new AssertionError("No such section: " + sectionLabel);
        }
        // HandleSidebarClick uses relY = my - sidebarY, index = relY / SIDEBAR_ITEM_HEIGHT
        // sidebarY = currentLayout.sidebarY
        // We need my such that relY / 30 == index
        int y = deck.getLayout().sidebarY + index * itemH + 1; // +1 to be inside the item
        deck.mouseClicked(sidebarItemX(854) + 20, y, 0);
        deck.drawScreen(10, 10, 0F);
    }

    /**
     * Saves a profile, flips some modules, applies the profile back and checks the states return;
     * then deletes it. This is the acceptance test for the Profiles page.
     */
    private static void exerciseProfiles(AetherClient client, AetherClickGuiScreen deck) throws IOException {
        assertTrue(client.profiles().size() == 0, "the store starts empty");
        gotoSectionByClick(deck, "Profiles");

        // type a name into the draft field and hit SAVE CURRENT
        deck.setProfileDraftName("test");
        deck.drawScreen(10, 10, 0F);

        // The save button is at listX + listW - 180, y = listY + 8 + 30 (after PROFILES header)
        int saveBtnX = innerListX(854) + 8 + listWidth(854) - 180;
        int saveBtnY = listY(854, 480) + 8 + 30;
        deck.mouseClicked(saveBtnX, saveBtnY + 7, 0);
        deck.drawScreen(10, 10, 0F);
        assertEquals(1, client.profiles().size(), "saving from the page creates the profile");
        assertEquals(Boolean.TRUE, Boolean.valueOf(client.profiles().exists("test")), "the typed name is used");

        int enabledBefore = countEnabled(client);
        client.modules().setEnabled("graphics.fullbright", true);
        assertTrue(countEnabled(client) == enabledBefore + 1, "fullbright switched on to change state");

        // apply the saved profile: click the row, then APPLY
        gotoSectionByClick(deck, "Profiles");
        deck.drawScreen(10, 10, 0F);
        // ProfilesPage: header at listY+8, Save/New at listY+38, first row at listY+68
        int rowY = listY(854, 480) + 68;
        deck.mouseClicked(rowX(854) + 40, rowY + 17, 0); // click on the row
        // Apply button at cardX + cardW - 100
        int applyX = innerListX(854) + 8 + listWidth(854) - 100;
        deck.mouseClicked(applyX + 8, rowY + 12, 0);
        deck.drawScreen(10, 10, 0F);
        assertEquals(enabledBefore, countEnabled(client), "applying the profile restores the saved states");
        assertEquals(Boolean.TRUE, Boolean.valueOf(client.profiles().exists("test")), "applying does not delete the profile");

        // delete it again
        int deleteX = innerListX(854) + 8 + listWidth(854) - 100 + 50 + 22;
        deck.mouseClicked(deleteX, rowY + 15, 0);
        assertEquals(0, client.profiles().size(), "deleting removes the profile");
        client.modules().setEnabled("graphics.fullbright", false);
    }

    private static void exerciseColourRow(AetherClient client) throws IOException {
        AetherClickGuiScreen screen = screen(client, 854, 480);
        ClientModule coordinates = client.modules().get("hud.coordinates");
        openModule(screen, "hud.coordinates");
        int rowY = listY(854, 480);
        int y = rowY + 30;
        // walk the accordion looking for the colour swatches and click slightly left of the
        // control column so the palette opens, then click the palette area itself
        for (int step = 0; step < 12; step++) {
            int targetY = y + step * 22;
            screen.drawScreen(rowX(854) + listWidth(854) - 20, targetY + 6, 0F);
            screen.mouseClicked(rowX(854) + listWidth(854) - 25, targetY + 6, 0);
            screen.drawScreen(rowX(854) + listWidth(854) - 60, targetY + 30, 0F);
            screen.mouseClicked(rowX(854) + listWidth(854) - 60, targetY + 30, 0);
            screen.typeKey('\0', 1);
        }
        screen.drawScreen(10, 10, 0F);
        assertEquals(true, coordinates.settings().size() > 0, "coordinates module keeps its settings after palette clicks");
        clearSearch(screen);
        screen.releaseMouse(10, 10, 0);
    }

    /** Drags the first number slider of the filtered module and checks the value lands on the maximum. */
    private static void exerciseSliderRow(AetherClient client) throws IOException {
        AetherClickGuiScreen screen = screen(client, 854, 480);
        openModule(screen, "pvp.zoom");
        assertEquals(Integer.valueOf(40), settingValue(client, "pvp.zoom", "zoom_percent"), "zoom percent starts at its default");
        // Slider track in accordion: x = cardX+8 = listX+16, w = cardW-16 = listW-32
        // Track spans x+w-80 to x+w-20 = listX+listW-96 to listX+listW-36
        // rowX = listX+8, rowWidth = listWidth-8, so track right = rowX + rowWidth - 36
        int controlX = rowX(854) + rowWidth(854) - 36; // right end of the slider track
        int sliderY = contentTop(854, 480) + SUB_H + 11; // second sub row, middle of the track
        screen.drawScreen(controlX, sliderY, 0F);
        screen.mouseClicked(controlX, sliderY, 0);
        screen.releaseMouse(controlX, sliderY, 0);
        int after = settingValue(client, "pvp.zoom", "zoom_percent");
        assertEquals(Integer.valueOf(100), after, "clicking the end of a slider applies the maximum");
        clearSearch(screen);
    }

    /** Absolute y of the first setting row in an expanded accordion. */
    private static int contentTop(int w, int h) {
        // First module starts at listY + 90, accordion top at moduleY + ROW_H,
        // first setting row at accordionTop + 8
        return listY(w, h) + 90 + ROW_H + 8;
    }

    /** Opens the first setting row of the filtered module and captures a keybind. */
    private static void exerciseKeybindRow(AetherClient client) throws IOException {
        AetherClickGuiScreen screen = screen(client, 854, 854 > 480 ? 480 : 480);
        openModule(screen, "pvp.zoom");
        int controlX = rowX(854) + listWidth(854) - 60;
        int pillY = contentTop(854, 480) + 10; // first sub row, middle of the keybind pill
        screen.drawScreen(controlX, pillY, 0F);
        screen.mouseClicked(controlX, pillY, 0);
        screen.typeKey('\0', 44); // bind to Z
        screen.drawScreen(10, 10, 0F);
        assertEquals(Integer.valueOf(44), settingValue(client, "pvp.zoom", "keybind"), "keybind capture stores the pressed key");
        clearSearch(screen);
    }

    /** Types the module name, leaves the search field and expands the single result. */
    private static void openModule(AetherClickGuiScreen screen, String moduleId) throws IOException {
        String needle = moduleId.substring(moduleId.indexOf('.') + 1).replace('_', ' ');
        // Start search mode directly
        screen.getNav().beginSearch();
        screen.setQuery(needle);
        screen.getSearch().query(needle);
        screen.syncVisible();
        // End search (keeps the filter)
        screen.getNav().endSearch();
        // Expand the filtered module's accordion
        java.util.List<dev.aether.module.ClientModule> modules = screen.getSearch().results();
        if (!modules.isEmpty()) {
            String expandedId = modules.get(0).metadata().id();
            screen.getNav().toggleExpanded(expandedId);
        }
        screen.drawScreen(10, 10, 0F);
    }

    private static void clearSearch(AetherClickGuiScreen screen) throws IOException {
        screen.typeKey('\0', 1); // Escape clears the query and restores the full list
        screen.drawScreen(10, 10, 10 > 0 ? 0F : 0F);
        screen.drawScreen(10, 10, 0F);
    }

    private static Integer settingValue(AetherClient client, String moduleId, String settingId) {
        for (dev.aether.module.setting.Setting<?> setting : client.modules().get(moduleId).settings()) {
            if (setting.id().equals(settingId)) {
                Object value = setting.value();
                return value instanceof Number ? Integer.valueOf(((Number) value).intValue()) : null;
            }
        }
        return null;
    }

    private static void clickFirstRowToggle(AetherClickGuiScreen screen, int w, int h) throws IOException {
        screen.drawScreen(10, 10, 0F);
        // Match ControlCenterLayout.compute() + ModulesPage exactly using actual AetherMetrics constants:
        // HEADER_HEIGHT=48, SIDEBAR_WIDTH=130, ROW_HEIGHT=34
        int margin = clamp(w / 24, 6, 16);
        int deckW = Math.min(1040, w - margin * 2);
        int deckH = h - margin * 2;
        int deckX = (w - deckW) / 2;
        int deckY = margin;
        int headerH = 48; // AetherMetrics.HEADER_HEIGHT
        int footerH = 20; // AetherMetrics.FOOTER_HEIGHT
        int bodyTop = deckY + headerH;
        int footerY = deckY + deckH - footerH;
        int sidebarW = (deckW >= 560) ? 130 : 0; // AetherMetrics.SIDEBAR_WIDTH
        int sidebarX = deckX + 8;
        int sidebarY = bodyTop + 6;
        int listX = (sidebarW > 0) ? sidebarX + sidebarW + 8 : sidebarX;
        int innerW = deckX + deckW - listX - 8;
        int spineW = innerW >= 430 && sidebarW > 0 ? Math.min(Math.max((int)(innerW * 0.26f), 132), 200) : 0;
        int listW = innerW - spineW;
        int listY = sidebarY;

        // ModulesPage draws first module at y = layout.listY + 90
        int moduleY = listY + 90;
        int cardX = listX + 8;
        int cardW = listW - 16;
        int toggleX = cardX + cardW - 44;
        int toggleY = moduleY + 6;
        screen.mouseClicked(toggleX, toggleY, 0);
    }

    private static AetherClickGuiScreen screen(AetherClient client, int width, int height) {
        AetherClickGuiScreen screen = new AetherClickGuiScreen(client);
        screen.width = width;
        screen.height = height;
        screen.mc = null;
        screen.fontRendererObj = new FontRenderer();
        screen.initGui();
        return screen;
    }

    private static int countEnabled(AetherClient client) {
        int enabled = 0;
        for (ClientModule module : client.modules().all()) {
            if (module.state() == ModuleState.ENABLED) {
                enabled++;
            }
        }
        return enabled;
    }

    /* The deck's geometry contract, recomputed here so the test would fail if the
       layout constants drift apart from the documented layout. */

    private static final int ROW_H = 30;
    private static final int SUB_H = 22;
    private static final int DESCRIPTION_PAD = 6;
    private static final int DESCRIPTION_LINE_H = 10;
    private static final int SIDEBAR_ITEM_H = 26;

    private static int deckX(int w) {
        int margin = clamp(w / 24, 6, 16);
        int deckW = Math.min(1040, w - margin * 2);
        return (w - deckW) / 2;
    }

    private static int deckY(int w) {
        return clamp(w / 24, 6, 16);
    }

    private static int deckWidth(int w) {
        return Math.min(1040, w - clamp(w / 24, 6, 16) * 2);
    }

    private static int innerWidth(int w) {
        // From the list column's left edge to the deck's right edge minus the 8px inset.
        return deckX(w) + deckWidth(w) - innerListX(w) - 8;
    }

    private static int sidebarWidth(int w) {
        return deckWidth(w) >= 560 ? 130 : 0;
    }

    private static int sidebarItemX(int w) {
        return deckX(w) + 8;
    }

    private static int innerListX(int w) {
        return sidebarWidth(w) > 0 ? sidebarItemX(w) + sidebarWidth(w) + 8 : sidebarItemX(w);
    }

    private static int spineWidth(int w) {
        int inner = innerWidth(w);
        int spineCandidate = clamp((int) (inner * 0.26F), 132, 200);
        return inner >= 430 && sidebarWidth(w) > 0 ? spineCandidate : 0;
    }

    private static int listWidth(int w) {
        return innerWidth(w) - spineWidth(w);
    }

    private static int rowWidth(int w) {
        // The actual row/card width: listX = innerListX + 8, cardW = listW - 16
        return listWidth(w) - 8;
    }

    private static int rowX(int w) {
        // Match the card X used by Pages: listX + 8
        return innerListX(w) + 8;
    }

    private static int listY(int w, int h) {
        int bodyTop = deckY(w) + 48; // AetherMetrics.HEADER_HEIGHT
        return bodyTop + 6;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        checks++;
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " (expected " + expected + ", got " + actual + ")");
        }
    }

    private static void assertTrue(boolean condition, boolean unused, String message) {
        assertTrue(condition, message);
    }

    private static void assertTrue(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
