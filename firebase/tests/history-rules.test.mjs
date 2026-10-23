import { before, after, beforeEach, test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, collection, getDoc, getDocs, setDoc, updateDoc, deleteDoc, serverTimestamp, runTransaction, setLogLevel } from 'firebase/firestore';

// This harness is pinned to a loopback emulator and a demo project. It cannot
// select the production project or use live credentials through environment vars.
const fragment = fs.readFileSync(new URL('../history-access.rules.fragment', import.meta.url), 'utf8');
const rules = `rules_version = '2'; service cloud.firestore { match /databases/{database}/documents { ${fragment} } }`;
let env;
setLogLevel('silent');
const history = db => doc(db, 'historialParticipaciones', '123');
const valid = () => ({ numeroUnidad: '123', revision: 1, records: [], actualizadoEn: serverTimestamp(), actualizadoPor: 'test@example.com' });
before(async () => {
  env = await initializeTestEnvironment({ projectId: 'demo-agenda-history', firestore: { host: '127.0.0.1', port: 8188, rules } });
});
after(async () => { await env?.cleanup(); });
beforeEach(async () => {
  await env.clearFirestore();
  await env.withSecurityRulesDisabled(async context => {
    for (const [uid, rol] of [['editor', 'editor'], ['reader', 'lector']]) {
      await setDoc(doc(context.firestore(), 'historialAccesos/123/miembros/' + uid), { rol });
    }
  });
});

test('unauthenticated users and nonmembers cannot read or write history', async () => {
  for (const context of [env.unauthenticatedContext(), env.authenticatedContext('stranger')]) {
    await assertFails(getDoc(history(context.firestore())));
    await assertFails(setDoc(history(context.firestore()), valid()));
  }
});

test('members can read their own unit, but not another unit or list the collection', async () => {
  const db = env.authenticatedContext('reader').firestore();
  await assertSucceeds(getDoc(history(db)));
  await assertFails(getDoc(doc(db, 'historialParticipaciones/456')));
  await assertFails(getDocs(collection(db, 'historialParticipaciones')));
  await assertFails(setDoc(history(db), valid()));
});

test('editors can create and update with a consecutive revision, but cannot delete', async () => {
  const db = env.authenticatedContext('editor').firestore();
  await assertSucceeds(setDoc(history(db), valid()));
  await assertSucceeds(updateDoc(history(db), { revision: 2, actualizadoEn: serverTimestamp() }));
  await assertFails(updateDoc(history(db), { revision: 2, actualizadoEn: serverTimestamp() }));
  await assertFails(deleteDoc(history(db)));
});

test('clients cannot provision, read, change or remove membership', async () => {
  const db = env.authenticatedContext('editor').firestore();
  await assertFails(setDoc(doc(db, 'historialAccesos/456/miembros/editor'), { rol: 'editor' }));
  const ref = doc(db, 'historialAccesos/123/miembros/editor');
  await assertFails(getDoc(ref));
  await assertFails(updateDoc(ref, { rol: 'owner' }));
  await assertFails(deleteDoc(ref));
});

test('invalid unit, schema, revision, timestamp and list size are rejected', async () => {
  const db = env.authenticatedContext('editor').firestore();
  for (const patch of [{ numeroUnidad: '456' }, { revision: 2 }, { revision: '1' }, { records: {} }, { records: Array(5001).fill(0) }, { unexpected: true }, { actualizadoEn: null }]) {
    await assertFails(setDoc(history(db), { ...valid(), ...patch }));
  }
  const missing = valid(); delete missing.records;
  await assertFails(setDoc(history(db), missing));
});

test('atomic import persists, rejects a stale preview and retains records omitted from a correction', async () => {
  const db = env.authenticatedContext('editor').firestore();
  const first = { id: '123:import:one', date: '2026-01-04', name: 'Ana', type: 'Discurso', topic: 'Fe' };
  const second = { ...first, id: '123:import:two', name: 'Juan' };
  const apply = (expected, changes) => runTransaction(db, async transaction => {
    const snapshot = await transaction.get(history(db));
    const current = snapshot.data() || { revision: 0, records: [] };
    if (current.revision !== expected) throw new Error('stale-preview');
    const records = new Map(current.records.map(r => [r.id, r]));
    changes.forEach(r => records.set(r.id, r));
    transaction.set(history(db), { ...valid(), revision: expected + 1, records: [...records.values()] });
  });
  await apply(0, [first, second]);
  await assert.rejects(apply(0, [{ ...first, topic: 'Unexpected' }]), /stale-preview/);
  await apply(1, [{ ...first, topic: 'Esperanza' }]);
  const data = (await getDoc(history(db))).data();
  assert.equal(data.records.length, 2);
  assert.equal(data.records[0].topic, 'Esperanza');
  assert.equal(data.records[1].name, 'Juan');
});
