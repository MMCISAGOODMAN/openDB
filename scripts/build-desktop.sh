#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo "==> Building frontend"
cd "$ROOT/frontend"
if [[ -f package-lock.json ]]; then
  npm ci
else
  npm install
fi
npm run build

echo "==> Copying frontend dist to backend static resources"
STATIC_DIR="$ROOT/backend/src/main/resources/static"
rm -rf "$STATIC_DIR"
mkdir -p "$STATIC_DIR"
cp -R dist/* "$STATIC_DIR/"

echo "==> Packaging backend JAR"
cd "$ROOT/backend"
mvn -B package -DskipTests

echo "==> Done: $ROOT/backend/target/opendb-server-0.2.0.jar"
