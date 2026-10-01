package dev.aether.input;

import dev.aether.TestSupport;

/**
 * Guards the rule the whole module key path depends on: a module key acts only when the client is
 * in a world, no screen is open and no keybind capture is running.
 * <p>
 * The failure this exists for is concrete - Zoom used to read the raw keyboard, so pressing its key
 * inside the Aether menu zoomed the world behind it. Every case below is written as a bead on that
 * sequence: some screen is up, some module would have acted, and the router has to say no.
 */
public final class ModuleInputRouterTest {

    private ModuleInputRouterTest() {
    }

    public static void main(String[] args) {
        inWorldOnly();
        screensBlockEveryKey();
        capturesBlockEveryKey();
        keyHeldThroughAMenu();
        System.out.println("ModuleInputRouterTest passed");
    }

    private static void inWorldOnly() {
        ModuleInputRouter router = new ModuleInputRouter();
        TestSupport.assertTrue(!router.moduleKeyDown(true),
            "a key at the main menu (no world) is not a module key press");
        TestSupport.assertTrue(router.suppressed(), "no world means module input is suppressed");

        router.describe(false, false, true);
        TestSupport.assertTrue(router.moduleKeyDown(true), "in game, with nothing on screen, the key is the module's");
        TestSupport.assertTrue(!router.moduleKeyDown(false), "a released key is never a press");
        TestSupport.assertTrue(!router.suppressed(), "in game with nothing open, module input is live");
    }

    private static void screensBlockEveryKey() {
        ModuleInputRouter router = new ModuleInputRouter();
        router.describe(true, true, true);
        TestSupport.assertTrue(router.aetherScreenOpen(), "the router recognises an Aether screen");
        TestSupport.assertTrue(router.suppressed(), "an Aether screen suppresses module input");
        TestSupport.assertTrue(!router.moduleKeyDown(true), "pressing C in the Aether GUI must not zoom");
        TestSupport.assertTrue(!router.moduleKeyDown(false), "and a release is still not a press");

        router.describe(true, false, true);
        TestSupport.assertTrue(router.screenOpen(), "chat counts as a screen");
        TestSupport.assertTrue(!router.aetherScreenOpen(), "chat is not an Aether screen");
        TestSupport.assertTrue(!router.moduleKeyDown(true), "typing in chat must not toggle modules");

        router.describe(false, true, true);
        TestSupport.assertTrue(!router.aetherScreenOpen(),
            "an Aether screen that is not open is not an Aether screen");

        router.describe(false, false, true);
        TestSupport.assertTrue(router.moduleKeyDown(true), "closing the screen hands the key back");
    }

    private static void capturesBlockEveryKey() {
        ModuleInputRouter router = new ModuleInputRouter();
        router.describe(false, false, true);
        router.capturingKey(true);
        TestSupport.assertTrue(router.capturingKey(), "the capture is reported");
        TestSupport.assertTrue(router.suppressed(), "a capture suppresses module input");
        TestSupport.assertTrue(!router.moduleKeyDown(true),
            "the key being assigned must not fire the module that owns it");

        router.capturingKey(false);
        TestSupport.assertTrue(!router.capturingKey(), "the capture ends");
        TestSupport.assertTrue(router.moduleKeyDown(true), "and the keyboard goes back to the modules");
    }

    /**
     * The router describes a moment, not an event: a key held down while a menu is open is a press
     * again the moment the menu closes. That is what the hold-style modules (Freelook, Zoom) want,
     * and the toggle-style ones have their own edge detection to notice.
     */
    private static void keyHeldThroughAMenu() {
        ModuleInputRouter router = new ModuleInputRouter();
        router.describe(true, true, true);
        TestSupport.assertTrue(!router.moduleKeyDown(true), "held through the menu: ignored");
        router.describe(false, false, true);
        TestSupport.assertTrue(router.moduleKeyDown(true), "menu closed, still held: live again");
    }
}
