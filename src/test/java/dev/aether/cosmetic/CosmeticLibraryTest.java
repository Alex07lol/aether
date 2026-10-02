package dev.aether.cosmetic;

import dev.aether.TestSupport;
import dev.aether.config.ConfigDocument;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CosmeticLibraryTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("aether-cosmetics");
        Path cape = Files.createTempFile("aether-cape", ".png");
        ImageIO.write(new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB), "png", cape.toFile());

        CosmeticLibrary library = new CosmeticLibrary(directory);
        library.load();

        // Legacy cape pathway stays intact.
        CosmeticValidationResult result = library.importCapePng(cape);
        TestSupport.assertTrue(result.valid(), "Valid PNG cape should import.");
        TestSupport.assertTrue(library.selected() != null, "Imported cape should become selected.");
        TestSupport.assertTrue(Files.isRegularFile(library.selected().localFile()), "Imported cape should be copied to storage.");

        ConfigDocument.Builder builder = ConfigDocument.builder();
        library.writeConfig(builder);
        ConfigDocument config = builder.build();

        CosmeticLibrary restored = new CosmeticLibrary(directory);
        restored.load();
        restored.applyConfig(config);
        TestSupport.assertTrue(restored.selected() != null, "Selected cosmetic should restore from config.");
        TestSupport.assertEquals(library.selected().id(), restored.selected().id(), "Selected cosmetic id should persist.");

        Path drop = library.importDirectory().resolve("dropped.png");
        Files.createDirectories(library.importDirectory());
        ImageIO.write(new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB), "png", drop.toFile());
        CosmeticValidationResult dropResult = library.importNewestDroppedCape();
        TestSupport.assertTrue(dropResult.valid(), "Newest dropped PNG should import.");

        // ── Generic importing: non-cape types land in their own folders ──────────
        Path wing = Files.createTempFile("aether-wing", ".png");
        ImageIO.write(new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB), "png", wing.toFile());
        CosmeticValidationResult wingResult = library.importPng(wing, CosmeticType.WINGS);
        TestSupport.assertTrue(wingResult.valid(), "Valid PNG wings should import.");
        // Built-ins share the WINGS slot (and carry no file), so find the imported
        // file-backed asset rather than assuming the import is the first entry.
        CosmeticAsset wingAsset = null;
        for (CosmeticAsset asset : library.forType(CosmeticType.WINGS)) {
            if (asset.localFile() != null) {
                wingAsset = asset;
            }
        }
        TestSupport.assertTrue(wingAsset != null, "The imported wing should be in the WINGS slot.");
        TestSupport.assertEquals(CosmeticType.WINGS, wingAsset.type(), "Imported wing should be typed WINGS.");
        TestSupport.assertEquals(library.typeDirectory(CosmeticType.WINGS),
                wingAsset.localFile().getParent(), "Wings should be stored in the wings folder.");
        TestSupport.assertTrue(Files.isDirectory(library.typeDirectory(CosmeticType.HALO)),
                "Core type folders should be created on load.");
        TestSupport.assertTrue(Files.isDirectory(library.typeDirectory(CosmeticType.TRAIL)),
                "Trail folder should be created on load.");

        // ── Validation rules per type ────────────────────────────────────────────
        Path badAspect = Files.createTempFile("aether-bad-aspect", ".png");
        ImageIO.write(new BufferedImage(50, 30, BufferedImage.TYPE_INT_ARGB), "png", badAspect.toFile());
        TestSupport.assertTrue(!library.validate(CosmeticType.STATIC_CAPE, badAspect).valid(),
                "Cape with a non 2:1, non-square aspect should be rejected.");
        TestSupport.assertTrue(library.validate(CosmeticType.HALO, badAspect).valid(),
                "Non-cape types accept arbitrary aspect ratios.");
        Path tiny = Files.createTempFile("aether-tiny", ".png");
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB), "png", tiny.toFile());
        TestSupport.assertTrue(!library.validate(CosmeticType.HALO, tiny).valid(),
                "8x8 is below the 16x16 minimum for non-cape cosmetics.");
        TestSupport.assertTrue(!library.importPng(tiny, CosmeticType.HAT).valid(),
                "Importing an undersized hat should fail without copying the file.");

        // ── Drop a PNG into a type folder and rescan without restarting ─────────
        Path haloDir = Files.createDirectories(directory.resolve("halos"));
        Path droppedHalo = haloDir.resolve("sun_halo.png");
        ImageIO.write(new BufferedImage(48, 48, BufferedImage.TYPE_INT_ARGB), "png", droppedHalo.toFile());
        library.rescan();
        boolean haloFound = false;
        for (CosmeticAsset asset : library.forType(CosmeticType.HALO)) {
            if ("sun_halo".equals(asset.name())) haloFound = true;
        }
        TestSupport.assertTrue(haloFound, "PNG dropped into halos/ should appear after rescan.");

        // Rescan must preserve selection and favourites.
        library.select(wingAsset.id());
        library.toggleFavorite(wingAsset.id());
        library.rescan();
        TestSupport.assertEquals(CosmeticType.WINGS, library.selectedFor(CosmeticType.WINGS).type(),
                "Selection should survive a rescan.");
        TestSupport.assertTrue(library.isFavorite(wingAsset.id()),
                "Favourite flag should survive a rescan.");

        // ...and favourites must round-trip through the config too.
        ConfigDocument.Builder favBuilder = ConfigDocument.builder();
        library.writeConfig(favBuilder);
        CosmeticLibrary favRestored = new CosmeticLibrary(directory);
        favRestored.load();
        favRestored.applyConfig(favBuilder.build());
        TestSupport.assertTrue(favRestored.isFavorite(wingAsset.id()),
                "Favourite should restore from config.");

        // ── .meta sidecar: legacy prefix form + key=value form ───────────────────
        Path hatDir = Files.createDirectories(directory.resolve("hats"));
        Path fancyHat = hatDir.resolve("fancy_hat.png");
        ImageIO.write(new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB), "png", fancyHat.toFile());
        Files.write(fancyHat.resolveSibling("fancy_hat.png.meta"),
                ("# comment line\n"
                        + "name=Fancy Hat\n"
                        + "scale=1.2\n"
                        + "offset_y=-0.5\n"
                        + "rotation_z=15\n"
                        + "frames: 8\n"
                        + "fps: 20\n").getBytes(StandardCharsets.UTF_8));
        library.rescan();
        CosmeticAsset parsed = null;
        for (CosmeticAsset asset : library.forType(CosmeticType.HAT)) {
            if ("fancy_hat".equals(asset.name()) || "Fancy Hat".equals(asset.name())) parsed = asset;
        }
        TestSupport.assertTrue(parsed != null, "Hats folder PNG should be found on rescan.");
        TestSupport.assertEquals("Fancy Hat", parsed.name(), "meta name= should override the file name.");
        TestSupport.assertTrue(parsed.animated(), "frames/fps metadata should mark the asset animated.");
        TestSupport.assertEquals(Integer.valueOf(8), Integer.valueOf(parsed.frameCount()), "Legacy frames: should parse.");
        TestSupport.assertEquals(Integer.valueOf(20), Integer.valueOf(parsed.frameRate()), "Legacy fps: should parse.");
        CosmeticMetadata meta = library.metadataFor(parsed.id());
        TestSupport.assertEquals(Double.valueOf(1.2), meta.scale(), "meta scale= should parse.");
        TestSupport.assertEquals(Double.valueOf(-0.5), meta.offsetY(), "meta offset_y= should parse.");
        TestSupport.assertEquals(Double.valueOf(15.0), meta.rotationZ(), "meta rotation_z= should parse.");
        TestSupport.assertTrue(meta.animated(), "Metadata should report animated for frames>0.");

        // ── .meta type override: a root PNG can declare its own slot ────────────
        Path rootWing = directory.resolve("clared_wing.png");
        ImageIO.write(new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB), "png", rootWing.toFile());
        Files.write(rootWing.resolveSibling("clared_wing.png.meta"),
                "type=wings\n".getBytes(StandardCharsets.UTF_8));
        library.rescan();
        boolean overrideFound = false;
        for (CosmeticAsset asset : library.forType(CosmeticType.WINGS)) {
            if ("clared_wing".equals(asset.name())) overrideFound = true;
        }
        TestSupport.assertTrue(overrideFound, "meta type=wings should move a root PNG into the wings slot.");
    }
}
