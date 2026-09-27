#!/usr/bin/env sh
set -eu

rm -rf build/classes build/test-classes build/forge189Stubs-classes build/forge189-classes
mkdir -p build/classes build/test-classes build/forge189Stubs-classes build/forge189-classes

find src/main/java -name '*.java' | sort > build/main-sources.txt
find src/test/java -name '*.java' | sort > build/test-sources.txt
find src/forge189Stubs/java -name '*.java' | sort > build/forge189Stubs-sources.txt
find src/forge189/java -name '*.java' | sort > build/forge189-sources.txt

FORGE_189_JAR=${AETHER_FORGE_189_JAR:-/home/laptop/.gradle/caches/minecraft/net/minecraftforge/forge/1.8.9-11.15.1.2318-1.8.9/stable/22/forgeSrc-1.8.9-11.15.1.2318-1.8.9.jar}

javac -source 1.8 -target 1.8 -d build/classes @build/main-sources.txt
javac -source 1.8 -target 1.8 -cp build/classes -d build/test-classes @build/test-sources.txt
javac -source 1.8 -target 1.8 -cp "build/classes:$FORGE_189_JAR" -d build/forge189Stubs-classes @build/forge189Stubs-sources.txt

if [ -f "$FORGE_189_JAR" ]; then
  javac -source 1.8 -target 1.8 -cp "build/classes:build/forge189Stubs-classes:$FORGE_189_JAR" -d build/forge189-classes @build/forge189-sources.txt
else
  echo "Notice: FORGE_189_JAR not found locally ($FORGE_189_JAR), compiling the Forge adapter against the local stubs instead."
  javac -source 1.8 -target 1.8 -cp "build/classes:build/forge189Stubs-classes" -d build/forge189-classes @build/forge189-sources.txt
fi

# Drive the Click Deck screen headlessly: layout, keyboard, mouse, palette and
# reset paths all run against the stubs, so regressions fail here instead of in game.
java -cp "build/classes:build/test-classes:build/forge189Stubs-classes:build/forge189-classes" \
  dev.aether.forge189.AetherClickDeckSelfTest

for test_class in \
  dev.aether.event.EventBusTest \
  dev.aether.module.ModuleRegistryTest \
  dev.aether.module.AetherSettingsMetadataTest \
  dev.aether.config.JsonConfigStoreTest \
  dev.aether.hud.HudLayoutTest \
  dev.aether.runtime.PlatformDetectorTest \
  dev.aether.graphics.FirstPersonAnimsTest \
  dev.aether.graphics.FreelookMathTest \
  dev.aether.graphics.ZoomMathTest \
  dev.aether.graphics.TimeChangerMathTest \
  dev.aether.graphics.HurtCamMathTest \
  dev.aether.module.state.ToggleKeyTest \
  dev.aether.module.state.ValueHoldTest \
  dev.aether.cosmetic.CosmeticLibraryTest \
  dev.aether.theme.ThemeModuleTest
do
  java -cp build/classes:build/test-classes "$test_class"
done

echo "Aether verification passed."
