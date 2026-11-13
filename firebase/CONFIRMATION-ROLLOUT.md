# Organizations and assignment acceptance — 1.7.0 / 47

Android and web share `organizacion` on `hermanos`, with these canonical values:
`ELDERS_QUORUM`, `RELIEF_SOCIETY`, `YOUNG_MEN`, `YOUNG_WOMEN`, `PRIMARY`, `BISHOPRIC`.
Empty or absent values mean unspecified. Labels are translated in both clients.

Agendas opt into `confirmacionVersion: 1`. They store `aceptaciones` keyed by
assignment ID; each record contains `firma`, `registradoPor`, `registradoEn`.
Messages have stable IDs. Legacy messages get deterministic `legacy-{index}` IDs
on initial reading, retained on the first write. New messages use UUIDs.
Each signature encodes the calendar date, meeting type, assignment ID, person
and relevant content using UTF-16 length-prefixed values. Both clients implement
the same contract and test a Unicode example.

Required checks: presiding, conducting, both prayers, and each of the three main
hymns separately for pianist and music director. Regular meetings additionally
require at least one talk/testimony and acceptance of every planned message.
Intermediate hymns require both musicians. Testimony meetings do not require
planned speakers. Primary presentations require `responsablePrimaria` instead.
Empty requirements cannot be checked. A talk topic is optional; changing it
invalidates an existing acceptance. Announcements and attendance are not checks.

The editor previews automatic status; changes and acceptances persist on Save.
The Draft / Confirmed / Completed selector is also available. Manual confirmation
accepts every assignment (only when all required data is complete); choosing Draft
preserves acceptances and derives the status from them (all accepted stays Confirmed). Completed preserves acceptances and remains independent.
Changing the date/type invalidates all checks. Changing a person, topic or hymn
invalidates only affected checks. Reordering retains IDs and checks. `REALIZADA`
remains independent. Legacy confirmed/completed records are not backfilled with
invented acceptances. Saving a legacy agenda in the new editor explicitly opts
it into individual checks, with an explanatory message.

Agenda edits use transactions and a `revision` plus last-edit timestamp check.
The hymn planner, topic assignment and prayer/talk assignment also reconcile
acceptances atomically. Live web snapshots do not overwrite an open editor.
Conflicting saves require reopening the agenda. Transactional saves need a
connection; the UI reports success only after the save completes.

## Release order

1. Apply the updated `/agendas/{agendaId}` rule from `firestore.rules` before
   users start recording acceptances. Compare against deployed rules first and
   preserve unrelated rules. No production rules are deployed by local tests.
2. Publish the Android release and the web assets together with the new module
   `agenda-confirmation.mjs` and updated service-worker cache.
3. Old clients can still read agendas and edit legacy records. Writes to agendas
   already using acceptance tracking require the new schema and next revision;
   old replacement writes are rejected instead of deleting acceptance data.

The rule protects schema/revision compatibility; it does not independently
recalculate the business rules. Existing authentication/unit-access policy is
unchanged. There is no background migration of historical agendas.

Notification entry now retains its destination through login/unit selection and
handles `onNewIntent`. It never opens an editor without an established unit.
The editor rejects a mismatched/unloaded agenda and preserves the document unit
when saving. Repository and Firestore rules also reject changes to an existing
agenda's unit. This prevents the previous notification path from saving an empty
unit and making the agenda disappear from the unit's query.

## Validation

- `node --test tests/agenda-confirmation.test.mjs tests/agenda-web.test.cjs tests/hymn-planner-web.test.mjs tests/web-i18n.test.mjs`
- `node tests/agenda-confirmation-ui.cjs` and `node tests/web-i18n-ui.cjs`
  (Playwright/Chrome, simulated Firebase, both languages and viewport sizes).
- `gradlew :app:testDebugUnitTest :app:assembleDebug :app:bundleRelease`
- `node --test firebase/tests/confirmation-rules.test.mjs`
  (loopback Firestore emulator at 8191, `demo-agenda-confirmation` only).
