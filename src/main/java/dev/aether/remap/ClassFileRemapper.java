package dev.aether.remap;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rewrites the development names in a compiled class file into their runtime names.
 * <p>
 * This is the build step Aether was missing. The adapter is compiled with {@code javac} against
 * stubs (or against a development Minecraft jar), so its constant pool holds names like
 * {@code drawScreen}, {@code pos} and {@code getGlTextureId}. A production Minecraft 1.8.9 runtime
 * only has {@code func_73863_a}, {@code func_181662_b} and {@code func_110552_b}: ForgeGradle's
 * {@code reobf} task is what translates one into the other, and a plain Gradle build has nothing
 * that does. An unremapped jar does not merely lose a feature - the virtual machine throws
 * {@code NoSuchMethodError} the first time any of those call sites executes, a screen that declares
 * {@code drawScreen} is never called by Minecraft at all, and a mixin target written in development
 * names never resolves.
 *
 * <h3>What is rewritten</h3>
 * <ul>
 *   <li>Member references ({@code Fieldref}, {@code Methodref}, {@code InterfaceMethodref}) whose
 *       owner belongs to the game's API.</li>
 *   <li>Member <em>declarations</em> - a method Aether implements for Minecraft, or a field a mixin
 *       shadows - but only in a class that is, or extends, a game type. The restriction matters: a
 *       private helper that happens to share a name and descriptor with a Minecraft member must keep
 *       its name, because Aether's own calls to it are not game references and would not be renamed
 *       alongside it.</li>
 *   <li>Class references, for completeness. On 1.8.9 the SRG class names are the MCP ones, so this is
 *       normally a no-op, but a mapping file that renames a class is honoured rather than ignored.</li>
 * </ul>
 *
 * <h3>Why renaming goes through new constant-pool entries</h3>
 * A name is not stored once. The same {@code CONSTANT_Utf8} entry backs a member reference and any
 * string literal with identical text, and Mixin annotation values are string literals: rewriting the
 * entry in place would turn {@code @Inject(method = "drawScreen")} into a name no refmap could resolve
 * afterwards. Every newly needed name therefore gets its <em>own</em> entry and only the
 * {@code NameAndType} and declaration indices are repointed at it. String literals, including the
 * reflective name lists in {@code Mc189Compat} and the mixin targets, come out byte for byte as the
 * author wrote them.
 *
 * <p>Nothing else in the file is touched: method bodies, stack map frames, line numbers and nested
 * attributes are copied verbatim, so renumbering the pool cannot disturb code the remapper does not
 * interpret.
 */
public final class ClassFileRemapper {
    private static final int CONSTANT_UTF8 = 1;
    private static final int CONSTANT_INTEGER = 3;
    private static final int CONSTANT_FLOAT = 4;
    private static final int CONSTANT_LONG = 5;
    private static final int CONSTANT_DOUBLE = 6;
    private static final int CONSTANT_CLASS = 7;
    private static final int CONSTANT_STRING = 8;
    private static final int CONSTANT_FIELDREF = 9;
    private static final int CONSTANT_METHODREF = 10;
    private static final int CONSTANT_INTERFACE_METHODREF = 11;
    private static final int CONSTANT_NAME_AND_TYPE = 12;
    private static final int CONSTANT_METHOD_HANDLE = 15;
    private static final int CONSTANT_METHOD_TYPE = 16;
    private static final int CONSTANT_DYNAMIC = 17;
    private static final int CONSTANT_INVOKE_DYNAMIC = 18;
    private static final int CONSTANT_MODULE = 19;
    private static final int CONSTANT_PACKAGE = 20;

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private final McpSrgMappings mappings;
    private final Collection<String> apiPrefixes;

    public ClassFileRemapper(McpSrgMappings mappings, Collection<String> apiPrefixes) {
        this.mappings = mappings;
        this.apiPrefixes = apiPrefixes;
    }

    /** What one class produced, and what the pass found and changed while producing it. */
    public static final class Outcome {
        public final byte[] bytes;
        /** The class's own internal name, read from the file rather than assumed from its path. */
        public final String className;
        /** Every reference this class makes into the game's API, mapped or not. */
        public final List<McpSrgMappings.MemberReference> references;
        public final int remappedReferences;
        public final int remappedDeclarations;
        public final int remappedClasses;

        Outcome(byte[] bytes, String className, List<McpSrgMappings.MemberReference> references,
                int remappedReferences, int remappedDeclarations, int remappedClasses) {
            this.bytes = bytes;
            this.className = className;
            this.references = references;
            this.remappedReferences = remappedReferences;
            this.remappedDeclarations = remappedDeclarations;
            this.remappedClasses = remappedClasses;
        }
    }

    /**
     * Remaps one class file.
     *
     * @param data the class file's bytes.
     * @param hierarchy class name to superclass name for every class in the build, used to decide
     *     whether a declaration implements a game member through an intermediate class.
     * @param apiTypes class names in the build that belong to the game's API (the stubs, when the
     *     build compiles against them).
     */
    public Outcome remap(byte[] data, Map<String, String> hierarchy, Set<String> apiTypes) {
        int constantCount = readU2(data, 8);
        int[] tag = new int[constantCount];
        int[] offset = new int[constantCount];
        String[] utf8 = new String[constantCount];
        int[] refClass = new int[constantCount];
        int[] refNameType = new int[constantCount];
        int[] nameTypeName = new int[constantCount];
        int[] nameTypeDescriptor = new int[constantCount];
        int[] classEntryName = new int[constantCount];

        int position = 10;
        for (int index = 1; index < constantCount; index++) {
            int entryTag = data[position] & 0xFF;
            tag[index] = entryTag;
            offset[index] = position;
            switch (entryTag) {
                case CONSTANT_UTF8: {
                    int length = readU2(data, position + 1);
                    utf8[index] = new String(data, position + 3, length, UTF8);
                    position += 3 + length;
                    break;
                }
                case CONSTANT_INTEGER:
                case CONSTANT_FLOAT:
                    position += 5;
                    break;
                case CONSTANT_LONG:
                case CONSTANT_DOUBLE:
                    position += 9;
                    index++;
                    break;
                case CONSTANT_CLASS:
                    classEntryName[index] = readU2(data, position + 1);
                    position += 3;
                    break;
                case CONSTANT_STRING:
                    position += 3;
                    break;
                case CONSTANT_FIELDREF:
                case CONSTANT_METHODREF:
                case CONSTANT_INTERFACE_METHODREF:
                    refClass[index] = readU2(data, position + 1);
                    refNameType[index] = readU2(data, position + 3);
                    position += 5;
                    break;
                case CONSTANT_NAME_AND_TYPE:
                    nameTypeName[index] = readU2(data, position + 1);
                    nameTypeDescriptor[index] = readU2(data, position + 3);
                    position += 5;
                    break;
                // Method handles point at a member reference that is remapped through that entry, and
                // an invokedynamic's name-and-type describes the lambda's own method. Neither is a game
                // member, so both are skipped, but both have to be understood: javac emits them for
                // lambdas and string concatenation, and a parser that stops on them remaps nothing.
                case CONSTANT_METHOD_HANDLE:
                    position += 4;
                    break;
                case CONSTANT_METHOD_TYPE:
                    position += 3;
                    break;
                case CONSTANT_DYNAMIC:
                case CONSTANT_INVOKE_DYNAMIC:
                    position += 5;
                    break;
                case CONSTANT_MODULE:
                case CONSTANT_PACKAGE:
                    position += 3;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported constant pool tag " + entryTag + " at "
                        + position + "; only the tags javac emits for Java 8 class files are understood.");
            }
        }
        int poolEnd = position;

        String thisClass = className(utf8, classEntryName, readU2(data, poolEnd + 2));
        String superClass = className(utf8, classEntryName, readU2(data, poolEnd + 4));

        // Pass one: collect the work. Nothing is written yet, so the pool can still be read.
        List<PendingPatch> patches = new ArrayList<PendingPatch>();
        List<McpSrgMappings.MemberReference> references = new ArrayList<McpSrgMappings.MemberReference>();
        int remappedReferences = 0;
        for (int index = 1; index < constantCount; index++) {
            int entryTag = tag[index];
            if (entryTag != CONSTANT_FIELDREF && entryTag != CONSTANT_METHODREF
                && entryTag != CONSTANT_INTERFACE_METHODREF) {
                continue;
            }
            String owner = className(utf8, classEntryName, refClass[index]);
            int nameType = refNameType[index];
            if (owner == null || nameType <= 0 || nameType >= constantCount) {
                continue;
            }
            String name = utf8[nameTypeName[nameType]];
            String descriptor = utf8[nameTypeDescriptor[nameType]];
            if (name == null || descriptor == null) {
                continue;
            }
            boolean gameReference = startsWithAny(owner, apiPrefixes);
            boolean field = entryTag == CONSTANT_FIELDREF;
            // A field is identified by owner and name only; its descriptor is the field's own type and
            // appears nowhere in the mapping tables. Using it as part of the key would miss every
            // field the artifact touches, which is exactly the kind of silent hole this must not have.
            String mapped = !gameReference ? null
                : (field ? mappings.lookupField(owner, name) : mappings.lookupMember(owner, name, descriptor));
            if (gameReference) {
                // A reference that needs no mapping (a constructor, or a name already in its runtime
                // form) is covered by definition; only a development name with no entry is a
                // reference that would fail on a production runtime.
                boolean covered = mapped != null || McpSrgMappings.requiresNoMapping(name);
                references.add(new McpSrgMappings.MemberReference(thisClass, owner, name, descriptor, covered));
            }
            if (mapped != null) {
                patches.add(new PendingPatch(offset[nameType] + 1, mapped));
                remappedReferences++;
            }
        }

        int remappedClasses = 0;
        for (int index = 1; index < constantCount; index++) {
            if (tag[index] != CONSTANT_CLASS) {
                continue;
            }
            String name = utf8[classEntryName[index]];
            if (name == null || name.startsWith("[")) {
                continue;
            }
            String mapped = mappings.lookupClass(name);
            if (!mapped.equals(name)) {
                patches.add(new PendingPatch(offset[index] + 1, mapped));
                remappedClasses++;
            }
        }

        int remappedDeclarations = 0;
        if (implementsGameType(thisClass, superClass, hierarchy, apiTypes)) {
            int declarationsBefore = patches.size();
            int cursor = poolEnd + 6;
            int interfaceCount = readU2(data, cursor);
            cursor += 2 + interfaceCount * 2;
            int fieldCount = readU2(data, cursor);
            cursor += 2;
            Set<String> declaredFields = declaredNames(data, cursor, fieldCount, utf8);
            // Fields count as declarations as well: a mixin's shadowed field is one.
            cursor = scanDeclarations(data, cursor, fieldCount, false, utf8, patches);
            int methodCount = readU2(data, cursor);
            cursor += 2;
            Set<String> declaredMethods = declaredNames(data, cursor, methodCount, utf8);
            scanDeclarations(data, cursor, methodCount, true, utf8, patches);

            // A class can already declare a member under its runtime name - Aether's screens carry
            // hand-written SRG delegates so they dispatch on an obfuscated runtime without any
            // remapping. Renaming the development declaration on top of that would emit two methods
            // with one name and signature, which is not a behaviour change but an uncallable class:
            // the JVM rejects the file outright (ClassFormatError, duplicate method name). A
            // declaration whose target name already exists is therefore left exactly as it is - the
            // existing member is the override. Reference patches are deliberately untouched here:
            // they repoint NameAndType entries inside the pool, and the super.func_73866_w_() call
            // that targets the existing member must keep its rename.
            java.util.Iterator<PendingPatch> it = patches.iterator();
            while (it.hasNext()) {
                PendingPatch patch = it.next();
                if (patch.offset >= poolEnd
                    && (declaredMethods.contains(patch.name) || declaredFields.contains(patch.name))) {
                    it.remove();
                }
            }
            remappedDeclarations = patches.size() - declarationsBefore;
        }

        // Pass two: intern every new name, then emit. An existing pool entry is reused when one
        // already holds the text; otherwise the name is appended as a fresh entry.
        Map<String, Integer> appendedIndex = new HashMap<String, Integer>();
        List<String> appendedOrder = new ArrayList<String>();
        for (int index = 1; index < constantCount; index++) {
            if (tag[index] == CONSTANT_UTF8 && utf8[index] != null) {
                if (!appendedIndex.containsKey(utf8[index])) {
                    appendedIndex.put(utf8[index], Integer.valueOf(index));
                }
            }
        }
        int[] resolved = new int[patches.size()];
        for (int i = 0; i < patches.size(); i++) {
            PendingPatch patch = patches.get(i);
            Integer known = appendedIndex.get(patch.name);
            if (known == null) {
                int index = constantCount + appendedOrder.size();
                appendedIndex.put(patch.name, Integer.valueOf(index));
                appendedOrder.add(patch.name);
                resolved[i] = index;
            } else {
                resolved[i] = known.intValue();
            }
        }

        byte[] appendedBytes = buildUtf8Entries(appendedOrder);
        byte[] output = new byte[data.length + appendedBytes.length];
        System.arraycopy(data, 0, output, 0, 8);
        writeU2(output, 8, constantCount + appendedOrder.size());
        System.arraycopy(data, 10, output, 10, poolEnd - 10);
        System.arraycopy(appendedBytes, 0, output, poolEnd, appendedBytes.length);
        System.arraycopy(data, poolEnd, output, poolEnd + appendedBytes.length, data.length - poolEnd);
        for (int i = 0; i < patches.size(); i++) {
            int value = resolved[i];
            if (value > 0xFFFF) {
                throw new IllegalStateException("Constant pool overflow in " + thisClass
                    + ": more entries than a class file can address.");
            }
            int at = patches.get(i).offset;
            if (at >= poolEnd) {
                at += appendedBytes.length;
            }
            writeU2(output, at, value);
        }

        return new Outcome(output, thisClass, references, remappedReferences, remappedDeclarations,
            remappedClasses);
    }

    /**
     * Reads a class file's own internal name without remapping it.
     *
     * @param data a class file's bytes.
     * @return the internal name, for example {@code dev/aether/forge189/AetherUi}.
     */
    public static String internalName(byte[] data) {
        return nameOf(data, 2);
    }

    /**
     * Reads a class file's superclass internal name without remapping it.
     *
     * @param data a class file's bytes.
     * @return the superclass internal name, or null for {@code java/lang/Object}'s own file.
     */
    public static String superName(byte[] data) {
        return nameOf(data, 4);
    }

    /** Resolves the class index in the header at {@code poolEnd + relativeOffset}. */
    private static String nameOf(byte[] data, int relativeOffset) {
        int constantCount = readU2(data, 8);
        String[] utf8 = new String[constantCount];
        int[] classEntryName = new int[constantCount];
        int position = 10;
        for (int index = 1; index < constantCount; index++) {
            int entryTag = data[position] & 0xFF;
            switch (entryTag) {
                case CONSTANT_UTF8: {
                    int length = readU2(data, position + 1);
                    utf8[index] = new String(data, position + 3, length, UTF8);
                    position += 3 + length;
                    break;
                }
                case CONSTANT_INTEGER:
                case CONSTANT_FLOAT:
                    position += 5;
                    break;
                case CONSTANT_LONG:
                case CONSTANT_DOUBLE:
                    position += 9;
                    index++;
                    break;
                case CONSTANT_CLASS:
                    classEntryName[index] = readU2(data, position + 1);
                    position += 3;
                    break;
                case CONSTANT_STRING:
                    position += 3;
                    break;
                case CONSTANT_FIELDREF:
                case CONSTANT_METHODREF:
                case CONSTANT_INTERFACE_METHODREF:
                case CONSTANT_NAME_AND_TYPE:
                case CONSTANT_DYNAMIC:
                case CONSTANT_INVOKE_DYNAMIC:
                    position += 5;
                    break;
                case CONSTANT_METHOD_HANDLE:
                    position += 4;
                    break;
                case CONSTANT_METHOD_TYPE:
                case CONSTANT_MODULE:
                case CONSTANT_PACKAGE:
                    position += 3;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported constant pool tag " + entryTag);
            }
        }
        return className(utf8, classEntryName, readU2(data, position + relativeOffset));
    }

    /** A declaration or reference to rename, held as text until the pool has been laid out. */
    private static final class PendingPatch {
        final int offset;
        final String name;

        PendingPatch(int offset, String name) {
            this.offset = offset;
            this.name = name;
        }
    }

    /** Collects the declared member names of one field or method table, without patching. */
    private Set<String> declaredNames(byte[] data, int cursor, int count, String[] utf8) {
        Set<String> names = new HashSet<String>();
        for (int i = 0; i < count; i++) {
            String name = utf8[readU2(data, cursor + 2)];
            if (name != null) {
                names.add(name);
            }
            int attributeCount = readU2(data, cursor + 6);
            cursor += 8;
            for (int attribute = 0; attribute < attributeCount; attribute++) {
                cursor += 6 + readU4(data, cursor + 2);
            }
        }
        return names;
    }

    /**
     * Walks a field or method table, recording a patch for each declaration that implements a game
     * member.
     *
     * @param utf8 the parsed constant pool, which is still intact during pass one.
     * @return the offset just past the table.
     */
    private int scanDeclarations(byte[] data, int cursor, int count, boolean methods, String[] utf8,
                                 List<PendingPatch> patches) {
        for (int i = 0; i < count; i++) {
            int nameOffset = cursor + 2;
            String name = utf8[readU2(data, nameOffset)];
            String descriptor = utf8[readU2(data, cursor + 4)];
            if (name != null) {
                String mapped = methods
                    ? (descriptor == null ? null : mappings.lookupDeclaration(name, descriptor))
                    : mappings.lookupFieldDeclaration(name);
                if (mapped != null) {
                    patches.add(new PendingPatch(nameOffset, mapped));
                }
            }
            int attributeCount = readU2(data, cursor + 6);
            cursor += 8;
            for (int attribute = 0; attribute < attributeCount; attribute++) {
                int length = readU4(data, cursor + 2);
                cursor += 6 + length;
            }
        }
        return cursor;
    }

    private boolean implementsGameType(String thisClass, String superClass, Map<String, String> hierarchy,
                                       Set<String> apiTypes) {
        if (startsWithAny(thisClass, apiPrefixes) || apiTypes.contains(thisClass)) {
            return true;
        }
        String cursor = superClass;
        Set<String> seen = new HashSet<String>();
        while (cursor != null && seen.add(cursor)) {
            if (startsWithAny(cursor, apiPrefixes) || apiTypes.contains(cursor)) {
                return true;
            }
            cursor = hierarchy.get(cursor);
        }
        return false;
    }

    private static byte[] buildUtf8Entries(List<String> names) {
        int size = 0;
        byte[][] encoded = new byte[names.size()][];
        for (int i = 0; i < names.size(); i++) {
            encoded[i] = names.get(i).getBytes(UTF8);
            size += 3 + encoded[i].length;
        }
        byte[] bytes = new byte[size];
        int cursor = 0;
        for (byte[] entry : encoded) {
            bytes[cursor] = CONSTANT_UTF8;
            writeU2(bytes, cursor + 1, entry.length);
            System.arraycopy(entry, 0, bytes, cursor + 3, entry.length);
            cursor += 3 + entry.length;
        }
        return bytes;
    }

    private static String className(String[] utf8, int[] classEntryName, int classIndex) {
        if (classIndex <= 0 || classIndex >= classEntryName.length || classEntryName[classIndex] == 0) {
            return null;
        }
        return utf8[classEntryName[classIndex]];
    }

    private static boolean startsWithAny(String name, Collection<String> prefixes) {
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

    private static int readU2(byte[] data, int offset) {
        return (data[offset] & 0xFF) << 8 | data[offset + 1] & 0xFF;
    }

    private static int readU4(byte[] data, int offset) {
        return (data[offset] & 0xFF) << 24 | (data[offset + 1] & 0xFF) << 16
            | (data[offset + 2] & 0xFF) << 8 | data[offset + 3] & 0xFF;
    }

    private static void writeU2(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >>> 8);
        data[offset + 1] = (byte) value;
    }
}
