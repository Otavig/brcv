#!/usr/bin/env bash
# Usage: ./install.sh [--uninstall]
set -euo pipefail

APP_DIR="$HOME/.local/share/brcv"
BIN_DIR="$HOME/.local/bin"
JAR_DEST="$APP_DIR/brcv.jar"
LAUNCHER="$BIN_DIR/brcv"
JAR_SRC="target/brcv.jar"

say()  { printf '\033[1;32m==>\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33mWarning:\033[0m %s\n' "$*"; }
die()  { printf '\033[1;31mError:\033[0m %s\n' "$*" >&2; exit 1; }

if [[ "${1:-}" == "--uninstall" ]]; then
  rm -rf "$APP_DIR"
  rm -f "$LAUNCHER"
  say "brcv removed."
  exit 0
fi

command -v java >/dev/null 2>&1 || die "Java not found. Install JDK 21+ (e.g. sudo apt install openjdk-21-jdk) and try again."
JAVA_MAJOR=$(java -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+)(\.[0-9]+)?.*/\1/')
if [[ "$JAVA_MAJOR" -lt 21 ]]; then
  die "Java $JAVA_MAJOR found; brcv needs Java 21 or newer."
fi
say "Java $JAVA_MAJOR found."

if [[ ! -f "$JAR_SRC" ]]; then
  command -v mvn >/dev/null 2>&1 || die "target/brcv.jar does not exist and Maven is not installed. Install it (e.g. sudo apt install maven, or brew install maven) and run ./build.sh"
  say "Building the JAR with Maven..."
  mvn -q clean package
fi
[[ -f "$JAR_SRC" ]] || die "Could not find $JAR_SRC."

say "Installing in $APP_DIR"
mkdir -p "$APP_DIR" "$BIN_DIR"
cp "$JAR_SRC" "$JAR_DEST"

cat > "$LAUNCHER" <<LAUNCH
#!/usr/bin/env bash
exec java -jar "$JAR_DEST" "\$@"
LAUNCH
chmod +x "$LAUNCHER"

case ":$PATH:" in
  *":$BIN_DIR:"*) ;;
  *) warn "$BIN_DIR is not in your PATH. Add this to your shell config (~/.bashrc or ~/.zshrc):"
     echo "    export PATH=\"\$HOME/.local/bin:\$PATH\"" ;;
esac

say "Installed. Test with: brcv --version"
