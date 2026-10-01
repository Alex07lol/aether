package dev.aether.module;

import dev.aether.AetherClient;
import dev.aether.TestSupport;
import dev.aether.module.ClientModule.ModuleCategory;
import dev.aether.module.ClientModule.ModuleMetadata;
import dev.aether.theme.ThemeModule;
import dev.aether.theme.ThemePalette;
import dev.aether.theme.ThemePalettes;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Guards the rule the module browser now depends on: the Modules tab lists features and nothing
 * else.
 * <p>
 * The check runs against the real built-in registry rather than a hand-built one, because the
 * failure mode it exists for is a new module forgetting its {@link ModuleKind} - which can only be
 * caught by looking at what actually got registered. It also pins the registry's own guard: a theme
 * module that declares a user kind must be rejected outright.
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
            "some registered entries are services or themes, not features");

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
            TestSupport.assertTrue(!(module instanceof ThemeModule), id + " is a theme and must not be listed");
            TestSupport.assertTrue(!id.startsWith("theme."), id + " is a palette and must not be listed");
            TestSupport.assertTrue(!id.startsWith("cosmetics."), id + " is a cosmetic service and must not be listed");
            TestSupport.assertTrue(module.metadata().kind() == ModuleKind.USER_MODULE,
                id + " reports itself as a user module");
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
            "the theme category disappears with the palettes");
        TestSupport.assertTrue(!categories.contains(ModuleCategory.COSMETICS),
            "the cosmetics category disappears with the cosmetic services");

        rejectUndeclaredTheme();

        System.out.println("ModuleVisibilityTest passed (" + visible.size() + " listed of " + all.size()
            + " registered; kinds " + describe(kindCounts) + "; categories " + categories.size() + ")");
    }

    /**
     * The registry refuses a palette that claims to be a feature: without this, the way to add a
     * theme would be to remember a detail, and the failure would be a switch in the browser that
     * changes nothing in the world.
     */
    private static void rejectUndeclaredTheme() {
        ModuleRegistry registry = new ModuleRegistry(dev.aether.fairplay.FairPlayPolicy.standard());
        boolean rejected = false;
        try {
            registry.register(new UndeclaredTheme());
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        TestSupport.assertTrue(rejected, "a theme module that forgets ModuleKind.THEME is rejected");
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

    /** A palette that declares the default kind, to prove the registry rejects it. */
    private static final class UndeclaredTheme extends AbstractModule implements ThemeModule {
        UndeclaredTheme() {
            super(ModuleMetadata.builder("theme.undeclared", "Undeclared Theme")
                .category(ModuleCategory.THEMES)
                .group(ThemeModule.GROUP)
                .build());
        }

        public ThemePalette palette() {
            return ThemePalettes.mono();
        }
    }
}
