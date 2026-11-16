import { before, after, beforeEach, test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { doc, setDoc, getDoc, getDocs, collection, deleteDoc, serverTimestamp } from 'firebase/firestore';
let env;
before(async () => { env = await initializeTestEnvironment({ projectId: 'demo-hymn-topics', firestore: { host: '127.0.0.1', port: 8188, rules: fs.readFileSync(new URL('../firestore.rules', import.meta.url), 'utf8') } }); });
after(async () => { await env?.cleanup(); });
beforeEach(async () => { await env.clearFirestore(); });
const ref = (db, unit = '123', hymn = 'es-1') => doc(db, `temasHimnosUnidad/${unit}/himnos/${hymn}`);
const data = (extra = {}) => ({numeroUnidad:'123', numero:1, idioma:'es', temas:['Fe'], actualizadoEn:serverTimestamp(), ...extra});
test('unauthenticated clients cannot read, write, or restore topics', async () => {
  const db=env.unauthenticatedContext().firestore();
  await assertFails(getDoc(ref(db)));
  await assertFails(setDoc(ref(db),data()));
  await assertFails(deleteDoc(ref(db)));
});
test('unit and language records are independent; restoring removes only the override', async () => {
  const db=env.authenticatedContext('member').firestore();
  await assertSucceeds(setDoc(ref(db),data()));
  await assertSucceeds(setDoc(ref(db,'456'),data({numeroUnidad:'456',temas:['Esperanza']})));
  await assertSucceeds(setDoc(ref(db,'123','en-1'),data({idioma:'en',temas:['Faith']})));
  await assertSucceeds(getDocs(collection(db,'temasHimnosUnidad/123/himnos')));
  await assertSucceeds(deleteDoc(ref(db)));
  assert.equal((await getDoc(ref(db))).exists(),false);
  assert.deepEqual((await getDoc(ref(db,'456'))).data().temas,['Esperanza']);
  assert.deepEqual((await getDoc(ref(db,'123','en-1'))).data().temas,['Faith']);
  await assertFails(setDoc(doc(db,'temasHimnosGlobal/es-1'),data()));
});
test('rejects mismatched scope, identity, unbounded content, and unknown fields', async () => {
  const db=env.authenticatedContext('member').firestore();
  for (const extra of [{numeroUnidad:'456'},{numero:2},{idioma:'en'},{temas:['x'.repeat(81)]},{temas:Array.from({length:31},(_,i)=>String(i))},{temas:['Fe',4]},{temas:['Fe','Fe']},{temas:['']},{global:true}]) {
    await assertFails(setDoc(ref(db),data(extra)));
  }
  await assertSucceeds(setDoc(ref(db),data({temas:[]})));
  await assertSucceeds(setDoc(ref(db),data({temas:Array.from({length:30},(_,i)=>`Topic ${i}`)})));
});
