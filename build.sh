#!/usr/bin/env bash
set -e
cd "$(dirname "${BASH_SOURCE[0]}")"

command -v java >/dev/null 2>&1 || { echo "Java not found. Install JDK 21+ (e.g. sudo apt install openjdk-21-jdk)" >&2; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "Maven not found. Install it (e.g. sudo apt install maven, or brew install maven)" >&2; exit 1; }

mvn -q clean package
java -jar target/brcv.jar "$@"
