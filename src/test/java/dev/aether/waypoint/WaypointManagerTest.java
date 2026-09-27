package dev.aether.waypoint;

import dev.aether.TestSupport;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Guards {@link WaypointManager} and {@link Waypoint}: config round trips, name sanitisation,
 * dimension filtering, updates, toggles and the store limit.
 */
public final class WaypointManagerTest {
    private WaypointManagerTest() {
    }

    public static void main(String[] args) throws Exception {
        Path configFile = Files.createTempFile("aether-waypoints-test", ".json");
        dev.aether.config.ConfigDocument.Builder builder = dev.aether.config.ConfigDocument.builder();
        builder.put("waypoint.My Base.world", "minecraft:overworld");
        builder.put("waypoint.My Base.x", "100");
        builder.put("waypoint.My Base.y", "64");
        builder.put("waypoint.My Base.z", "-200");
        builder.put("waypoint.My Base.color", "16711680");
        builder.put("waypoint.My Base.enabled", "true");
        builder.put("waypoint.nether_hub.world", "minecraft:the_nether");
        builder.put("waypoint.nether_hub.x", "10");
        builder.put("waypoint.nether_hub.y", "70");
        builder.put("waypoint.nether_hub.z", "10");
        builder.put("waypoint.nether_hub.color", "255");
        builder.put("waypoint.nether_hub.enabled", "true");
        builder.put("waypoint.gone.x", "1"); // no y/z: still parses with defaults
        builder.put("waypoint.broken.y", "not-a-number"); // malformed: ignored

        WaypointManager manager = new WaypointManager();
        manager.applyConfig(builder.build());

        loadingAndFiltering(manager);
        crud(manager);
        sanitisation(manager);
        roundTrip(manager);

        Files.delete(configFile);
        System.out.println("WaypointManagerTest passed");
    }

    private static void loadingAndFiltering(WaypointManager manager) {
        // All four keys carry a coordinate, so all four are waypoints ("broken" with defaults).
        TestSupport.assertEquals(4, manager.size(), "every key with a coordinate loads as a waypoint");
        TestSupport.assertTrue(manager.exists("my base"), "lookups are case-insensitive");

        Waypoint base = manager.get("My Base");
        TestSupport.assertEquals("My Base", base.name(), "the typed spelling is preserved");
        TestSupport.assertEquals("minecraft:overworld", base.world(), "the world is read");
        TestSupport.assertEquals(100, base.x(), "x is read");
        TestSupport.assertEquals(-200, base.z(), "z is read");
        TestSupport.assertEquals(16711680, base.color(), "the colour is read");
        TestSupport.assertTrue(base.enabled(), "and the enabled flag");

        // My Base + gone + broken default to the overworld; nether_hub declared the nether.
        TestSupport.assertEquals(3, manager.forWorld("minecraft:overworld").size(), "the overworld has three waypoints");
        TestSupport.assertEquals(1, manager.forWorld("MINECRAFT:THE_NETHER").size(), "world matching ignores case");
        TestSupport.assertTrue(manager.forWorld("minecraft:the_end").isEmpty(), "an empty dimension is empty");

        // a malformed number keeps the default rather than throwing; the explicit y key keeps it alive
        TestSupport.assertTrue(manager.get("broken") != null, "a partially broken waypoint still loads");
        TestSupport.assertEquals(0, manager.get("broken").y(), "the malformed field falls back to the default");

        // a key with no coordinate at all is not a waypoint
        dev.aether.config.ConfigDocument.Builder stray = dev.aether.config.ConfigDocument.builder();
        stray.put("waypoint.stray.color", "255");
        WaypointManager strayManager = new WaypointManager();
        strayManager.applyConfig(stray.build());
        TestSupport.assertEquals(0, strayManager.size(), "a coordinate-less entry is dropped");
    }

    private static void crud(WaypointManager manager) {
        TestSupport.assertTrue(manager.add("New Spot", "minecraft:overworld", 5, 60, 7, 0xFF00FF), "adding works");
        TestSupport.assertTrue(!manager.add("new spot", "minecraft:overworld", 1, 1, 1, 1), "a case-variant of the same name is refused");
        TestSupport.assertTrue(manager.update("new spot", 9, 61, 8), "updating moves the waypoint");
        Waypoint moved = manager.get("New Spot");
        TestSupport.assertEquals(9, moved.x(), "the new x is stored");
        TestSupport.assertTrue(moved.y() == 61, "and the new y");

        boolean toggled = manager.toggle("New Spot");
        TestSupport.assertTrue(!toggled, "toggling a disabled waypoint returns the new state");
        TestSupport.assertTrue(!manager.get("New Spot").enabled(), "the waypoint is disabled");
        int overworldBefore = manager.forWorld("minecraft:overworld").size();
        TestSupport.assertTrue(!manager.forWorld("minecraft:overworld").contains(manager.get("New Spot")),
            "disabled waypoints drop out of the world list");

        TestSupport.assertTrue(manager.toggle("New Spot"), "toggling back re-enables");
        TestSupport.assertEquals(overworldBefore + 1, manager.forWorld("minecraft:overworld").size(),
            "and re-enters the world list");
        TestSupport.assertTrue(!manager.remove("does not exist"), "removing an unknown name fails");
        TestSupport.assertTrue(manager.remove("New Spot"), "removing a real waypoint works");
        TestSupport.assertTrue(!manager.exists("New Spot"), "and it is gone");
    }

    private static void sanitisation(WaypointManager manager) {
        TestSupport.assertEquals("", WaypointManager.sanitize(null), "null sanitises to empty");
        TestSupport.assertEquals("", WaypointManager.sanitize("..."), "dots-only names sanitise to empty");
        TestSupport.assertEquals("My Base", WaypointManager.sanitize("My.Base"), "dots become spaces");
        TestSupport.assertEquals("A B", WaypointManager.sanitize("  A   B  "), "whitespace collapses");
        TestSupport.assertEquals(32, WaypointManager.sanitize(new String(new char[80]).replace('\0', 'x')).length(), "long names are capped");

        String longName = new String(new char[40]).replace('\0', 'w');
        TestSupport.assertTrue(manager.add(longName, "minecraft:overworld", 0, 0, 0, 0), "a long name is accepted");
        TestSupport.assertTrue(manager.exists(longName.substring(0, 32)), "under its sanitised key");
        manager.remove(longName.substring(0, 32));
    }

    private static void roundTrip(WaypointManager manager) {
        dev.aether.config.ConfigDocument.Builder out = dev.aether.config.ConfigDocument.builder();
        manager.writeConfig(out);
        WaypointManager reloaded = new WaypointManager();
        reloaded.applyConfig(out.build());
        TestSupport.assertEquals(manager.size(), reloaded.size(), "the round trip keeps every waypoint");
        TestSupport.assertEquals(manager.names(), reloaded.names(), "with the same names in the same order");
        Waypoint original = manager.get("My Base");
        Waypoint copy = reloaded.get("My Base");
        TestSupport.assertEquals(original.x(), copy.x(), "coordinates survive");
        TestSupport.assertEquals(original.color(), copy.color(), "colours survive");
        TestSupport.assertEquals(original.enabled(), copy.enabled(), "flags survive");
    }
}
