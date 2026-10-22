#!/bin/sh
# Refreshes api/operations.json from Hammer's operation registry: exports a hammer-editor revision to a
# temporary directory, adds tools/operations/DumpOperationsTest.kt to its operations module, and runs it.
# The test writes what ops.list returns for every core operation, with each type's host name (x-name) and
# each field's KDoc (description) added to its schema.
# Usage: dump-operations.sh [hammer-editor checkout] [revision]
#   defaults: ../hammer-editor beside this repo, and origin/plugin-architecture
set -e
cd "$(dirname "$0")/.."
sdk=$(pwd)
editor=$(cd "${1:-../hammer-editor}" && pwd)
revision=${2:-origin/plugin-architecture}
work=$(mktemp -d)
trap 'rm -rf "$work"' EXIT
git -C "$editor" archive "$revision" | tar -x -C "$work"
cp tools/operations/DumpOperationsTest.kt "$work/operations/src/desktopTest/kotlin/operations/"
(cd "$work" && ./gradlew -q :operations:desktopTest --tests operations.DumpOperationsTest)
cp "$work/operations/build/operations.json" "$sdk/api/operations.json"
echo "Wrote api/operations.json from hammer-editor $revision ($(git -C "$editor" rev-parse --short "$revision"))"
