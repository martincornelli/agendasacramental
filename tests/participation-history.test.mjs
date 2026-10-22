import { test } from 'node:test';
import assert from 'node:assert/strict';
import { parseCSV, parseDate, guessColumns, mapRows, previewRows, exportCSV } from '../docs/participation-history.mjs';

const record = { id: '123:import:one', date: '2026-01-04', name: 'José Pérez', type: 'Discurso', topic: 'Fe', source: 'import' };
const read = text => { const table = parseCSV(text); return mapRows(table, guessColumns(table[0])); };

test('export, append and reimport keeps existing rows and adds only the new participation', () => {
  const rows = read(exportCSV([record, { ...record, id: '', date: '2026-02-01' }]));
  assert.deepEqual(previewRows(rows, [record]).map(r => r.status), ['unchanged', 'new']);
  const stored = [...[record], { ...rows[1], id: '123:import:two', source: 'import' }];
  assert.deepEqual(previewRows(rows, stored).map(r => r.status), ['unchanged', 'duplicate']);
});

test('import never requests deletion when an existing row is absent from the file', () => {
  const before = structuredClone(record);
  assert.deepEqual(previewRows([], [record]), []);
  assert.deepEqual(record, before);
});

test('known ID corrections are explicit, unknown IDs cannot create records', () => {
  assert.equal(previewRows([{ ...record, date: '2026-01-11' }], [record])[0].status, 'update');
  assert.equal(previewRows([{ ...record, id: 'another-unit:one' }], [record])[0].status, 'invalid');
  assert.equal(previewRows([{ ...record, topic: 'Esperanza' }], [{ ...record, source: 'agenda' }])[0].status, 'invalid');
});

test('duplicates inside the file, duplicate IDs, and changed IDs cannot silently multiply records', () => {
  const row = { ...record, id: '' };
  assert.deepEqual(previewRows([row, row], []).map(r => r.status), ['new', 'duplicate']);
  assert.deepEqual(previewRows([record, record], [record]).map(r => r.status), ['unchanged', 'invalid']);
  const second = { ...record, id: 'two', date: '2026-01-11' };
  assert.equal(previewRows([{ ...second, date: record.date }], [record, second])[0].status, 'duplicate');
});

test('accent and case variants suggest an existing name without silently changing it', () => {
  const [row] = previewRows([{ ...record, id: '', name: 'jose perez' }], [record]);
  assert.equal(row.name, 'jose perez');
  assert.deepEqual(row.candidates, ['José Pérez']);
  assert.equal(previewRows([{ ...row, name: 'José Pérez' }], [record])[0].status, 'duplicate');
});

test('dates are strict and day/month order is explicit', () => {
  assert.equal(parseDate('05/06/2026'), '2026-06-05');
  assert.equal(parseDate('05/06/2026', 'MDY'), '2026-05-06');
  assert.equal(parseDate('2026-06-05', 'MDY'), '2026-06-05');
  assert.equal(parseDate('31/02/2026'), '');
  assert.equal(parseDate('29/02/2024'), '2024-02-29');
  assert.equal(parseDate('12/05/26'), '');
});

test('CSV supports BOM, separators, accents, quoted line breaks and escaped quotes', () => {
  for (const separator of [',', ';', '\t']) {
    const table = parseCSV('\uFEFF' + ['Fecha', 'Nombre', 'Participación', 'Tema'].join(separator) + '\r\n' + ['2026-01-04', 'José Pérez', 'Discurso', '"Fe, esperanza; y\n""amor"""'].join(separator));
    const [row] = mapRows(table, guessColumns(table[0]));
    assert.equal(row.topic, 'Fe, esperanza; y\n"amor"');
    assert.equal(row.error, '');
  }
  assert.throws(() => parseCSV('a,b\n"oops'), /comillas/);
  assert.throws(() => parseCSV(''), /vacío/);
});

test('row per meeting expands prayers and discourse using the same date', () => {
  const table = parseCSV('Fecha;Discursante;Primera oración;Última oración;Tema\n04/01/2026;Ana;Juan;María;Fe');
  const rows = mapRows(table, guessColumns(table[0]), 'DMY', true);
  assert.deepEqual(rows.map(r => [r.type, r.name, r.topic]), [['Discurso', 'Ana', 'Fe'], ['Primera oración', 'Juan', ''], ['Última oración', 'María', '']]);
});

test('invalid rows and overlapping column mappings are blocked', () => {
  const table = parseCSV('Fecha;Nombre;Participación\n31/02/2026;Ana;Discurso\n2026-01-04;Ana;Desconocido\n2026-01-04;Ana;Discurso;Extra');
  assert.ok(mapRows(table, guessColumns(table[0])).every(r => r.error));
  assert.throws(() => mapRows(table, { date: 0, name: 1, type: 1 }), /diferente/);
});

test('spreadsheet formula protection survives a round trip without creating a false correction', () => {
  const dangerous = { ...record, topic: '=HYPERLINK("https://example.com")' };
  const csv = exportCSV([dangerous]);
  assert.ok(csv.includes("'=HYPERLINK"));
  assert.equal(previewRows(read(csv), [dangerous])[0].status, 'unchanged');
});
