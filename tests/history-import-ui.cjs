// Run with Playwright on NODE_PATH. Firebase is mocked; no live data is written.
const fs = require('node:fs');
const assert = require('node:assert/strict');
const source = fs.readFileSync('docs/app.js', 'utf8');
const domain = fs.readFileSync('docs/participation-history.mjs', 'utf8').replace(/^export /gm, '');
const workbook = fs.readFileSync('docs/history-workbook.mjs', 'utf8').replace(/^export /gm, '');
const names = ['exportableHistory', 'downloadHistoryCSV', 'historySpreadsheetJob', 'downloadHistoryFile', 'openHistoryWorkbook', 'syncHistoryTransferAccess', 'openHistoryTransfer', 'openHistoryMapping', 'openHistoryPreview', 'openModal', 'closeModal', 'escapeHtml', 'escapeAttr', 'option'];
const functions = names.map(name => {
  const start = source.indexOf(`function ${name}(`);
  assert.ok(start >= 0, name);
  return source.slice(source.slice(start - 6, start) === 'async ' ? start - 6 : start, source.indexOf('\n}', start) + 2);
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
function toastMessage(message) { window.lastToast = message; document.querySelector('#fixture-status').textContent = message; }
async function runTransaction(db, callback) {
  await callback({ get: async () => ({ data: () => persisted }), set: (_, data) => {
    persisted = data; state.importedHistory = data.records; state.historyRevision = data.revision;
  }});
}
window.stored = () => persisted;
window.conflict = () => { persisted.revision++; state.historyRevision = persisted.revision; };
window.start = openHistoryTransfer;
window.historyAccess = (ready, error = '') => { state.historyReady = ready; state.historyError = error; syncHistoryTransferAccess(); };
openHistoryTransfer();
`;
const fixtureScript = domain + '\n' + workbook + '\n' + functions + '\n' + fixtures;
const fixtureHTML = '<html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><link rel="stylesheet" href="/styles.css"></head><body><button onclick="window.start()">Abrir importación de prueba</button><p id="fixture-status" role="status"></p><dialog></dialog><script type="module" src="/history-fixture.mjs"></script></body></html>';
if (process.argv.includes('--serve')) {
  const http = require('node:http');
  const path = require('node:path');
  const root = path.resolve('docs');
  http.createServer((req, res) => {
    const url = new URL(req.url, 'http://localhost');
    if (url.pathname === '/') { res.setHeader('Content-Type', 'text/html; charset=utf-8'); res.end(fixtureHTML); return; }
    if (url.pathname === '/history-fixture.mjs') { res.setHeader('Content-Type', 'text/javascript; charset=utf-8'); res.end(fixtureScript); return; }
    const file = path.resolve(root, '.' + decodeURIComponent(url.pathname));
    if (!file.startsWith(root + path.sep) || !fs.existsSync(file) || !fs.statSync(file).isFile()) { res.writeHead(404); res.end(); return; }
    res.setHeader('Content-Type', file.endsWith('.css') ? 'text/css' : 'text/javascript; charset=utf-8');
    fs.createReadStream(file).pipe(res);
  }).listen(4178, '127.0.0.1', () => console.log('History fixture: http://127.0.0.1:4178 (mock database, real Excel worker)'));
} else {
(async () => {
  const { chromium } = require('playwright');
  const browser = await chromium.launch({ headless: true, ...(process.env.PLAYWRIGHT_CHANNEL ? { channel: process.env.PLAYWRIGHT_CHANNEL } : {}) });
  try {
    const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.route('https://history.test/**', route => {
      const pathname = new URL(route.request().url()).pathname;
      if (pathname === '/') return route.fulfill({ contentType: 'text/html; charset=utf-8', body: fixtureHTML });
      if (pathname === '/history-fixture.mjs') return route.fulfill({ contentType: 'text/javascript; charset=utf-8', body: fixtureScript });
      const file = require('node:path').resolve('docs', '.' + pathname);
      return route.fulfill({ contentType: pathname.endsWith('.css') ? 'text/css' : 'text/javascript; charset=utf-8', body: fs.readFileSync(file) });
    });
    await page.goto('https://history.test/');
    await page.evaluate(() => { window.historyAccess(false); window.start(); });
    assert.equal(await page.locator('#history-file').isDisabled(), true);
    await page.evaluate(() => window.historyAccess(true));
    assert.equal(await page.locator('#history-file').isEnabled(), true);
    assert.equal(await page.locator('#history-export').isEnabled(), true);
    assert.equal(await page.locator('#history-access-status').textContent(), '');
    await page.evaluate(() => window.historyAccess(false, 'Sin acceso'));
    assert.equal(await page.locator('#history-file').isDisabled(), true);
    assert.equal(await page.locator('#history-access-status').textContent(), 'Sin acceso');
    await page.evaluate(() => window.historyAccess(true));
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
}
