#!/usr/bin/env sh
set -eu

rm -rf build/classes build/test-classes build/forge189Stubs-classes build/forge189-classes
mkdir -p build/classes build/test-classes build/forge189Stubs-classes build/forge189-classes

find src/main/java -name '*.java' | sort > build/main-sources.txt
find src/test/java -name '*.java' | sort > build/test-sources.txt
find src/forge189Stubs/java -name '*.java' | sort > build/forge189Stubs-sources.txt
find src/forge189/java -name '*.java' | sort > build/forge189-sources.txt

FORGE_189_JAR=${AETHER_FORGE_189_JAR:-/home/laptop/.gradle/caches/minecraft/net/minecraftforge/forge/1.8.9-11.15.1.2318-1.8.9/stable/22/forgeSrc-1.8.9-11.15.1.2318-1.8.9.jar}

# javac.exe wants ';' as the classpath separator; javac on POSIX wants ':'. Git Bash on Windows
# runs this script, so detect through the OS name instead of the shell flavor.
case "$(uname -s)" in
  CYGWIN*|MINGW*|MSYS*) CP_SEP=";" ;;
  *) CP_SEP=":" ;;
esac

# The build must run on a modern JDK without a configured cross-compile toolchain: --release 8
# gives the same bytecode promise without needing javac's legacy -source/-target machinery.
JAVAC_FLAGS="--release 8 -nowarn -encoding UTF-8"

javac $JAVAC_FLAGS -d build/classes @build/main-sources.txt
javac $JAVAC_FLAGS -cp "build/classes" -d build/test-classes @build/test-sources.txt
javac $JAVAC_FLAGS -cp "build/classes$CP_SEP""$FORGE_189_JAR" -d build/forge189Stubs-classes @build/forge189Stubs-sources.txt

if [ -f "$FORGE_189_JAR" ]; then
  javac $JAVAC_FLAGS -cp "build/classes$CP_SEP""build/forge189Stubs-classes$CP_SEP""$FORGE_189_JAR" -d build/forge189-classes @build/forge189-sources.txt
else
  echo "Notice: FORGE_189_JAR not found locally ($FORGE_189_JAR), compiling the Forge adapter against the local stubs instead."
  javac $JAVAC_FLAGS -cp "build/classes$CP_SEP""build/forge189Stubs-classes" -d build/forge189-classes @build/forge189-sources.txt
fi

# Drive the adapter headlessly. The Control Center test covers layout, sections, keyboard, mouse,
# palette, profiles and reset paths; the font test covers the glyph atlas, the packing and the
# measurement contract, the fallback a missing OpenGL texture has to take, and the colour contract:
# glyph quads and their underline/strikethrough decorations both carry their colour on the vertex,
# so a text draw neither reads nor leaves GL colour state. Both run against the stubs, so a
# regression fails here instead of in game.
for self_test in \
  dev.aether.forge189.AetherClickDeckSelfTest \
  dev.aether.forge189.font.AetherFontSelfTest
do
  java -cp "build/classes$CP_SEP""build/test-classes$CP_SEP""build/forge189Stubs-classes$CP_SEP""build/forge189-classes" \
    "$self_test"
done

for test_class in \
  dev.aether.remap.McpSrgRemapperTest \
  dev.aether.graphics.TextureIdResolverTest \
  dev.aether.event.EventBusTest \
  dev.aether.module.ModuleRegistryTest \
  dev.aether.module.AetherSettingsMetadataTest \
  dev.aether.config.JsonConfigStoreTest \
  dev.aether.config.ClientPreferencesTest \
  dev.aether.config.ProfileStoreTest \
  dev.aether.hud.HudLayoutTest \
  dev.aether.runtime.PlatformDetectorTest \
  dev.aether.runtime.FpsLimiterTest \
  dev.aether.graphics.FirstPersonAnimsTest \
  dev.aether.graphics.FreelookMathTest \
  dev.aether.graphics.FreelookViewTest \
  dev.aether.graphics.ZoomMathTest \
  dev.aether.graphics.TimeChangerMathTest \
  dev.aether.graphics.HurtCamMathTest \
  dev.aether.graphics.WeatherValuesTest \
  dev.aether.module.state.ToggleKeyTest \
  dev.aether.module.state.ValueHoldTest \
  dev.aether.module.state.ActivationLatchTest \
  dev.aether.module.state.ForceKeyMachineTest \
  dev.aether.ui.ModuleSearchTest \
  dev.aether.ui.ControlCenterStateTest \
  dev.aether.screenshot.ScreenshotStoreTest \
  dev.aether.waypoint.WaypointManagerTest \
  dev.aether.cosmetic.CosmeticLibraryTest \
  dev.aether.theme.ThemeModuleTest
do
  java -cp "build/classes$CP_SEP""build/test-classes" "$test_class"
done

# The UI draws exclusively through the Tessellator vertex pipeline. Immediate mode and the
# client-array colour are what made text depend on GL state in the first place, so their return is a
# failure, not a style issue. GL_TRIANGLE_FAN is deliberately not matched: as a Tessellator mode it
# is a legitimate primitive, and the cosmetics renderer uses it that way.
IMMEDIATE_MODE='glBegin|glEnd[[:space:]]*\(|glVertex2f|glVertex3f|glTexCoord2f|glColor4f'
# Comment lines are excluded: the sources legitimately name these entry points while explaining why
# they are gone.
if grep -rnE "$IMMEDIATE_MODE" src/forge189/java src/main/java \
  | grep -vE ':[[:space:]]*(//|\*|/\*)' | grep -qv '^$'; then
  echo "Verification failed: immediate-mode or client-array GL calls are back in the drawing path:"
  grep -rnE "$IMMEDIATE_MODE" src/forge189/java src/main/java | grep -vE ':[[:space:]]*(//|\*|/\*)'
  exit 1
fi
echo "No immediate-mode or client-array GL entry points in the drawing path."

# The release step has to leave the artifact addressing names a production runtime has, and this
# check used to be missing entirely. Run with an empty mapping file it is a report: it names every
# Minecraft member the shipping classes call in development form, which is exactly the set that
# throws NoSuchMethodError in game until -PaetherSrgMappings=<mcp-srg.srg> remaps them.
mkdir -p build/reports build/audit
java -cp "build/classes" dev.aether.remap.RemapTool \
  --mappings scripts/empty-mappings.srg \
  --input build/classes --input build/forge189-classes \
  --output build/audit/remapped \
  --report build/reports/aether-unmapped-game-references.txt \
  --exclude SelfTest --exclude dev/aether/remap/ \
  > build/audit/remap-summary.txt 2>&1 || true
grep -E "distinct Minecraft members|call sites that would fail" build/audit/remap-summary.txt || true

echo "Aether verification passed."
