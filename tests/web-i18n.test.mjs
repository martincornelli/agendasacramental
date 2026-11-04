import { test } from 'node:test';
import assert from 'node:assert/strict';
import { resolveLanguage, translate } from '../docs/i18n.mjs';
import { exportCSV, parseCSV, guessColumns, mapRows } from '../docs/participation-history.mjs';

test('explicit language wins over saved preference and browser; unsupported languages use Spanish', () => {
  assert.equal(resolveLanguage({ requested: 'es', saved: 'en', browser: 'en-US' }), 'es');
  assert.equal(resolveLanguage({ saved: 'en', browser: 'es-UY' }), 'en');
  assert.equal(resolveLanguage({ browser: 'en-GB' }), 'en');
  assert.equal(resolveLanguage({ requested: 'fr', saved: 'invalid', browser: 'pt-BR' }), 'es');
});

test('UI translations retain Spanish and leave unknown text unchanged', () => {
  assert.equal(translate('Guardar', 'en'), 'Save');
  assert.equal(translate('Guardar', 'es'), 'Guardar');
  assert.equal(translate('José Pérez — Fe y esperanza', 'en'), 'José Pérez — Fe y esperanza');
});

test('English CSV preserves names, topics, IDs, dates, and canonical participation types on reimport', () => {
  const records = [{ id: 'stable-id', date: '2026-11-01', name: 'Fe', type: 'Discurso', topic: 'Santa Cena' }];
  const table = parseCSV(exportCSV(records, 'en'));
  assert.deepEqual(table[0], ['ID', 'Date', 'Name', 'Participation', 'Topic']);
  const result = mapRows(table, guessColumns(table[0]));
  for (const key of ['id', 'date', 'name', 'type', 'topic']) assert.equal(result[0][key], records[0][key]);
});
