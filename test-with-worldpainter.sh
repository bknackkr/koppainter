#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

WORLDPAINTER_DIR="${WORLDPAINTER_DIR:-${WORLDPAINTER_HOME:-${WP_HOME:-}}}"
JIDE_VERSION="${JIDE_VERSION:-}"
FORCE=false
MAVEN_ARGS=()

while [[ $# -gt 0 ]]; do
    case "$1" in
        -w|--worldpainter-dir)
            WORLDPAINTER_DIR="$2"
            shift 2
            ;;
        --worldpainter-dir=*)
            WORLDPAINTER_DIR="${1#*=}"
            shift 1
            ;;
        -j|--jide-version)
            JIDE_VERSION="$2"
            shift 2
            ;;
        --jide-version=*)
            JIDE_VERSION="${1#*=}"
            shift 1
            ;;
        -f|--force)
            FORCE=true
            shift 1
            ;;
        *)
            MAVEN_ARGS+=("$1")
            shift 1
            ;;
    esac
done

# 0. Configure JAVA_HOME if not already set
if [[ -z "${JAVA_HOME:-}" || ! -x "${JAVA_HOME}/bin/java" ]]; then
    JAVA_CANDIDATES=(
        "/usr/lib/jvm/java-21-openjdk-amd64"
        "/usr/lib/jvm/java-21-openjdk"
        "/usr/lib/jvm/jdk-21"
        "/usr/lib/jvm/default-java"
        "/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home"
        "/Library/Java/JavaVirtualMachines/zulu-21.jdk/Contents/Home"
    )
    for j in "${JAVA_CANDIDATES[@]}"; do
        if [[ -x "$j/bin/java" ]]; then
            export JAVA_HOME="$j"
            echo "Configured JAVA_HOME=$JAVA_HOME"
            break
        fi
    done
fi

# 1. Locate WorldPainter directory
if [[ -z "$WORLDPAINTER_DIR" ]]; then
    CANDIDATES=(
        "/opt/worldpainter"
        "/usr/local/worldpainter"
        "/usr/share/worldpainter"
        "${HOME}/.local/share/worldpainter"
        "${HOME}/worldpainter"
        "/Applications/WorldPainter.app/Contents/Resources/app"
        "/Applications/WorldPainter.app/Contents/Java"
        "${HOME}/Applications/WorldPainter.app/Contents/Resources/app"
        "/c/Program Files/WorldPainter"
        "/mnt/c/Program Files/WorldPainter"
        "C:\\Program Files\\WorldPainter"
    )
    for c in "${CANDIDATES[@]}"; do
        if [[ -d "$c" ]]; then
            WORLDPAINTER_DIR="$c"
            break
        fi
    done
fi

if [[ -z "$WORLDPAINTER_DIR" || ! -d "$WORLDPAINTER_DIR" ]]; then
    echo "Error: WorldPainter directory not found." >&2
    echo "Please specify --worldpainter-dir <path> or set WORLDPAINTER_HOME." >&2
    exit 1
fi

WP_LIB_DIR="$WORLDPAINTER_DIR/lib"
if [[ ! -d "$WP_LIB_DIR" ]]; then
    WP_LIB_DIR="$WORLDPAINTER_DIR"
fi

JIDE_COMMON_JAR="$WP_LIB_DIR/jide-common.jar"
JIDE_DOCK_JAR="$WP_LIB_DIR/jide-dock.jar"

if [[ ! -f "$JIDE_COMMON_JAR" || ! -f "$JIDE_DOCK_JAR" ]]; then
    echo "Error: Could not find jide-common.jar and/or jide-dock.jar in '$WP_LIB_DIR'." >&2
    exit 1
fi

# 2. Detect JIDE version
if [[ -z "$JIDE_VERSION" ]]; then
    if command -v unzip >/dev/null 2>&1; then
        DETECTED=$(unzip -p "$JIDE_COMMON_JAR" META-INF/MANIFEST.MF 2>/dev/null | grep -i "^Jide-Version:" | head -n1 | awk '{print $2}' | tr -d '\r' || true)
        if [[ -n "$DETECTED" ]]; then
            JIDE_VERSION="$DETECTED"
        fi
    elif command -v jar >/dev/null 2>&1; then
        DETECTED=$(jar xf "$JIDE_COMMON_JAR" META-INF/MANIFEST.MF 2>/dev/null && grep -i "^Jide-Version:" META-INF/MANIFEST.MF | head -n1 | awk '{print $2}' | tr -d '\r' && rm -rf META-INF || true)
        if [[ -n "$DETECTED" ]]; then
            JIDE_VERSION="$DETECTED"
        fi
    fi
fi

if [[ -z "$JIDE_VERSION" ]]; then
    JIDE_VERSION="3.8.1"
fi

echo "Target JIDE version: $JIDE_VERSION"

# 3. Locate Maven wrapper
MVNW="$SCRIPT_DIR/mvnw"
if [[ ! -f "$MVNW" ]]; then
    MVNW="mvn"
else
    chmod +x "$MVNW" 2>/dev/null || true
fi

# 4. Check if JIDE jars are already installed in ~/.m2/repository
M2_REPO="${M2_REPO:-${HOME}/.m2/repository}"
INSTALLED_COMMON="$M2_REPO/com/jidesoft/jide-common/$JIDE_VERSION/jide-common-$JIDE_VERSION.jar"
INSTALLED_DOCK="$M2_REPO/com/jidesoft/jide-dock/$JIDE_VERSION/jide-dock-$JIDE_VERSION.jar"

if [[ "$FORCE" = true || ! -f "$INSTALLED_COMMON" || ! -f "$INSTALLED_DOCK" ]]; then
    echo "Installing JIDE $JIDE_VERSION jars into local Maven repository..."
    "$MVNW" install:install-file "-Dfile=$JIDE_COMMON_JAR" "-DgroupId=com.jidesoft" "-DartifactId=jide-common" "-Dversion=$JIDE_VERSION" "-Dpackaging=jar"
    "$MVNW" install:install-file "-Dfile=$JIDE_DOCK_JAR" "-DgroupId=com.jidesoft" "-DartifactId=jide-dock" "-Dversion=$JIDE_VERSION" "-Dpackaging=jar"
    echo "JIDE $JIDE_VERSION installed successfully."
else
    echo "JIDE $JIDE_VERSION is already installed in local Maven repository. Skipping installation."
fi

# 5. Execute mvnw test -P testWithWorldPainter
echo "Running Maven test with WorldPainter profile..."
exec "$MVNW" test -P testWithWorldPainter "${MAVEN_ARGS[@]+"${MAVEN_ARGS[@]}"}"
