import test from 'node:test';
import assert from 'node:assert/strict';
import {generateHymnPlans, hymnMetadata} from '../docs/hymn-planner.mjs';
import {HYMNS_ES,HYMNS_EN} from '../docs/hymns.js';
const date=new Date(2026,11,20,12);
for(const [language,catalog] of [['es',HYMNS_ES],['en',HYMNS_EN]]) {
  const meta=number=>hymnMetadata(number,catalog[number],language);
  for(const seasonalMode of ['ONE_CHRISTMAS','CHRISTMAS_ONLY','EASTER_ONLY']) {
    test(`${language}: ${seasonalMode} coexists with new collection and sacrament rules`,()=>{
      for(const forceNewCollection of [false,true]) for(const seed of [0,1,7]) {
        const result=generateHymnPlans({catalog,language,agendas:[],dates:[date],options:{seasonalMode,forceNewCollection,includeIntermediate:true,seed}});
        const plan=result.proposals[0].proposal;
        assert.ok(meta(plan.sacrament.number).sacramental);
        const others=[plan.opening,plan.intermediate,plan.closing];
        assert.ok(others.every(item=>!meta(item.number).sacramental));
        const category=seasonalMode==='EASTER_ONLY'?'easter':'christmas';
        const matches=others.filter(item=>meta(item.number).categories.includes(category));
        assert.ok(seasonalMode==='ONE_CHRISTMAS'?matches.length>=1:matches.length===3);
        if(forceNewCollection) assert.ok(Object.values(plan).some(item=>meta(item.number).newCollection));
        assert.equal(new Set(Object.values(plan).map(item=>item.number)).size,4);
      }
    });
  }
  test(`${language}: unavailable seasonal hymns never silently fall back`,()=>{
    const excludedHymns=Object.keys(catalog).map(Number).filter(number=>meta(number).categories.includes('christmas'));
    const result=generateHymnPlans({catalog,language,agendas:[],dates:[date],options:{seasonalMode:'CHRISTMAS_ONLY',excludedHymns}});
    const proposal=result.proposals[0];
    assert.equal(proposal.proposal.opening.number,0);
    assert.equal(proposal.proposal.closing.number,0);
    assert.ok(meta(proposal.proposal.sacrament.number).sacramental);
    assert.ok(proposal.warnings.some(item=>item.code==='SEASON_UNMET'));
  });
  test(`${language}: manual selections are retained and conflict is reported`,()=>{
    const result=generateHymnPlans({catalog,language,agendas:[{fecha:date,primerHimnoNumero:1,primerHimnoNombre:catalog[1],primerHimnoOrigen:'PLANIFICADOR_MANUAL'}],dates:[date],options:{seasonalMode:'CHRISTMAS_ONLY',onlyEmptyDates:false}});
    assert.equal(result.proposals[0].proposal.opening.number,1);
    assert.ok(result.proposals[0].warnings.some(item=>item.code==='SEASON_UNMET'));
  });
  test(`${language}: one Christmas hymn does not depend on talk topics`,()=>{
    const result=generateHymnPlans({catalog,language,agendas:[{fecha:date,mensajesEvangelio:[{tipo:'DISCURSO',tema:'Obra misional / missionary work'}]}],dates:[date],options:{seasonalMode:'ONE_CHRISTMAS',prioritizeTopics:true}});
    assert.ok(['opening','closing'].some(position=>meta(result.proposals[0].proposal[position].number).categories.includes('christmas')));
  });
}
