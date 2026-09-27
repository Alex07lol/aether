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

# Drive the Control Center screen headlessly: layout, sections, keyboard, mouse, palette,
# profiles and reset paths all run against the stubs, so regressions fail here instead of in game.
java -cp "build/classes$CP_SEP""build/test-classes$CP_SEP""build/forge189Stubs-classes$CP_SEP""build/forge189-classes" \
  dev.aether.forge189.AetherClickDeckSelfTest

for test_class in \
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

echo "Aether verification passed."
