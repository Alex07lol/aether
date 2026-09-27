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

/**
 * Owns the selectable cosmetics and which asset fills each slot.
 * <p>
 * Slots are per {@link CosmeticType}: a cape and a halo are chosen independently, and each
 * slot has a built-in fallback so a renderer always has something to draw when the matching
 * module is on. The cape slot stays exposed through {@link #selected()} for the legacy
 * single-selection callers.
 */
public final class CosmeticLibrary {
    private static final CosmeticType DEFAULT_SLOT = CosmeticType.STATIC_CAPE;

    private final Path storageDirectory;
    private final Map<String, CosmeticAsset> assets = new LinkedHashMap<String, CosmeticAsset>();
    private final Map<CosmeticType, String> selection = new LinkedHashMap<CosmeticType, String>();
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
        // Selections survive a reload as long as their asset is still on disk.
        java.util.Iterator<Map.Entry<CosmeticType, String>> slots = selection.entrySet().iterator();
        while (slots.hasNext()) {
            if (!assets.containsKey(slots.next().getValue())) {
                slots.remove();
            }
        }
        if (selectedId == null || !assets.containsKey(selectedId)) {
            CosmeticAsset cape = firstBuiltIn(DEFAULT_SLOT);
            selectedId = cape == null ? null : cape.id();
        }
        if (selectedId != null) {
            selection.put(DEFAULT_SLOT, selectedId);
        }
        for (CosmeticType type : CosmeticType.values()) {
            if (!selection.containsKey(type)) {
                CosmeticAsset fallback = firstBuiltIn(type);
                if (fallback != null) {
                    selection.put(type, fallback.id());
                }
            }
        }
    }

    public List<CosmeticAsset> all() {
        return Collections.unmodifiableList(new ArrayList<CosmeticAsset>(assets.values()));
    }

    public CosmeticAsset selected() {
        return selectedId == null ? null : assets.get(selectedId);
    }

    /**
     * @return the asset filling the given slot, or {@code null} when nothing of that type
     *     is available yet. Built-ins register on {@link #load()}, so this is normally
     *     non-null for every slot a module can render.
     */
    public CosmeticAsset selectedFor(CosmeticType type) {
        if (type == null) {
            return null;
        }
        String id = selection.get(type);
        CosmeticAsset asset = id == null ? null : assets.get(id);
        return asset != null ? asset : (type == DEFAULT_SLOT ? selected() : null);
    }

    /**
     * @return the asset a renderer should draw for this slot: the selected one, else the
     *     first built-in of that type, else {@code null} when the slot has nothing.
     */
    public CosmeticAsset effective(CosmeticType type) {
        CosmeticAsset chosen = selectedFor(type);
        if (chosen != null) {
            return chosen;
        }
        CosmeticAsset fallback = firstBuiltIn(type);
        if (fallback == null) {
            return null;
        }
        selection.put(type, fallback.id());
        return fallback;
    }

    /** Selects a cosmetic into the slot that matches its own type. */
    public void select(String id) {
        if (!assets.containsKey(id)) {
            throw new IllegalArgumentException("Unknown cosmetic: " + id);
        }
        CosmeticAsset asset = assets.get(id);
        selection.put(asset.type(), asset.id());
        if (asset.type() == DEFAULT_SLOT) {
            selectedId = asset.id();
        }
    }

    /** @return every asset that can fill the given slot. */
    public List<CosmeticAsset> forType(CosmeticType type) {
        List<CosmeticAsset> matches = new ArrayList<CosmeticAsset>();
        for (CosmeticAsset asset : assets.values()) {
            if (asset.type() == type) {
                matches.add(asset);
            }
        }
        return Collections.unmodifiableList(matches);
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
        put(asset);
        // Importing a cape selects it into the cape slot so it is worn immediately.
        select(id);
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
            selection.put(DEFAULT_SLOT, configured);
        }
        for (CosmeticType type : CosmeticType.values()) {
            String stored = document.get(slotKey(type), null);
            if (stored != null && assets.containsKey(stored)) {
                selection.put(type, stored);
            }
        }
    }

    public void writeConfig(ConfigDocument.Builder builder) {
        if (selectedId != null) {
            // Kept for configs written before slots existed.
            builder.put("cosmetics.selected", selectedId);
        }
        for (Map.Entry<CosmeticType, String> entry : selection.entrySet()) {
            builder.put(slotKey(entry.getKey()), entry.getValue());
        }
    }

    private static String slotKey(CosmeticType type) {
        return "cosmetics." + type.name().toLowerCase(Locale.ENGLISH);
    }

    private CosmeticAsset firstBuiltIn(CosmeticType type) {
        for (CosmeticAsset asset : assets.values()) {
            if (asset.builtIn() && asset.type() == type) {
                return asset;
            }
        }
        return null;
    }

    public Path storageDirectory() {
        return storageDirectory;
    }

    public Path importDirectory() {
        return storageDirectory.resolve("imports");
    }

    /** Registers the procedural catalogue: every slot has at least one asset to draw. */
    private void registerBuiltIns() {
        put(new CosmeticAsset("builtin.frost_cape", "Aether Frost Cape", CosmeticType.STATIC_CAPE, null, true,
            0xFF9FD8FF, 0xFF2C5C93));
        put(new CosmeticAsset("builtin.ember_cape", "Ember Cape", CosmeticType.STATIC_CAPE, null, true,
            0xFFFF9A4D, 0xFF7A2B0A));
        put(new CosmeticAsset("builtin.frost_wings", "Frost Wings", CosmeticType.WINGS, null, true,
            0xFFDFF3FF, 0xFF3E8CD6));
        put(new CosmeticAsset("builtin.ember_wings", "Ember Wings", CosmeticType.WINGS, null, true,
            0xFFFFD9A8, 0xFFB4491A));
        put(new CosmeticAsset("builtin.sky_halo", "Sky Halo", CosmeticType.HALO, null, true,
            0xFFB8ECFF, 0x66FFFFFF));
        put(new CosmeticAsset("builtin.gold_halo", "Golden Halo", CosmeticType.HALO, null, true,
            0xFFFFE08A, 0x66FFD75E));
        put(new CosmeticAsset("builtin.aether_hat", "Aether Cap", CosmeticType.HAT, null, true,
            0xFF2E3B58, 0xFFFFD75E));
        put(new CosmeticAsset("builtin.cloud_trail", "Cloud Trail", CosmeticType.TRAIL, null, true,
            0xCCFFFFFF, 0x33FFFFFF));
        put(new CosmeticAsset("builtin.spark_trail", "Spark Trail", CosmeticType.TRAIL, null, true,
            0xFFFFF0A0, 0x44FFD34D));
    }

    private void put(CosmeticAsset asset) {
        assets.put(asset.id(), asset);
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
