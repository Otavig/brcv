#!/usr/bin/env bash
# Builds the Linux release: dist/brcv-<version>-linux.tar.gz
# Contents: brcv.jar, brcv-app (own runtime, no Java needed), install.sh, LEIA-ME.md
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

VERSION=$(sed -n 's|.*<version>\(.*\)</version>.*|\1|p' pom.xml | head -1 | tr -d '\r')
NAME="brcv-$VERSION-linux"

need() { command -v "$1" >/dev/null 2>&1 || { echo "Error: '$1' not found. $2" >&2; exit 1; }; }
need java "Install JDK 21+ (sudo apt install openjdk-21-jdk)."
need mvn "Install it (sudo apt install maven)."
need tar "sudo apt install tar"

major=$(java -version 2>&1 | sed -n '1s/[^0-9]*\([0-9]*\).*/\1/p')
(( major >= 21 )) || { echo "Error: Java $major found; need 21+." >&2; exit 1; }

echo "==> Building jar..."
mvn -q clean package
[[ -f target/brcv.jar ]] || { echo "Error: target/brcv.jar not generated." >&2; exit 1; }

rm -rf dist
mkdir -p dist/in dist/tmp "dist/$NAME"
cp target/brcv.jar dist/in/

# bundled runtime needs jpackage + jmods (full JDK) + objcopy; otherwise the package is jar-only
JDK_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v jpackage || echo /nonexistent/x)")")")"
if command -v jpackage >/dev/null && command -v objcopy >/dev/null && [[ -d "$JDK_HOME/jmods" ]]; then
    echo "==> Bundling runtime (jpackage)..."
    jpackage --type app-image --name brcv --app-version "$VERSION" \
        --input dist/in --main-jar brcv.jar --main-class br.com.brcodevalidator.Main \
        --dest dist/tmp --add-modules java.base \
        --jlink-options "--strip-debug --no-man-pages --no-header-files" >/dev/null
    mv dist/tmp/brcv "dist/$NAME/brcv-app"
else
    echo "Warning: full JDK 21 (jpackage + jmods) or binutils missing: package will need Java on the target." >&2
    echo "         Fix: sudo apt install openjdk-21-jdk binutils" >&2
fi

cp target/brcv.jar install.sh "dist/$NAME/"
cp INSTALL-LINUX.md "dist/$NAME/LEIA-ME.md"
chmod +x "dist/$NAME/install.sh"

echo "==> Smoke test..."
[[ -x "dist/$NAME/brcv-app/bin/brcv" ]] && "dist/$NAME/brcv-app/bin/brcv" --version
java -jar "dist/$NAME/brcv.jar" --version >/dev/null

tar -C dist -czf "dist/$NAME.tar.gz" "$NAME"
rm -rf dist/in dist/tmp
echo "==> Done: dist/$NAME.tar.gz"
