package dev.aether.remap;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The build's remapping step: rewrites compiled classes from development names into runtime names
 * using an SRG mapping file, and reports what it could not map.
 * <p>
 * This is Aether's replacement for ForgeGradle's {@code reobf} task. It runs after the adapter is
 * compiled and before the jar is assembled, so the artifact that ships contains names a production
 * Minecraft 1.8.9 client actually has.
 *
 * <h3>Usage</h3>
 * <pre>
 * java dev.aether.remap.RemapTool \
 *     --mappings build/mappings/mcp-srg.srg \
 *     --input build/classes --input build/forge189-classes \
 *     --output build/remapped \
 *     [--prefix net/minecraft/] [--report build/reports/unmapped.txt] [--require-complete]
 * </pre>
 * Exit codes: {@code 0} when every game reference was mapped, {@code 2} when references remain that
 * the mapping file does not cover and {@code --require-complete} was passed, {@code 1} on a usage or
 * I/O failure. A release build should pass {@code --require-complete}: a jar with unmapped
 * references throws {@code NoSuchMethodError} in its first second of gameplay, and that is not a
 * failure worth discovering in a bug report.
 *
 * <p>Input and output are separate trees on purpose. Remapping in place would leave a build tree that
 * no longer matches the sources, so the next build would remap already-remapped names.
 */
public final class RemapTool {
    private final List<File> inputs = new ArrayList<File>();
    private final List<String> prefixes = new ArrayList<String>();
    private final List<String> exclusions = new ArrayList<String>();
    private File output;
    private File mappingsFile;
    private File reportFile;
    private boolean requireComplete;
    private boolean prefixesGiven;

    private RemapTool() {
        prefixes.addAll(McpSrgMappings.defaultPrefixes());
    }

    /** Entry point. See the class documentation for the flags and exit codes. */
    public static void main(String[] args) throws IOException {
        RemapTool tool = new RemapTool();
        try {
            tool.parse(args);
        } catch (IllegalArgumentException problem) {
            System.err.println("[Aether] " + problem.getMessage());
            System.exit(1);
            return;
        }
        System.exit(tool.run(System.out));
    }

    private void parse(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if ("--mappings".equals(arg)) {
                mappingsFile = new File(args[++i]);
            } else if ("--input".equals(arg)) {
                inputs.add(new File(args[++i]));
            } else if ("--output".equals(arg)) {
                output = new File(args[++i]);
            } else if ("--report".equals(arg)) {
                reportFile = new File(args[++i]);
            } else if ("--exclude".equals(arg)) {
                // Classes that are compiled but not shipped: the headless self-tests, and the
                // remapper itself. They link against the build's stubs, so they have no mapping and
                // must not be allowed to make a release look unmappable.
                exclusions.add(args[++i]);
            } else if ("--prefix".equals(arg)) {
                if (!prefixesGiven) {
                    prefixes.clear();
                    prefixesGiven = true;
                }
                prefixes.add(args[++i]);
            } else if ("--require-complete".equals(arg)) {
                requireComplete = true;
            } else {
                throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }
        if (mappingsFile == null || output == null || inputs.isEmpty()) {
            throw new IllegalArgumentException(
                "Required: --mappings <srg> --input <dir> [--input <dir> ...] --output <dir>");
        }
        if (!mappingsFile.isFile()) {
            throw new IllegalArgumentException("Mapping file not found: " + mappingsFile);
        }
    }

    /** Runs the remap and returns the process exit code. */
    public int run(PrintStream out) throws IOException {
        McpSrgMappings mappings = McpSrgMappings.read(mappingsFile);
        out.println("[Aether] Remapping with " + mappingsFile.getName() + ": " + mappings.memberCount()
            + " members, " + mappings.classCount() + " class renames"
            + (mappings.ambiguousNameKeyCount() > 0
                ? ", " + mappings.ambiguousNameKeyCount() + " name+descriptor keys ignored as ambiguous"
                : ""));

        List<File> classFiles = new ArrayList<File>();
        for (File input : inputs) {
            collect(input, classFiles);
        }
        if (classFiles.isEmpty()) {
            throw new IOException("No .class files under " + inputs);
        }

        // The hierarchy is built first: whether a declaration is a Minecraft implementation depends
        // on what the declaring class extends, and that is only knowable across the whole input set.
        Map<String, byte[]> loaded = new HashMap<String, byte[]>();
        Map<String, String> hierarchy = new HashMap<String, String>();
        Set<String> apiTypes = new HashSet<String>();
        for (File file : classFiles) {
            byte[] data = Files.readAllBytes(file.toPath());
            String name = ClassFileRemapper.internalName(data);
            if (excluded(name)) {
                continue;
            }
            loaded.put(name, data);
            hierarchy.put(name, ClassFileRemapper.superName(data));
            if (startsWithAny(name, prefixes)) {
                apiTypes.add(name);
            }
        }

        ClassFileRemapper remapper = new ClassFileRemapper(mappings, prefixes);
        List<McpSrgMappings.MemberReference> references = new ArrayList<McpSrgMappings.MemberReference>();
        int remappedReferences = 0;
        int remappedDeclarations = 0;
        int remappedClasses = 0;
        for (Map.Entry<String, byte[]> entry : loaded.entrySet()) {
            ClassFileRemapper.Outcome outcome = remapper.remap(entry.getValue(), hierarchy, apiTypes);
            write(new File(output, outcome.className + ".class"), outcome.bytes);
            references.addAll(outcome.references);
            remappedReferences += outcome.remappedReferences;
            remappedDeclarations += outcome.remappedDeclarations;
            remappedClasses += outcome.remappedClasses;
        }

        List<String> unmapped = McpSrgMappings.unmapped(references, prefixes);
        int developmentForm = 0;
        Set<String> distinctMembers = new HashSet<String>();
        for (McpSrgMappings.MemberReference reference : references) {
            if (!reference.mapped) {
                developmentForm++;
                distinctMembers.add(reference.owner + '.' + reference.name + ' ' + reference.descriptor);
            }
        }
        out.println("[Aether] " + loaded.size() + " classes: " + remappedReferences
            + " member references, " + remappedDeclarations + " declarations and " + remappedClasses
            + " class references remapped into runtime names");
        out.println("[Aether] " + references.size() + " references into " + String.join(", ", prefixes)
            + ": " + (references.size() - developmentForm) + " need no mapping (constructors and names"
            + " already in runtime form), " + developmentForm + " are in development form");
        out.println("[Aether] distinct Minecraft members needing a mapping: " + distinctMembers.size()
            + "; call sites that would fail on a production runtime: " + developmentForm);
        if (mappings.ambiguousNameKeyCount() > 0) {
            out.println("[Aether] " + mappings.ambiguousNameKeyCount() + " member name+descriptor keys are"
                + " shared by classes with different runtime names, so those declarations cannot be"
                + " renamed by name alone and are reported instead of guessed at:");
            for (String key : mappings.ambiguousKeys()) {
                out.println("    " + key);
            }
        }

        if (reportFile != null) {
            writeReport(reportFile, references, unmapped);
            out.println("[Aether] Unmapped-reference report: " + reportFile);
        }

        if (unmapped.isEmpty()) {
            out.println("[Aether] The artifact references only names that exist on a production runtime.");
            return 0;
        }
        out.println("[Aether] " + unmapped.size() + " game references have no mapping entry. They resolve"
            + " in a development runtime and throw NoSuchMethodError in a production one:");
        for (String line : unmapped) {
            out.println("    " + line);
        }
        if (requireComplete) {
            out.println("[Aether] Build failed: --require-complete was set.");
            return 2;
        }
        return 0;
    }

    private void writeReport(File file, List<McpSrgMappings.MemberReference> references, List<String> unmapped)
            throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        OutputStream stream = new FileOutputStream(file);
        PrintStream out = new PrintStream(stream, true, "UTF-8");
        try {
            out.println("Game references found in Aether's classes, after remapping.");
            out.println();
            List<String> lines = new ArrayList<String>();
            for (McpSrgMappings.MemberReference reference : references) {
                lines.add((reference.mapped ? "mapped   " : "UNMAPPED ") + reference);
            }
            java.util.Collections.sort(lines);
            for (String line : lines) {
                out.println(line);
            }
            out.println();
            out.println("Unmapped references, which fail on a production runtime:");
            for (String line : unmapped) {
                out.println("    " + line);
            }
        } finally {
            out.close();
        }
    }

    /** @return true when the internal name matches one of the {@code --exclude} text fragments. */
    private boolean excluded(String internalName) {
        for (String exclusion : exclusions) {
            if (internalName.contains(exclusion)) {
                return true;
            }
        }
        return false;
    }

    private static void collect(File root, List<File> classFiles) {
        if (root.isDirectory()) {
            File[] children = root.listFiles();
            if (children == null) {
                return;
            }
            Arrays.sort(children);
            for (File child : children) {
                collect(child, classFiles);
            }
        } else if (root.isFile() && root.getName().endsWith(".class")) {
            classFiles.add(root);
        }
    }

    private static void write(File file, byte[] data) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        OutputStream stream = new FileOutputStream(file);
        try {
            stream.write(data);
        } finally {
            stream.close();
        }
    }

    private static boolean startsWithAny(String name, List<String> prefixes) {
        if (name == null) {
            return false;
        }
        for (String prefix : prefixes) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}
