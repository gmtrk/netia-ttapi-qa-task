#!/usr/bin/env bash
set -euo pipefail

ALLURE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
QA_DIR="$(cd "$ALLURE_DIR/.." && pwd)"

API_RESULTS="$QA_DIR/api-tests/build/allure-results"
UI_RESULTS="$QA_DIR/ui-tests/allure-results"
OUTPUT="$ALLURE_DIR/report"
ALLURE_BIN="$QA_DIR/ui-tests/node_modules/.bin/allure"

if [ ! -x "$ALLURE_BIN" ]; then
  echo "Allure CLI not found. Run 'npm install' in qa/ui-tests first." >&2
  exit 1
fi

results=()
for dir in "$API_RESULTS" "$UI_RESULTS"; do
  if [ -d "$dir" ] && [ -n "$(ls -A "$dir" 2>/dev/null)" ]; then
    cp "$ALLURE_DIR/categories.json" "$dir/categories.json"
    cp "$ALLURE_DIR/environment.properties" "$dir/environment.properties"
    results+=("$dir")
  fi
done

if [ "${#results[@]}" -eq 0 ]; then
  echo "No Allure results found. Run the API and/or UI suite first." >&2
  exit 1
fi

"$ALLURE_BIN" generate "${results[@]}" --clean -o "$OUTPUT"

echo "Unified report: $OUTPUT/index.html"
echo "Open it with:   \"$ALLURE_BIN\" open \"$OUTPUT\""
