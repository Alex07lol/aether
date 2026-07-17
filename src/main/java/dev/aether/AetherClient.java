package dev.aether;

import dev.aether.config.ConfigDocument;
import dev.aether.config.JsonConfigStore;
import dev.aether.cosmetic.CosmeticLibrary;
import dev.aether.event.EventBus;
import dev.aether.fairplay.FairPlayPolicy;
import dev.aether.hud.HudLayout;
import dev.aether.module.builtin.BuiltInModules;
import dev.aether.module.ModuleRegistry;
import dev.aether.runtime.ClientVersion;
import dev.aether.runtime.PlatformDetector;
import dev.aether.runtime.PlatformInfo;
import dev.aether.theme.AetherTheme;

import java.io.IOException;
import java.nio.file.Path;

public final class AetherClient {
    private final ClientVersion version;
    private final EventBus eventBus;
    private final ModuleRegistry modules;
    private final HudLayout hudLayout;
    private final AetherTheme theme;
    private final JsonConfigStore configStore;
    private final PlatformInfo platform;
    private final CosmeticLibrary cosmetics;

    public AetherClient(Path configFile) {
        this.version = ClientVersion.current();
        this.eventBus = new EventBus();
        this.modules = new ModuleRegistry(FairPlayPolicy.standard());
        this.hudLayout = new HudLayout(4);
        this.theme = AetherTheme.defaultTheme();
        this.configStore = new JsonConfigStore(configFile);
        this.platform = PlatformDetector.detect();
        Path baseDirectory = configFile.getParent() == null ? configFile.toAbsolutePath().getParent() : configFile.getParent();
        this.cosmetics = new CosmeticLibrary(baseDirectory.resolve("cosmetics"));
        BuiltInModules.registerAll(modules, hudLayout);
    }

    public void start() throws IOException {
        ConfigDocument config = configStore.exists() ? configStore.load() : ConfigDocument.empty();
        cosmetics.load();
        cosmetics.applyConfig(config);
        modules.applyConfig(config);
    }

    public void save() throws IOException {
        ConfigDocument.Builder builder = ConfigDocument.builder();
        builder.putAll(modules.toConfig().values());
        cosmetics.writeConfig(builder);
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

    public AetherTheme theme() {
        return theme;
    }

    public PlatformInfo platform() {
        return platform;
    }

    public CosmeticLibrary cosmetics() {
        return cosmetics;
    }

    public Path configFile() {
        return configStore.file();
    }
}
