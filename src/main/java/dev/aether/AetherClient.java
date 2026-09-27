package dev.aether;

import dev.aether.config.ClientPreferences;
import dev.aether.config.ConfigDocument;
import dev.aether.config.JsonConfigStore;
import dev.aether.config.ProfileStore;
import dev.aether.cosmetic.CosmeticLibrary;
import dev.aether.event.EventBus;
import dev.aether.fairplay.FairPlayPolicy;
import dev.aether.hud.HudLayout;
import dev.aether.module.ClientModule;
import dev.aether.module.builtin.BuiltInModules;
import dev.aether.module.ModuleRegistry;
import dev.aether.runtime.ClientVersion;
import dev.aether.runtime.PlatformDetector;
import dev.aether.runtime.PlatformInfo;
import dev.aether.screenshot.ScreenshotManager;
import dev.aether.screenshot.ScreenshotStore;
import dev.aether.theme.AetherTheme;
import dev.aether.theme.ThemeModule;
import dev.aether.waypoint.WaypointManager;

import java.io.IOException;
import java.nio.file.Path;

public final class AetherClient {
    private final ClientVersion version;
    private final EventBus eventBus;
    private final ModuleRegistry modules;
    private final HudLayout hudLayout;
    private final AetherTheme defaultTheme;
    private ThemeModule cachedThemeModule;
    private AetherTheme cachedTheme;
    private final JsonConfigStore configStore;
    private final PlatformInfo platform;
    private final CosmeticLibrary cosmetics;
    private final ClientPreferences preferences = new ClientPreferences();
    private final ProfileStore profiles = new ProfileStore();
    private final WaypointManager waypoints = new WaypointManager();
    private final ScreenshotManager screenshots;

    public AetherClient(Path configFile) {
        this.version = ClientVersion.current();
        this.eventBus = new EventBus();
        this.modules = new ModuleRegistry(FairPlayPolicy.standard());
        this.hudLayout = new HudLayout(4);
        this.defaultTheme = AetherTheme.defaultTheme();
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
        preferences.applyConfig(config);
        profiles.applyConfig(config);
        waypoints.applyConfig(config);
    }

    public void save() throws IOException {
        ConfigDocument.Builder builder = ConfigDocument.builder();
        builder.putAll(modules.toConfig().values());
        cosmetics.writeConfig(builder);
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

    public HudLayout hudLayout() {
        return hudLayout;
    }

    /**
     * @return the palette of the enabled {@link ThemeModule}, or the built-in default
     *     palette when no theme module is active. Adapters render from this so the
     *     theme switches change the actual UI instead of only storing a flag.
     *     <p>
     *     The result is cached and the same instance is handed out while the active
     *     theme does not change, because renderers call this every frame and comparing
     *     identity is the cheapest way for a screen to notice a theme switch.
     */
    public AetherTheme theme() {
        ThemeModule active = activeThemeModule();
        if (active == null) {
            this.cachedThemeModule = null;
            this.cachedTheme = defaultTheme;
            return defaultTheme;
        }
        if (active != this.cachedThemeModule) {
            this.cachedThemeModule = active;
            this.cachedTheme = AetherTheme.of(active.metadata().name(), active.palette());
        }
        return cachedTheme;
    }

    private ThemeModule activeThemeModule() {
        // Theme modules share a registry group, so at most one can be enabled at a time and
        // the first match is the answer.
        for (ClientModule module : modules.all()) {
            if (module instanceof ThemeModule && module.state() == ClientModule.ModuleState.ENABLED) {
                return (ThemeModule) module;
            }
        }
        return null;
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
