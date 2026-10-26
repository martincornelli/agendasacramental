const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const { chromium } = require('playwright');
const source = fs.readFileSync('docs/app.js', 'utf8');
const names = ['pdfHtml','pdfField','pdfListSection','pdfBusinessSection','pdfMessagesSection','pdfEmptyLines','splitAnnouncements','splitCommaItems','pdfDensityClass','escapeHtml','escapeAttr','fitPdfPages'];
const code = source.slice(source.indexOf('const MEETING_TEXT'), source.indexOf('const UNIT_STORAGE_KEY')) + names.map(name => {
  const start = source.indexOf(`function ${name}(`);
  return source.slice(start, source.indexOf('\n}', start) + 2);
}).join('\n');
(async () => {
  const browser = await chromium.launch({ headless: true, channel: process.env.PLAYWRIGHT_CHANNEL || 'chrome' });
  fs.mkdirSync('output/history-feature-qa', { recursive: true });
  try {
    for (const language of ['es','en']) {
      const context = vm.createContext({ navigator: { language }, formatDateShort: () => '26/10/2026', hymnLabel: () => '', businessBlocksInOrder: () => [], messageText: () => '' });
      vm.runInContext(code, context);
      const announcement = language === 'es' ? 'El sábado, a las 18:00, habrá una actividad. Traer agua, comida y abrigo. ' : 'On Saturday, at 6 pm, there will be an activity. Bring water, food and a coat. ';
      const agenda = { fecha: new Date(), anuncios: announcement.repeat(12) + '\nhttps://example.com/' + 'abcdefghij'.repeat(25), reconocimientos:'', asuntosEstacaBarrio:[], mensajesEvangelio:[], presentacionPrimaria:true, reunionTestimonios:false };
      const page = await browser.newPage({ viewport:{width:1000,height:1400} });
      await page.setContent(`<html lang="${language}"><head><style>${fs.readFileSync('docs/styles.css','utf8')}</style></head><body><div class="pdf-document">${context.pdfHtml(agenda)}</div></body></html>`);
      await page.addScriptTag({ content: code });
      await page.evaluate(() => fitPdfPages());
      assert.equal(await page.locator('.pdf-list li').count(), 2);
      assert.equal(await page.locator('.pdf-list li').first().textContent(), announcement.repeat(12).trim());
      assert.ok((await page.locator('.pdf-box').last().textContent()).includes(language === 'en' ? 'General Handbook 29.8' : 'Manual General 29.8'));
      for (const print of [false,true]) {
        await page.emulateMedia({media:print?'print':'screen'});
        const overflow = await page.locator('.pdf-list').evaluate(el => el.scrollWidth > el.clientWidth + 1);
        assert.equal(overflow,false,`${language}: no horizontal announcement overflow`);
      }
      await page.pdf({path:`output/history-feature-qa/web-${language}.pdf`,format:'A4',printBackground:true,preferCSSPageSize:true});
      await page.emulateMedia({media:'screen'});
      await page.locator('.pdf-page').screenshot({path:`output/history-feature-qa/web-${language}.png`});
      await page.close();
    }
    console.log('PDF web: ES/EN, long announcements and unbroken URLs fit the margins; commas remain within each announcement.');
  } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
