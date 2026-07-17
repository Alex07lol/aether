#!/usr/bin/env sh
set -eu

PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
JAVA8_HOME=${JAVA8_HOME:-/usr/lib/jvm/java-8-openjdk}
GRADLE_HOME=${AETHER_GRADLE_HOME:-/home/laptop/.gradle/wrapper/dists/gradle-7.6.4-bin/eq47ircvekz8t0m2yd28ebfsh/gradle-7.6.4}

if [ ! -x "$JAVA8_HOME/bin/java" ]; then
  echo "Java 8 not found at $JAVA8_HOME. Set JAVA8_HOME to a Java 8 JDK." >&2
  exit 1
fi

if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  echo "Gradle 7.6.4 not found at $GRADLE_HOME. Set AETHER_GRADLE_HOME to a Java-8-compatible Gradle install." >&2
  exit 1
fi

export JAVA_HOME="$JAVA8_HOME"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-$PROJECT_DIR/.gradle-home-java8}"

exec "$GRADLE_HOME/bin/gradle" "$@"
