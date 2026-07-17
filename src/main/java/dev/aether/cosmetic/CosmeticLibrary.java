package dev.aether.cosmetic;

import dev.aether.config.ConfigDocument;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CosmeticLibrary {
    private final Path storageDirectory;
    private final Map<String, CosmeticAsset> assets = new LinkedHashMap<String, CosmeticAsset>();
    private String selectedId;

    public CosmeticLibrary(Path storageDirectory) {
        if (storageDirectory == null) {
            throw new IllegalArgumentException("Cosmetic storage directory is required.");
        }
        this.storageDirectory = storageDirectory;
    }

    public void load() throws IOException {
        assets.clear();
        registerBuiltIns();
        Files.createDirectories(storageDirectory);
        Files.createDirectories(importDirectory());

        List<Path> localFiles = new ArrayList<Path>();
        java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(storageDirectory, "*.png");
        try {
            for (Path path : stream) {
                localFiles.add(path);
            }
        } finally {
            stream.close();
        }
        Collections.sort(localFiles);
        for (Path path : localFiles) {
            CosmeticValidationResult result = validateCapePng(path);
            if (result.valid()) {
                String id = "local." + sanitize(stripExtension(path.getFileName().toString()));
                assets.put(id, new CosmeticAsset(id, stripExtension(path.getFileName().toString()), CosmeticType.STATIC_CAPE, path, false));
            }
        }
        if (selectedId == null || !assets.containsKey(selectedId)) {
            selectedId = "builtin.frost_cape";
        }
    }

    public List<CosmeticAsset> all() {
        return Collections.unmodifiableList(new ArrayList<CosmeticAsset>(assets.values()));
    }

    public CosmeticAsset selected() {
        return selectedId == null ? null : assets.get(selectedId);
    }

    public void select(String id) {
        if (!assets.containsKey(id)) {
            throw new IllegalArgumentException("Unknown cosmetic: " + id);
        }
        selectedId = id;
    }

    public CosmeticValidationResult importCapePng(Path source) throws IOException {
        CosmeticValidationResult validation = validateCapePng(source);
        if (!validation.valid()) {
            return validation;
        }

        Files.createDirectories(storageDirectory);
        String baseName = sanitize(stripExtension(source.getFileName().toString()));
        String fileName = baseName + "-" + System.currentTimeMillis() + ".png";
        Path destination = storageDirectory.resolve(fileName);
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

        String id = "local." + sanitize(stripExtension(fileName));
        CosmeticAsset asset = new CosmeticAsset(id, stripExtension(source.getFileName().toString()), CosmeticType.STATIC_CAPE, destination, false);
        assets.put(id, asset);
        selectedId = id;
        return CosmeticValidationResult.valid(asset.name() + " imported.");
    }

    public CosmeticValidationResult importNewestDroppedCape() throws IOException {
        Files.createDirectories(importDirectory());
        Path newest = null;
        long newestModified = Long.MIN_VALUE;
        java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(importDirectory(), "*.png");
        try {
            for (Path path : stream) {
                long modified = Files.getLastModifiedTime(path).toMillis();
                if (newest == null || modified > newestModified) {
                    newest = path;
                    newestModified = modified;
                }
            }
        } finally {
            stream.close();
        }
        if (newest == null) {
            return CosmeticValidationResult.invalid("Drop a PNG cape into " + importDirectory().toString() + " first.");
        }
        return importCapePng(newest);
    }

    public void applyConfig(ConfigDocument document) {
        String configured = document.get("cosmetics.selected", selectedId);
        if (configured != null && assets.containsKey(configured)) {
            selectedId = configured;
        }
    }

    public void writeConfig(ConfigDocument.Builder builder) {
        if (selectedId != null) {
            builder.put("cosmetics.selected", selectedId);
        }
    }

    public Path storageDirectory() {
        return storageDirectory;
    }

    public Path importDirectory() {
        return storageDirectory.resolve("imports");
    }

    private void registerBuiltIns() {
        assets.put("builtin.frost_cape", new CosmeticAsset("builtin.frost_cape", "Aether Frost Cape", CosmeticType.STATIC_CAPE, null, true));
        assets.put("builtin.sky_halo", new CosmeticAsset("builtin.sky_halo", "Sky Halo", CosmeticType.HALO, null, true));
        assets.put("builtin.cloud_trail", new CosmeticAsset("builtin.cloud_trail", "Cloud Trail", CosmeticType.TRAIL, null, true));
    }

    private static CosmeticValidationResult validateCapePng(Path source) throws IOException {
        if (source == null || !Files.isRegularFile(source)) {
            return CosmeticValidationResult.invalid("Choose an existing PNG file.");
        }
        String name = source.getFileName().toString().toLowerCase(Locale.ENGLISH);
        if (!name.endsWith(".png")) {
            return CosmeticValidationResult.invalid("Only PNG capes are supported in this build.");
        }
        BufferedImage image = ImageIO.read(source.toFile());
        if (image == null) {
            return CosmeticValidationResult.invalid("The selected file is not a readable PNG image.");
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width < 32 || height < 16) {
            return CosmeticValidationResult.invalid("Cape image is too small. Minimum size is 32x16.");
        }
        if (width > 4096 || height > 4096) {
            return CosmeticValidationResult.invalid("Cape image is too large. Maximum size is 4096x4096.");
        }
        if (!(width == height * 2 || width == height)) {
            return CosmeticValidationResult.invalid("Cape dimensions must be 2:1 or square for high-resolution cape layouts.");
        }
        return CosmeticValidationResult.valid("PNG cape is valid.");
    }

    private static String stripExtension(String value) {
        int dot = value.lastIndexOf('.');
        return dot < 0 ? value : value.substring(0, dot);
    }

    private static String sanitize(String value) {
        String lower = value.toLowerCase(Locale.ENGLISH);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                out.append(c);
            } else if (out.length() == 0 || out.charAt(out.length() - 1) != '_') {
                out.append('_');
            }
        }
        if (out.length() == 0) {
            return "cosmetic";
        }
        return out.toString();
    }
}
