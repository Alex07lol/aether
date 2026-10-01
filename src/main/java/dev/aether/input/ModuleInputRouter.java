package dev.aether.input;

/**
 * The one gate between the physical keyboard and every module keybind.
 * <p>
 * Aether's module keys are polled (Freelook, Zoom, SnapLook, Toggle Sprint and Toggle Sneak read
 * the key state every tick), so a key pressed while a menu is open reaches them unless something
 * stops it. The previous answer was a {@code currentScreen == null} test at each call site, which
 * meant a new keybind could forget it - and Zoom did, so pressing C inside the GUI zoomed the world
 * behind the menu. The rule now lives here, once, and every module key goes through
 * {@link #moduleKeyDown(boolean)}:
 * <ul>
 *   <li>a screen is open - any screen, because typing in chat or working a container is not a
 *       gameplay key press either;</li>
 *   <li>a keybind control is capturing a key, so assigning a module cannot fire the binding it is
 *       replacing;</li>
 *   <li>no world with a local player is live, so a key held at the main menu cannot follow the
 *       player into the next world.</li>
 * </ul>
 * The class is deliberately Minecraft-free: the Forge bridge describes the client once per tick,
 * the GUI reports keybind captures, and the modules ask whether their key is theirs. That is what
 * makes the rule testable without a game running.
 */
public final class ModuleInputRouter {

    private boolean screenOpen;
    private boolean aetherScreen;
    private boolean capturingKey;
    private boolean inWorld;

    /**
     * Describes what has the input right now. Called once per client tick by the Forge bridge.
     *
     * @param screenOpen true when any GUI screen is displayed
     * @param aetherScreen true when that screen is one of Aether's own
     * @param inWorld true when a world with a local player is loaded
     */
    public void describe(boolean screenOpen, boolean aetherScreen, boolean inWorld) {
        this.screenOpen = screenOpen;
        this.aetherScreen = screenOpen && aetherScreen;
        this.inWorld = inWorld;
    }

    /**
     * Starts or ends a keybind capture. While a capture is live every module key is suppressed,
     * including the key being assigned, so the module that owns the old binding cannot react to the
     * press that is meant to replace it.
     */
    public void capturingKey(boolean capturing) {
        this.capturingKey = capturing;
    }

    /**
     * @param physicalDown the raw key state the module read
     * @return true when the module may treat its key as pressed
     */
    public boolean moduleKeyDown(boolean physicalDown) {
        return physicalDown && !suppressed();
    }

    /** @return true while module keybinds must not act, whatever the physical key state is. */
    public boolean suppressed() {
        return this.screenOpen || this.capturingKey || !this.inWorld;
    }

    /** @return true when any screen owns the keyboard. */
    public boolean screenOpen() {
        return this.screenOpen;
    }

    /** @return true when the screen on top is one of Aether's own. */
    public boolean aetherScreenOpen() {
        return this.aetherScreen;
    }

    /** @return true while a keybind control is waiting for a key. */
    public boolean capturingKey() {
        return this.capturingKey;
    }

    @Override
    public String toString() {
        return "ModuleInputRouter{screen=" + this.screenOpen + ", aether=" + this.aetherScreen
            + ", capturing=" + this.capturingKey + ", world=" + this.inWorld + "}";
    }
}
