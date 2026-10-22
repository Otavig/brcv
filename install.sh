#!/usr/bin/env bash
# brcv installer for Linux/macOS. Usage: ./install.sh [--uninstall]
set -euo pipefail
HERE="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
APP_DIR="$HOME/.local/share/brcv"
BIN_DIR="$HOME/.local/bin"
say() { printf '\033[1;32m==>\033[0m %s\n' "$*"; }
die() { printf '\033[1;31mError:\033[0m %s\n' "$*" >&2; exit 1; }

if [[ "${1:-}" == "--uninstall" ]]; then
    rm -rf "$APP_DIR" "$BIN_DIR/brcv"
    say "brcv removed."; exit 0
fi

mkdir -p "$BIN_DIR"
rm -rf "$APP_DIR"

if [[ -x "$HERE/brcv-app/bin/brcv" ]]; then
    # bundled runtime: no Java needed
    cp -r "$HERE/brcv-app" "$APP_DIR"
    ln -sf "$APP_DIR/bin/brcv" "$BIN_DIR/brcv"
else
    JAR="$HERE/brcv.jar"; [[ -f "$JAR" ]] || JAR="$HERE/target/brcv.jar"
    [[ -f "$JAR" ]] || die "brcv.jar not found."
    command -v java >/dev/null 2>&1 || die "Java 21+ not found (sudo apt install openjdk-21-jdk)."
    major=$(java -version 2>&1 | sed -n '1s/[^0-9]*\([0-9]*\).*/\1/p')
    (( major >= 21 )) || die "Java $major found; brcv needs 21 or newer."
    mkdir -p "$APP_DIR"; cp "$JAR" "$APP_DIR/brcv.jar"
    printf '#!/usr/bin/env bash\nexec java -jar "%s/brcv.jar" "$@"\n' "$APP_DIR" > "$BIN_DIR/brcv"
    chmod +x "$BIN_DIR/brcv"
fi

case ":$PATH:" in
    *":$BIN_DIR:"*) ;;
    *)  for rc in "$HOME/.bashrc" "$HOME/.zshrc"; do
            [[ -f "$rc" ]] && echo 'export PATH="$HOME/.local/bin:$PATH"' >> "$rc"
        done
        say "Added $BIN_DIR to PATH. Open a new terminal." ;;
esac
say "Installed. Test with: brcv --version"
