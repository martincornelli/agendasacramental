import test from 'node:test';
import assert from 'node:assert/strict';
import {generateHymnPlans, hymnMetadata, dateKey} from '../docs/hymn-planner.mjs';
import {HYMNS_ES,HYMNS_EN} from '../docs/hymns.js';
const date=new Date(2026,11,20,12);
for(const [language,catalog] of [['es',HYMNS_ES],['en',HYMNS_EN]]) {
  test(`${language}: repeated retries differ while preserving seasonal, sacrament and manual rules`,()=>{
    for(const seasonalMode of ['NONE','ONE_CHRISTMAS','CHRISTMAS_ONLY','EASTER_ONLY']) {
      const manual=Object.keys(catalog).map(Number).find(n=>{
        const m=hymnMetadata(n,catalog[n],language);
        return !m.sacramental && m.categories.includes(seasonalMode==='EASTER_ONLY'?'easter':'christmas');
      });
      const agendas=[{fecha:date,primerHimnoNumero:manual,primerHimnoNombre:catalog[manual],primerHimnoOrigen:'PLANIFICADOR_MANUAL'}];
      const original=structuredClone(agendas);
      const options={seasonalMode,onlyEmptyDates:false,forceNewCollection:true,includeIntermediate:true,excludedHymns:[2],seed:1};
      let result=generateHymnPlans({catalog,language,agendas,dates:[date],options});
      for(let attempt=1;attempt<=4;attempt++) {
        const previous=result.proposals[0].proposal;
        result=generateHymnPlans({catalog,language,agendas,dates:[date],options:{...options,seed:attempt+1,previousCombination:{[dateKey(date)]:previous}}});
        const next=result.proposals[0].proposal;
        assert.notDeepEqual(next,previous);
        assert.deepEqual(next.opening,previous.opening);
        assert.ok(hymnMetadata(next.sacrament.number,next.sacrament.name,language).sacramental);
        const numbers=Object.values(next).map(item=>item.number);
        assert.equal(new Set(numbers).size,4);
        assert.ok(numbers.some(n=>n>=1000));
        assert.ok(!numbers.includes(2));
        if(seasonalMode.endsWith('_ONLY')) for(const slot of ['opening','intermediate','closing']) {
          assert.ok(hymnMetadata(next[slot].number,next[slot].name,language).categories.includes(seasonalMode==='EASTER_ONLY'?'easter':'christmas'));
        }
      }
      assert.deepEqual(agendas,original);
    }
  });
}
test('a constrained request returns the same valid combination without emptying slots',()=>{
 const catalog={1:HYMNS_ES[1],101:HYMNS_ES[101],2:HYMNS_ES[2]};
 const agendas=[{fecha:date,primerHimnoNumero:1,primerHimnoNombre:catalog[1],primerHimnoOrigen:'PLANIFICADOR_MANUAL'}];
 const request={catalog,agendas,dates:[date],options:{onlyEmptyDates:false}};
 const first=generateHymnPlans(request);
 const retry=generateHymnPlans({...request,options:{...request.options,seed:99,previousCombination:{[dateKey(date)]:first.proposals[0].proposal}}});
 assert.deepEqual(retry.proposals,first.proposals);
});
test('retry does not break the interval just to force a different hymn',()=>{
 const catalog=Object.fromEntries([1,2,3,101,102].map(n=>[n,HYMNS_ES[n]]));
 const recent=new Date(date);recent.setDate(recent.getDate()-7);
 const request={catalog,dates:[date],agendas:[
   {fecha:date,primerHimnoNumero:1,primerHimnoNombre:catalog[1],primerHimnoOrigen:'PLANIFICADOR_MANUAL'},
   {fecha:recent,estado:'REALIZADA',primerHimnoNumero:3,primerHimnoNombre:catalog[3],himnoSacramentalNumero:102,himnoSacramentalNombre:catalog[102]}
 ],options:{onlyEmptyDates:false,minimumIntervalDays:180}};
 const first=generateHymnPlans(request);
 const next=generateHymnPlans({...request,options:{...request.options,seed:99,previousCombination:{[dateKey(date)]:first.proposals[0].proposal}}});
 assert.deepEqual(next.proposals,first.proposals);
 assert.ok(!next.proposals[0].warnings.some(w=>w.code==='INTERVAL_RELAXED'));
});
