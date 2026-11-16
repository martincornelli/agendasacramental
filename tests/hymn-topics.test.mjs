import test from 'node:test';
import assert from 'node:assert/strict';
import { HYMN_TOPICS_DATA } from '../docs/hymn-topics-data.mjs';
import { effectiveHymnTopics, officialHymnTopics, parseHymnTopics } from '../docs/hymn-topics.mjs';
import { hymnMetadata, generateHymnPlans } from '../docs/hymn-planner.mjs';
import { HYMNS_ES, HYMNS_EN } from '../docs/hymns.js';
test('every shipped hymn has a sourced record in its own language', () => {
  for (const [lang, catalog] of [['es',HYMNS_ES],['en',HYMNS_EN]]) {
    for (const number of Object.keys(catalog)) assert.ok(HYMN_TOPICS_DATA.hymns[lang][number], `${lang}-${number}`);
  }
  assert.equal(Object.keys(HYMN_TOPICS_DATA.hymns.es).length,299);
  assert.equal(Object.keys(HYMN_TOPICS_DATA.hymns.en).length,431);
});
test('official topics replace number-range guesses and preserve empty source records', () => {
  assert.ok(officialHymnTopics(1,'en').includes('gathering of Israel'));
  for (const [lang, records] of Object.entries(HYMN_TOPICS_DATA.hymns)) {
    for (const record of Object.values(records)) if (!record.topics.length) assert.deepEqual(hymnMetadata(record.number,record.title,lang).topics,[]);
  }
  assert.notDeepEqual(officialHymnTopics(19,'es'),officialHymnTopics(19,'en'));
});
test('unit overrides including empty sets do not mutate official or another language', () => {
  const before=officialHymnTopics(1,'es');
  const unitA={'es-1':['Tema local']};
  assert.deepEqual(effectiveHymnTopics(1,'es',unitA),['Tema local']);
  assert.deepEqual(effectiveHymnTopics(1,'es',{}),before);
  assert.deepEqual(effectiveHymnTopics(1,'en',unitA),officialHymnTopics(1,'en'));
  assert.deepEqual(effectiveHymnTopics(1,'es',{'es-1':[]}),[]);
  assert.equal(hymnMetadata(101,'','es',{'es-101':[]}).sacramental,true);
});
test('topic edits affect generation and validation bounds are enforced', () => {
  const date=new Date(2027,0,3);
  const result=generateHymnPlans({catalog:{1:'A',2:'B',101:'C',102:'D'},agendas:[{fecha:date,mensajesEvangelio:[{tipo:'DISCURSO',tema:'Local topic'}]}],dates:[date],language:'es',options:{topicOverrides:{'es-2':['Local topic']},seed:1}});
  assert.equal(result.proposals[0].proposal.opening.number,2);
  assert.deepEqual(parseHymnTopics(' Fe \n\nFe\nEsperanza '),['Fe','Esperanza']);
  assert.throws(()=>parseHymnTopics('a'.repeat(81)));
  assert.throws(()=>parseHymnTopics(Array.from({length:31},(_,i)=>i).join('\n')));
});
