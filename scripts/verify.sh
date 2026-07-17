#!/usr/bin/env sh
set -eu

rm -rf build/classes build/test-classes
mkdir -p build/classes build/test-classes build/forge189-classes

find src/main/java -name '*.java' | sort > build/main-sources.txt
find src/test/java -name '*.java' | sort > build/test-sources.txt
find src/forge189/java -name '*.java' | sort > build/forge189-sources.txt

FORGE_189_JAR=${AETHER_FORGE_189_JAR:-/home/laptop/.gradle/caches/minecraft/net/minecraftforge/forge/1.8.9-11.15.1.2318-1.8.9/stable/22/forgeSrc-1.8.9-11.15.1.2318-1.8.9.jar}

javac -source 1.8 -target 1.8 -d build/classes @build/main-sources.txt
javac -source 1.8 -target 1.8 -cp build/classes -d build/test-classes @build/test-sources.txt
javac -source 1.8 -target 1.8 -cp "build/classes:$FORGE_189_JAR" -d build/forge189-classes @build/forge189-sources.txt

for test_class in \
  dev.aether.event.EventBusTest \
  dev.aether.module.ModuleRegistryTest \
  dev.aether.config.JsonConfigStoreTest \
  dev.aether.hud.HudLayoutTest \
  dev.aether.runtime.PlatformDetectorTest \
  dev.aether.cosmetic.CosmeticLibraryTest
do
  java -cp build/classes:build/test-classes "$test_class"
done

echo "Aether verification passed."
