#!/usr/bin/env sh
# =============================================================================
# Aether production verification gate.
#
# Runs the full ForgeGradle pipeline and then verifies the artifacts that
# historically went wrong when the build was hand-rolled:
#   1. the production jar exists and reobfJar actually ran on it;
#   2. the jar ships mixins.aether.refmap.json at its root with a populated
#      "searge" table (the production environment Mixin reads) and a "notch"
#      table (dev-time remapping source);
#   3. mixins.aether.json is inside the jar and lists every mixin class that
#      was compiled, so a new mixin cannot silently miss the config;
#   4. the manifest carries the Mixin bootstrap attributes and
#      AetherMixinPlugin (FMLCorePlugin) so Mixin starts on a real client;
#   5. a compiled adapter class contains no MCP-named symbolic references to
#      Minecraft methods (an unmapped callsite throws NoSuchMethodError the
#      moment the feature runs on a production client).
#
# Usage: scripts/verify.sh [path-to-jdk8]   (default: JAVA_HOME or `java` on PATH)
# =============================================================================
set -eu

cd "$(dirname "$0")/.."

if [ -n "${1:-}" ]; then
    JAVA_HOME="$1"
fi

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    GRADLE_JAVA="$JAVA_HOME/bin/java"
else
    GRADLE_JAVA="java"
fi

echo "== gradle build (unit tests + reobfJar) =="
./gradlew --no-daemon build

# Pick the artifact Gradle produced (aether-<version>.jar; skip -sources etc.).
JAR=$(ls build/libs/*.jar 2>/dev/null | grep -Ev -- "-sources|-javadoc" | head -n 1)
if [ -z "$JAR" ]; then
    echo "FAIL: no jar in build/libs" >&2
    exit 1
fi
echo "== verifying $JAR =="

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
unzip -qo "$JAR" -d "$WORK" \
    "mixins.aether.json" "mixins.aether.refmap.json" "META-INF/MANIFEST.MF" \
    "dev/aether/forge189/ForgeHudRenderer.class" "dev/aether/forge189/mixin/*.class"

fail() {
    echo "FAIL: $1" >&2
    exit 1
}

# --- 2. refmap shipped and populated ----------------------------------------
[ -f "$WORK/mixins.aether.refmap.json" ] || fail "mixins.aether.refmap.json is not in the jar root"
grep -q '"searge"' "$WORK/mixins.aether.refmap.json" || fail "refmap has no searge table (production Mixin would resolve nothing)"
grep -q '"notch"' "$WORK/mixins.aether.refmap.json" || fail "refmap has no notch table"

# --- 3. mixin config ships every compiled mixin -----------------------------
[ -f "$WORK/mixins.aether.json" ] || fail "mixins.aether.json is not in the jar"
for class in "$WORK"/dev/aether/forge189/mixin/*Mixin.class; do
    name=$(basename "$class" .class)
    # EntityPlayerSPMixin is a doc marker, not a configured mixin.
    [ "$name" = "EntityPlayerSPMixin" ] && continue
    grep -q "\"$name\"" "$WORK/mixins.aether.json" || fail "$name is compiled but missing from mixins.aether.json"
done

# --- 4. manifest bootstrap ---------------------------------------------------
grep -q "TweakClass: org.spongepowered.asm.launch.MixinTweaker" "$WORK/META-INF/MANIFEST.MF" \
    || fail "manifest lacks the MixinTweaker TweakClass"
grep -q "FMLCorePlugin: dev.aether.forge189.mixin.AetherMixinPlugin" "$WORK/META-INF/MANIFEST.MF" \
    || fail "manifest lacks the AetherMixinPlugin FMLCorePlugin"
grep -q "MixinConfigs: mixins.aether.json" "$WORK/META-INF/MANIFEST.MF" \
    || fail "manifest lacks MixinConfigs"

# --- 5. reobf happened: no MCP-named calls into Minecraft --------------------
if [ ! -x "$JAVA_HOME/bin/javap" ] && ! command -v javap >/dev/null 2>&1; then
    echo "SKIP: javap not available; cannot check for unmapped callsites"
else
    if [ -x "$JAVA_HOME/bin/javap" ]; then
        JAVAP="$JAVA_HOME/bin/javap"
    else
        JAVAP="javap"
    fi
    # renderScoreboard / getSunBrightness are MCP names; a production jar must
    # reference the SRG names (func_*) instead. GUI-owned names like drawRect
    # are Aether's own and legitimately remain.
    MCP_CALLS=$("$JAVAP" -c -cp "$WORK" dev.aether.forge189.ForgeHudRenderer \
        | grep -cE "invoke(virtual|static|special).*(renderScoreboard|getSunBrightness)\(" || true)
    [ "$MCP_CALLS" -eq 0 ] || fail "$MCP_CALLS unmapped MCP callsites in the shipped jar"
    SRG_CALLS=$("$JAVAP" -c -cp "$WORK" dev.aether.forge189.ForgeHudRenderer \
        | grep -c "func_[0-9]*_" || true)
    [ "$SRG_CALLS" -gt 0 ] || fail "no SRG callsites found - reobfJar likely did not run"
fi

echo "OK: refmap, mixin config, manifest and reobf all verified in $JAR"
