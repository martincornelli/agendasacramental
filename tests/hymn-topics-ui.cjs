// Real browser UI with local Firebase doubles; never contacts a production database.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {chromium}=require('playwright');
const base=fs.readFileSync('tests/web-i18n-ui.cjs','utf8').match(/const fixture = `([\s\S]*?)`;/)[1];
const fixture=base+`
state.hymnTopicsReady = true;
setDoc = async (ref, data) => { if(window.failSave) throw Error('offline'); window.savedTopic = {ref,data}; };
deleteDoc = async ref => { window.restoredTopic = ref; };
window.qa.openTopics = openHymnTopicsDialog;
window.qa.openGenerator = openHymnGeneratorDialog;
state.route='hymns'; state.hymnPlannerTab='library'; render();
`;
(async()=>{
 const browser=await chromium.launch({headless:true,channel:'chrome'});
 fs.mkdirSync('output/hymn-topics-ui',{recursive:true});
 try {
  for(const language of ['es','en']) for(const viewport of [{width:375,height:812},{width:812,height:375}]) for(const theme of ['light','dark']) {
   const ctx=await browser.newContext({viewport,serviceWorkers:'block',reducedMotion:'reduce'});
   const page=await ctx.newPage(), errors=[];page.on('pageerror',e=>{errors.push(e.message);console.error(e.message);});
   await page.route('https://hymn-topics.test/**',route=>{
    const url=new URL(route.request().url()),file=path.join(path.resolve('docs'),url.pathname==='/'?'index.html':url.pathname);
    if(!fs.existsSync(file))return route.fulfill({status:404});
    let body=fs.readFileSync(file);if(url.pathname==='/app.js')body=body.toString().replace('\nboot();','\n'+fixture);
    return route.fulfill({body,contentType:file.endsWith('.html')?'text/html':file.endsWith('.css')?'text/css':/\.(js|mjs)$/.test(file)?'text/javascript':'application/octet-stream'});
   });
   await page.goto('https://hymn-topics.test/?lang='+language);
   await page.waitForFunction(()=>window.qa);
   await page.evaluate(theme=>document.documentElement.dataset.theme=theme,theme);
   assert.equal(await page.locator('.hymn-library-filters').getAttribute('open'),null);
   await page.screenshot({path:'output/hymn-topics-ui/library-'+language+'-'+viewport.width+'-'+theme+'.png'});
   await page.locator('[data-edit-hymn-topics="1"]').click();
   const input=page.locator('#unit-hymn-topics');
   assert.ok((await input.inputValue()).length>0);
   await input.fill('Local topic\nAnother topic');
   await page.evaluate(()=>window.failSave=true);
   await page.locator('[data-save-topics]').click();
   await page.waitForFunction(()=>document.querySelector('#unit-topics-error').textContent.length>0);
   assert.equal(await input.inputValue(),'Local topic\nAnother topic');
   await page.evaluate(()=>window.failSave=false);
   await page.locator('[data-save-topics]').click();
   await page.waitForFunction(()=>!document.querySelector('dialog').open);
   const saved=await page.evaluate(()=>window.savedTopic);
   assert.ok(saved.ref.endsWith('/temasHimnosUnidad/123/himnos/'+language+'-1'));
   assert.deepEqual(saved.data.temas,['Local topic','Another topic']);
   await page.locator('.hymn-library-filters > summary').click();
   await page.locator('#hymn-category').selectOption('Local topic');
   assert.equal(await page.locator('[data-edit-hymn-topics]').count(),1);
   await page.locator('[data-edit-hymn-topics="1"]').click();
   await page.screenshot({path:'output/hymn-topics-ui/'+language+'-'+viewport.width+'-'+theme+'.png',fullPage:true});
   assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1),false);
   await page.locator('[data-restore-topics]').click();
   await page.waitForFunction(()=>window.restoredTopic);
   assert.equal(await page.evaluate(language=>Object.hasOwn(qa.state.hymnTopicOverrides,language+'-1'),language),false);
   await page.evaluate(()=>{qa.state.hymnPlannerYear=new Date().getFullYear()+1; qa.openGenerator();});
   await page.locator('#hymn-season').selectOption('CHRISTMAS_ONLY');
   assert.equal(await page.locator('#hymn-season').inputValue(),'CHRISTMAS_ONLY');
   const seasonLabel=await page.locator('#hymn-season option:checked').innerText();
   assert.equal(seasonLabel,language==='en'?'Christmas only':'Solo Navidad');
   await page.screenshot({path:'output/hymn-topics-ui/season-'+language+'-'+viewport.width+'-'+theme+'.png'});
   await page.locator('button[form="hymn-generator-form"]').click();
   await page.waitForFunction(()=>!document.querySelector('#hymn-generator-form'));
   assert.equal(await page.evaluate(()=>qa.state.config.modoTemporadaHimnos),'CHRISTMAS_ONLY');
   await page.evaluate(()=>qa.closeModal());
   assert.deepEqual(errors,[]);
   await ctx.close();
  }
  console.log('Passed: Spanish/English, portrait/landscape, light/dark, save failure/retry, filter and restore.');
 } finally {await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
