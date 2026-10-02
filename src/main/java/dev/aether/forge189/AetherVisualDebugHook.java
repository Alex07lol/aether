package dev.aether.forge189;

import dev.aether.AetherClient;
import dev.aether.menu.AetherMenuScreen;
import dev.aether.menu.impl.AppearanceCategory;
import dev.aether.menu.impl.CosmeticsCategory;
import dev.aether.menu.impl.ModsCategory;
import dev.aether.menu.impl.ProfilesCategory;
import dev.aether.ui.GuiSection;
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
 * The value is either {@code true} (window keeps its size) or a {@code WIDTHxHEIGHT}
 * pair such as {@code 1920x1080}. The hook drives the client through every menu view
 * and saves real screenshots into {@code run/screenshots}: main menu, menu home,
 * modules (list / search / category / toggled / settings), cosmetics (gallery /
 * category / selected / import popover), appearance, profiles, settings, the HUD
 * editor, and one in-game frame after everything closes.
 * <p>
 * Each step fires when its screen is visible (after a settle delay); the sequence
 * ends with a marker file so an outside observer can pick the images up.
 */
public final class AetherVisualDebugHook {

    private enum Step {
        RESIZE, WAIT_MENU, SHOOT_MAINMENU,
        OPEN_MENU, SHOOT_HOME,
        NAV_MODS, SHOOT_MODS,
        MODS_SEARCH, SHOOT_MODS_SEARCH,
        MODS_CATEGORY, SHOOT_MODS_CATEGORY,
        MODS_TOGGLE, SHOOT_MODS_TOGGLED,
        MODS_SETTINGS, SHOOT_MODS_SETTINGS,
        MODS_BACK,
        NAV_COSMETICS, SHOOT_COSMETICS,
        COSMETICS_CATEGORY, SHOOT_COSMETICS_CATEGORY,
        COSMETICS_SELECT, SHOOT_COSMETICS_SELECTED,
        COSMETICS_IMPORT, SHOOT_COSMETICS_IMPORT,
        COSMETICS_EQUIP_ALL, SHOOT_COSMETICS_ALL,
        NAV_APPEARANCE, SHOOT_APPEARANCE,
        APPEARANCE_SELECT, SHOOT_APPEARANCE_SELECTED,
        NAV_PROFILES, SHOOT_PROFILES,
        PROFILES_CREATE, SHOOT_PROFILES_CREATE, PROFILES_CLOSE_CREATE,
        NAV_SETTINGS, SHOOT_SETTINGS,
        OPEN_HUD, SHOOT_HUD,
        CLOSE_ALL, LOAD_WORLD, WAIT_WORLD, SHOOT_INGAME,
        DONE
    }

    private final AetherClient client;
    private final int targetWidth;
    private final int targetHeight;
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
                if (targetWidth > 0) {
                    try {
                        if (Display.getWidth() != targetWidth || Display.getHeight() != targetHeight) {
                            Display.setDisplayMode(new org.lwjgl.opengl.DisplayMode(targetWidth, targetHeight));
                            Display.setResizable(true);
                        }
                    } catch (org.lwjgl.LWJGLException failed) {
                        System.out.println("[AetherVisualDebug] resize failed: " + failed);
                    }
                }
                step = Step.WAIT_MENU;
                settleTicks = 60;
                break;

            case WAIT_MENU:
                if (--settleTicks <= 0 && mc.currentScreen instanceof AetherMainMenuScreen) {
                    step = Step.SHOOT_MAINMENU;
                    settleTicks = 30;
                }
                break;

            case SHOOT_MAINMENU:
                if (--settleTicks <= 0) {
                    shoot(mc, "debug-aether-mainmenu");
                    openMenu(mc, GuiSection.HOME);
                }
                break;

            case OPEN_MENU:
                if (mc.currentScreen instanceof AetherMenuScreen && --settleTicks <= 0) {
                    shoot(mc, "debug-aether-menu-home");
                    ((AetherMenuScreen) mc.currentScreen).debugNavigate(GuiSection.MODULES);
                    step = Step.SHOOT_MODS;
                    settleTicks = 25;
                }
                break;

            case SHOOT_MODS:
                if (isMenu(mc, GuiSection.MODULES) && --settleTicks <= 0) {
                    shoot(mc, "debug-aether-mods");
                    ((AetherMenuScreen) mc.currentScreen).category(ModsCategory.class).debugSearch("zoom");
                    step = Step.SHOOT_MODS_SEARCH;
                    settleTicks = 20;
                }
                break;

            case SHOOT_MODS_SEARCH:
                if (isMenu(mc, GuiSection.MODULES) && --settleTicks <= 0) {
                    shoot(mc, "debug-aether-mods-search");
                    ModsCategory mods = ((AetherMenuScreen) mc.currentScreen).category(ModsCategory.class);
                    mods.debugSearch("");
                    mods.debugSelectCategory("HUD");
                    step = Step.SHOOT_MODS_CATEGORY;
                    settleTicks = 20;
                }
                break;

            case SHOOT_MODS_CATEGORY:
                if (isMenu(mc, GuiSection.MODULES) && --settleTicks <= 0) {
                    shoot(mc, "debug-aether-mods-category");
                    ((AetherMenuScreen) mc.currentScreen).category(ModsCategory.class).debugToggleFirst();
                    step = Step.SHOOT_MODS_TOGGLED;
                    settleTicks = 20;
                }
                break;

            case SHOOT_MODS_TOGGLED:
                if (isMenu(mc, GuiSection.MODULES) && --settleTicks <= 0) {
                    shoot(mc, "debug-aether-mods-toggled");
                    ((AetherMenuScreen) mc.currentScreen).category(ModsCategory.class).debugOpenFirstSettings();
                    step = Step.SHOOT_MODS_SETTINGS;
                    settleTicks = 25;
                }
                break;

            case SHOOT_MODS_SETTINGS:
                if (isMenu(mc, GuiSection.MODULES) && --settleTicks <= 0) {
                    shoot(mc, "debug-aether-mods-settings");
                    ((AetherMenuScreen) mc.currentScreen).category(ModsCategory.class).debugBack();
                    step = Step.MODS_BACK;
                    settleTicks = 15;
                }
                break;

            case MODS_BACK:
                if (--settleTicks <= 0) {
                    openMenu(mc, GuiSection.COSMETICS);
                }
                break;

            case SHOOT_COSMETICS:
                if (isMenu(mc, GuiSection.COSMETICS) && --settleTicks <= 0) {
                    shoot(mc, "debug-aether-cosmetics");
                    ((AetherMenuScreen) mc.currentScreen).category(CosmeticsCategory.class).debugSelectFilter(2); // Wings
                    step = Step.SHOOT_COSMETICS_CATEGORY;
                    settleTicks = 20;
                }
                break;

            case SHOOT_COSMETICS_CATEGORY:
                if (isMenu(mc, GuiSection.COSMETICS) && --settleTicks <= 0) {
                    shoot(mc, "debug-aether-cosmetics-category");
                    ((AetherMenuScreen) mc.currentScreen).category(CosmeticsCategory.class).debugSelectCard(1);
                    step = Step.SHOOT_COSMETICS_SELECTED;
                    settleTicks = 20;
                }
                break;

            case SHOOT_COSMETICS_SELECTED:
                if (isMenu(mc, GuiSection.COSMETICS) && --settleTicks <= 0) {
                    shoot(mc, "debug-aether-cosmetics-selected");
                    ((AetherMenuScreen) mc.currentScreen).category(CosmeticsCategory.class).debugToggleImportMenu();
                    step = Step.SHOOT_COSMETICS_IMPORT;
                    settleTicks = 15;
                }
                break;

            case SHOOT_COSMETICS_IMPORT:
                if (isMenu(mc, GuiSection.COSMETICS) && --settleTicks <= 0) {
                    shoot(mc, "debug-aether-cosmetics-import");
                    ((AetherMenuScreen) mc.currentScreen).category(CosmeticsCategory.class).debugToggleImportMenu();
                    step = Step.COSMETICS_EQUIP_ALL;
                    settleTicks = 10;
                }
                break;

            case COSMETICS_EQUIP_ALL:
                if (--settleTicks <= 0) {
                    // One of every rendered cosmetic type: a downloaded PNG cape plus the
                    // wing/halo/hat built-ins, so the gallery shows a full wardrobe.
                    try {
                        client.cosmetics().select("local.void_drifter");
                    } catch (IllegalArgumentException unknownLocal) {
                        client.cosmetics().select("builtin.frost_cape");
                    }
                    client.cosmetics().select("builtin.aurora_wings");
                    client.cosmetics().select("builtin.solar_halo");
                    client.cosmetics().select("builtin.arcane_hat");
                    try {
                        client.save();
                    } catch (Exception ignored) {
                    }
                    System.out.println("[AetherVisualDebug] equipped one of every cosmetic type");
                    step = Step.SHOOT_COSMETICS_ALL;
                    settleTicks = 20;
                }
                break;

            case SHOOT_COSMETICS_ALL:
                if (isMenu(mc, GuiSection.COSMETICS) && --settleTicks <= 0) {
                    shoot(mc, "debug-aether-cosmetics-all");
                    openMenu(mc, GuiSection.APPEARANCE);
                }
                break;

            case SHOOT_APPEARANCE:
                if (isMenu(mc, GuiSection.APPEARANCE) && --settleTicks <= 0) {
                    shoot(mc, "debug-aether-appearance");
                    ((AetherMenuScreen) mc.currentScreen).category(AppearanceCategory.class).debugSelect(2);
                    step = Step.SHOOT_APPEARANCE_SELECTED;
                    settleTicks = 20;
                }
                break;

            case SHOOT_APPEARANCE_SELECTED:
                if (isMenu(mc, GuiSection.APPEARANCE) && --settleTicks <= 0) {
                    shoot(mc, "debug-aether-appearance-selected");
                    openMenu(mc, GuiSection.PROFILES);
                }
                break;

            case SHOOT_PROFILES:
                if (isMenu(mc, GuiSection.PROFILES) && --settleTicks <= 0) {
                    shoot(mc, "debug-aether-profiles");
                    ((AetherMenuScreen) mc.currentScreen).category(ProfilesCategory.class).debugOpenCreate();
                    step = Step.SHOOT_PROFILES_CREATE;
                    settleTicks = 20;
                }
                break;

            case SHOOT_PROFILES_CREATE:
                if (isMenu(mc, GuiSection.PROFILES) && --settleTicks <= 0) {
                    shoot(mc, "debug-aether-profiles-create");
                    ((AetherMenuScreen) mc.currentScreen).category(ProfilesCategory.class).debugBack();
                    step = Step.PROFILES_CLOSE_CREATE;
                    settleTicks = 15;
                }
                break;

            case PROFILES_CLOSE_CREATE:
                if (--settleTicks <= 0) {
                    openMenu(mc, GuiSection.SETTINGS);
                }
                break;

            case SHOOT_SETTINGS:
                if (isMenu(mc, GuiSection.SETTINGS) && --settleTicks <= 0) {
                    shoot(mc, "debug-aether-settings");
                    step = Step.OPEN_HUD;
                    settleTicks = 10;
                }
                break;

            case OPEN_HUD:
                if (--settleTicks <= 0) {
                    mc.displayGuiScreen(dev.aether.gui.AetherGui.hudEditor(client));
                    step = Step.SHOOT_HUD;
                    settleTicks = 25;
                }
                break;

            case SHOOT_HUD:
                if (--settleTicks <= 0 && mc.currentScreen instanceof dev.aether.menu.AetherHudEditorScreen) {
                    shoot(mc, "debug-aether-hud");
                    step = Step.CLOSE_ALL;
                    settleTicks = 10;
                }
                break;

            case CLOSE_ALL:
                if (--settleTicks <= 0) {
                    mc.displayGuiScreen(null);
                    step = mc.theWorld == null ? Step.LOAD_WORLD : Step.SHOOT_INGAME;
                    settleTicks = 20;
                }
                break;

            case LOAD_WORLD:
                if (--settleTicks <= 0) {
                    try {
                        // Loads the existing singleplayer save so the final frame shows
                        // the real game running normally after everything closed.
                        mc.launchIntegratedServer("New World", "New World", null);
                        step = Step.WAIT_WORLD;
                        settleTicks = 400;
                    } catch (Throwable failed) {
                        System.out.println("[AetherVisualDebug] world load failed: " + failed);
                        step = Step.DONE;
                        finish(mc);
                    }
                }
                break;

            case WAIT_WORLD:
                if (mc.theWorld != null && mc.thePlayer != null) {
                    if (--settleTicks <= 0) {
                        step = Step.SHOOT_INGAME;
                        settleTicks = 15;
                    }
                } else if (--settleTicks <= 0) {
                    System.out.println("[AetherVisualDebug] world never loaded, skipping in-game shot");
                    step = Step.DONE;
                    finish(mc);
                }
                break;

            case SHOOT_INGAME:
                if (--settleTicks <= 0 && mc.currentScreen == null) {
                    shoot(mc, "debug-aether-ingame");
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
                break;

            default:
                break;
        }

        // Heartbeat + safety give-up.
        idleTicks++;
        if (idleTicks % 200 == 0) {
            System.out.println("[AetherVisualDebug] tick " + idleTicks + " step=" + step
                + " world=" + (mc.theWorld != null) + " screen="
                + (mc.currentScreen == null ? "null" : mc.currentScreen.getClass().getSimpleName()));
        }
        if (idleTicks > 3000) {
            step = Step.DONE;
            finish(mc);
        }
    }

    private boolean isMenu(Minecraft mc, GuiSection section) {
        if (!(mc.currentScreen instanceof AetherMenuScreen)) {
            return false;
        }
        AetherMenuScreen menu = (AetherMenuScreen) mc.currentScreen;
        return menu.visibleSection() == section;
    }

    private void openMenu(Minecraft mc, GuiSection section) {
        mc.displayGuiScreen(dev.aether.gui.AetherGui.menu(client, section));
        step = stepFor(section);
        settleTicks = 70; // first frames run slow; let the intro settle
    }

    private Step stepFor(GuiSection section) {
        switch (section) {
            case COSMETICS: return Step.SHOOT_COSMETICS;
            case APPEARANCE: return Step.SHOOT_APPEARANCE;
            case PROFILES: return Step.SHOOT_PROFILES;
            case SETTINGS: return Step.SHOOT_SETTINGS;
            case HOME: return Step.OPEN_MENU;
            case MODULES:
            default: return Step.SHOOT_MODS;
        }
    }

    private static void shoot(Minecraft mc, String name) {
        try {
            ScreenShotHelper.saveScreenshot(mc.mcDataDir, name,
                mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
            System.out.println("[AetherVisualDebug] saved " + name);
        } catch (Exception exception) {
            System.out.println("[AetherVisualDebug] screenshot failed: " + exception);
        }
    }

    private static void finish(Minecraft mc) {
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
