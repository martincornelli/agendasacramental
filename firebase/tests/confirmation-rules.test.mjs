import {before,after,test} from 'node:test';
import fs from 'node:fs';
import {initializeTestEnvironment,assertFails,assertSucceeds} from '@firebase/rules-unit-testing';
import {doc,setDoc,updateDoc,getDoc,setLogLevel} from 'firebase/firestore';
let env;
setLogLevel('silent');
before(async()=>{env=await initializeTestEnvironment({projectId:'demo-agenda-confirmation',firestore:{host:'127.0.0.1',port:8191,rules:fs.readFileSync(new URL('../firestore.rules',import.meta.url),'utf8')}});});
after(async()=>env?.cleanup());
test('legacy data can migrate; old replacements cannot erase confirmations; current clients can update atomically',async()=>{
 const ref=doc(env.authenticatedContext('editor').firestore(),'agendas','agenda');
 await assertSucceeds(setDoc(ref,{estado:'CONFIRMADA'}));
 await assertSucceeds(updateDoc(ref,{estado:'BORRADOR',confirmacionVersion:1,revision:1,aceptaciones:{}}));
 await assertFails(setDoc(ref,{estado:'CONFIRMADA'}));
 await assertFails(updateDoc(ref,{estado:'CONFIRMADA'}));
 await assertFails(updateDoc(ref,{confirmacionVersion:0,revision:2}));
 await assertFails(updateDoc(ref,{revision:2,aceptaciones:'invalid'}));
 await assertSucceeds(updateDoc(ref,{revision:2,estado:'CONFIRMADA',aceptaciones:{primeraOracion:{firma:'accepted',registradoPor:'editor'}}}));
 await assertFails(updateDoc(ref,{revision:2,aceptaciones:{}}));
 await assertSucceeds(updateDoc(ref,{revision:3,estado:'BORRADOR',aceptaciones:{}}));
});
test('unauthenticated users cannot access acceptance data',async()=>{
 const ref=doc(env.unauthenticatedContext().firestore(),'agendas','agenda');
 await assertFails(getDoc(ref));await assertFails(setDoc(ref,{confirmacionVersion:1,aceptaciones:{}}));
});
test('notification edits cannot detach a legacy or current agenda from its unit',async()=>{
 const ref=doc(env.authenticatedContext('editor').firestore(),'agendas','notification');
 await assertSucceeds(setDoc(ref,{numeroUnidad:'123',estado:'BORRADOR'}));
 await assertFails(updateDoc(ref,{numeroUnidad:''}));
 await assertFails(updateDoc(ref,{numeroUnidad:'456'}));
 await assertSucceeds(updateDoc(ref,{confirmacionVersion:1,revision:1,aceptaciones:{}}));
 await assertFails(updateDoc(ref,{numeroUnidad:'456',revision:2}));
 await assertSucceeds(updateDoc(ref,{numeroUnidad:'123',revision:2}));
});
