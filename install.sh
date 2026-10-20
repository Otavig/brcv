#!/usr/bin/env bash
# Uso: ./install.sh [--uninstall]
set -euo pipefail

APP_DIR="$HOME/.local/share/brcv"
BIN_DIR="$HOME/.local/bin"
JAR_DEST="$APP_DIR/brcv.jar"
LAUNCHER="$BIN_DIR/brcv"
JAR_SRC="target/brcv.jar"

say()  { printf '\033[1;32m==>\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33mAviso:\033[0m %s\n' "$*"; }
die()  { printf '\033[1;31mErro:\033[0m %s\n' "$*" >&2; exit 1; }

if [[ "${1:-}" == "--uninstall" ]]; then
  rm -rf "$APP_DIR"
  rm -f "$LAUNCHER"
  say "brcv removido."
  exit 0
fi

command -v java >/dev/null 2>&1 || die "Java não encontrado. Instale o JDK 21+ e tente de novo."
JAVA_MAJOR=$(java -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+)(\.[0-9]+)?.*/\1/')
if [[ "$JAVA_MAJOR" -lt 21 ]]; then
  die "Java $JAVA_MAJOR detectado; o brcv precisa do Java 21 ou superior."
fi
say "Java $JAVA_MAJOR encontrado."

if [[ ! -f "$JAR_SRC" ]]; then
  command -v mvn >/dev/null 2>&1 || die "target/brcv.jar não existe e o Maven não está instalado. Rode 'mvn package' primeiro."
  say "Gerando o JAR com Maven..."
  mvn -q clean package
fi
[[ -f "$JAR_SRC" ]] || die "Não encontrei $JAR_SRC."

say "Instalando em $APP_DIR"
mkdir -p "$APP_DIR" "$BIN_DIR"
cp "$JAR_SRC" "$JAR_DEST"

cat > "$LAUNCHER" <<LAUNCH
#!/usr/bin/env bash
exec java -jar "$JAR_DEST" "\$@"
LAUNCH
chmod +x "$LAUNCHER"

case ":$PATH:" in
  *":$BIN_DIR:"*) ;;
  *) warn "$BIN_DIR não está no PATH. Adicione ao seu shell (~/.bashrc ou ~/.zshrc):"
     echo "    export PATH=\"\$HOME/.local/bin:\$PATH\"" ;;
esac

say "Instalado. Teste com: brcv --version"