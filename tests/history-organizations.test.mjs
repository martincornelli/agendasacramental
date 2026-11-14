import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
import { organizationLabel, organizationUpdates, parseOrganization } from '../docs/history-organizations.mjs';
import { exportCSV, parseCSV, mapRows, guessColumns, previewRows } from '../docs/participation-history.mjs';
import { writeHistoryWorkbook, readHistoryWorkbook, workbookTable } from '../docs/history-workbook.mjs';
const XLSX = createRequire(import.meta.url)('../docs/vendor/xlsx-0.20.3.full.min.js');
const original = { id: '123:agenda:a:opening', name: 'Ana', date: '2026-01-04', type: 'Primera oración', topic: '', organization: 'RELIEF_SOCIETY', source: 'agenda' };
test('all six organizations round trip in both languages through Excel and CSV', () => {
  for (const language of ['es', 'en']) for (const organization of ['ELDERS_QUORUM','RELIEF_SOCIETY','YOUNG_MEN','YOUNG_WOMEN','PRIMARY','BISHOPRIC']) {
    const record = { ...original, organization };
    const [sheet] = readHistoryWorkbook(writeHistoryWorkbook([record], XLSX, language), XLSX);
    assert.equal(sheet.rows[1][5], organizationLabel(organization, language));
    for (const table of [workbookTable(sheet, 1), parseCSV(exportCSV([record], language))]) {
      const rows = mapRows(table, guessColumns(table[0]));
      assert.equal(rows[0].organization, organization);
      assert.equal(previewRows(rows, [record])[0].status, 'unchanged');
    }
  }
});
test('organization-only corrections on agenda/manual rows never modify the participation', () => {
  for (const source of ['agenda', 'manual']) for (const id of [original.id, '']) {
    const result = previewRows([{ ...original, id, organization: 'PRIMARY' }], [{ ...original, source }])[0];
    assert.equal(result.status, 'update'); assert.equal(result.organizationOnly, true); assert.equal(result.id, original.id);
  }
  assert.equal(previewRows([{ ...original, topic: 'changed', organization: 'PRIMARY' }], [original])[0].status, 'invalid');
});
test('blank or absent organization preserves current profile; identical values are not written', () => {
  const members = [{ id: 'ana', nombre: 'Ana', organizacion: 'RELIEF_SOCIETY' }];
  for (const organization of ['', undefined, 'RELIEF_SOCIETY']) {
    assert.deepEqual(organizationUpdates([{ ...original, organization }], members), []);
    assert.equal(previewRows([{ ...original, organization }], [original])[0].status, 'unchanged');
  }
  assert.equal(organizationUpdates([{ ...original, organization: 'PRIMARY' }], members)[0].organization, 'PRIMARY');
});
test('conflicting organizations, unknown values, and namesakes cannot choose a profile silently', () => {
  assert.throws(() => organizationUpdates([original, { ...original, organization: 'PRIMARY' }], []), /distintas/);
  assert.throws(() => organizationUpdates([original], [{nombre:'Ana'}, {nombre:'Ana'}]), /más de una/);
  assert.equal(parseOrganization('unrecognized'), null);
  const table = [['Date','Name','Participation','Organization'], ['2026-01-04','Ana','Talk','unknown']];
  assert.match(mapRows(table, guessColumns(table[0]))[0].error, /Organización/);
});
