// Run with Playwright on NODE_PATH. Firebase is mocked; no live data is written.
const { chromium } = require('playwright');
const fs = require('node:fs');
const assert = require('node:assert/strict');
const source = fs.readFileSync('docs/app.js', 'utf8');
const domain = fs.readFileSync('docs/participation-history.mjs', 'utf8').replace(/^export /gm, '');
const names = ['exportableHistory', 'downloadHistoryCSV', 'openHistoryTransfer', 'openHistoryMapping', 'openHistoryPreview', 'openModal', 'closeModal', 'escapeHtml', 'escapeAttr', 'option'];
const functions = names.map(name => {
  const start = source.indexOf(`function ${name}(`);
  assert.ok(start >= 0, name);
  return source.slice(start, source.indexOf('\n}', start) + 2);
}).join('\n');
const fixtures = `
const modal = document.querySelector('dialog');
const state = { unitNumber: '123', historyReady: true, historyRevision: 0, importedHistory: [], agendas: [], hermanos: [] };
const db = {};
let persisted = { revision: 0, records: [] };
function field(id, label, html) { return '<label for="' + id + '">' + label + html + '</label>'; }
function toDate(v) { return v ? new Date(v) : null; }
function dateInputValue(v) { return v ? new Date(v).toISOString().slice(0, 10) : ''; }
function doc(...args) { return args.join('/'); }
function serverTimestamp() { return 'test-time'; }
function userEmail() { return 'test@example.com'; }
function toastMessage(message) { window.lastToast = message; }
async function runTransaction(db, callback) {
  await callback({ get: async () => ({ data: () => persisted }), set: (_, data) => {
    persisted = data; state.importedHistory = data.records; state.historyRevision = data.revision;
  }});
}
window.stored = () => persisted;
window.conflict = () => { persisted.revision++; state.historyRevision = persisted.revision; };
window.start = openHistoryTransfer;
openHistoryTransfer();
`;
(async () => {
  const browser = await chromium.launch({ headless: true, ...(process.env.PLAYWRIGHT_CHANNEL ? { channel: process.env.PLAYWRIGHT_CHANNEL } : {}) });
  try {
    const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.route('https://history.test/**', route => route.fulfill({ contentType: 'text/html; charset=utf-8', body: '<html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><style>' + fs.readFileSync('docs/styles.css', 'utf8') + '</style></head><body><dialog></dialog><script>' + domain + '\n' + functions + '\n' + fixtures + '</script></body></html>' }));
    await page.goto('https://history.test/');
    const csv = 'ID;Fecha;Nombre;Participación;Tema\n;2026-01-04;Ana;Discurso;Fe\n;2026-01-04;Ana;Discurso;Fe\n;2026-01-04;Juan;Primera oración;';
    const upload = async text => {
      await page.locator('#history-file').setInputFiles({ name: 'historial.csv', mimeType: 'text/csv', buffer: Buffer.from(text) });
      await page.getByRole('button', { name: 'Vista previa', exact: true }).click();
    };
    await upload(csv);
    assert.equal(await page.locator('[data-history-row]:checked').count(), 2);
    assert.ok(await page.getByText('Fila 3: Coincidencia: se omite', { exact: true }).isVisible());
    fs.mkdirSync('output/history-import-qa', { recursive: true });
    await page.screenshot({ path: 'output/history-import-qa/mobile-preview.png', fullPage: true });
    await page.getByRole('button', { name: 'Importar seleccionadas', exact: true }).click();
    await page.waitForFunction(() => window.stored().records.length === 2);
    await page.evaluate(() => window.start());
    await upload(csv);
    assert.equal(await page.locator('[data-history-row]:checked').count(), 0);
    await page.getByRole('button', { name: 'Cancelar', exact: true }).click();
    // Correct a known ID, retaining an omitted record. Simulate a conflicting writer first.
    const id = await page.evaluate(() => window.stored().records[0].id);
    await page.evaluate(() => window.start());
    await upload('ID;Fecha;Nombre;Participación;Tema\n' + id + ';2026-01-11;Ana;Discurso;Esperanza');
    assert.equal(await page.locator('[data-history-row]:checked').count(), 0);
    await page.locator('[data-history-row="0"]').check();
    await page.evaluate(() => window.conflict());
    await page.getByRole('button', { name: 'Importar seleccionadas', exact: true }).click();
    await page.waitForFunction(() => document.querySelector('#history-save-error').textContent.includes('Otra persona'));
    assert.equal(await page.evaluate(() => window.stored().records[0].date), '2026-01-04');
    assert.equal(await page.evaluate(() => window.stored().records.length), 2);
    await page.getByRole('button', { name: 'Volver a columnas', exact: true }).click();
    await page.getByRole('button', { name: 'Vista previa', exact: true }).click();
    await page.locator('[data-history-row="0"]').check();
    await page.getByRole('button', { name: 'Importar seleccionadas', exact: true }).click();
    await page.waitForFunction(() => window.stored().records[0].date === '2026-01-11');
    assert.equal(await page.evaluate(() => window.stored().records.length), 2);
    assert.deepEqual(errors, []);
    console.log('UI OK: mobile preview, import, reimport without duplicates, explicit corrections, atomic conflict rejection.');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
