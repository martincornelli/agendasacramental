# Hymn topics and Android refresh

The branch ships official topic metadata for 730 localized hymn records: Spanish
209 traditional + 90 Home and Church; English 341 traditional + 90 Home and Church.
The source is the public `song-filtered-list` API used by the two Church music
collection pages. Each language/book response was checked against its `song-book`
index. All hymn numbers already shipped by the app are covered. Retrieval date:
2026-11-16. The data contains titles and topics, not lyrics or audio.

`docs/hymn-topics-data.mjs` retains exact source tags, slugs, source URLs, and counts.
The API publishes no topics for 21 records; these stay empty. Nine untranslated
English tags in the Spanish source have explicit display translations in both
clients. Topic associations are no longer inferred from hymn numbers or titles.
Sacrament-slot suitability remains a separate, unchanged curated constraint.

To refresh the catalog from the official source:

```sh
node tools/fetch-hymn-topics.mjs
node tools/build-hymn-topics.mjs
```

The second command generates both clients from the same downloaded records.
Review the source changes and rerun the coverage tests before shipping updates.

## Unit edits

Overrides live at `temasHimnosUnidad/{unit}/himnos/{language}-{number}`. The fields
are `numeroUnidad`, `numero`, `idioma`, `temas` and `actualizadoEn`. Clients pass
the current unit's overrides explicitly into filtering and generation; there is
no process-global override cache. Each write changes one hymn, avoiding lost
updates to other hymns and avoiding older clients overwriting new fields in the
planning configuration. Same-hymn edits use last-write-wins semantics.

An absent document means official defaults. An empty topic list intentionally
means no topics. Restore deletes only that hymn's override. Both clients listen
for unit changes and prevent editing/generating before overrides load. Custom
topics are user content and are not machine-translated into the other catalog.

The rules follow this project's existing authenticated-client compatibility
policy, like planning/history data. Unit scoping separates storage and checks
payload identity; it is **not** a new server-enforced unit-membership system.
The existing shared-password flow does not issue membership claims. A malicious
authenticated client can still target another unit's path under that existing
policy. Introducing server-enforced membership across the app is a separate
authorization migration.

Deploy the additive `hymn-topics.rules.fragment` inside the database match of the
currently deployed rules **before** releasing either client. It is also included
in `firestore.rules` for emulator testing. Do not replace production rules with
an outdated repository snapshot.

On 2026-11-17 the additive fragment was tested against the exact live rules and
published as ruleset `2ec386e2-5111-4711-8abd-0d93dbd73c26`. The previous release
was `c8d64d5a-f22f-4847-b3aa-2f90447deb40`; backups and the verified release receipt
are in local `output/hymn-topics-release/`. Existing rule content was preserved.
The regular Android package was installed on the user's phone. The user confirmed
web-to-Android synchronization and verified another unit retained official defaults
using the local preview before authorizing publication of the web client.

## Refresh

Android Agendas, Participation Planning (all tabs), and Hymn Planning (both tabs)
use Material pull-to-refresh. Hymn Planning retains a quick Refresh button;
Agendas and Participation Planning expose a TalkBack refresh action without a
dedicated header row. User refreshes
request server data, keep existing content/filters/scroll position, suppress
duplicate refreshes and report failures. Startup reads retain offline fallback.
Editing forms and web do not add a refresh gesture.

## Compact library and seasonal generation

Library filters open on demand; cards initially show two topics with an expandable
remainder. Both clients retain search and show an indicator when filters are active.
The web Library uses a compact year header. This follows the UI/UX skill's
documented accessibility, touch-target, and progressive-disclosure checklist as
general guidance: its targeted search returned no relevant verified match.

`modoTemporadaHimnos` defaults to `NONE`, with `ONE_CHRISTMAS`, `CHRISTMAS_ONLY`,
and `EASTER_ONLY` choices. The mode applies to every generated meeting and uses
the effective hymn topics. Christmas and Easter/Resurrection categories restrict
non-sacrament candidate pools; sacrament eligibility remains independent of topics.
One-Christmas reserves an editable non-sacrament slot, independent of talk topics.
The new-hymnbook pass respects that reservation and preserved automatic selections.
Manual selections remain untouched; conflicts and exhausted seasonal pools are
reported in the preview. A missing seasonal candidate does not fall back to an
ordinary hymn. Generation still requires preview and explicit saving.

## Verification

```sh
node --test tests/hymn-topics.test.mjs tests/hymn-planner-web.test.mjs
node --test tests/hymn-seasonal.test.mjs
node tests/hymn-topics-ui.cjs
node tests/hymn-topics-phone.cjs DEVICE_SERIAL
./gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest
node --test firebase/tests/hymn-topics-rules.test.mjs
```

The UI test needs Playwright on `NODE_PATH` and Chrome. It uses mocked Firebase,
covering Spanish/English, portrait/landscape, light/dark, failed save/retry,
filtering and restore. Rule tests use only a loopback emulator on port 8188 with
a `demo-` project, including maximum-size edits and rejection of malformed data.
The phone test installs only the `.design.qa` package and exercises the real
refresh gesture and editor with an in-memory QA fixture, in both languages and
with large text/dark landscape. It never reads or writes a production database.

Verification on this branch: 43 Android unit tests, 50 web domain tests, all eight
browser UI combinations, and three Firestore emulator tests passed. Android debug
compilation and the QA APK build passed. After the user accepted installation,
the physical phone tests passed in Spanish and English, light portrait and
dark/large-text landscape. They verify the actual pull gesture, refresh button,
editor localization, visible dialog actions and cancellation. Screenshot review
found and fixed clipped dialog controls in large-text landscape. The installed
QA fixture uses temporary in-memory data; live unit synchronization remains
covered by the web tests and Firestore emulator tests, not this phone fixture.

The November 17 follow-up passed Android compilation/unit tests and regular APK
assembly, 12 additional bilingual seasonal domain cases, all eight browser UI
combinations including seasonal selection/preview, and all three rule tests using
the exact proposed live rules (`FIRESTORE_RULES_FILE` override). On the regular
phone app, live topic loading, opening/canceling the enabled topic editor, the
filter dialog, and all four seasonal choices were verified without saving changes.
