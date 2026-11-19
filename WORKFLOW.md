# GitHub Actions workflow — nije add koro

Muse GitHub App diye workflow file push korte pare na, tai ei file-ta
GitHub web-e nije banao:

1. Repo-te jao: `allinonea2z00002-ai/tradesense`
2. `Add file` → `Create new file`
3. Path dao: `.github/workflows/android-build.yml`
4. Niche-r code-ta pura paste koro, tarpor `Commit changes`

```yaml
name: Build APK
on:
  push:
    branches: [main]
  workflow_dispatch:
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Set up Gradle 8.7
        uses: gradle/actions/setup-gradle@v3
        with:
          gradle-version: '8.7'
      - name: Build debug APK
        run: gradle assembleDebug --stacktrace
      - name: Upload APK
        uses: actions/upload-artifact@v4
        with:
          name: tradelens-apk
          path: app/build/outputs/apk/debug/app-debug.apk
```

Tarpor Actions tab-e giye build green hole `tradelens-apk` download koro.
