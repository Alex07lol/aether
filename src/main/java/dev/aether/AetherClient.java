package dev.aether;

import dev.aether.config.ClientPreferences;
import dev.aether.config.ConfigDocument;
import dev.aether.config.JsonConfigStore;
import dev.aether.config.ProfileStore;
import dev.aether.cosmetic.CosmeticLibrary;
import dev.aether.event.EventBus;
import dev.aether.fairplay.FairPlayPolicy;
import dev.aether.hud.HudLayout;
import dev.aether.input.ModuleInputRouter;
import dev.aether.module.ClientModule;
import dev.aether.module.builtin.BuiltInModules;
import dev.aether.module.ModuleRegistry;
import dev.aether.runtime.ClientVersion;
import dev.aether.runtime.PlatformDetector;
import dev.aether.runtime.PlatformInfo;
import dev.aether.screenshot.ScreenshotManager;
import dev.aether.screenshot.ScreenshotStore;
import dev.aether.theme.AetherTheme;
import dev.aether.theme.ThemeDefinition;
import dev.aether.theme.ThemeManager;
import dev.aether.waypoint.WaypointManager;

import java.io.IOException;
import java.nio.file.Path;

public final class AetherClient {
    private final ClientVersion version;
    private final EventBus eventBus;
    private final ModuleRegistry modules;
    private final HudLayout hudLayout;
    private final AetherTheme defaultTheme;
    private AetherTheme cachedTheme;
    private final ThemeManager themes = new ThemeManager();
    private final JsonConfigStore configStore;
    private final PlatformInfo platform;
    private final CosmeticLibrary cosmetics;
    private final ClientPreferences preferences = new ClientPreferences();
    private final ProfileStore profiles = new ProfileStore();
    private final WaypointManager waypoints = new WaypointManager();
    private final ScreenshotManager screenshots;
    private final ModuleInputRouter input = new ModuleInputRouter();

    public AetherClient(Path configFile) {
        this.version = ClientVersion.current();
        this.eventBus = new EventBus();
        this.modules = new ModuleRegistry(FairPlayPolicy.standard());
        this.hudLayout = new HudLayout(4);
        this.defaultTheme = AetherTheme.of(themes.active());
        this.configStore = new JsonConfigStore(configFile);
        this.platform = PlatformDetector.detect();
        Path baseDirectory = configFile.getParent() == null ? configFile.toAbsolutePath().getParent() : configFile.getParent();
        this.cosmetics = new CosmeticLibrary(baseDirectory.resolve("cosmetics"));
        this.screenshots = new ScreenshotManager(modules, new ScreenshotStore(baseDirectory.resolve("screenshots")));
        BuiltInModules.registerAll(modules, hudLayout);
    }

    public void start() throws IOException {
        ConfigDocument config = configStore.exists() ? configStore.load() : ConfigDocument.empty();
        cosmetics.load();
        cosmetics.applyConfig(config);
        modules.applyConfig(config);
        themes.applyConfig(config);
        preferences.applyConfig(config);
        profiles.applyConfig(config);
        hudLayout.applyConfig(config);
        waypoints.applyConfig(config);
    }

    public void save() throws IOException {
        ConfigDocument.Builder builder = ConfigDocument.builder();
        builder.putAll(modules.toConfig().values());
        builder.putAll(hudLayout.toConfig().values());
        cosmetics.writeConfig(builder);
        themes.writeConfig(builder);
        preferences.writeConfig(builder);
        profiles.writeConfig(builder);
        waypoints.writeConfig(builder);
        configStore.save(builder.build());
    }

    public ClientVersion version() {
        return version;
    }

    public EventBus eventBus() {
        return eventBus;
    }

    public ModuleRegistry modules() {
        return modules;
    }

    /**
     * The gate every module keybind goes through. The bridge feeds it the current screen, the GUI
     * feeds it keybind captures, and the modules read their keys through it - see
     * {@link ModuleInputRouter} for why that is one object instead of a check per module.
     */
    public ModuleInputRouter input() {
        return input;
    }

    public HudLayout hudLayout() {
        return hudLayout;
    }

    /**
     * @return the palette of the theme the player chose on the Appearance screen, or the built-in
     *     default palette when the default is active. Adapters render from this so a theme
     *     switch changes the actual UI instead of only storing a flag.
     *     <p>
     *     The result is cached and the same instance is handed out while the active theme does
     *     not change, because renderers call this every frame and comparing identity is the
     *     cheapest way for a screen to notice a theme switch.
     */
    public AetherTheme theme() {
        ThemeDefinition active = themes.active();
        if (active == null) {
            return defaultTheme;
        }
        if (cachedTheme == null || !active.id().equals(cachedTheme.id())) {
            this.cachedTheme = AetherTheme.of(active);
        }
        return cachedTheme;
    }

    /** The theme system's own state, outside the module registry. See {@link ThemeManager}. */
    public ThemeManager themes() {
        return themes;
    }

    public PlatformInfo platform() {
        return platform;
    }

    public CosmeticLibrary cosmetics() {
        return cosmetics;
    }

    /** The client-wide options that belong to Aether itself, not to any one module. */
    public ClientPreferences preferences() {
        return preferences;
    }

    /** Named snapshots of the module configuration, stored inside the same config file. */
    public ProfileStore profiles() {
        return profiles;
    }

    /** Saved locations, stored inside the same config file. */
    public WaypointManager waypoints() {
        return waypoints;
    }

    /** The async screenshot pipeline; the capture call belongs on the render thread only. */
    public ScreenshotManager screenshots() {
        return screenshots;
    }

    public Path configFile() {
        return configStore.file();
    }
}
