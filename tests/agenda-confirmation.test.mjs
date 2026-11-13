import {test} from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import {assignmentChecks, reconcileConfirmation, changeAgendaStatus, ORGANIZATIONS} from '../docs/agenda-confirmation.mjs';
import {translate} from '../docs/i18n.mjs';
const agenda = () => ({fecha:new Date(2026,10,15),estado:'BORRADOR',confirmacionVersion:1,preside:'A',dirige:'B',primeraOracion:'C',oracionFinal:'D',pianista:'E',directorMusica:'F',primerHimnoNumero:1,himnoSacramentalNumero:2,himnoFinalNumero:3,mensajesEvangelio:[{id:'one',tipo:'DISCURSO',nombre:'G',tema:'Fe'},{id:'two',tipo:'DISCURSO',nombre:'H',tema:'Amor'}]});
function acceptAll(a) { return reconcileConfirmation({...a,aceptaciones:Object.fromEntries(assignmentChecks(a).filter(c=>c.complete).map(c=>[c.id,{firma:c.fingerprint,registradoPor:'qa'}]))}); }
test('manual status selector accepts all and preserves acceptances across status changes',()=>{
  const confirmed=changeAgendaStatus(agenda(),'CONFIRMADA','editor',new Date());
  assert.equal(confirmed.estado,'CONFIRMADA');assert.equal(Object.keys(confirmed.aceptaciones).length,12);
  const completed=changeAgendaStatus(confirmed,'REALIZADA','editor',new Date());
  assert.equal(completed.estado,'REALIZADA');assert.deepEqual(completed.aceptaciones,confirmed.aceptaciones);
  const draft=changeAgendaStatus(completed,'BORRADOR','editor',new Date());
  assert.equal(draft.estado,'CONFIRMADA');assert.deepEqual(draft.aceptaciones,confirmed.aceptaciones);
  const partial=structuredClone(confirmed);delete partial.aceptaciones.primeraOracion;
  const partialDraft=changeAgendaStatus(partial,'BORRADOR','editor',new Date());
  assert.equal(partialDraft.estado,'BORRADOR');assert.deepEqual(partialDraft.aceptaciones,partial.aceptaciones);
  assert.throws(()=>changeAgendaStatus({...agenda(),pianista:''},'CONFIRMADA','editor',new Date()),/ASSIGNMENTS_INCOMPLETE/);
});
test('empty agendas cannot confirm; complete agendas confirm only after every acceptance',()=>{
  assert.equal(reconcileConfirmation({fecha:new Date(),confirmacionVersion:1}).estado,'BORRADOR');
  const a=agenda(); const full=acceptAll(a); assert.equal(full.estado,'CONFIRMADA');
  delete full.aceptaciones.primeraOracion; assert.equal(reconcileConfirmation(full).estado,'BORRADOR');
  assert.equal(assignmentChecks({...a,pianista:''}).filter(c=>!c.complete).length,3);
});
test('stable assignment IDs preserve acceptance on reorder; replacement invalidates only affected rows',()=>{
  const a=acceptAll(agenda());
  assert.equal(reconcileConfirmation({...a,mensajesEvangelio:[...a.mensajesEvangelio].reverse()}).estado,'CONFIRMADA');
  const replaced=reconcileConfirmation({...a,primeraOracion:'New'});
  assert.equal(Object.keys(replaced.aceptaciones).length,Object.keys(a.aceptaciones).length-1);
  assert.ok(!reconcileConfirmation({...replaced,primeraOracion:'C'}).aceptaciones.primeraOracion,'reverting does not resurrect acceptance');
  const topic=reconcileConfirmation({...a,mensajesEvangelio:a.mensajesEvangelio.map(m=>m.id==='one'?{...m,tema:'New'}:m)});
  assert.ok(!topic.aceptaciones.mensaje_one); assert.ok(topic.aceptaciones.mensaje_two);
});
test('hymn replacement invalidates both musicians only for that hymn; pianist replacement invalidates all piano checks',()=>{
  const a=acceptAll(agenda());
  const changed=reconcileConfirmation({...a,primerHimnoNumero:99});
  assert.equal(Object.keys(changed.aceptaciones).length,Object.keys(a.aceptaciones).length-2);
  assert.ok(changed.aceptaciones.himnoFinal_pianista);
  assert.equal(Object.keys(reconcileConfirmation({...a,pianista:'New'}).aceptaciones).length,Object.keys(a.aceptaciones).length-3);
});
test('date and meeting type changes invalidate acceptances; testimony meetings do not need planned speakers',()=>{
  const a=acceptAll(agenda());
  assert.deepEqual(reconcileConfirmation({...a,fecha:new Date(2026,10,22)}).aceptaciones,{});
  const testimony=acceptAll({...agenda(),reunionTestimonios:true,mensajesEvangelio:[]}); assert.equal(testimony.estado,'CONFIRMADA');
  const primary={...agenda(),presentacionPrimaria:true,mensajesEvangelio:[]}; assert.equal(acceptAll(primary).estado,'BORRADOR');
  assert.equal(acceptAll({...primary,responsablePrimaria:'Leader'}).estado,'CONFIRMADA');
});
test('legacy confirmed and completed agendas retain lifecycle without fabricated acceptances',()=>{
  assert.equal(reconcileConfirmation({estado:'CONFIRMADA'}).estado,'CONFIRMADA');
  assert.equal(reconcileConfirmation({...agenda(),estado:'REALIZADA'}).estado,'REALIZADA');
  assert.equal(reconcileConfirmation({estado:'CONFIRMADA',confirmacionVersion:1}).estado,'BORRADOR');
});
test('wire signature is deterministic and language independent, including Unicode',()=>{
  const row=assignmentChecks({...agenda(),preside:'José 😀'})[0];
  assert.equal(row.fingerprint,'10:2026-11-156:NORMAL7:preside7:José 😀');
  assert.equal(Object.keys(ORGANIZATIONS).length,6);
  for(const label of [...Object.values(ORGANIZATIONS),'Organización','Aceptaciones','Aceptada','Sin completar','Responsable de Primaria']) {
    assert.notEqual(translate(label,'en'),label); assert.equal(translate(label,'es'),label);
  }
});
test('transaction checks reject stale revisions and legacy timestamps; patch clears obsolete acceptances atomically',()=>{
  const source=fs.readFileSync('docs/app.js','utf8');
  const ctx=vm.createContext({reconcileConfirmation, t:v=>v, toDate:v=>v?.toDate?.()||v, serverTimestamp:()=>123});
  for(const name of ['assertAgendaRevision','confirmationPatch']) { const start=source.indexOf(`function ${name}(`); vm.runInContext(source.slice(start,source.indexOf('\n}',start)+2),ctx); }
  const a={...acceptAll(agenda()),revision:3,ultimaEdicionEn:new Date(1000)};
  ctx.assertAgendaRevision(a,a);
  assert.throws(()=>ctx.assertAgendaRevision({...a,revision:4},a));
  assert.throws(()=>ctx.assertAgendaRevision({...a,ultimaEdicionEn:new Date(2000)},a));
  const patch=ctx.confirmationPatch(a,{primeraOracion:'Other'});
  assert.equal(patch.estado,'BORRADOR');assert.equal(patch.revision,4);assert.ok(!patch.aceptaciones.primeraOracion);
});
