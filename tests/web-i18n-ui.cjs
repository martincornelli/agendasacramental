// Real browser, mocked Firebase. Run with Playwright available on NODE_PATH.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { chromium } = require('playwright');
const root = path.resolve('docs');
const fixture = `
state.isBooting = false; state.isReady = true; state.historyReady = true;
state.user = { email: 'qa@example.com' }; state.unitNumber = '123';
state.agendas = [{ ...createBlankAgenda(new Date(2026, 10, 8)), id: 'qa-agenda',
  preside: 'Fe', dirige: 'Santa Cena', anuncios: 'Guardar\\nAnuncio, con coma',
  primeraOracion: 'José Pérez', oracionFinal: 'Ana',
  mensajesEvangelio: [{tipo:'DISCURSO',nombre:'Nombre',tema:'Tema original',etiquetaTema:'Fe'}]
}];
state.hermanos = [{id:'qa-member',nombre:'Fe'}];
collection = () => 'agendas'; doc = (...args) => args.join('/'); db = {};
serverTimestamp = () => 0; Timestamp = { fromDate: d => d };
updateDoc = async (ref, data) => { window.qaSaved = data; };
addDoc = async (ref, data) => { window.qaSaved = data; return {id:'qa-created'}; };
writeBatch = () => ({ update(){},set(){},commit:async()=>{} });
window.qa = { state, render, renderLogin, renderUnitAccess, renderAgendaEditor, renderReadingMode,
renderPdfMode, renderPlanning, renderHymnPlanner, renderSettings, openHistoryTransfer,
openHistoryMapping, openHistoryPreview, exportableHistory, agendaToFirestore, pdfHtml,
language: uiLanguage, hymnCatalog, closeModal, createBlankAgenda, readAgendaForm };
render();
`;

(async () => {
  const browser = await chromium.launch({headless:true, channel:process.env.PLAYWRIGHT_CHANNEL || 'chrome'});
  fs.mkdirSync('output/web-i18n', {recursive:true});
  try {
    for (const language of ['es','en']) for (const width of [390,1280]) {
      const context = await browser.newContext({viewport:{width,height:900},locale:language==='en'?'es-UY':'en-US',serviceWorkers:'block'});
      const page = await context.newPage(); const errors=[];
      page.on('pageerror', e => errors.push(e.message));
      await page.route('https://i18n.test/**', route => {
        const pathname = new URL(route.request().url()).pathname;
        const file = path.join(root, pathname==='/'?'index.html':pathname);
        if(!fs.existsSync(file))return route.fulfill({status:404});
        let body=fs.readFileSync(file);
        if(pathname==='/app.js')body=body.toString().replace('\nboot();', '\n'+fixture);
        const type=file.endsWith('.html')?'text/html':file.endsWith('.css')?'text/css':/\.(mjs|js)$/.test(file)?'text/javascript':file.endsWith('.webp')?'image/webp':'application/json';
        return route.fulfill({body,contentType:type});
      });
      await page.goto('https://i18n.test/?lang='+language);
      await page.waitForFunction(()=>window.qa);
      assert.equal(await page.locator('html').getAttribute('lang'),language);
      const original = await page.evaluate(()=>JSON.stringify(window.qa.state.agendas));
      for (const route of ['agendas','planning','hymns','settings','edit','reading','pdf']) {
        await page.evaluate(route=>{qa.state.route=route;qa.state.activeAgendaId='qa-agenda';qa.render();},route);
        await page.waitForTimeout(60);
        assert.equal(await page.locator('#screen').innerText().then(t=>t.length>10),true,route);
        if(route!=='pdf')assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1),false,`${language}/${width}/${route}: horizontal overflow`);
        if(language==='en'&&route==='edit') {
          assert.equal(await page.locator('#preside').inputValue(),'Fe');
          assert.equal(await page.locator('#dirige').inputValue(),'Santa Cena');
          assert.equal(await page.locator('#estado').inputValue(),'BORRADOR');
          assert.ok((await page.locator('#screen').innerText()).includes('Save'));
          const payload=await page.evaluate(()=>qa.agendaToFirestore(qa.readAgendaForm(qa.state.agendas[0]),qa.state.agendas[0]));
          assert.equal(payload.preside,'Fe'); assert.equal(payload.dirige,'Santa Cena');
          assert.equal(payload.estado,'BORRADOR'); assert.equal(payload.mensajesEvangelio[0].tipo,'DISCURSO');
          assert.equal(payload.mensajesEvangelio[0].tema,'Tema original');
          // Canceling a language change must retain unsaved edits.
          await page.locator('#preside').fill('Unsaved name');
          page.once('dialog',dialog=>dialog.dismiss());
          await page.locator('#sidebar-language select').selectOption('es',{force:true});
          assert.equal(await page.locator('#preside').inputValue(),'Unsaved name');
          assert.equal(await page.locator('#sidebar-language select').inputValue(),'en');
        }
        if(route==='pdf') {
          const text=await page.locator('.pdf-document').innerText();
          assert.ok(text.includes(language==='en'?'SACRAMENT MEETING AGENDA':'AGENDA REUNIÓN SACRAMENTAL'));
          assert.ok(text.includes('Guardar')); assert.ok(text.includes('Anuncio, con coma'));
          assert.equal(await page.locator('.pdf-page').evaluate(el=>el.scrollWidth>el.clientWidth+1),false);
        }
        if(width===390&&['agendas','edit','pdf'].includes(route))await page.screenshot({path:`output/web-i18n/${language}-${route}.png`,fullPage:true});
      }
      assert.equal(await page.evaluate(()=>JSON.stringify(qa.state.agendas)),original,'Rendering never changes stored content');
      await page.evaluate(()=>{qa.state.route='hymns';qa.state.hymnPlannerTab='library';qa.render();});
      assert.ok((await page.locator('#screen').innerText()).includes(language==='en'?'New hymnbook':'Himnario nuevo'));
      await page.evaluate(()=>{qa.openHistoryTransfer();});
      assert.ok((await page.locator('#modal').innerText()).includes(language==='en'?'Export history':'Exportar historial'));
      await page.evaluate(()=>qa.closeModal());
      await page.evaluate(()=>qa.openHistoryMapping([['Date','Name','Participation','Topic'],['2026-01-04','Fe','Talk','Santa Cena']]));
      assert.ok((await page.locator('#modal').innerText()).includes(language==='en'?'Choose columns':'Elegir las columnas'));
      await page.locator('#history-mapping').evaluate(form=>form.requestSubmit());
      assert.ok((await page.locator('#modal').innerText()).includes(language==='en'?'Review before importing':'Revisar antes de importar'));
      assert.ok((await page.locator('#modal').innerText()).includes('Santa Cena'));
      await page.evaluate(()=>qa.closeModal());
      await page.evaluate(()=>qa.renderLogin());
      assert.ok((await page.locator('#screen').innerText()).includes(language==='en'?'Sign in with Google':'Iniciar sesión con Google'));
      assert.equal(await page.locator('#screen [data-language]').inputValue(),language);
      // A language change uses the URL and saved preference, even against browser locale.
      await page.evaluate(()=>{qa.state.route='agendas';qa.render();});
      const next=language==='en'?'es':'en';
      await page.evaluate(()=>qa.renderSettings());
      await page.locator('#screen [data-language]').selectOption(next);
      await page.waitForURL(url => url.searchParams.get('lang') === next);
      await page.waitForFunction(expected=>window.qa?.language()===expected,next);
      assert.equal(await page.evaluate(()=>localStorage.getItem('agenda-sacramental-language')),next);
      assert.deepEqual(errors,[],`${language}/${width}: browser errors`);
      await context.close();
    }
    console.log('ES/EN browser checks passed: desktop/mobile, routes, PDF, user data, canonical values, library, import, login, and language persistence.');
  } finally {await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
