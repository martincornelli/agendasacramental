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

## Physical device testing alongside Play Store

Build `:app:assembleQa` to install `app/build/outputs/apk/qa/app-qa.apk` as
`com.martincornelli.agendasacramental.qa`. This debug-signed variant has its own app
label and Firebase Android registration, so it does not replace the Play Store app.
Use only the explicitly authorized test unit (123456); Google sign-in alone does not
identify a safe test unit. Additional developer signing certificates must be registered
with the QA Firebase app for Google sign-in to work.

On a physical device, check the native document picker with CSV and Excel from
Downloads, persist selected rows, then reimport to check duplicates. Android may label
CSV as `text/comma-separated-values`; that MIME type must remain accepted alongside
`text/csv`. Check exports with their existing IDs, appended rows, explicit corrections
and omitted rows. In-app update prompts require a Play-installed build and a real
Play update path; this separate QA package cannot validate that behavior.

Physical QA on 2026-10-27 (REDMI Note 15 5G, unit 123456): CSV saved two new
participations; XLSX containing those two plus one new row skipped both duplicates
and saved one. Exporting all 40 records, appending one row in Excel and reimporting
reported 40 unchanged and one new. A CSV containing only one existing ID with a
changed topic required explicit selection. The final native CSV export contained
41 unique IDs: all previous rows survived, with only the selected topic changed.
The planner displayed the imported talk/prayer ages and corrected topic. A saved
Primary agenda with a long comma-containing announcement opened in Acrobat in both
Spanish and English: one announcement, wrapped text through its final sentence,
and the complete photography/video notice with General Handbook 29.8.
