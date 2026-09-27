package dev.aether.screenshot;

import dev.aether.TestSupport;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Guards {@link ScreenshotStore} and {@link ScreenshotManager}: safe naming, metadata-only
 * listing, deletion rules, PNG encoding through the worker and the one-capture-at-a-time rule.
 */
public final class ScreenshotStoreTest {
    private ScreenshotStoreTest() {
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("aether-shots-test");
        ScreenshotStore store = new ScreenshotStore(directory);

        safeNaming(store);
        listingAndDeletion(store);
        pngRoundTrip(store);
        singleCaptureRule(store);

        System.out.println("ScreenshotStoreTest passed");
    }

    private static void safeNaming(ScreenshotStore store) throws Exception {
        TestSupport.assertTrue(store.safeResolve("aether_x.png") != null, "a plain name resolves");
        TestSupport.assertTrue(store.safeResolve("..") == null, "a parent traversal is rejected");
        TestSupport.assertTrue(store.safeResolve("a/b.png") == null, "a path separator is rejected");
        TestSupport.assertTrue(store.safeResolve("a\\b.png") == null, "a windows separator is rejected");
        TestSupport.assertTrue(store.safeResolve("") == null, "an empty name is rejected");
        TestSupport.assertTrue(store.safeResolve(null) == null, "a null name is rejected");
        TestSupport.assertTrue(store.safeResolve("sub\\..\\x.png") == null, "a smuggled traversal is rejected");

        Path unique = store.newScreenshotFile();
        TestSupport.assertTrue(unique.getFileName().toString().startsWith("aether_"), "screenshot files are prefixed");
        TestSupport.assertTrue(unique.getFileName().toString().endsWith(".png"), "and end in .png");
        TestSupport.assertTrue(!Files.exists(unique), "a fresh capture target does not exist yet");
    }

    private static void listingAndDeletion(ScreenshotStore store) throws Exception {
        Files.write(store.directory().resolve("aether_b.png"), new byte[] {1, 2, 3, 4});
        Files.write(store.directory().resolve("aether_a.png"), new byte[] {1, 2, 3, 4, 5, 6, 7, 8});
        Thread.sleep(10); // separate the timestamps
        Files.write(store.directory().resolve("notes.txt"), new byte[] {9});

        java.util.List<ScreenshotInfo> shots = store.list();
        TestSupport.assertEquals(2, shots.size(), "only PNG files are listed");
        TestSupport.assertTrue(shots.get(0).lastModified() >= shots.get(1).lastModified(), "the list is newest first");
        TestSupport.assertEquals(8L, shots.get(0).sizeBytes() == 8L ? 8L : shots.get(1).sizeBytes(), "sizes are read from disk");
        TestSupport.assertTrue(!shots.get(0).timestamp().isEmpty(), "a timestamp is formatted");

        TestSupport.assertTrue(store.delete("aether_a.png"), "deleting a real file works");
        TestSupport.assertTrue(!store.delete("aether_a.png"), "deleting it twice fails quietly");
        TestSupport.assertTrue(!store.delete("../escape.png"), "deleting outside the store is refused");
        TestSupport.assertEquals(1, store.list().size(), "the listing reflects the deletion");
    }

    private static void pngRoundTrip(ScreenshotStore store) throws Exception {
        // A 2x2 gradient: encodes through the real worker, decodes back through ImageIO.
        int[] pixels = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF};
        Path target = store.newScreenshotFile();
        ScreenshotStore.writePng(pixels, 2, 2, target);
        TestSupport.assertTrue(Files.exists(target), "the encoded PNG exists");
        TestSupport.assertTrue(Files.size(target) > 0, "and is not empty");

        java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(target.toFile());
        TestSupport.assertEquals(2, image.getWidth(), "the width round trips");
        TestSupport.assertEquals(2, image.getHeight(), "the height round trips");
        TestSupport.assertEquals(0xFFFF0000, image.getRGB(0, 0), "the first pixel round trips");
        TestSupport.assertEquals(0xFFFFFFFF, image.getRGB(1, 1), "the last pixel round trips");
        Files.delete(target);
    }

    private static void singleCaptureRule(ScreenshotManager manager, dev.aether.AetherClient client) {
        manager.encode(new int[] {0xFF000000}, 1, 1);
    }

    private static void singleCaptureRule(ScreenshotStore store) throws Exception {
        dev.aether.AetherClient client = new dev.aether.AetherClient(
            java.nio.file.Paths.get(store.directory().toString(), "config.json"));
        ScreenshotManager manager = new ScreenshotManager(client.modules(), store);
        int before = manager.store().list().size();
        TestSupport.assertTrue(manager.encode(new int[] {0xFF112233}, 1, 1), "the first capture is accepted");
        TestSupport.assertTrue(!manager.encode(new int[] {0xFF445566}, 1, 1), "a second concurrent capture is refused");
        TestSupport.assertTrue(manager.state().awaitCompletion(5000L), "the encode finishes");
        TestSupport.assertTrue(manager.state().lastError() == null, "a 1x1 encode does not fail");
        TestSupport.assertTrue(manager.state().lastSavedFile() != null, "the saved file is reported");
        TestSupport.assertEquals(before + 1, manager.store().list().size(), "exactly one screenshot landed on disk");
        manager.shutdown();
    }
}
