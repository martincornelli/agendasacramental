// Visual coverage of the shared design in both languages, themes and viewport sizes.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {chromium}=require('playwright');
const fixture=fs.readFileSync('tests/web-i18n-ui.cjs','utf8').match(/const fixture = `([\s\S]*?)`;/)[1];
(async()=>{
 const browser=await chromium.launch({headless:true,channel:'chrome'});
 fs.mkdirSync('output/redesign-web',{recursive:true});
 try {
  for(const language of ['es','en']) for(const width of [375,768,1280]) for(const theme of ['light','dark']) {
   const ctx=await browser.newContext({viewport:{width,height:900},serviceWorkers:'block',reducedMotion:'reduce'});
   const page=await ctx.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message));
   await page.route('https://design.test/**',r=>{
    const url=new URL(r.request().url()),f=path.join(path.resolve('docs'),url.pathname==='/'?'index.html':url.pathname);
    if(!fs.existsSync(f))return r.fulfill({status:404});
    let body=fs.readFileSync(f);if(url.pathname==='/app.js')body=body.toString().replace('\nboot();','\n'+fixture);
    return r.fulfill({body,contentType:f.endsWith('.html')?'text/html':f.endsWith('.css')?'text/css':/\.(js|mjs)$/.test(f)?'text/javascript':'application/octet-stream'});
   });
   await page.goto('https://design.test/?lang='+language);await page.waitForFunction(()=>window.qa);
   await page.evaluate(theme=>{document.documentElement.dataset.theme=theme;qa.state.agendas[0].fecha=new Date(2026,10,15);},theme);
   for(const route of ['agendas','planning','hymns','settings','edit','reading']) {
    await page.evaluate(route=>{qa.state.route=route;qa.state.activeAgendaId='qa-agenda';qa.render();},route);
    await page.waitForTimeout(100);
    assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth+1),false,`${language}/${width}/${theme}/${route}`);
    await page.screenshot({path:`output/redesign-web/${language}-${width}-${theme}-${route}.png`});
   }
   assert.deepEqual(errors,[]);await ctx.close();
  }
  console.log('Design visuals passed: 72 views, ES/EN, light/dark, 375/768/1280px, reduced motion; no overflow or JS errors.');
 } finally {await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
