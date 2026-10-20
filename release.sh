#!/usr/bin/env bash
# Gera a distribuição com o JAR compilado e os instaladores (sem o código-fonte).
set -euo pipefail

VERSION="1.0.0"
cd "$(dirname "${BASH_SOURCE[0]}")"

mvn -q clean package

NAME="brcv-$VERSION"
rm -rf dist
mkdir -p "dist/$NAME"
cp target/brcv.jar "dist/$NAME/brcv.jar"
cp install.sh install.ps1 "dist/$NAME/"
cp INSTALL-LINUX.md "dist/$NAME/LEIA-ME-LINUX.md"
cp INSTALL-WINDOWS.md "dist/$NAME/LEIA-ME-WINDOWS.md"

(cd dist && zip -qr "$NAME.zip" "$NAME")
echo "Distribuição pronta: dist/$NAME.zip"
