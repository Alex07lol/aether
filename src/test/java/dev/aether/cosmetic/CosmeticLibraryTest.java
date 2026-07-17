package dev.aether.cosmetic;

import dev.aether.TestSupport;
import dev.aether.config.ConfigDocument;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CosmeticLibraryTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("aether-cosmetics");
        Path cape = Files.createTempFile("aether-cape", ".png");
        ImageIO.write(new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB), "png", cape.toFile());

        CosmeticLibrary library = new CosmeticLibrary(directory);
        library.load();
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
    }
}
