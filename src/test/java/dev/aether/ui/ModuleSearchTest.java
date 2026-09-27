package dev.aether.ui;

import dev.aether.AetherClient;
import dev.aether.TestSupport;
import dev.aether.module.ClientModule;
import dev.aether.module.ClientModule.ModuleCategory;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Guards {@link ModuleSearch}: filtering, fuzzy ranking, filter composition and the caching rule that
 * keeps a per-frame list from re-scoring every module.
 */
public final class ModuleSearchTest {
    private ModuleSearchTest() {
    }

    public static void main(String[] args) {
        AetherClient client = new AetherClient(Paths.get("build", "test-config", "search.json"));
        List<ClientModule> all = client.modules().all();

        queryFiltering(all);
        ranking(all);
        composedFilters(client, all);
        cachingRuns(client, all);
        scoringRules();
        System.out.println("ModuleSearchTest passed");
    }

    private static void queryFiltering(List<ClientModule> all) {
        ModuleSearch search = new ModuleSearch();
        search.source(all);
        TestSupport.assertEquals(Integer.valueOf(all.size()), Integer.valueOf(search.size()),
            "an empty query shows everything");

        search.query("reach");
        TestSupport.assertTrue(contains(search.results(), "hud.reach_display"),
            "a search finds the module by name");
        search.query("graphics.fullbright");
        TestSupport.assertTrue(contains(search.results(), "graphics.fullbright"),
            "a search finds the module by id");
        search.query("qqqqzzzz");
        TestSupport.assertTrue(search.results().isEmpty(), "a query that matches nothing returns nothing");
        search.query("");
        TestSupport.assertEquals(Integer.valueOf(all.size()), Integer.valueOf(search.size()),
            "clearing the query restores the full list");
    }

    private static void ranking(List<ClientModule> all) {
        ModuleSearch search = new ModuleSearch();
        search.source(all);
        search.query("zoom");
        List<ClientModule> results = search.results();
        TestSupport.assertTrue(!results.isEmpty(), "zoom matches at least one module");
        TestSupport.assertEquals("pvp.zoom", results.get(0).metadata().id(),
            "the module whose name is the query ranks first");
    }

    private static void composedFilters(AetherClient client, List<ClientModule> all) {
        ModuleSearch search = new ModuleSearch();
        search.source(all);
        search.category(ModuleCategory.PVP);
        TestSupport.assertTrue(search.size() > 0, "the PvP category has modules");
        for (ClientModule module : search.results()) {
            TestSupport.assertEquals(ModuleCategory.PVP, module.metadata().category(),
                "a category filter only returns that category");
        }

        search.query("sprint");
        TestSupport.assertEquals(Integer.valueOf(1), Integer.valueOf(search.size()),
            "category and query compose");
        TestSupport.assertEquals("pvp.toggle_sprint", search.results().get(0).metadata().id(),
            "and the composed result is the sprint module");

        search.category(null);
        search.favoritesOnly(true);
        search.query("");
        for (ClientModule module : search.results()) {
            TestSupport.assertTrue(module.metadata().favoriteByDefault(),
                "the favorites filter only returns favorites");
        }
        search.favoritesOnly(false);

        // Live is a state filter and has to follow the registry, not a cached snapshot.
        client.modules().setEnabled("graphics.fullbright", true);
        search.liveOnly(true);
        search.query("fullbright");
        TestSupport.assertEquals(Integer.valueOf(1), Integer.valueOf(search.size()),
            "an enabled module shows up in the live filter");
        client.modules().setEnabled("graphics.fullbright", false);
        search.invalidate();
        TestSupport.assertEquals(Integer.valueOf(0), Integer.valueOf(search.size()),
            "and disappears again once it is switched off");
        search.liveOnly(false);
    }

    private static void cachingRuns(AetherClient client, List<ClientModule> all) {
        ModuleSearch search = new ModuleSearch();
        search.source(all);
        search.query("hud");
        TestSupport.assertTrue(search.isDirty(), "a new query marks the results stale");
        List<ClientModule> first = search.results();
        TestSupport.assertTrue(!search.isDirty(), "reading the results clears the dirty flag");
        TestSupport.assertTrue(first == search.results(), "the same cached list is handed out");
        int cachedSize = search.size();

        search.query("hud");
        TestSupport.assertTrue(!search.isDirty(), "setting the same query does not invalidate");
        search.category(ModuleCategory.HUD);
        TestSupport.assertTrue(search.isDirty(), "changing the category invalidates");
        TestSupport.assertTrue(search.size() <= cachedSize, "the HUD filter is a subset of the query matches");
        search.invalidate();
        TestSupport.assertTrue(search.isDirty(), "an explicit invalidate is honoured");

        search.source(all);
        TestSupport.assertTrue(search.isDirty(), "replacing the source invalidates once");
        search.results();
        search.source(all);
        TestSupport.assertTrue(!search.isDirty(), "re-setting the identical list does not");
    }

    private static void scoringRules() {
        TestSupport.assertTrue(ModuleSearch.score("zoom", "zoom") > ModuleSearch.score("zoom", "z"),
            "an exact match scores above a partial one");
        TestSupport.assertEquals(-1, ModuleSearch.score("fullbright", "zzz"),
            "a query that does not fit scores -1");
        TestSupport.assertEquals(0, ModuleSearch.score("anything", ""), "an empty query scores 0");
        List<ClientModule> empty = new ArrayList<ClientModule>();
        ModuleSearch search = new ModuleSearch();
        search.source(empty);
        TestSupport.assertEquals(Integer.valueOf(0), Integer.valueOf(search.size()),
            "an empty registry searches cleanly");
    }

    private static boolean contains(List<ClientModule> modules, String id) {
        for (ClientModule module : modules) {
            if (module.metadata().id().equals(id)) {
                return true;
            }
        }
        return false;
    }
}
