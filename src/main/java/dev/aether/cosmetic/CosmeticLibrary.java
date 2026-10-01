package dev.aether.cosmetic;

import dev.aether.config.ConfigDocument;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Owns the selectable cosmetics and which asset fills each slot.
 * <p>
 * Slots are per {@link CosmeticType}: a cape and a halo are chosen independently, and each
 * slot has a built‑in fallback so a renderer always has something to draw when the matching
 * module is on. The cape slot stays exposed through {@link #selected()} for the legacy
 * single‑selection callers.
 * <p>
 * Storage layout: PNGs dropped directly in the storage root or in {@code capes/} are capes
 * (back compatible with the old flat folder), and each other core type owns a subfolder
 * ({@code animated_capes/}, {@code wings/}, {@code hats/}, {@code halos/}, {@code trails/})
 * that is created on load and scanned the same way. A {@code <name>.png.meta} sidecar may
 * override the type and carries animation/transform metadata (see {@link CosmeticMetadata}).
 * {@link #rescan()} re-reads the folders without restarting the game, preserving the
 * current selection and favourites.
 */
public final class CosmeticLibrary {

    private static final CosmeticType DEFAULT_SLOT = CosmeticType.STATIC_CAPE;

    /** Folder name → type for the folders this build auto-creates and scans. */
    private static final Map<String, CosmeticType> CORE_FOLDERS = new LinkedHashMap<String, CosmeticType>();

    static {
        CORE_FOLDERS.put("capes", CosmeticType.STATIC_CAPE);
        CORE_FOLDERS.put("animated_capes", CosmeticType.ANIMATED_CAPE);
        CORE_FOLDERS.put("wings", CosmeticType.WINGS);
        CORE_FOLDERS.put("hats", CosmeticType.HAT);
        CORE_FOLDERS.put("halos", CosmeticType.HALO);
        CORE_FOLDERS.put("trails", CosmeticType.TRAIL);
    }

    private final Path storageDirectory;
    private final Map<String, CosmeticAsset> assets = new LinkedHashMap<String, CosmeticAsset>();
    private final Map<CosmeticType, String> selection = new LinkedHashMap<CosmeticType, String>();
    private final Set<String> favoriteIds = new LinkedHashSet<String>();
    private final Map<String, CosmeticMetadata> metadata = new HashMap<String, CosmeticMetadata>();
    private String selectedId;

    private final Map<String, BufferedImage> previewCache = new HashMap<String, BufferedImage>();
    private final Set<String> unreadableAssets = new HashSet<String>();
    private final Map<String, CosmeticAnimation> animationCache = new HashMap<String, CosmeticAnimation>();

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
        metadata.clear();
        registerBuiltIns();
        Files.createDirectories(storageDirectory);
        Files.createDirectories(importDirectory());
        for (String folder : CORE_FOLDERS.keySet()) {
            Files.createDirectories(storageDirectory.resolve(folder));
        }

        for (Path path : scanLocalFiles()) {
            CosmeticType declared = CosmeticMetadata.read(path).type();
            Path parent = path.getParent();
            boolean atRoot = parent != null && parent.equals(storageDirectory);
            CosmeticType type = declared != null ? declared : (atRoot ? DEFAULT_SLOT : typeOfFolder(parent));
            if (type == null) type = DEFAULT_SLOT;
            CosmeticValidationResult validation = validate(type, path);
            if (validation.valid()) {
                put(buildLocalAsset(path, type));
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
            CosmeticAsset built = firstBuiltIn(type);
            if (built != null && !selection.containsKey(type)) selection.put(type, built.id());
        }
        // Re-apply favourites onto the freshly built assets.
        for (String id : favoriteIds) {
            CosmeticAsset asset = assets.get(id);
            if (asset != null) asset.setFavorite(true);
        }
        previewCache.clear();
        animationCache.clear();
        unreadableAssets.clear();
    }

    /**
     * Re-read the storage folders after the user dropped/edited/removed PNGs.
     * Selection and favourites survive the rescan; preview and animation caches drop.
     */
    public void rescan() throws IOException {
        load();
    }

    /**
     * The root PNGs (capes, for back compatibility) plus every recognised type folder,
     * sorted so id assignment is stable across scans. Unrecognized folders (such as the
     * {@code imports/} drop folder) are skipped.
     */
    private List<Path> scanLocalFiles() throws IOException {
        List<Path> files = new ArrayList<Path>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(storageDirectory, "*.png")) {
            for (Path path : stream) files.add(path);
        }
        try (DirectoryStream<Path> dirs = Files.newDirectoryStream(storageDirectory)) {
            for (Path dir : dirs) {
                if (!Files.isDirectory(dir) || typeOfFolder(dir) == null) continue;
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.png")) {
                    for (Path path : stream) files.add(path);
                }
            }
        }
        Collections.sort(files);
        return files;
    }

    private CosmeticAsset buildLocalAsset(Path path, CosmeticType type) {
        CosmeticMetadata meta = CosmeticMetadata.read(path);
        String baseName = stripExtension(path.getFileName().toString());
        String id = localIdFor(path);
        String displayName = meta.name() != null ? meta.name() : baseName;
        boolean animated = meta.animated() || nameHeuristicAnimated(path.getFileName().toString());
        int frameCount = Math.max(0, meta.frameCount());
        int frameRate = Math.max(0, meta.frameRate());
        Path previewPath = extractPreviewPath(path);
        CosmeticAsset asset = new CosmeticAsset(id, displayName, type,
                path, false,
                0xFF52BEEB, 0xFF20476B,
                animated, frameCount, frameRate, previewPath);
        metadata.put(id, meta);
        return asset;
    }

    /**
     * Stable id for a local PNG: the sanitized file stem, prefixed {@code local.}. Two
     * files with the same name in different folders get a numeric suffix instead of
     * silently overwriting each other.
     */
    private String localIdFor(Path path) {
        String base = "local." + sanitize(stripExtension(path.getFileName().toString()));
        if (!assets.containsKey(base)) return base;
        for (int n = 2; ; n++) {
            String candidate = base + "_" + n;
            if (!assets.containsKey(candidate)) return candidate;
        }
    }

    public List<CosmeticAsset> all() {
        return Collections.unmodifiableList(new ArrayList<CosmeticAsset>(assets.values()));
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
        List<CosmeticAsset> matches = new ArrayList<CosmeticAsset>();
        for (CosmeticAsset asset : assets.values()) {
            if (asset.type() == type) matches.add(asset);
        }
        return Collections.unmodifiableList(matches);
    }

    /**
     * @return the sidecar metadata recorded for an asset; an empty instance when the
     * asset is built-in or has no {@code .meta} file.
     */
    public CosmeticMetadata metadataFor(String id) {
        CosmeticMetadata meta = metadata.get(id);
        return meta != null ? meta : CosmeticMetadata.read(null);
    }

    // -------------------------------------------------------------------
    // Validation and importing (type-generic; capes keep their stricter rules)
    // -------------------------------------------------------------------

    /**
     * Validate a PNG for the given cosmetic slot. All types share the readable-PNG and
     * size bounds; capes additionally keep the historic 32×16 minimum and 2:1-or-square
     * aspect rule, while shapes like wings/hats/halos/trails only need a square-ish
     * canvas of at least 16×16.
     */
    public CosmeticValidationResult validate(CosmeticType type, Path source) throws IOException {
        if (source == null || !Files.isRegularFile(source))
            return CosmeticValidationResult.invalid("Choose an existing PNG file.");
        String name = source.getFileName().toString().toLowerCase(Locale.ENGLISH);
        if (!name.endsWith(".png"))
            return CosmeticValidationResult.invalid("Only PNG cosmetic files are supported in this build.");
        BufferedImage image = ImageIO.read(source.toFile());
        if (image == null)
            return CosmeticValidationResult.invalid("The selected file is not a readable PNG image.");
        int width = image.getWidth();
        int height = image.getHeight();
        if (width > 4096 || height > 4096)
            return CosmeticValidationResult.invalid("Cosmetic image is too large. Maximum size is 4096x4096.");
        if (isCapeType(type)) {
            if (width < 32 || height < 16)
                return CosmeticValidationResult.invalid("Cape image is too small. Minimum size is 32x16.");
            if (!(width == height * 2 || width == height))
                return CosmeticValidationResult.invalid("Cape dimensions must be 2:1 or square for high-resolution cape layouts.");
        } else {
            if (width < 16 || height < 16)
                return CosmeticValidationResult.invalid("Cosmetic image is too small. Minimum size is 16x16.");
        }
        return CosmeticValidationResult.valid("PNG cosmetic is valid.");
    }

    private static boolean isCapeType(CosmeticType type) {
        return type == CosmeticType.STATIC_CAPE || type == CosmeticType.ANIMATED_CAPE
                || type == CosmeticType.CLOAK;
    }

    /** @return the folder a type's PNGs live in, e.g. {@code wings} for {@link CosmeticType#WINGS}. */
    public Path typeDirectory(CosmeticType type) {
        if (type == null) type = DEFAULT_SLOT;
        String folder = CORE_FOLDERS.containsValue(type) ? folderFor(type) : type.name().toLowerCase(Locale.ENGLISH) + "s";
        return storageDirectory.resolve(folder);
    }

    private static String folderFor(CosmeticType type) {
        for (Map.Entry<String, CosmeticType> entry : CORE_FOLDERS.entrySet()) {
            if (entry.getValue() == type) return entry.getKey();
        }
        return type.name().toLowerCase(Locale.ENGLISH) + "s";
    }

    /** @return the type a scan folder represents; null for unrecognized folders. */
    private static CosmeticType typeOfFolder(Path folder) {
        if (folder == null) return DEFAULT_SLOT;
        String name = folder.getFileName().toString().toLowerCase(Locale.ENGLISH);
        CosmeticType core = CORE_FOLDERS.get(name);
        if (core != null) return core;
        for (CosmeticType type : CosmeticType.values()) {
            if (folderFor(type).equals(name)) return type;
        }
        return null;
    }

    /**
     * Copy {@code source} into the folder of {@code type}, validate it and register it
     * as a selectable asset. A sibling {@code <name>.png.meta} sidecar is copied along.
     */
    public CosmeticValidationResult importPng(Path source, CosmeticType type) throws IOException {
        if (type == null) type = DEFAULT_SLOT;
        CosmeticValidationResult validation = validate(type, source);
        if (!validation.valid()) return validation;

        Path destinationFolder = typeDirectory(type);
        Files.createDirectories(destinationFolder);
        String baseName = sanitize(stripExtension(source.getFileName().toString()));
        String fileName = baseName + "-" + System.currentTimeMillis() + ".png";
        Path destination = destinationFolder.resolve(fileName);
        Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);

        Path sourceMeta = CosmeticMetadata.sidecarFor(source);
        if (sourceMeta != null && Files.isRegularFile(sourceMeta)) {
            Files.copy(sourceMeta, CosmeticMetadata.sidecarFor(destination), StandardCopyOption.REPLACE_EXISTING);
        }

        CosmeticAsset asset = buildLocalAsset(destination, type);
        put(asset);
        select(asset.id());
        return CosmeticValidationResult.valid(asset.name() + " imported.");
    }

    /** Import the newest PNG dropped into the shared {@code imports/} drop folder. */
    public CosmeticValidationResult importNewestDroppedCosmetic(CosmeticType type) throws IOException {
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
            return CosmeticValidationResult.invalid("Drop a PNG cosmetic into " + importDirectory().toString() + " first.");
        }
        return importPng(newest, type);
    }

    /** Legacy cape import; delegates to the generic importer with the cape slot. */
    public CosmeticValidationResult importCapePng(Path source) throws IOException {
        return importPng(source, DEFAULT_SLOT);
    }

    /** Legacy cape drop-import; delegates to the generic importer with the cape slot. */
    public CosmeticValidationResult importNewestDroppedCape() throws IOException {
        return importNewestDroppedCosmetic(DEFAULT_SLOT);
    }

    // -------------------------------------------------------------------
    // Config persistence
    // -------------------------------------------------------------------

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
                    favoriteIds.add(favId);
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

    // -------------------------------------------------------------------
    // Favourites
    // -------------------------------------------------------------------

    public void toggleFavorite(String id) {
        CosmeticAsset asset = assets.get(id);
        if (asset == null) return;
        asset.setFavorite(!asset.favorite());
        if (asset.favorite()) favoriteIds.add(id);
        else favoriteIds.remove(id);
    }

    public boolean isFavorite(String id) {
        CosmeticAsset asset = assets.get(id);
        return asset != null && asset.favorite();
    }

    public List<CosmeticAsset> favorites() {
        List<CosmeticAsset> favs = new ArrayList<CosmeticAsset>();
        for (CosmeticAsset asset : assets.values()) {
            if (asset.favorite()) favs.add(asset);
        }
        return Collections.unmodifiableList(favs);
    }

    // -------------------------------------------------------------------
    // Previews and animations
    // -------------------------------------------------------------------

    public BufferedImage getPreview(String id) throws IOException {
        if (unreadableAssets.contains(id)) return null;
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
            unreadableAssets.add(id);
            throw e;
        }
    }

    public CosmeticAnimation getAnimation(String id) {
        if (unreadableAssets.contains(id)) return null;
        return animationCache.computeIfAbsent(id, this::loadAnimation);
    }

    private CosmeticAnimation loadAnimation(String id) {
        CosmeticAsset asset = assets.get(id);
        if (asset == null || !asset.animated()) return null;

        List<BufferedImage> frames = new ArrayList<BufferedImage>();
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
                unreadableAssets.add(id);
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

    /** Naming-convention animation hint, kept for files written before sidecars existed. */
    private static boolean nameHeuristicAnimated(String fileName) {
        String lower = fileName.toLowerCase(Locale.ENGLISH);
        return lower.contains("anim") || lower.contains("frames");
    }

    /**
     * Thumbnail preference: a per-asset {@code <name>.preview.png} wins, then the legacy
     * folder-wide {@code preview.png} that older drops shared.
     */
    private static Path extractPreviewPath(Path path) {
        String base = stripExtension(path.getFileName().toString());
        Path own = path.resolveSibling(base + ".preview.png");
        if (Files.exists(own)) return own;
        Path preview = path.resolveSibling("preview.png");
        return Files.exists(preview) ? preview : null;
    }

    private static String slotKey(CosmeticType type) {
        return "cosmetics." + type.name().toLowerCase(Locale.ENGLISH);
    }
}
