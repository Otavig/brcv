#!/usr/bin/env bash
set -e
cd "$(dirname "${BASH_SOURCE[0]}")"
mvn -q clean package
java -jar target/brcv.jar "$@"