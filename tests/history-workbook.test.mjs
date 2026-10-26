import { test } from 'node:test';
import assert from 'node:assert/strict';
import { createRequire } from 'node:module';
import { readHistoryWorkbook, writeHistoryWorkbook, workbookTable } from '../docs/history-workbook.mjs';
import { mapRows, guessColumns, previewRows } from '../docs/participation-history.mjs';
const XLSX = createRequire(import.meta.url)('../docs/vendor/xlsx-0.20.3.full.min.js');
const record = { id: '123:import:one', date: '2026-01-04', name: 'José Pérez', type: 'Discurso', topic: 'Fe', source: 'import' };
const encode = (sheet, properties = {}, bookType = 'xlsx') => {
  const book = XLSX.utils.book_new();
  XLSX.utils.book_append_sheet(book, sheet, 'Historial');
  book.Workbook = { WBProps: properties };
  return XLSX.write(book, { type: 'array', bookType });
};

test('Excel export and reimport preserve IDs and classify existing data without duplicates', () => {
  const [sheet] = readHistoryWorkbook(writeHistoryWorkbook([record], XLSX), XLSX);
  const table = workbookTable(sheet, 1);
  const rows = mapRows(table, guessColumns(table[0]));
  assert.equal(rows[0].name, 'José Pérez');
  assert.equal(previewRows(rows, [record])[0].status, 'unchanged');
});

test('Excel template contains headers only, no sample participations', () => {
  const [sheet] = readHistoryWorkbook(writeHistoryWorkbook([], XLSX), XLSX);
  assert.deepEqual(sheet.rows, [['ID', 'Fecha', 'Nombre', 'Participación', 'Tema']]);
  assert.throws(() => workbookTable(sheet, 1), /No hay participaciones/);
});

test('English Excel headers and roles round-trip to the shared canonical history', () => {
  const [sheet] = readHistoryWorkbook(writeHistoryWorkbook([record], XLSX, 'en'), XLSX);
  assert.deepEqual(sheet.rows[0], ['ID', 'Date', 'Name', 'Participation', 'Topic']);
  assert.equal(sheet.rows[1][3], 'Talk');
  const table = workbookTable(sheet, 1);
  assert.equal(previewRows(mapRows(table, guessColumns(table[0])), [record])[0].status, 'unchanged');
});

test('1900 and 1904 Excel date systems yield the same calendar date', () => {
  for (const date1904 of [false, true]) {
    const sheet = XLSX.utils.aoa_to_sheet([['Fecha', 'Nombre', 'Participación'], [date1904 ? 44564 : 46026, 'Ana', 'Discurso']]);
    sheet.A2.z = 'dd/mm/yyyy';
    const [result] = readHistoryWorkbook(encode(sheet, { date1904 }), XLSX);
    assert.equal(result.rows[1][0], '2026-01-04');
  }
});

test('legacy XLS reads native dates too', () => {
  const sheet = XLSX.utils.aoa_to_sheet([['Fecha', 'Nombre', 'Participación'], [46026, 'María', 'Primera oración']]);
  sheet.A2.z = 'dd/mm/yyyy';
  const [result] = readHistoryWorkbook(encode(sheet, {}, 'biff8'), XLSX);
  assert.equal(result.rows[1][0], '2026-01-04');
  assert.equal(result.rows[1][1], 'María');
});

test('a title above the header and empty interior rows retain physical Excel row numbers', () => {
  const sheet = { rows: [['Historial', '', ''], ['Fecha', 'Nombre', 'Participación'], ['', '', ''], ['2026-01-04', 'Ana', 'Discurso']] };
  const table = workbookTable(sheet, 2);
  const rows = mapRows(table, guessColumns(table[0]));
  assert.equal(rows.length, 1);
  assert.equal(rows[0].row, 4);
  assert.equal(rows[0].error, '');
});

test('multiple sheets remain separate and hidden sheets are not imported', () => {
  const book = XLSX.utils.book_new();
  for (const name of ['2024', '2025', 'Privada']) XLSX.utils.book_append_sheet(book, XLSX.utils.aoa_to_sheet([['Fecha'], ['2026-01-04']]), name);
  book.Workbook = { Sheets: [{ Hidden: 0 }, { Hidden: 0 }, { Hidden: 1 }] };
  assert.deepEqual(readHistoryWorkbook(XLSX.write(book, { type: 'array' }), XLSX).map(s => s.name), ['2024', '2025']);
});

test('formulas require a values-only copy and exported formula-like text stays literal', () => {
  const formulaSheet = XLSX.utils.aoa_to_sheet([['Fecha'], ['2026-01-04']]);
  formulaSheet.A2 = { t: 'n', f: 'TODAY()', v: 46026, z: 'dd/mm/yyyy' };
  const [sheet] = readHistoryWorkbook(encode(formulaSheet), XLSX);
  assert.throws(() => workbookTable(sheet, 1), /fórmulas/);
  const topic = '=HYPERLINK("https://example.com")';
  const buffer = writeHistoryWorkbook([{ ...record, topic }], XLSX);
  assert.equal(XLSX.read(buffer).Sheets.Historial.E2.f, undefined);
  assert.equal(readHistoryWorkbook(buffer, XLSX)[0].rows[1][4], topic);
});

test('invalid files, blank headers, and oversized sheet ranges fail explicitly', () => {
  assert.throws(() => readHistoryWorkbook(new TextEncoder().encode('not excel').buffer, XLSX), /no es un libro/);
  assert.throws(() => workbookTable({ rows: [[''], ['Ana']] }, 1), /encabezados/);
  const sheet = XLSX.utils.aoa_to_sheet([['Fecha']]);
  sheet.A5002 = { t: 's', v: 'too many' }; sheet['!ref'] = 'A1:A5002';
  const [result] = readHistoryWorkbook(encode(sheet), XLSX);
  assert.match(result.error, /supera/);
});

test('Excel literal apostrophes are preserved during export and reimport', () => {
  const original = { ...record, topic: "'=literal" };
  const [sheet] = readHistoryWorkbook(writeHistoryWorkbook([original], XLSX), XLSX);
  const table = workbookTable(sheet, 1);
  assert.equal(previewRows(mapRows(table, guessColumns(table[0])), [original])[0].status, 'unchanged');
});
