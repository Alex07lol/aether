package dev.aether.remap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * Verifies the remapping step that gives Aether a production-usable artifact.
 * <p>
 * The test compiles its own fixtures at run time, so it asserts against real class files rather than
 * against a description of them:
 *
 * <ol>
 *   <li>A call into the game's API is rewritten from the development name to the runtime name, and so
 *       is the declaration of a method that implements one - which is what makes a screen's
 *       {@code drawScreen} get called at all on a runtime whose method table says {@code func_73863_a}.</li>
 *   <li>A string literal that happens to equal a member name is <em>not</em> rewritten. Mixin
 *       annotation values and Aether's reflective name lists are literals, and rewriting them would
 *       break exactly the code the remap is meant to repair.</li>
 *   <li>A class that is not part of the game's type hierarchy keeps its own helpers, so nothing is
 *       renamed that Aether's own call sites would then miss.</li>
 *   <li>What cannot be mapped is reported, so a release build can refuse to ship it.</li>
 * </ol>
 *
 * <p>The fixtures stand in for both sides: {@code fixture/mc} is "Minecraft" and {@code fixture/mod} is
 * "Aether". Both are remapped, exactly as a runtime's own classes would already carry the runtime
 * names, which is what makes it possible to load the result and check that overriding still works.
 */
public final class McpSrgRemapperTest {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static int checks;

    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.out.println("McpSrgRemapperTest skipped (no system Java compiler available).");
            return;
        }

        Path root = Files.createTempDirectory("aether-remap-test");
        Path classes = root.resolve("classes");
        Path fixtures = root.resolve("fixtures");
        Path remapped = root.resolve("remapped");
        Files.createDirectories(classes);

        writeFixture(fixtures, "fixture/mc/Ref.java", ""
            + "package fixture.mc;\n"
            + "public class Ref {\n"
            + "    public long big;\n"
            + "    public void drawRect(int a, int b, int c, int d, int e) {}\n"
            + "    public String getName() { return \"ref\"; }\n"
            + "}\n");
        writeFixture(fixtures, "fixture/mc/FakeScreen.java", ""
            + "package fixture.mc;\n"
            + "public class FakeScreen {\n"
            + "    public void initGui() {}\n"
            + "    public void drawScreen(int mouseX, int mouseY, float partialTicks) {}\n"
            + "}\n");
        writeFixture(fixtures, "fixture/mod/Screen.java", ""
            + "package fixture.mod;\n"
            + "public class Screen extends fixture.mc.FakeScreen {\n"
            + "    public static final String LITERAL = \"drawScreen\";\n"
            + "    private final fixture.mc.Ref ref = new fixture.mc.Ref();\n"
            + "    public long touch() { ref.big = 7L; ref.drawRect(1, 2, 3, 4, 5); ref.getName(); return ref.big; }\n"
            + "    @Override public void initGui() { super.initGui(); }\n"
            + "    @Override public void drawScreen(int mouseX, int mouseY, float partialTicks) { super.drawScreen(mouseX, mouseY, partialTicks); }\n"
            + "}\n");
        writeFixture(fixtures, "fixture/mod/Standalone.java", ""
            + "package fixture.mod;\n"
            // Deliberately shares a name and descriptor with a game method while extending nothing.
            + "public class Standalone {\n"
            + "    public void initGui() {}\n"
            + "    public void use() { initGui(); }\n"
            + "}\n");
        writeFixture(fixtures, "fixture/mod/PreNamed.java", ""
            + "package fixture.mod;\n"
            // The trap the remapper has to survive: a class that already declares the runtime name
            // beside the development one. Renaming the development declaration on top of this would
            // emit two methods with one signature, and the JVM rejects the file outright.
            + "public class PreNamed extends fixture.mc.FakeScreen {\n"
            + "    public void initGui() { super.initGui(); }\n"
            + "    public void func_73866_w_() { initGui(); }\n"
            + "}\n");

        List<String> sources = Arrays.asList(
            fixtures.resolve("fixture/mc/Ref.java").toString(),
            fixtures.resolve("fixture/mc/FakeScreen.java").toString(),
            fixtures.resolve("fixture/mod/Screen.java").toString(),
            fixtures.resolve("fixture/mod/Standalone.java").toString(),
            fixtures.resolve("fixture/mod/PreNamed.java").toString());
        List<String> arguments = new ArrayList<String>(Arrays.asList("-d", classes.toString()));
        arguments.addAll(sources);
        int result = compiler.run(null, null, null, arguments.toArray(new String[0]));
        Check.assertEquals(0, result, "the fixture sources compile");

        McpSrgMappings mappings = McpSrgMappings.read(new ByteArrayInputStream(mappingText().getBytes(UTF8)));
        Check.assertEquals(4, mappings.memberCount(), "the mapping table read every member entry");
        Check.assertTrue(mappings.lookupDeclaration("drawScreen", "(IIF)V") != null,
            "a declaration is looked up by name and descriptor alone");
        Check.assertEquals("func_73734_a", mappings.lookupMember("fixture/mc/Ref", "drawRect", "(IIIII)V"),
            "an owner-qualified lookup answers before the shared name lookup");
        Check.assertTrue(McpSrgMappings.requiresNoMapping("<init>"),
            "a constructor needs no entry");
        Check.assertTrue(McpSrgMappings.requiresNoMapping("func_73734_a"),
            "a name already in its runtime form needs no entry");
        Check.assertTrue(!McpSrgMappings.requiresNoMapping("drawRect"),
            "a development name always needs an entry");

        List<String> prefixes = Arrays.asList("fixture/mc/");
        ClassFileRemapper remapper = new ClassFileRemapper(mappings, prefixes);
        Map<String, String> hierarchy = originalsHierarchy(classes);
        Set<String> apiTypes = new HashSet<String>();
        apiTypes.add("fixture/mc/FakeScreen");
        apiTypes.add("fixture/mc/Ref");

        byte[] screen = Files.readAllBytes(classes.resolve("fixture/mod/Screen.class"));
        Check.assertEquals("fixture/mod/Screen", ClassFileRemapper.internalName(screen),
            "the class's own name is read from its header");
        Check.assertEquals("fixture/mc/FakeScreen", ClassFileRemapper.superName(screen),
            "the superclass name is read from its header");

        // Remap the whole fixture world, as the build remaps everything it compiles, then load it.
        List<McpSrgMappings.MemberReference> references = new ArrayList<McpSrgMappings.MemberReference>();
        ClassFileRemapper.Outcome screenOutcome = null;
        for (String name : new String[] {"fixture/mc/Ref", "fixture/mc/FakeScreen", "fixture/mod/Screen",
            "fixture/mod/Standalone", "fixture/mod/PreNamed"}) {
            byte[] data = Files.readAllBytes(classes.resolve(name + ".class"));
            ClassFileRemapper.Outcome outcome = remapper.remap(data, hierarchy, apiTypes);
            if (name.endsWith("Screen") && name.startsWith("fixture/mod")) {
                screenOutcome = outcome;
            }
            Files.createDirectories(remapped.resolve(parentOf(name)));
            Files.write(remapped.resolve(name + ".class"), outcome.bytes);
            references.addAll(outcome.references);
        }
        Check.assertNotNull(screenOutcome, "the mod-side screen was remapped");
        Check.assertEquals(Integer.valueOf(2), Integer.valueOf(screenOutcome.remappedDeclarations),
            "both overridden methods were renamed as declarations");

        String screenText = new String(screenOutcome.bytes, UTF8);
        Check.assertTrue(screenText.contains("func_73863_a"), "drawScreen is rewritten to its runtime name");
        Check.assertTrue(screenText.contains("func_73866_w_"), "initGui is rewritten to its runtime name");
        Check.assertTrue(screenText.contains("func_73734_a"), "the drawRect call is rewritten");
        Check.assertTrue(screenText.contains("field_9999_b"), "the field write is rewritten");
        Check.assertTrue(screenText.contains("drawScreen"),
            "the string literal \"drawScreen\" is still in the constant pool");

        String standaloneText = new String(Files.readAllBytes(remapped.resolve("fixture/mod/Standalone.class")), UTF8);
        Check.assertTrue(standaloneText.contains("initGui"),
            "a class outside the game's hierarchy keeps its own helper's name");
        Check.assertTrue(!standaloneText.contains("func_73866_w_"),
            "and nothing in it is renamed to a runtime name");

        // The strongest statement available without a game client: the remapped classes load, the
        // renamed override is still an override, and the literal survived.
        URLClassLoader loader = new URLClassLoader(new URL[] {
            remapped.toUri().toURL(), classes.toUri().toURL()
        }, McpSrgRemapperTest.class.getClassLoader());
        try {
            Class<?> base = Class.forName("fixture.mc.FakeScreen", false, loader);
            Class<?> loaded = Class.forName("fixture.mod.Screen", false, loader);
            Class<?> ref = Class.forName("fixture.mc.Ref", false, loader);
            Check.assertTrue(base.isAssignableFrom(loaded),
                "the remapped screen is still a subclass of its game-side base class");
            Check.assertNotNull(base.getDeclaredMethod("func_73863_a", Integer.TYPE, Integer.TYPE, Float.TYPE),
                "the game-side base class declares the runtime name");
            Check.assertTrue(loaded.getMethod("func_73863_a", Integer.TYPE, Integer.TYPE, Float.TYPE)
                .getDeclaringClass() == loaded,
                "the screen's runtime-named method is its own override, not an inherited one");
            Check.assertNotNull(loaded.getDeclaredMethod("func_73866_w_"),
                "the renamed no-argument override is declared");
            Check.assertTrue(!hasMethod(loaded, "drawScreen", Integer.TYPE, Integer.TYPE, Float.TYPE),
                "the development name is gone from the declaration table");
            Check.assertEquals("drawScreen", loaded.getField("LITERAL").get(null),
                "the string literal survives the remap unchanged");
            Check.assertNotNull(ref.getMethod("func_73734_a", Integer.TYPE, Integer.TYPE, Integer.TYPE,
                Integer.TYPE, Integer.TYPE), "the game-side class declares its runtime name");
            Check.assertNotNull(Class.forName("fixture.mod.Standalone", false, loader)
                .getDeclaredMethod("initGui"), "the untouched helper is still callable by its own name");

            Class<?> preNamed = Class.forName("fixture.mod.PreNamed", false, loader);
            int runtimeDeclarations = 0;
            for (java.lang.reflect.Method method : preNamed.getDeclaredMethods()) {
                if (method.getName().equals("func_73866_w_")) {
                    runtimeDeclarations++;
                }
            }
            Check.assertEquals(1, runtimeDeclarations,
                "a class that already declares the runtime name keeps exactly one copy of it");
            Check.assertNotNull(preNamed.getDeclaredMethod("initGui"),
                "the development declaration is left in place beside its runtime delegate");
        } finally {
            loader.close();
        }

        List<String> unmapped = McpSrgMappings.unmapped(references, prefixes);
        Check.assertEquals(Integer.valueOf(1), Integer.valueOf(unmapped.size()),
            "only the genuinely unmapped reference is reported, got " + unmapped);
        Check.assertTrue(unmapped.get(0).contains("fixture/mc/Ref.getName"),
            "the report names the unmapped member, got " + unmapped.get(0));
        Check.assertTrue(unmapped.get(0).contains("fixture/mod/Screen"),
            "the report names the class that makes the reference, got " + unmapped.get(0));

        ambiguityIsNotGuessed();

        System.out.println("McpSrgRemapperTest passed (" + checks + " checks).");
    }

    /**
     * Two classes can declare the same name and descriptor and receive different runtime names. A
     * lookup with no owner must refuse to guess: renaming a call to the wrong method is worse than
     * leaving it alone and reporting it.
     */
    private static void ambiguityIsNotGuessed() throws IOException {
        // SRG writes a member as owner/name, so two classes that declare the same member produce two
        // entries with the same name and descriptor and different owners.
        String text = ""
            + "MD: a/One/getValue ()Ljava/lang/String; a/One/func_111111_a ()Ljava/lang/String;\n"
            + "MD: b/Two/getValue ()Ljava/lang/String; b/Two/func_222222_b ()Ljava/lang/String;\n";
        McpSrgMappings ambiguous = McpSrgMappings.read(new ByteArrayInputStream(text.getBytes(UTF8)));
        Check.assertTrue(ambiguous.lookupDeclaration("getValue", "()Ljava/lang/String;") == null,
            "an ambiguous name+descriptor lookup returns nothing");
        Check.assertEquals("func_111111_a", ambiguous.lookupMember("a/One", "getValue", "()Ljava/lang/String;"),
            "an owner-qualified lookup still answers");
        Check.assertEquals(Integer.valueOf(1), Integer.valueOf(ambiguous.ambiguousNameKeyCount()),
            "the ambiguous key is counted so the build can report it");
    }

    private static boolean hasMethod(Class<?> type, String name, Class<?>... parameters) {
        try {
            type.getDeclaredMethod(name, parameters);
            return true;
        } catch (NoSuchMethodException absent) {
            return false;
        }
    }

    /** Reads the superclass of every compiled fixture, which is what the declaration guard consults. */
    private static Map<String, String> originalsHierarchy(Path classes) throws IOException {
        Map<String, String> hierarchy = new HashMap<String, String>();
        for (String name : new String[] {"fixture/mc/Ref", "fixture/mc/FakeScreen", "fixture/mod/Screen",
            "fixture/mod/Standalone"}) {
            byte[] data = Files.readAllBytes(classes.resolve(name + ".class"));
            hierarchy.put(ClassFileRemapper.internalName(data), ClassFileRemapper.superName(data));
        }
        return hierarchy;
    }

    private static String parentOf(String internalName) {
        int slash = internalName.lastIndexOf('/');
        return slash < 0 ? "" : internalName.substring(0, slash);
    }

    private static void writeFixture(Path root, String relative, String source) throws IOException {
        Path file = root.resolve(relative);
        Files.createDirectories(file.getParent());
        Files.write(file, source.getBytes(UTF8));
    }

    private static String mappingText() {
        return ""
            + "PK: fixture\n"
            + "CL: fixture/mc/Ref fixture/mc/Ref\n"
            + "FD: fixture/mc/Ref/big fixture/mc/Ref/field_9999_b\n"
            + "MD: fixture/mc/Ref/drawRect (IIIII)V fixture/mc/Ref/func_73734_a (IIIII)V\n"
            + "MD: fixture/mc/FakeScreen/initGui ()V fixture/mc/FakeScreen/func_73866_w_ ()V\n"
            + "MD: fixture/mc/FakeScreen/drawScreen (IIF)V fixture/mc/FakeScreen/func_73863_a (IIF)V\n";
    }

    private static final class Check {
        private Check() {
        }

        static void assertTrue(boolean condition, String message) {
            checks++;
            if (!condition) {
                throw new AssertionError(message);
            }
        }

        static void assertEquals(Object expected, Object actual, String message) {
            checks++;
            if (expected == null ? actual != null : !expected.equals(actual)) {
                throw new AssertionError(message + " (expected " + expected + ", got " + actual + ")");
            }
        }

        static void assertNotNull(Object value, String message) {
            checks++;
            if (value == null) {
                throw new AssertionError(message);
            }
        }
    }
}
