#!/usr/bin/env bash
set -euo pipefail

OUTPUT="${1:-desktop/jre}"
MODULES="java.base,java.logging,java.sql,java.naming,java.management,java.instrument,java.xml,java.security.jgss,java.net.http,jdk.unsupported"

if [[ -z "${JAVA_HOME:-}" ]]; then
  echo "JAVA_HOME is not set" >&2
  exit 1
fi

rm -rf "$OUTPUT"
"$JAVA_HOME/bin/jlink" \
  --add-modules "$MODULES" \
  --strip-debug \
  --no-man-pages \
  --no-header-files \
  --compress=2 \
  --output "$OUTPUT"

echo "JRE prepared at $OUTPUT"
