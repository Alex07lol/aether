package dev.aether.remap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A mapping table between development (MCP) names and runtime (SRG) names, read from an SRG file.
 * <p>
 * This is the input Aether's release build was missing. ForgeGradle generates the same file
 * ({@code build/mappings/mcp-srg.srg} or the searge.srg used by Mixin) and feeds it to its
 * {@code reobf} task; here it drives {@link ClassFileRemapper} instead, because Aether's build has
 * no ForgeGradle to do the remapping for it.
 *
 * <h3>Format</h3>
 * The SRG format is line oriented, one entry per line, with the kind first:
 * <pre>
 * PK: package/prefix                        (ignored: package renames are not a member remap)
 * CL: mcp/Class srg/Class                   (class renames; identical on 1.8.9, kept for completeness)
 * FD: owner/name srgOwner/srgName           (field)
 * MD: owner/name desc srgOwner/srgName desc (method; descriptors may repeat the MCP one)
 * </pre>
 * Lines that do not parse are ignored rather than aborting the build: mapping files from different
 * ForgeGradle versions carry extra comment lines, and a malformed line must not silently disable the
 * remap. What cannot be ignored is a member the build needs and the table does not contain - see
 * {@link #lookupMember} and {@link #unmapped}.
 *
 * <h3>Why lookups are ambiguity-checked</h3>
 * A member is identified by owner, name and descriptor. Two different Minecraft classes can declare
 * the same (name, descriptor) pair and receive different SRG names - {@code getName()} is declared
 * on many of them - so a name+descriptor lookup is only trusted when the table agrees on a single
 * target. Anything else would rename a call to the wrong method, which is worse than not renaming it.
 */
public final class McpSrgMappings {
    /** class-name -> class-name, from {@code CL:} lines. */
    private final Map<String, String> classes = new HashMap<String, String>();
    /** "owner\u0000name\u0000desc" -> srgName. */
    private final Map<String, String> membersByOwner = new HashMap<String, String>();
    /** "name\u0000desc" -> srgName, only where the whole table agrees on one target. */
    private final Map<String, String> membersByName = new HashMap<String, String>();
    /** "name\u0000desc" -> every target the table gives it, to detect disagreement. */
    private final Map<String, Set<String>> memberTargetsByName = new HashMap<String, Set<String>>();

    private final List<String> ambiguousKeys = new ArrayList<String>();
    private int classCount;
    private int memberCount;

    private McpSrgMappings() {
    }

    /**
     * Reads an SRG mapping file.
     *
     * @param file the mapping file, in the SRG format ForgeGradle and Mixin both consume.
     * @throws IOException when the file cannot be read.
     */
    public static McpSrgMappings read(File file) throws IOException {
        InputStream stream = new FileInputStream(file);
        try {
            return read(stream);
        } finally {
            stream.close();
        }
    }

    /** Reads an SRG mapping file from an already-open stream, which the tests use. */
    public static McpSrgMappings read(InputStream stream) throws IOException {
        McpSrgMappings mappings = new McpSrgMappings();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, Charset.forName("UTF-8")));
        String line;
        while ((line = reader.readLine()) != null) {
            mappings.parseLine(line.trim());
        }
        mappings.finish();
        return mappings;
    }

    private void parseLine(String line) {
        if (line.length() == 0 || line.charAt(0) == '#') {
            return;
        }
        if (line.startsWith("CL:")) {
            String[] parts = split(line.substring(3).trim(), 2);
            if (parts != null && !parts[0].equals(parts[1])) {
                classes.put(parts[0], parts[1]);
                classCount++;
            }
            return;
        }
        if (line.startsWith("MD:") || line.startsWith("FD:")) {
            boolean method = line.startsWith("MD:");
            String[] parts = split(line.substring(3).trim(), method ? 4 : 2);
            if (parts == null) {
                return;
            }
            String owner = owner(parts[0]);
            String name = simple(parts[0]);
            String descriptor = method ? parts[1] : "";
            String srgName = simple(method ? parts[2] : parts[1]);
            if (owner == null || name == null || srgName == null || name.equals(srgName)) {
                return;
            }
            membersByOwner.put(owner + '\u0000' + name + '\u0000' + descriptor, srgName);
            String key = name + '\u0000' + descriptor;
            Set<String> targets = memberTargetsByName.get(key);
            if (targets == null) {
                targets = new LinkedHashSet<String>();
                memberTargetsByName.put(key, targets);
            }
            targets.add(srgName);
            memberCount++;
        }
    }

    /** Keeps only the name+descriptor keys the whole table agrees on, and remembers the rest. */
    private void finish() {
        for (Map.Entry<String, Set<String>> entry : memberTargetsByName.entrySet()) {
            if (entry.getValue().size() == 1) {
                membersByName.put(entry.getKey(), entry.getValue().iterator().next());
            } else {
                ambiguousKeys.add(entry.getKey().replace('\u0000', ' ') + " -> " + entry.getValue());
            }
        }
        Collections.sort(ambiguousKeys);
    }

    /**
     * @return the name+descriptor keys that several classes map to different runtime names. A
     *     declaration matching one of these cannot be renamed safely - the table does not say which
     *     class's name applies - so the remapper leaves it and reports it: a silently unrenamed
     *     override is precisely the failure that makes a screen inert in production.
     */
    public List<String> ambiguousKeys() {
        return Collections.unmodifiableList(ambiguousKeys);
    }

    private static String[] split(String text, int expected) {
        String[] parts = text.split("\\s+");
        return parts.length < expected ? null : parts;
    }

    private static String owner(String qualified) {
        int slash = qualified.lastIndexOf('/');
        return slash < 0 ? null : qualified.substring(0, slash);
    }

    private static String simple(String qualified) {
        int slash = qualified.lastIndexOf('/');
        return slash < 0 ? qualified : qualified.substring(slash + 1);
    }

    /**
     * Looks up a member reference.
     *
     * @param owner the internal name of the declaring class.
     * @param name the development name the reference uses.
     * @param descriptor the member descriptor, or the empty string for a field.
     * @return the runtime name, or null when the table has no usable answer.
     */
    public String lookupMember(String owner, String name, String descriptor) {
        String exact = membersByOwner.get(owner + '\u0000' + name + '\u0000' + descriptor);
        if (exact != null) {
            return exact;
        }
        return membersByName.get(name + '\u0000' + descriptor);
    }

    /**
     * Looks up a field reference. Fields carry no descriptor in the SRG table - a field is named by
     * its owner and name alone - so this is a separate lookup rather than a descriptor that happens
     * to be empty.
     *
     * @return the runtime name, or null when the table has no usable answer.
     */
    public String lookupField(String owner, String name) {
        return membersByOwner.get(owner + '\u0000' + name + '\u0000');
    }

    /**
     * Looks up a field declaration - the field a mixin shadows.
     *
     * @return the runtime name, or null when the table gives the name no single answer.
     */
    public String lookupFieldDeclaration(String name) {
        return membersByName.get(name + '\u0000');
    }

    /**
     * Looks up a member declaration - a method a mod class implements for Minecraft, or a field a
     * mixin shadows.
     * <p>
     * The owner is deliberately not part of this lookup: the declaring class is an Aether class, and
     * a method whose name and descriptor match a Minecraft entry is that entry's implementation. That
     * is how {@code reobf} keeps a mod's overrides working after Minecraft's own names change.
     *
     * @return the runtime name, or null when the table gives the (name, descriptor) pair no answer.
     */
    public String lookupDeclaration(String name, String descriptor) {
        return membersByName.get(name + '\u0000' + descriptor);
    }

    /**
     * @param name a member name found in compiled code.
     * @return true when the name is already the runtime name, so no mapping entry is needed and its
     *     absence is not a build failure. Constructors keep their name in both environments, and a
     *     name that already carries a runtime prefix came from a mixin, an SRG mapping or a runtime
     *     string lookup that is deliberately written for the obfuscated runtime.
     */
    public static boolean requiresNoMapping(String name) {
        if (name == null) {
            return false;
        }
        return "<init>".equals(name) || "<clinit>".equals(name)
            || name.startsWith("func_") || name.startsWith("field_")
            || name.startsWith("m_") && name.length() > 2 && Character.isDigit(name.charAt(2));
    }

    /** @return the remapped class name, or the name unchanged when the table does not rename it. */
    public String lookupClass(String internalName) {
        String mapped = classes.get(internalName);
        return mapped == null ? internalName : mapped;
    }

    public int classCount() {
        return classCount;
    }

    public int memberCount() {
        return memberCount;
    }

    /** @return how many name+descriptor keys were rejected because several SRG names shared them. */
    public int ambiguousNameKeyCount() {
        return ambiguousKeys.size();
    }

    /**
     * Finds the references a mapping table does not cover.
     * <p>
     * This is the question the release build has to be able to answer: a reference to a Minecraft
     * member that nothing remaps is a call that will resolve in a development runtime and throw
     * {@code NoSuchMethodError} in a production one. It is reported per reference, with the classes
     * that make it, so the build can fail instead of shipping.
     *
     * @param references the references collected while remapping.
     * @param prefixes the class-name prefixes that count as "the game's own API".
     * @return one entry per unmapped reference, ordered by owner then name.
     */
    public static List<String> unmapped(Collection<MemberReference> references, Collection<String> prefixes) {
        Map<String, Set<String>> byReference = new HashMap<String, Set<String>>();
        for (MemberReference reference : references) {
            if (!startsWithAny(reference.owner, prefixes)) {
                continue;
            }
            if (reference.mapped) {
                continue;
            }
            String key = reference.owner + '.' + reference.name + ' ' + describe(reference.descriptor);
            Set<String> users = byReference.get(key);
            if (users == null) {
                users = new LinkedHashSet<String>();
                byReference.put(key, users);
            }
            users.add(reference.referrer);
        }
        List<String> lines = new ArrayList<String>(byReference.size());
        List<String> keys = new ArrayList<String>(byReference.keySet());
        Collections.sort(keys);
        for (String key : keys) {
            lines.add(key + "  <- " + join(byReference.get(key)));
        }
        return lines;
    }

    private static boolean startsWithAny(String name, Collection<String> prefixes) {
        for (String prefix : prefixes) {
            if (name.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static String join(Set<String> names) {
        StringBuilder builder = new StringBuilder();
        for (String name : names) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(name);
        }
        return builder.toString();
    }

    private static String describe(String descriptor) {
        return descriptor.length() == 0 ? "(field)" : descriptor;
    }

    /** One member reference found in a class file, and whether a mapping table covered it. */
    public static final class MemberReference {
        public final String referrer;
        public final String owner;
        public final String name;
        public final String descriptor;
        public final boolean mapped;

        MemberReference(String referrer, String owner, String name, String descriptor, boolean mapped) {
            this.referrer = referrer;
            this.owner = owner;
            this.name = name;
            this.descriptor = descriptor;
            this.mapped = mapped;
        }

        @Override
        public String toString() {
            return referrer + " -> " + owner + '.' + name + describe(descriptor)
                + (mapped ? " (mapped)" : " (UNMAPPED)");
        }
    }

    /**
     * @return the prefixes whose members are renamed between development and production.
     *     <p>
     *     Only Minecraft's own classes are obfuscated. Forge ships pre-compiled against SRG names
     *     and never obfuscates its own API, so {@code RenderGameOverlayEvent.Pre.type} and
     *     {@code MinecraftForge.EVENT_BUS} are the same strings on a production runtime as in a
     *     development one. Treating Forge as remappable would report every event hook Aether
     *     registers as a failure, and would try to rename members that must not be renamed.
     */
    public static Collection<String> defaultPrefixes() {
        List<String> prefixes = new ArrayList<String>(1);
        prefixes.add("net/minecraft/");
        return Collections.unmodifiableList(prefixes);
    }
}
