// Exercises the bundled Android UI and real Excel worker; all native/Firebase IO is fake.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { chromium } = require('playwright');
const XLSX = require('../docs/vendor/xlsx-0.20.3.full.min.js');
(async () => {
  const browser = await chromium.launch({ headless: true, channel: process.env.PLAYWRIGHT_CHANNEL || 'chrome' });
  try {
    for (const language of ['es', 'en']) {
      const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
      const errors = [];
      page.on('pageerror', e => errors.push(e.message));
      await page.route('https://appassets.androidplatform.net/history/**', route => {
        const name = new URL(route.request().url()).pathname.replace('/history/', '');
        const local = path.join('app/src/main/assets/history', name);
        const file = fs.existsSync(local) ? local : path.join('docs', name);
        return route.fulfill({ contentType: name.endsWith('.html') ? 'text/html' : name.endsWith('.css') ? 'text/css' : 'application/javascript', body: name === 'import.mjs' ? fs.readFileSync(file, 'utf8').replace('function message(e){', 'function message(e){window.rawError=e.message;') : fs.readFileSync(file) });
      });
      await page.addInitScript(() => {
        window.records = [{ id: '123456:import:old', date: '2026-01-04', name: 'Ana', type: 'Discurso', topic: 'Fe', source: 'import' }];
        window.revision = 1; window.downloads = []; window.saves = []; window.nextFile = null;
        window.AndroidHistory = { request: json => {
          const r = JSON.parse(json);
          setTimeout(() => {
            let result;
            if (r.action === 'load') result = { unit: '123456', revision: window.revision, existing: window.records };
            if (r.action === 'pick') result = window.nextFile;
            if (r.action === 'download') { window.downloads.push(r); result = true; }
            if (r.action === 'save') {
              if (window.forceConflict) { window.nativeResult(r.id, null, 'History changed. Reopen the preview.'); return; }
              window.saves.push(r); window.revision++;
              r.changes.forEach(change => { const i = window.records.findIndex(x => x.id === change.id); const value = { ...change, source: 'import' }; if (i < 0) window.records.push(value); else window.records[i] = value; });
              result = true;
            }
            window.nativeResult(r.id, result, null);
          }, 10);
        }};
      });
      await page.goto(`https://appassets.androidplatform.net/history/index.html?lang=${language}`);
      await page.locator('#template').click();
      await page.waitForFunction(() => window.downloads.length === 1 || document.querySelector('#error')?.textContent); assert.equal(await page.locator('#error').textContent(), '', language + ': initial workbook export: ' + await page.evaluate(() => window.rawError));
      const template = await page.evaluate(() => window.downloads[0]);
      const book = XLSX.read(Buffer.from(template.base64, 'base64'));
      assert.equal(XLSX.utils.sheet_to_json(book.Sheets[book.SheetNames[0]], { header: 1 }).length, 1);
      await page.locator('#format').selectOption('csv');
      await page.locator('#export').click();
      await page.waitForFunction(() => window.downloads.length === 2);
      const exported = await page.evaluate(() => window.downloads[1]);
      const csv = Buffer.from(exported.base64, 'base64').toString('utf8');
      assert.ok(csv.includes(language === 'en' ? 'Participation' : 'Participación'));
      const extra = '\r\n"";"2026-01-11";"José Pérez";"Talk";"Faith, hope and charity"';
      await page.evaluate(file => { window.nextFile = file; }, { name: 'history.csv', base64: Buffer.from(csv + extra).toString('base64') });
      await page.locator('#pick').click();
      await page.locator('#layout').waitFor();
      assert.equal(await page.locator('[data-field="talk"]').isVisible(), false);
      assert.equal(await page.locator('[data-field="name"]').isVisible(), true);
      await page.locator('#layout').selectOption('wide');
      assert.equal(await page.locator('[data-field="talk"]').isVisible(), true);
      assert.equal(await page.locator('[data-field="name"]').isVisible(), false);
      await page.locator('#layout').selectOption('long');
      await page.locator('#preview').click();
      assert.equal(await page.locator('[data-row]:checked').count(), 1);
      assert.equal(await page.locator('[data-row]:disabled').count(), 1);
      await page.locator('#save').click(); await page.locator('#status').waitFor();
      assert.equal(await page.evaluate(() => window.records.length), 2);
      // Export, append in Excel, reimport: existing IDs stay unchanged, only one new row saves.
      await page.locator('#export').click();
      await page.waitForFunction(() => window.downloads.length === 3);
      const excel = await page.evaluate(() => window.downloads[2]);
      const workbook = XLSX.read(Buffer.from(excel.base64, 'base64'));
      const sheet = workbook.Sheets[workbook.SheetNames[0]];
      XLSX.utils.sheet_add_aoa(sheet, [['', '2026-01-18', 'María', 'Opening prayer', '']], { origin: -1 });
      const bytes = XLSX.write(workbook, { type: 'buffer', bookType: 'xlsx' });
      await page.evaluate(file => { window.nextFile = file; }, { name: 'history.xlsx', base64: Buffer.from(bytes).toString('base64') });
      await page.locator('#pick').click(); await page.waitForFunction(() => document.querySelector('#next') || document.querySelector('#error')?.textContent); assert.equal(await page.locator('#error').textContent(), ''); await page.locator('#next').click(); await page.locator('#preview').click();
      assert.equal(await page.locator('[data-row]:checked').count(), 1);
      await page.locator('#save').click(); await page.locator('#status').waitFor();
      assert.equal(await page.evaluate(() => window.records.length), 3);
      // Omission does not delete; a correction is unchecked until explicitly confirmed.
      const correction = 'ID;Date;Name;Participation;Topic\n123456:import:old;2026-01-04;Ana;Talk;Hope';
      await page.evaluate(file => { window.nextFile = file; }, { name: 'correction.csv', base64: Buffer.from(correction).toString('base64') });
      await page.locator('#pick').click(); await page.locator('#preview').click();
      assert.equal(await page.locator('[data-row]:checked').count(), 0);
      await page.locator('[data-row]').check();
      await page.evaluate(() => { window.forceConflict = true; });
      await page.locator('#save').click();
      await page.waitForFunction(() => document.querySelector('#error').textContent.length > 0);
      assert.equal(await page.evaluate(() => window.saves.length), 2);
      await page.evaluate(() => { window.forceConflict = false; });
      await page.locator('#save').click(); await page.locator('#status').waitFor();
      assert.equal(await page.evaluate(() => window.records.length), 3);
      assert.equal(await page.evaluate(() => window.records[0].topic), 'Hope');
      assert.deepEqual(errors, []);
      await page.close();
      console.log(`${language}: CSV + XLSX import/export, template, unchanged IDs, correction, conflict, omissions passed`);
    }
  } finally { await browser.close(); }
})().catch(e => { console.error(e); process.exitCode = 1; });
