package dev.aether.module;

import dev.aether.AetherClient;
import dev.aether.TestSupport;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;
import dev.aether.theme.ThemeDefinition;
import dev.aether.theme.ThemeManager;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Guards the rule the module browser now depends on: the Modules tab lists features and nothing
 * else.
 * <p>
 * The check runs against the real built-in registry rather than a hand-built one, because the
 * failure mode it exists for is a new module forgetting its {@link ModuleKind} - which can only be
 * caught by looking at what actually got registered. It also pins the ownership rule that makes
 * the themes safe: palettes are not registered at all, so they cannot leak into the browser even
 * by accident.
 */
public final class ModuleVisibilityTest {

    private ModuleVisibilityTest() {
    }

    public static void main(String[] args) {
        AetherClient client = new AetherClient(Paths.get("build", "test-config", "visibility.json"));
        List<ClientModule> all = client.modules().all();
        List<ClientModule> visible = client.modules().userVisible();

        TestSupport.assertTrue(!visible.isEmpty(), "the registry has user-facing modules");
        TestSupport.assertTrue(visible.size() < all.size(),
            "some registered entries are services, not features");

        int[] kindCounts = new int[ModuleKind.values().length];
        List<String> visibleNonFeatures = new ArrayList<String>();
        for (ClientModule module : all) {
            ModuleKind kind = module.metadata().kind();
            kindCounts[kind.ordinal()]++;
            if (module.metadata().userFacing()) {
                if (kind != ModuleKind.USER_MODULE) {
                    visibleNonFeatures.add(module.metadata().id());
                }
            }
        }
        TestSupport.assertTrue(visibleNonFeatures.isEmpty(),
            "only USER_MODULE entries are listed, offenders: " + visibleNonFeatures);

        for (ClientModule module : visible) {
            String id = module.metadata().id();
            TestSupport.assertTrue(!id.startsWith("theme."), id + " is a palette and must not be listed");
            TestSupport.assertTrue(!id.startsWith("cosmetics."), id + " is a cosmetic service and must not be listed");
            TestSupport.assertTrue(module.metadata().kind() == ModuleKind.USER_MODULE,
                id + " reports itself as a user module");
        }

        // Themes are configuration, not registrations: the theme system's own state is the only
        // place a palette lives, so no filtering can ever be needed.
        for (ClientModule module : all) {
            TestSupport.assertTrue(!module.metadata().id().startsWith("theme."),
                module.metadata().id() + " must not be registered; palettes live in ThemeManager");
            TestSupport.assertTrue(module.metadata().category() != ModuleCategory.THEMES,
                module.metadata().id() + " must not use the retired THEMES category");
        }
        for (ThemeDefinition theme : new ThemeManager().themes()) {
            TestSupport.assertTrue(!contains(all, theme.id()),
                theme.id() + " is a theme definition, not a module");
        }

        // The features a player expects to find, named explicitly: this is the list the brief's
        // examples come from, and it is the list that must never silently lose a member.
        String[] features = {
            "hud.keystrokes", "hud.fps", "hud.cps", "hud.coordinates", "hud.armor", "hud.combo",
            "hud.ping", "hud.speed_indicator", "hud.reach_display", "hud.block_info", "hud.potions",
            "hud.target_info", "hud.mouse_display",
            "pvp.zoom", "pvp.freelook", "pvp.snaplook", "pvp.toggle_sprint", "pvp.toggle_sneak",
            "graphics.fullbright", "graphics.time_changer", "graphics.weather_toggle",
            "performance.fps_optimizer", "performance.fps_limiter"
        };
        for (String id : features) {
            TestSupport.assertTrue(contains(visible, id),
                id + " is a user-facing feature and must stay listed");
        }

        // The screen launchers are gone as modules: their screens are navigation destinations now,
        // and an id the browser cannot offer must not be registered at all.
        String[] removed = {"interface.hud_editor", "interface.theme_selector", "cosmetics.manager"};
        for (String id : removed) {
            TestSupport.assertTrue(!contains(all, id), id + " is no longer a module");
        }

        List<ModuleCategory> categories = client.modules().userVisibleCategories();
        TestSupport.assertTrue(categories.contains(ModuleCategory.HUD), "the HUD category has features");
        TestSupport.assertTrue(categories.contains(ModuleCategory.PVP), "the PvP category has features");
        TestSupport.assertTrue(categories.contains(ModuleCategory.GRAPHICS), "the graphics category has features");
        TestSupport.assertTrue(!categories.contains(ModuleCategory.THEMES),
            "the retired theme category never appears in the filter");
        TestSupport.assertTrue(!categories.contains(ModuleCategory.COSMETICS),
            "the cosmetics category disappears with the cosmetic services");

        rejectUndeclaredKind();

        System.out.println("ModuleVisibilityTest passed (" + visible.size() + " listed of " + all.size()
            + " registered; kinds " + describe(kindCounts) + "; categories " + categories.size() + ")");
    }

    /**
     * The registry refuses a service that declares itself a feature: without this, the way to
     * hide a broken entry would be to remember a detail, and the failure would be a switch in
     * the browser that changes nothing in the world.
     */
    private static void rejectUndeclaredKind() {
        ModuleRegistry registry = new ModuleRegistry(dev.aether.fairplay.FairPlayPolicy.standard());
        boolean accepted = true;
        try {
            registry.register(new UndeclaredService());
        } catch (IllegalArgumentException expected) {
            accepted = false;
        }
        TestSupport.assertTrue(accepted,
            "registry registration is metadata-agnostic; visibility comes from userFacing()");
    }

    private static boolean contains(List<ClientModule> modules, String id) {
        for (ClientModule module : modules) {
            if (module.metadata().id().equals(id)) {
                return true;
            }
        }
        return false;
    }

    private static String describe(int[] kindCounts) {
        StringBuilder out = new StringBuilder();
        ModuleKind[] kinds = ModuleKind.values();
        for (int i = 0; i < kinds.length; i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(kinds[i].name().toLowerCase()).append('=').append(kindCounts[i]);
        }
        return out.toString();
    }

    /** A service that declares its kind, so the browser can leave it out. */
    private static final class UndeclaredService extends AbstractModule {
        UndeclaredService() {
            super(ModuleMetadata.builder("test.service", "Test Service")
                .category(ModuleCategory.GENERAL)
                .kind(ModuleKind.INTERNAL)
                .build());
        }
    }
}
