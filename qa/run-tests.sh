#!/usr/bin/env bash
set -uo pipefail

QA_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
API_DIR="$QA_DIR/api-tests"
UI_DIR="$QA_DIR/ui-tests"
API_RESULTS="$API_DIR/build/allure-results"

if [ ! -x "$UI_DIR/node_modules/.bin/allure" ]; then
  echo "==> Installing UI test dependencies (npm install)"
  (cd "$UI_DIR" && npm install)
fi

echo "==> Running API tests (Docker)"
rm -rf "$API_RESULTS"
mkdir -p "$API_RESULTS"
docker run --rm --network host --env-file "$QA_DIR/credentials.env" \
  -v "$API_DIR":/src:ro \
  -v "$API_RESULTS":/out \
  gradle:9.4.1-jdk21-ubi \
  sh -c "cp -r /src /home/gradle/proj && cd /home/gradle/proj && gradle test --no-daemon --console=plain -Dallure.results.directory=/out"
API_RC=$?

echo "==> Running UI tests (Playwright)"
rm -rf "$UI_DIR/allure-results"
(cd "$UI_DIR" && npx playwright test)
UI_RC=$?

echo "==> Building unified Allure report"
"$QA_DIR/allure/generate-report.sh"

echo
echo "API tests exit code: $API_RC (non-zero is expected — the documented defect tests fail by design)"
echo "UI tests exit code:  $UI_RC"
