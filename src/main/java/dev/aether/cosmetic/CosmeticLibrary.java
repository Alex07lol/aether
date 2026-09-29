package dev.aether.cosmetic;

import dev.aether.config.ConfigDocument;
import dev.aether.cosmetic.CosmeticAnimation;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Owns the selectable cosmetics and which asset fills each slot.
 * <p>
 * Slots are per {@link CosmeticType}: a cape and a halo are chosen independently, and each
 * slot has a built‑in fallback so a renderer always has something to draw when the matching
 * module is on. The cape slot stays exposed through {@link #selected()} for the legacy
 * single‑selection callers.
 */
public final class CosmeticLibrary {

    private static final CosmeticType DEFAULT_SLOT = CosmeticType.STATIC_CAPE;

    private final Path storageDirectory;
    private final Map<String, CosmeticAsset> assets = new LinkedHashMap<>();
    private final Map<CosmeticType, String> selection = new LinkedHashMap<>();
    private String selectedId;

    // New fields for advanced features
    private final Map<String, BufferedImage> previewCache = new HashMap<>();
    private final Set<String> unreadableCapes = new HashSet<>();
    private final Map<String, CosmeticAnimation> animationCache = new HashMap<>();

    public CosmeticLibrary(Path storageDirectory) {
        if (storageDirectory == null) {
            throw new IllegalArgumentException("Cosmetic storage directory is required.");
        }
        this.storageDirectory = storageDirectory;
    }

    /**
     * Load built‑ins, local PNGs and restore selection/favourites.
     */
    public void load() throws IOException {
        assets.clear();
        registerBuiltIns();
        Files.createDirectories(storageDirectory);
        Files.createDirectories(importDirectory());

        // Load local PNG capes
        List<Path> localFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(storageDirectory, "*.png")) {
            for (Path path : stream) localFiles.add(path);
        }
        Collections.sort(localFiles);
        for (Path path : localFiles) {
            CosmeticValidationResult validation = validateCapePng(path);
            if (validation.valid()) {
                String id = "local." + sanitize(stripExtension(path.getFileName().toString()));
                String name = stripExtension(path.getFileName().toString());
                // Detect if it's an animated cape (based on metadata file or naming convention)
                boolean animated = isAnimated(path);
                int frameCount = 0;
                int frameRate = 0;
                Path previewPath = null;

                if (animated) {
                    frameCount = extractFrameCount(path);
                    frameRate = extractFrameRate(path);
                    previewPath = extractPreviewPath(path);
                }

                CosmeticAsset asset = new CosmeticAsset(id, name, CosmeticType.STATIC_CAPE,
                        path, false,
                        0xFF52BEEB, 0xFF20476B,
                        animated, frameCount, frameRate, previewPath);
                assets.put(id, asset);
            }
        }

        // Preserve previous selections if possible
        for (Iterator<Map.Entry<CosmeticType, String>> it = selection.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<CosmeticType, String> e = it.next();
            if (!assets.containsKey(e.getValue())) it.remove();
        }
        if (selectedId == null || !assets.containsKey(selectedId)) {
            CosmeticAsset fallback = firstBuiltIn(DEFAULT_SLOT);
            selectedId = (fallback == null) ? null : fallback.id();
        }
        if (selectedId != null) selection.put(DEFAULT_SLOT, selectedId);
        for (CosmeticType type : CosmeticType.values()) {
            selection.computeIfAbsent(type, t -> {
                CosmeticAsset built = firstBuiltIn(t);
                return (built != null) ? built.id() : null;
            });
        }
    }

    public List<CosmeticAsset> all() {
        return Collections.unmodifiableList(new ArrayList<>(assets.values()));
    }

    public CosmeticAsset selected() {
        return selectedId == null ? null : assets.get(selectedId);
    }

    public CosmeticAsset selectedFor(CosmeticType type) {
        if (type == null) return null;
        String id = selection.get(type);
        CosmeticAsset a = (id != null) ? assets.get(id) : null;
        return (a != null) ? a : (type == DEFAULT_SLOT ? selected() : null);
    }

    public CosmeticAsset effective(CosmeticType type) {
        CosmeticAsset chosen = selectedFor(type);
        if (chosen != null) return chosen;
        CosmeticAsset fallback = firstBuiltIn(type);
        if (fallback != null) {
            selection.put(type, fallback.id());
            return fallback;
        }
        return null;
    }

    public void select(String id) {
        CosmeticAsset asset = assets.get(id);
        if (asset == null) throw new IllegalArgumentException("Unknown cosmetic: " + id);
        selection.put(asset.type(), asset.id());
        if (asset.type() == DEFAULT_SLOT) selectedId = asset.id();
    }

    /**
     * Empties a slot, so the player can wear nothing of that type. The stored selection
     * is removed and {@link #effective} returns null unless a built-in fallback exists
     * for the type; the in-world renderer and the preview both treat null as "draw
     * nothing", which is exactly what an emptied slot should look like.
     */
    public void clear(CosmeticType type) {
        if (type == null) return;
        selection.remove(type);
        if (type == DEFAULT_SLOT) selectedId = null;
    }

    /** @return true when the slot has no selection of its own. */
    public boolean isCleared(CosmeticType type) {
        return type != null && selection.get(type) == null;
    }

    public List<CosmeticAsset> forType(CosmeticType type) {
        List<CosmeticAsset> matches = new ArrayList<>();
        for (CosmeticAsset asset : assets.values()) {
            if (asset.type() == type) matches.add(asset);
        }
        return Collections.unmodifiableList(matches);
    }

    public CosmeticValidationResult importCapePng(Path source) throws IOException {
        CosmeticValidationResult validation = validateCapePng(source);
        if (!validation.valid()) return validation;

        Files.createDirectories(storageDirectory);
        String baseName = sanitize(stripExtension(source.getFileName().toString()));
        String fileName = baseName + "-" + System.currentTimeMillis() + ".png";
        Path destination = storageDirectory.resolve(fileName);
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

        String id = "local." + sanitize(stripExtension(fileName));
        String name = stripExtension(source.getFileName().toString());
        boolean animated = isAnimated(destination);
        int frameCount = 0;
        int frameRate = 0;
        Path previewPath = null;

        if (animated) {
            frameCount = extractFrameCount(destination);
            frameRate = extractFrameRate(destination);
            previewPath = extractPreviewPath(destination);
        }

        CosmeticAsset asset = new CosmeticAsset(id, name, CosmeticType.STATIC_CAPE,
                destination, false,
                0xFF52BEEB, 0xFF20476B,
                animated, frameCount, frameRate, previewPath);
        put(asset);
        select(id);
        return CosmeticValidationResult.valid(asset.name() + " imported.");
    }

    public CosmeticValidationResult importNewestDroppedCape() throws IOException {
        Files.createDirectories(importDirectory());
        Path newest = null;
        long newestModified = Long.MIN_VALUE;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(importDirectory(), "*.png")) {
            for (Path path : stream) {
                long modified = Files.getLastModifiedTime(path).toMillis();
                if (newest == null || modified > newestModified) {
                    newest = path;
                    newestModified = modified;
                }
            }
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
        // Load favourites
        for (String key : document.values().keySet()) {
            if (key.startsWith("cosmetics.favorite.") && document.getBoolean(key, false)) {
                String favId = key.substring("cosmetics.favorite.".length());
                if (assets.containsKey(favId)) {
                    assets.get(favId).setFavorite(true);
                }
            }
        }
    }

    public void writeConfig(ConfigDocument.Builder builder) {
        if (selectedId != null) {
            builder.put("cosmetics.selected", selectedId);
        }
        for (Map.Entry<CosmeticType, String> entry : selection.entrySet()) {
            builder.put(slotKey(entry.getKey()), entry.getValue());
        }
        for (CosmeticAsset asset : assets.values()) {
            if (asset.favorite()) {
                builder.putBoolean("cosmetics.favorite." + asset.id(), true);
            }
        }
    }

    public void toggleFavorite(String id) {
        CosmeticAsset asset = assets.get(id);
        if (asset != null) asset.setFavorite(!asset.favorite());
    }

    public boolean isFavorite(String id) {
        CosmeticAsset asset = assets.get(id);
        return asset != null && asset.favorite();
    }

    public List<CosmeticAsset> favorites() {
        List<CosmeticAsset> favs = new ArrayList<>();
        for (CosmeticAsset asset : assets.values()) {
            if (asset.favorite()) favs.add(asset);
        }
        return Collections.unmodifiableList(favs);
    }

    public BufferedImage getPreview(String id) throws IOException {
        if (unreadableCapes.contains(id)) return null;
        BufferedImage img = previewCache.get(id);
        if (img != null) return img;
        CosmeticAsset asset = assets.get(id);
        if (asset == null) return null;
        Path p = asset.previewPath() != null ? asset.previewPath() : asset.localFile();
        if (p == null) return null;
        try {
            img = ImageIO.read(p.toFile());
            if (img != null) previewCache.put(id, img);
            return img;
        } catch (IOException e) {
            unreadableCapes.add(id);
            throw e;
        }
    }

    public CosmeticAnimation getAnimation(String id) {
        if (unreadableCapes.contains(id)) return null;
        return animationCache.computeIfAbsent(id, this::loadAnimation);
    }

    private CosmeticAnimation loadAnimation(String id) {
        CosmeticAsset asset = assets.get(id);
        if (asset == null || !asset.animated()) return null;

        List<BufferedImage> frames = new ArrayList<>();
        Path base = asset.localFile() != null ? asset.localFile().getParent() : null;
        if (base == null) return null;

        String baseName = stripExtension(asset.localFile().getFileName().toString());
        for (int i = 0; i < asset.frameCount(); i++) {
            Path framePath = base.resolve(baseName + "_" + i + ".png");
            if (!Files.exists(framePath)) {
                // If frames are missing, fallback to static rendering
                return null;
            }
            try {
                frames.add(ImageIO.read(framePath.toFile()));
            } catch (IOException e) {
                unreadableCapes.add(id);
                return null;
            }
        }
        return new CosmeticAnimation(frames, asset.frameRate());
    }

    // -------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------
    private void registerBuiltIns() {
        put(new CosmeticAsset("builtin.frost_cape", "Aether Frost Cape",
                CosmeticType.STATIC_CAPE, null, true,
                0xFF9FD8FF, 0xFF2C5C93));
        // ... other built‑ins omitted for brevity
    }

    private void put(CosmeticAsset asset) { assets.put(asset.id(), asset); }

    private CosmeticAsset firstBuiltIn(CosmeticType type) {
        for (CosmeticAsset asset : assets.values())
            if (asset.builtIn() && asset.type() == type) return asset;
        return null;
    }

    public Path storageDirectory() { return storageDirectory; }
    public Path importDirectory()   { return storageDirectory.resolve("imports"); }

    private CosmeticValidationResult validateCapePng(Path source) throws IOException {
        if (source == null || !Files.isRegularFile(source))
            return CosmeticValidationResult.invalid("Choose an existing PNG file.");
        String name = source.getFileName().toString().toLowerCase(Locale.ENGLISH);
        if (!name.endsWith(".png"))
            return CosmeticValidationResult.invalid("Only PNG capes are supported in this build.");
        BufferedImage image = ImageIO.read(source.toFile());
        if (image == null)
            return CosmeticValidationResult.invalid("The selected file is not a readable PNG image.");
        int width = image.getWidth();
        int height = image.getHeight();
        if (width < 32 || height < 16)
            return CosmeticValidationResult.invalid("Cape image is too small. Minimum size is 32x16.");
        if (width > 4096 || height > 4096)
            return CosmeticValidationResult.invalid("Cape image is too large. Maximum size is 4096x4096.");
        if (!(width == height * 2 || width == height))
            return CosmeticValidationResult.invalid("Cape dimensions must be 2:1 or square for high-resolution cape layouts.");
        return CosmeticValidationResult.valid("PNG cape is valid.");
    }

    private static String stripExtension(String value) {
        int dot = value.lastIndexOf('.');
        return (dot < 0) ? value : value.substring(0, dot);
    }

    private static String sanitize(String value) {
        String lower = value.toLowerCase(Locale.ENGLISH);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) out.append(c);
            else if (out.length() == 0 || out.charAt(out.length() - 1) != '_') out.append('_');
        }
        if (out.length() == 0) return "cosmetic";
        return out.toString();
    }

    private static boolean isAnimated(Path path) {
        // Check for metadata file or naming convention
        Path meta = path.resolveSibling(path.getFileName().toString() + ".meta");
        if (Files.exists(meta)) return true;
        String name = path.getFileName().toString().toLowerCase();
        return name.contains("anim") || name.contains("frames");
    }

    private static int extractFrameCount(Path path) {
        // Read metadata file for frame count
        Path meta = path.resolveSibling(path.getFileName().toString() + ".meta");
        if (Files.exists(meta)) {
            try {
                List<String> lines = Files.readAllLines(meta);
                for (String line : lines) {
                    if (line.toLowerCase().startsWith("frames:")) {
                        return Integer.parseInt(line.substring(6).trim());
                    }
                }
            } catch (IOException ignored) {}
        }
        return 0; // default
    }

    private static int extractFrameRate(Path path) {
        Path meta = path.resolveSibling(path.getFileName().toString() + ".meta");
        if (Files.exists(meta)) {
            try {
                List<String> lines = Files.readAllLines(meta);
                for (String line : lines) {
                    if (line.toLowerCase().startsWith("fps:")) {
                        return Integer.parseInt(line.substring(3).trim());
                    }
                }
            } catch (IOException ignored) {}
        }
        return 0; // default
    }

    private static Path extractPreviewPath(Path path) {
        // Look for preview.png in the same directory
        Path preview = path.getParent().resolve("preview.png");
        return Files.exists(preview) ? preview : null;
    }

    private static String slotKey(CosmeticType type) {
        return "cosmetics." + type.name().toLowerCase(Locale.ENGLISH);
    }
}