# Access review — 2026-10-22

The deployed rules were inspected read-only in Firebase Console for project
`agendasacramental-333f2`, default database. The active revision was labeled
March 25, 2026 at 09:55. Nothing was published or changed.

Observed rules authorize `read, write` when `request.auth != null` separately for
`unidades`, `agendas`, `hermanos`, and `configuracion`. They have no rule for
`historialParticipaciones`, so the new history collection is currently denied.
Existing collection rules do not check unit membership. The password comparison
in `ensureUnitAccess` is client-side and is not a server-side authorization boundary.

## Proposed authorization for the new feature

Review `history-access.rules.fragment` before integrating it into the full rules.
Use a separate, client-inaccessible membership path:
`historialAccesos/{numeroUnidad}/miembros/{FirebaseAuthUID}`, with `rol: "editor"`
for writes or another role for read-only access. The console/Admin SDK operator
must verify the person's unit before provisioning access. Do not base membership
on a field in `unidades`: current clients can write that collection.

No membership documents were created. No permissions were granted. Before
publishing, an administrator must approve the policy and the specific users/units,
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

The fragment is tested but is still a proposal, not a deployed policy. Array
elements are validated by the importer; the rules validate the document envelope
and access. Full server-side per-participation validation would require a different
storage layout or a trusted import service.

To reproduce, install dependencies with `npm ci` in `firebase/tests`, start an
isolated Firestore emulator on loopback port 8188 with project
`demo-agenda-history`, then run `npm test` in that folder. Java 21 is required by
the emulator used here. The tests explicitly set the demo project and loopback
endpoint and cannot select production via environment variables.
