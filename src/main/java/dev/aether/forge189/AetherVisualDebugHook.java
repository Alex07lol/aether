package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.module.ClientModule;
import dev.aether.gui.AetherGui;
import dev.aether.gui.screens.AetherAppearanceScreen;
import dev.aether.gui.screens.AetherCosmeticScreen;
import dev.aether.gui.screens.AetherHudEditorScreen;
import dev.aether.gui.screens.AetherModScreen;
import dev.aether.gui.screens.AetherClientSettingsScreen;
import dev.aether.gui.screens.AetherProfilesScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.Display;

import java.io.File;

/**
 * TEMPORARY dev-only visual debugger, active only when the JVM property
 * {@code aether.debugShots} is set (runClient passes it with
 * {@code -PaetherDebugShots=<value>}).
 * <p>
 * The value is either {@code true} (window keeps its current size) or a
 * {@code WIDTHxHEIGHT} pair such as {@code 1920x1080}: the hook then resizes the
 * window on the first tick, which is how the GUI is verified at several resolutions.
 * It drives the client through every screen and every interaction that changes a
 * screen's state, and saves real screenshots into {@code run/screenshots}:
 * <ol>
 *   <li>{@code debug-aether-mainmenu};</li>
 *   <li>{@code debug-aether-modules} - the Modules screen;</li>
 *   <li>{@code debug-aether-modules-search} - after typing into the search field;</li>
 *   <li>{@code debug-aether-modules-category} - after switching the category chip;</li>
 *   <li>{@code debug-aether-modules-scrolled} - after scrolling the list;</li>
 *   <li>{@code debug-aether-modules-toggled} - after toggling the first module;</li>
 *   <li>{@code debug-aether-module-settings} - one module's configuration panel;</li>
 *   <li>{@code debug-aether-cosmetics} - the gallery: filters, search, cards, preview;</li>
 *   <li>{@code debug-aether-cosmetics-category} - after switching the chip filter;</li>
 *   <li>{@code debug-aether-cosmetics-search} - after typing a query;</li>
 *   <li>{@code debug-aether-cosmetics-scrolled} - the gallery after one scroll;</li>
 *   <li>{@code debug-aether-cosmetics-selected} - after selecting a cosmetic;</li>
 *   <li>{@code debug-aether-cosmetics-import} - the import popover open;</li>
 *   <li>{@code debug-aether-appearance} - the Appearance screen, default palette;</li>
 *   <li>{@code debug-aether-appearance-equipped} - after wearing a theme;</li>
 *   <li>{@code debug-aether-appearance-scrolled} - the same screen on its second page;</li>
 *   <li>{@code debug-aether-hud} - the HUD editor;</li>
 *   <li>{@code debug-aether-settings} - the client settings screen;</li>
 *   <li>{@code debug-aether-profiles} - the profile manager;</li>
 *   <li>{@code debug-aether-preview} - cosmetics with a live player in world.</li>
 * </ol>
 * Each step fires the moment its screen is visible (after a settle delay), and the
 * sequence finishes with a marker file so an outside observer can pick the images up.
 * Remove once the UI renders as designed.
 */
public final class AetherVisualDebugHook {

    private enum Step {
        RESIZE, WAIT_MENU, SHOOT_MENU,
        OPEN_MODULES, SHOOT_MODULES,
        MODULES_SEARCH, SHOOT_MODULES_SEARCH,
        MODULES_CATEGORY, SHOOT_MODULES_CATEGORY,
        SCROLL_MODULES, SHOOT_MODULES_SCROLLED,
        TOGGLE_MODULE, SHOOT_MODULES_TOGGLED,
        OPEN_SETTINGS_PANEL, SHOOT_MODULE_SETTINGS,
        OPEN_COSMETICS, SHOOT_COSMETICS,
        COSMETICS_CATEGORY, SHOOT_COSMETICS_CATEGORY,
        COSMETICS_SEARCH, SHOOT_COSMETICS_SEARCH,
        SCROLL_COSMETICS, SHOOT_COSMETICS_SCROLLED,
        SELECT_COSMETIC, SHOOT_COSMETICS_SELECTED,
        IMPORT_MENU, SHOOT_COSMETICS_IMPORT,
        OPEN_APPEARANCE, SHOOT_APPEARANCE_PAGE,
        SELECT_THEME, SHOOT_APPEARANCE_EQUIPPED,
        SCROLL_APPEARANCE, SHOOT_APPEARANCE_SCROLLED,
        OPEN_HUD, SHOOT_HUD,
        OPEN_CLIENT_SETTINGS, SHOOT_CLIENT_SETTINGS,
        OPEN_PROFILES, SHOOT_PROFILES,
        LOAD_WORLD, WAIT_WORLD, SHOOT_PREVIEW, DONE
    }

    private final AetherClient client;
    private final int targetWidth;
    private final int targetHeight;
    private final java.util.LinkedHashMap<String, Boolean> modulesAtStart =
        new java.util.LinkedHashMap<String, Boolean>();
    private Step step = Step.RESIZE;
    private int settleTicks;
    private int idleTicks;

    public AetherVisualDebugHook(AetherClient client) {
        this.client = client;
        String value = System.getProperty("aether.debugShots", "true");
        int width = 0;
        int height = 0;
        int x = value.toLowerCase().indexOf('x');
        if (x > 0 && x < value.length() - 1) {
            try {
                width = Integer.parseInt(value.substring(0, x).trim());
                height = Integer.parseInt(value.substring(x + 1).trim());
            } catch (NumberFormatException malformed) {
                width = 0;
                height = 0;
            }
        }
        this.targetWidth = width >= 320 ? width : 0;
        this.targetHeight = height >= 240 ? height : 0;
        // The switches the client booted with. The walk toggles a module, and the client saves its
        // configuration on exit, so without this snapshot one run's toggle would leak into the next
        // run's images (and into the player's own configuration). Restored at the start of the walk
        // and again when it ends.
        for (ClientModule module : client.modules().all()) {
            modulesAtStart.put(module.metadata().id(),
                Boolean.valueOf(module.state() == ClientModule.ModuleState.ENABLED));
        }
        System.out.println("[AetherVisualDebug] enabled, resolution target: "
            + (targetWidth > 0 ? targetWidth + "x" + targetHeight : "current window"));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || step == Step.DONE) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();

        switch (step) {
            case RESIZE:
                // First thing in the walk: the default palette and an empty cosmetic selection, so
                // the images show the states the walk itself creates and not what a previous run
                // happened to leave behind.
                resetThemes();
                resetCosmetics();
                restoreModules("walk start");
                if (targetWidth > 0) {
                    try {
                        Display.setResizable(true);
                        if (Display.getWidth() != targetWidth || Display.getHeight() != targetHeight) {
                            Display.setDisplayMode(new org.lwjgl.opengl.DisplayMode(targetWidth, targetHeight));
                        }
                    } catch (org.lwjgl.LWJGLException failed) {
                        System.out.println("[AetherVisualDebug] window resize failed: " + failed);
                    }
                    // A windowed LWJGL window can ignore setDisplayMode, and Minecraft only adopts
                    // Display's size when Display.wasResized() is set - so tell it outright. The
                    // screenshots come from the framebuffer, which is what this resizes, and a
                    // framebuffer that is not the window size is exactly what makes the saved
                    // images comparable between machines.
                    try {
                        mc.resize(targetWidth, targetHeight);
                        System.out.println("[AetherVisualDebug] framebuffer " + mc.displayWidth
                            + "x" + mc.displayHeight);
                    } catch (Throwable failed) {
                        System.out.println("[AetherVisualDebug] framebuffer resize failed: " + failed);
                    }
                }
                step = Step.WAIT_MENU;
                settleTicks = 60;
                break;

            case WAIT_MENU:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherMainMenuScreen) {
                    step = Step.SHOOT_MENU;
                    settleTicks = 30;
                }
                break;

            case SHOOT_MENU:
                if (--settleTicks <= 0) {
                    shoot(mc, "debug-aether-mainmenu");
                    open(mc, AetherGui.modules(client), Step.SHOOT_MODULES);
                }
                break;

            case SHOOT_MODULES:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherModScreen) {
                    shoot(mc, "debug-aether-modules");
                    step = Step.MODULES_SEARCH;
                    settleTicks = 10;
                }
                break;

            case MODULES_SEARCH:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherModScreen) {
                    ((AetherModScreen) mc.currentScreen).debugSearch("zoom");
                    step = Step.SHOOT_MODULES_SEARCH;
                    settleTicks = 15;
                }
                break;

            case SHOOT_MODULES_SEARCH:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherModScreen) {
                    shoot(mc, "debug-aether-modules-search");
                    step = Step.MODULES_CATEGORY;
                    settleTicks = 10;
                }
                break;

            case MODULES_CATEGORY:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherModScreen) {
                    // Clears the query and lands on a real category, so the chip bar and the
                    // list are both exercised.
                    ((AetherModScreen) mc.currentScreen).debugSearch("");
                    ((AetherModScreen) mc.currentScreen).debugSelectCategory("HUD");
                    step = Step.SHOOT_MODULES_CATEGORY;
                    settleTicks = 15;
                }
                break;

            case SHOOT_MODULES_CATEGORY:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherModScreen) {
                    shoot(mc, "debug-aether-modules-category");
                    ((AetherModScreen) mc.currentScreen).debugSelectCategory("All");
                    step = Step.SCROLL_MODULES;
                    settleTicks = 10;
                }
                break;

            case SCROLL_MODULES:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherModScreen) {
                    ((AetherModScreen) mc.currentScreen).debugNextPage();
                    step = Step.SHOOT_MODULES_SCROLLED;
                    settleTicks = 15;
                }
                break;

            case SHOOT_MODULES_SCROLLED:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherModScreen) {
                    shoot(mc, "debug-aether-modules-scrolled");
                    step = Step.TOGGLE_MODULE;
                    settleTicks = 10;
                }
                break;

            case TOGGLE_MODULE:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherModScreen) {
                    ((AetherModScreen) mc.currentScreen).debugToggleVisible();
                    step = Step.SHOOT_MODULES_TOGGLED;
                    settleTicks = 15;
                }
                break;

            case SHOOT_MODULES_TOGGLED:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherModScreen) {
                    shoot(mc, "debug-aether-modules-toggled");
                    ((AetherModScreen) mc.currentScreen).debugOpenFirstModuleSettings();
                    step = Step.SHOOT_MODULE_SETTINGS;
                    settleTicks = 20;
                }
                break;

            case SHOOT_MODULE_SETTINGS:
                if (--settleTicks <= 0 && mc.currentScreen instanceof dev.aether.gui.screens.AetherModuleSettingsScreen) {
                    shoot(mc, "debug-aether-module-settings");
                    open(mc, AetherGui.cosmetics(client), Step.SHOOT_COSMETICS);
                }
                break;

            case SHOOT_COSMETICS:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherCosmeticScreen) {
                    shoot(mc, "debug-aether-cosmetics");
                    step = Step.COSMETICS_CATEGORY;
                    settleTicks = 10;
                }
                break;

            case COSMETICS_CATEGORY:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherCosmeticScreen) {
                    ((AetherCosmeticScreen) mc.currentScreen).debugSelectFilter(3); // Wings
                    step = Step.SHOOT_COSMETICS_CATEGORY;
                    settleTicks = 15;
                }
                break;

            case SHOOT_COSMETICS_CATEGORY:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherCosmeticScreen) {
                    shoot(mc, "debug-aether-cosmetics-category");
                    ((AetherCosmeticScreen) mc.currentScreen).debugSearch("test");
                    step = Step.SHOOT_COSMETICS_SEARCH;
                    settleTicks = 15;
                }
                break;

            case SHOOT_COSMETICS_SEARCH:
                // A live query: proves searching filters the grid without breaking its paging.
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherCosmeticScreen) {
                    shoot(mc, "debug-aether-cosmetics-search");
                    ((AetherCosmeticScreen) mc.currentScreen).debugSearch("");
                    ((AetherCosmeticScreen) mc.currentScreen).debugSelectFilter(0); // All
                    step = Step.SCROLL_COSMETICS;
                    settleTicks = 10;
                }
                break;

            case SCROLL_COSMETICS:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherCosmeticScreen) {
                    ((AetherCosmeticScreen) mc.currentScreen).debugNextPage();
                    step = Step.SHOOT_COSMETICS_SCROLLED;
                    settleTicks = 15;
                }
                break;

            case SHOOT_COSMETICS_SCROLLED:
                // One viewport down: proves the gallery scrolls and the thumb moves.
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherCosmeticScreen) {
                    shoot(mc, "debug-aether-cosmetics-scrolled");
                    ((AetherCosmeticScreen) mc.currentScreen).debugSelectCard(1);
                    step = Step.SHOOT_COSMETICS_SELECTED;
                    settleTicks = 15;
                }
                break;

            case SHOOT_COSMETICS_SELECTED:
                // A selected card carries the EQUIPPED badge and updates the preview.
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherCosmeticScreen) {
                    shoot(mc, "debug-aether-cosmetics-selected");
                    ((AetherCosmeticScreen) mc.currentScreen).debugToggleImportMenu();
                    step = Step.SHOOT_COSMETICS_IMPORT;
                    settleTicks = 15;
                }
                break;

            case SHOOT_COSMETICS_IMPORT:
                // The import popover: open folder / type folder / drop import / choose file.
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherCosmeticScreen) {
                    shoot(mc, "debug-aether-cosmetics-import");
                    ((AetherCosmeticScreen) mc.currentScreen).debugToggleImportMenu();
                    open(mc, AetherGui.appearance(client), Step.SHOOT_APPEARANCE_PAGE);
                }
                break;

            case SHOOT_APPEARANCE_PAGE:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherAppearanceScreen) {
                    shoot(mc, "debug-aether-appearance");
                    step = Step.SELECT_THEME;
                    settleTicks = 10;
                }
                break;

            case SELECT_THEME:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherAppearanceScreen) {
                    ((AetherAppearanceScreen) mc.currentScreen).debugSelect(1);
                    step = Step.SHOOT_APPEARANCE_EQUIPPED;
                    settleTicks = 15;
                }
                break;

            case SHOOT_APPEARANCE_EQUIPPED:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherAppearanceScreen) {
                    shoot(mc, "debug-aether-appearance-equipped");
                    step = Step.SCROLL_APPEARANCE;
                    settleTicks = 10;
                }
                break;

            case SCROLL_APPEARANCE:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherAppearanceScreen) {
                    ((AetherAppearanceScreen) mc.currentScreen).debugNextPage();
                    step = Step.SHOOT_APPEARANCE_SCROLLED;
                    settleTicks = 15;
                }
                break;

            case SHOOT_APPEARANCE_SCROLLED:
                // Page two: proves the pager works and moves the scrollbar thumb down its track.
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherAppearanceScreen) {
                    shoot(mc, "debug-aether-appearance-scrolled");
                    open(mc, AetherGui.hudEditor(client), Step.SHOOT_HUD);
                }
                break;

            case SHOOT_HUD:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherHudEditorScreen) {
                    shoot(mc, "debug-aether-hud");
                    open(mc, AetherGui.settings(client), Step.SHOOT_CLIENT_SETTINGS);
                }
                break;

            case SHOOT_CLIENT_SETTINGS:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherClientSettingsScreen) {
                    shoot(mc, "debug-aether-settings");
                    open(mc, AetherGui.profiles(client), Step.SHOOT_PROFILES);
                }
                break;

            case SHOOT_PROFILES:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherProfilesScreen) {
                    shoot(mc, "debug-aether-profiles");
                    step = Step.LOAD_WORLD;
                    settleTicks = 10;
                }
                break;

            case LOAD_WORLD:
                if (--settleTicks <= 0) {
                    try {
                        // Loads the existing singleplayer save so the player preview has
                        // a real player to render.
                        mc.launchIntegratedServer("New World", "New World", null);
                        step = Step.WAIT_WORLD;
                        settleTicks = 400; // up to 20s for the server to come up
                    } catch (Throwable failed) {
                        System.out.println("[AetherVisualDebug] world load failed: " + failed);
                        step = Step.DONE;
                        finish(mc);
                    }
                }
                break;

            case WAIT_WORLD:
                if (mc.theWorld != null && mc.thePlayer != null) {
                    // Let the terrain finish its initial upload burst before opening the
                    // GUI over the world - chunk uploads at first join are flaky here.
                    if (--settleTicks <= 0) {
                        // Park the cursor right of centre so the preview turns and the
                        // equipped cape (worn on the back) comes into view, like a real
                        // user would see by moving the mouse.
                        try {
                            org.lwjgl.input.Mouse.setCursorPosition(
                                Display.getX() + Display.getWidth() / 2 + 300,
                                Display.getY() + Display.getHeight() / 2 - 40);
                        } catch (Throwable ignored) {
                        }
                        open(mc, AetherGui.cosmetics(client), Step.SHOOT_PREVIEW);
                    }
                } else if (--settleTicks <= 0) {
                    // The server never came up - give up gracefully.
                    System.out.println("[AetherVisualDebug] world never loaded, skipping preview");
                    step = Step.DONE;
                    finish(mc);
                }
                break;

            case SHOOT_PREVIEW:
                if (mc.currentScreen instanceof AetherCosmeticScreen) {
                    if (--settleTicks <= 0) {
                        shoot(mc, "debug-aether-preview");
                        step = Step.DONE;
                        try {
                            if (mc.theWorld != null) {
                                mc.theWorld.sendQuittingDisconnectingPacket();
                                mc.loadWorld(null);
                            }
                        } catch (Throwable ignored) {
                        }
                        finish(mc);
                    }
                } else if (--settleTicks <= 0) {
                    // The focus-loss pause menu (or the world join flow) replaced the
                    // screen; put the preview back up and try once more.
                    System.out.println("[AetherVisualDebug] preview screen gone (current: "
                        + (mc.currentScreen == null ? "null" : mc.currentScreen.getClass().getSimpleName())
                        + "), reopening");
                    open(mc, AetherGui.cosmetics(client), Step.SHOOT_PREVIEW);
                }
                break;

            default:
                break;
        }

        // Heartbeat: proves the tick handler is alive and says what it sees.
        if (idleTicks % 100 == 0) {
            System.out.println("[AetherVisualDebug] tick " + idleTicks + " step=" + step
                + " world=" + (mc.theWorld != null) + " screen="
                + (mc.currentScreen == null ? "null" : mc.currentScreen.getClass().getSimpleName()));
        }

        // Safety: give up after ~2.5 minutes of nothing so a blocked step cannot spin forever.
        idleTicks++;
        if (idleTicks > 3600) {
            step = Step.DONE;
            finish(mc);
        }
    }

    /**
     * Puts the client back on the default palette at the start of the walk. The client saves its
     * configuration, so without this the whole sequence would depend on what the previous run
     * happened to wear (the Appearance "before" image most visibly, but every screen shows the
     * palette).
     */
    private void resetThemes() {
        client.themes().resetToDefault();
        System.out.println("[AetherVisualDebug] reset the theme to the default palette");
    }

    /**
     * Empties every cosmetic slot at the start of the walk, for the same reason the palette is
     * reset: the gallery's equipped ring and badge must show the selection the walk makes, not
     * leftover state from a previous run.
     */
    private void resetCosmetics() {
        for (dev.aether.cosmetic.CosmeticType type : dev.aether.cosmetic.CosmeticType.values()) {
            client.cosmetics().clear(type);
        }
        System.out.println("[AetherVisualDebug] cleared the cosmetic selection");
    }

    /**
     * Puts every module's switch back to the state the client booted with. Used at the start of the
     * walk (so a previous run's toggle cannot change the images) and again when the walk ends (so
     * the debug run leaves the player's configuration exactly as it found it).
     */
    private void restoreModules(String when) {
        int changed = 0;
        for (java.util.Map.Entry<String, Boolean> entry : modulesAtStart.entrySet()) {
            ClientModule module = client.modules().get(entry.getKey());
            boolean enabled = entry.getValue().booleanValue();
            if ((module.state() == ClientModule.ModuleState.ENABLED) != enabled) {
                client.modules().setEnabled(entry.getKey(), enabled);
                changed++;
            }
        }
        System.out.println("[AetherVisualDebug] " + when + ": restored " + changed
            + " module switch(es)");
    }

    private void open(Minecraft mc, net.minecraft.client.gui.GuiScreen screen, Step next) {
        System.out.println("[AetherVisualDebug] open " + screen.getClass().getSimpleName()
            + " -> step " + next + ", world=" + (mc.theWorld != null));
        mc.displayGuiScreen(screen);
        step = next;
        settleTicks = 30;
    }

    private static void shoot(Minecraft mc, String name) {
        try {
            // ScreenShotHelper writes the name verbatim, so the extension is added here: these
            // files are meant to be opened and looked at.
            String file = name.endsWith(".png") ? name : name + ".png";
            ScreenShotHelper.saveScreenshot(mc.mcDataDir, file,
                mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            System.out.println("[AetherVisualDebug] saved " + file);
        } catch (Exception exception) {
            System.out.println("[AetherVisualDebug] screenshot failed: " + exception);
        }
    }

    private void finish(Minecraft mc) {
        restoreModules("walk end");
        try {
            File done = new File(mc.mcDataDir, "aether-debug-shots-done.txt");
            if (!done.exists()) {
                done.createNewFile();
            }
            System.out.println("[AetherVisualDebug] sequence complete");
        } catch (Exception ignored) {
        }
    }
}
