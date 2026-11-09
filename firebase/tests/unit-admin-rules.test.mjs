import { before, after, beforeEach, test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, getDoc, setDoc, updateDoc, deleteDoc, serverTimestamp, runTransaction, setLogLevel } from 'firebase/firestore';
import { adminTransition } from '../../docs/unit-admin.mjs';

const fragment = fs.readFileSync(new URL('../unit-admin.rules.fragment', import.meta.url), 'utf8');
const rules = fs.readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8');
assert.ok(rules.includes(fragment), 'The deployable rules must contain the tested administrator policy');
let env;
setLogLevel('silent');
const identity = uid => ({ uid, email: `${uid}@example.com` });
const db = (uid, overrides = {}) => env.authenticatedContext(uid, { email: identity(uid).email, email_verified: true, firebase: { sign_in_provider: 'google.com' }, ...overrides }).firestore();
const ref = database => doc(database, 'unidades/123');
const initial = () => ({ numeroUnidad: '123', passwordHash: 'a'.repeat(64), creadoPor: 'owner@example.com', creadoEn: serverTimestamp(), administradores: { owner: 'owner@example.com' }, invitacionesAdmin: [] });
async function seed(data) { await env.withSecurityRulesDisabled(context => setDoc(ref(context.firestore()), data)); }
async function change(uid, action, value) {
  const database = db(uid);
  return runTransaction(database, async tx => {
    const snapshot = await tx.get(ref(database));
    tx.update(ref(database), adminTransition(snapshot.data(), identity(uid), action, value));
  });
}
before(async () => { env = await initializeTestEnvironment({ projectId: 'demo-agenda-admin', firestore: { host: '127.0.0.1', port: 8188, rules } }); });
after(async () => { await env?.cleanup(); });
beforeEach(async () => { await env.clearFirestore(); });

test('only verified Google creator can create; an existing unit cannot be overwritten', async () => {
  await assertFails(setDoc(ref(env.unauthenticatedContext().firestore()), initial()));
  await assertFails(setDoc(ref(db('owner', { email_verified: false })), initial()));
  await assertFails(setDoc(ref(db('owner', { firebase: { sign_in_provider: 'password' } })), initial()));
  await assertFails(setDoc(ref(db('stranger')), initial()));
  await assertSucceeds(setDoc(ref(db('owner')), initial()));
  await assertFails(setDoc(ref(db('stranger')), { ...initial(), creadoPor: 'stranger@example.com', administradores: { stranger: 'stranger@example.com' } }));
  await assertFails(deleteDoc(ref(db('owner'))));
});

test('legacy creator claims once; cannot change password or creator while claiming', async () => {
  const { administradores, invitacionesAdmin, ...legacy } = initial();
  await seed({ ...legacy, creadoPor: 'Owner@Example.com' });
  const patch = { administradores: { owner: 'owner@example.com' }, invitacionesAdmin: [] };
  await assertFails(updateDoc(ref(db('stranger')), { administradores: { stranger: 'stranger@example.com' }, invitacionesAdmin: [] }));
  await assertFails(updateDoc(ref(db('owner')), { ...patch, passwordHash: 'b'.repeat(64) }));
  await assertSucceeds(change('owner', 'claim'));
  await assertSucceeds(updateDoc(ref(db('owner')), { passwordHash: 'b'.repeat(64) }));
  await assertFails(updateDoc(ref(db('owner')), { creadoPor: 'stranger@example.com' }));
});

test('only administrators change passwords, and the last administrator cannot leave', async () => {
  await seed(initial());
  await assertFails(updateDoc(ref(db('stranger')), { passwordHash: 'b'.repeat(64) }));
  await assertFails(updateDoc(ref(db('owner')), { administradores: {} }));
  await assertFails(updateDoc(ref(db('owner')), { administradores: { owner: 'owner@example.com', other: 'other@example.com' } }));
  await assertSucceeds(updateDoc(ref(db('owner')), { passwordHash: 'b'.repeat(64) }));
});

test('invitation grants no role until the intended verified account accepts', async () => {
  await seed(initial());
  await assertSucceeds(change('owner', 'invite', 'Other@Example.com'));
  await assertFails(updateDoc(ref(db('other')), { passwordHash: 'b'.repeat(64) }));
  const patch = { administradores: { owner: 'owner@example.com', other: 'other@example.com' }, invitacionesAdmin: [] };
  await assertFails(updateDoc(ref(db('stranger')), patch));
  await assertFails(updateDoc(ref(db('other', { email_verified: false })), patch));
  await assertFails(updateDoc(ref(db('other')), { ...patch, passwordHash: 'b'.repeat(64) }));
  await assertFails(updateDoc(ref(db('other')), { ...patch, administradores: { other: 'other@example.com' } }));
  await assertSucceeds(change('other', 'accept'));
  await assertSucceeds(updateDoc(ref(db('other')), { passwordHash: 'b'.repeat(64) }));
  await assertSucceeds(change('other', 'remove', 'owner'));
  await assertFails(updateDoc(ref(db('owner')), { passwordHash: 'c'.repeat(64) }));
  await assertFails(updateDoc(ref(db('owner')), { administradores: { owner: 'owner@example.com' }, invitacionesAdmin: [] }));
});

test('cancelled invitation cannot be accepted and unrelated invitations are preserved', async () => {
  await seed({ ...initial(), invitacionesAdmin: ['other@example.com', 'third@example.com'] });
  await assertSucceeds(change('owner', 'cancel', 'other@example.com'));
  await assertFails(updateDoc(ref(db('other')), { administradores: { owner: 'owner@example.com', other: 'other@example.com' }, invitacionesAdmin: ['third@example.com'] }));
  await assertSucceeds(change('third', 'accept'));
  assert.deepEqual((await getDoc(ref(db('owner')))).data().invitacionesAdmin, []);
});

test('concurrent administrators leaving cannot remove the last one', async () => {
  await seed({ ...initial(), administradores: { owner: 'owner@example.com', other: 'other@example.com' } });
  const outcomes = await Promise.allSettled([change('owner', 'remove', 'owner'), change('other', 'remove', 'other')]);
  assert.equal(outcomes.filter(result => result.status === 'fulfilled').length, 1);
  assert.equal(Object.keys((await getDoc(ref(db('owner')))).data().administradores).length, 1);
});

test('concurrent creation keeps the first unit intact', async () => {
  const create = uid => {
    const database = db(uid);
    return runTransaction(database, async tx => {
      const snapshot = await tx.get(ref(database));
      if (snapshot.exists()) throw Error('ADMIN_EXISTS');
      tx.set(ref(database), { ...initial(), creadoPor: identity(uid).email, administradores: { [uid]: identity(uid).email } });
    });
  };
  const outcomes = await Promise.allSettled([create('owner'), create('other')]);
  assert.equal(outcomes.filter(result => result.status === 'fulfilled').length, 1);
  assert.equal(Object.keys((await getDoc(ref(db('owner')))).data().administradores).length, 1);
});

test('recipient may decline only their own invitation without gaining a role or changing password', async () => {
  await seed({ ...initial(), invitacionesAdmin: ['other@example.com', 'third@example.com'] });
  const patch = { invitacionesAdmin: ['third@example.com'] };
  await assertFails(updateDoc(ref(db('stranger')), patch));
  await assertFails(updateDoc(ref(db('other')), { invitacionesAdmin: [] }));
  await assertFails(updateDoc(ref(db('other')), { ...patch, passwordHash: 'b'.repeat(64) }));
  await assertFails(updateDoc(ref(db('other', { email_verified: false })), patch));
  await assertSucceeds(change('other', 'decline'));
  const unit = (await getDoc(ref(db('owner')))).data();
  assert.deepEqual(unit.administradores, { owner: 'owner@example.com' });
  assert.deepEqual(unit.invitacionesAdmin, ['third@example.com']);
  await assertFails(updateDoc(ref(db('other')), { administradores: { owner: 'owner@example.com', other: 'other@example.com' } }));
  await assertSucceeds(change('owner', 'invite', 'other@example.com; fourth@example.com\nfifth@example.com'));
  await assertSucceeds(change('other', 'accept'));
});
