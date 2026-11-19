# TradeLens — GitHub Actions workflow

Muse's GitHub App cannot write workflow files, so create this file yourself:
repo → **Add file → Create new file** → path `.github/workflows/android-build.yml` → paste the block below → **Commit changes**.

This version is the same as before, but on failure it prints the real error lines
as annotations (so the error can be seen without opening the log) and uploads
`build.log` as an artifact.

```yaml
name: Build APK

on:
  push:
  workflow_dispatch:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: 17

      - uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: 8.7

      - name: Build debug APK
        run: |
          set +e
          gradle assembleDebug --stacktrace > build.log 2>&1
          CODE=$?
          echo "Gradle exit code: $CODE"
          if [ "$CODE" -ne 0 ]; then
            echo "--- error lines ---"
            grep -E "^e: |FAILURE|What went wrong|error:" build.log | head -40 > error-lines.txt || true
            while IFS= read -r line; do
              esc=$(printf '%s' "$line" | sed 's/%/%25/g; s/\r/%0D/g')
              echo "::error::$esc"
            done < error-lines.txt
          fi
          exit $CODE

      - name: Upload build log
        if: failure()
        uses: actions/upload-artifact@v4
        with:
          name: build-log
          path: build.log

      - uses: actions/upload-artifact@v4
        if: success()
        with:
          name: tradelens-apk
          path: app/build/outputs/apk/debug/app-debug.apk
```
