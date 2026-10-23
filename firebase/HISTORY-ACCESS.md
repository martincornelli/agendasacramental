# Access review — 2026-10-22

## Current decision — 2026-10-23

The user explicitly requested finishing Excel with the existing app access model
and handling the broader unit-authorization flaw separately. The compatibility
fragment now requires authentication for history get/create/update, without the
pilot unit or membership restrictions. It retains schema/revision validation and
denies list/delete. This does not establish server-side unit isolation; that known
limitation is the same as the existing collections. Anonymous access is denied.

The unpublished security work is isolated on `codex/unit-access-security` and must
not be deployed as part of this fix. No Android update is needed for Excel access.
The six updated emulator tests verify this compatibility policy, not the earlier
member-only policy documented below. The browser regression test additionally
checks that an open file dialog unlocks when history finishes loading, and locks
again if access fails. Historical notes below describe the initial pilot.

Published compatibility release:
`projects/agendasacramental-333f2/rulesets/a97a9c76-2b93-461f-8074-b2d05b4ae8d2`.
The prior release was backed up locally before publishing; the original rules
for unidades/agendas/hermanos/configuracion were preserved byte-for-byte.
Publication checked that the active release had not changed since the backup,
and verified the new release reference afterwards. No live unit data was written.

The deployed rules were inspected read-only in Firebase Console for project
`agendasacramental-333f2`, default database. The active revision was labeled
March 25, 2026 at 09:55. This initial review did not change anything; the limited
pilot described below was subsequently enabled.

Observed rules authorize `read, write` when `request.auth != null` separately for
`unidades`, `agendas`, `hermanos`, and `configuracion`. They have no rule for
`historialParticipaciones`, so the new history collection was denied at that point.
Existing collection rules do not check unit membership. The password comparison
in `ensureUnitAccess` is client-side and is not a server-side authorization boundary.

## Proposed authorization for the new feature

Review `history-access.rules.fragment` before integrating it into the full rules.
Use a separate, client-inaccessible membership path:
`historialAccesos/{numeroUnidad}/miembros/{FirebaseAuthUID}`, with `rol: "editor"`
for writes or another role for read-only access. The console/Admin SDK operator
must verify the person's unit before provisioning access. Do not base membership
on a field in `unidades`: current clients can write that collection.

Before enabling additional units, an administrator must approve the policy and the specific users/units,
review the complete rules for overlapping broad matches, and provision membership.
The fragment intentionally does not change existing Android or web collection
rules: closing the pre-existing cross-unit gap requires a coordinated auth migration.

## Emulator validation

Six tests in `tests/history-rules.test.mjs` passed against Firestore emulator 1.22.0
on 2026-10-22, using only `demo-agenda-history` on `127.0.0.1:8188`:

1. Unauthenticated access: deny; authenticated nonmember: deny.
2. Unit A member: get A allowed, get B denied, collection list denied.
3. Reader: get allowed, writes denied; editor: valid create/update allowed.
4. Client membership creation or edit: deny; history deletion: deny.
5. Wrong unit, wrong revision, omitted/extra fields, oversized records list: deny.
6. Atomic import persists, a stale preview is rejected, and a correction retains
   existing rows that were absent from the imported file. This test simulates a
   stale preview sequentially; it does not force a simultaneous transaction retry.

The unrestricted fragment is still a proposal; the pilot adds an explicit unit restriction. Array
elements are validated by the importer; the rules validate the document envelope
and access. Full server-side per-participation validation would require a different
storage layout or a trusted import service.

To reproduce, install dependencies with `npm ci` in `firebase/tests`, start an
isolated Firestore emulator on loopback port 8188 with project
`demo-agenda-history`, then run `npm test` in that folder. Java 21 is required by
the emulator used here. The tests explicitly set the demo project and loopback
endpoint and cannot select production via environment variables.

## Limited live pilot — October 2026

The new history rules were added in Firebase Console with the additional condition
`unidad == '123456'` in `puedeLeerHistorial`. Existing collection rules were retained.
Only the explicitly authorized disposable unit 123456 is enabled. One editor
membership was provisioned for the account authorized by the user. The general
fragment above must not be mistaken for the exact deployed pilot policy.

On October 23, the local web build successfully imported an Excel containing two
synthetic participations and one repeated row. Preview showed two new records and
one omitted match; Firebase save completed with "Se guardaron 2 participaciones."
Both appeared in planning. The downloaded Excel contained 31 participation rows,
including exactly those two synthetic records with stable import IDs.

The live round trip was then completed in a fresh browser tab (also confirming
persistence): retain the exported rows, change Ana's topic from Fe to Esperanza,
omit Luis, and add Eva with a blank ID. Preview showed 29 unchanged, one correction
(unchecked until explicitly selected), and one new record. Saving succeeded.
The subsequent export contained 32 unique IDs: all 31 previous IDs survived,
Ana's topic changed, every other previous row matched exactly (including omitted
Luis), and Eva appeared once. Reimporting that final export showed zero new,
zero corrections, and 32 unchanged rows. No production unit data was touched.

The 38 web/importer/workbook tests also passed on October 23. The six emulator
checks above are separate validation from October 22.

`tests/serve-history-pilot.cjs` serves the web build at `http://localhost:4180`,
restricting the account and unit before subscriptions and unit access. It does not
store a password or install a service worker. This local guard is an additional
test precaution, not a substitute for Firebase authorization.
