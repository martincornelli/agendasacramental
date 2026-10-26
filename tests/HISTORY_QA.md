# History transfer and announcement regression checks

Android bundles the domain modules, Excel worker and SheetJS from `docs/` through the
`syncHistoryAssets` Gradle task. Edit those shared sources; do not copy a separate
parser into Android. The bundled UI uses the native document picker and the signed-in
Firebase session for the unit opened from the planner. It never loads the hosted site.

Run from the repository root:

```powershell
node --test tests/agenda-web.test.cjs tests/participation-history.test.mjs tests/history-workbook.test.mjs tests/hymn-planner-web.test.mjs
node tests/android-history-ui.cjs
node tests/history-import-ui.cjs
node tests/announcements-pdf-ui.cjs
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
$env:ANDROID_SERIAL='emulator-5554'
.\gradlew.bat :app:connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.class=com.example.agendasacramental.HistoryFeatureTest'
```

The browser checks require Playwright on `NODE_PATH` and installed Chrome
(`PLAYWRIGHT_CHANNEL=chrome` for the existing web import test). All browser/native
worker tests use synthetic data, with no production Firebase writes. Select an
emulator explicitly for instrumented tests.

Covered cases include CSV and XLSX round trips in both languages, existing IDs,
explicit corrections, stale previews, omitted rows, Excel templates, imported dates
and topics in the Android planner, long announcements, unbroken URLs, and multiline
Primary presentation wording. The shared workbook unit tests also cover legacy XLS,
Excel date systems, formulas, and size limits.

Announcements now use newline separators. Legacy comma-only values remain intact as
one announcement because a comma in prose cannot safely be distinguished from an old
separator. Users can split those old announcements explicitly when editing them.

The PDF browser check writes examples to `output/history-feature-qa`. Instrumented
PDF checks render examples into the emulator application's cache; Gradle may uninstall
the test application afterward. These are disposable QA artifacts, not release files.
