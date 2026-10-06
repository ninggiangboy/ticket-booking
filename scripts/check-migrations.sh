#!/usr/bin/env bash
# Migration rules of DOC-63 §4.1: name pattern, immutability against the target branch, unique versions.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"
BASE="origin/${BASE_REF:-dev}"
DIRS=(backend/src/main/resources/db/migration backend/src/main/resources/db/migration-fake backend/src/main/resources/db/migration-experiment)
bad=0
files=$(git ls-files "${DIRS[@]}")
for f in $files; do
  [[ $(basename "$f") =~ ^V[0-9]{12}__[a-z0-9_]+\.sql$ ]] || { echo "bad name: $f"; bad=1; }
done
if git rev-parse --verify -q "$BASE" >/dev/null; then
  changed=$(git diff --name-status "$BASE"...HEAD -- "${DIRS[@]}" | awk '$1 ~ /^[MDR]/' || true)
  [ -z "$changed" ] || { echo "immutable migration changed:"; echo "$changed"; bad=1; }
fi
if [ -n "$files" ]; then
  dups=$(echo "$files" | xargs -n1 basename | cut -c1-13 | sort | uniq -d)
  [ -z "$dups" ] || { echo "duplicate version prefix: $dups"; bad=1; }
fi
exit $bad
