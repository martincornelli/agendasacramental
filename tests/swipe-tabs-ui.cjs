const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict');
const { chromium } = require('playwright');
const fixture = fs.readFileSync('tests/web-i18n-ui.cjs', 'utf8').match(/const fixture = `([\s\S]*?)`;/)[1];
(async () => {
  const browser = await chromium.launch({ headless: true, channel: 'chrome' });
  try {
    for (const language of ['es', 'en']) for (const width of [390, 820]) {
      const context = await browser.newContext({ viewport: { width, height: 900 }, hasTouch: true });
      const page = await context.newPage(), errors = [];
      page.on('pageerror', error => errors.push(error.message));
      await page.route('https://swipe.test/**', route => {
        const url = new URL(route.request().url());
        const file = path.join(path.resolve('docs'), url.pathname === '/' ? 'index.html' : url.pathname);
        if (!fs.existsSync(file)) return route.fulfill({ status: 404 });
        let body = fs.readFileSync(file);
        if (url.pathname === '/app.js') body = body.toString().replace('\nboot();', '\n' + fixture);
        return route.fulfill({ body, contentType: file.endsWith('.html') ? 'text/html' : file.endsWith('.css') ? 'text/css' : 'text/javascript' });
      });
      await page.goto('https://swipe.test/?lang=' + language);
      await page.waitForFunction(() => window.qa);
      const cdp = await context.newCDPSession(page);
      const swipe = async (x1, y1, x2, y2) => {
        await cdp.send('Input.dispatchTouchEvent', { type: 'touchStart', touchPoints: [{ x: x1, y: y1 }] });
        for (let i = 1; i <= 6; i++) await cdp.send('Input.dispatchTouchEvent', { type: 'touchMove', touchPoints: [{ x: x1 + (x2-x1)*i/6, y: y1 + (y2-y1)*i/6 }] });
        await cdp.send('Input.dispatchTouchEvent', { type: 'touchEnd', touchPoints: [] });
      };
      for (const route of ['planning', 'hymns']) {
        await page.evaluate(route => { qa.state.route = route; qa.state.planningTab = 'talks'; qa.state.hymnPlannerTab = 'dates'; qa.render(); }, route);
        const selector = route === 'planning' ? '[data-planning-tab]' : '[data-hymn-tab]';
        const selected = () => page.locator(selector).evaluateAll(tabs => tabs.findIndex(tab => tab.getAttribute('aria-selected') === 'true'));
        // The tabs themselves provide a stable, non-interactive text swipe surface.
        const box = await page.locator(selector).first().locator('..').boundingBox();
        const y = box.y + box.height / 2;
        await swipe(width*.72, y, width*.25, y); assert.equal(await selected(), 1);
        await swipe(width*.25, y, width*.72, y); assert.equal(await selected(), 0);
        await swipe(width*.25, y, width*.72, y); assert.equal(await selected(), 0, 'first tab never wraps');
        await page.evaluate(({selector, y}) => {
          const target = document.querySelector(selector);
          const point = x => new Touch({identifier: 1, target, clientX: x, clientY: y});
          target.dispatchEvent(new TouchEvent('touchstart', {bubbles:true, touches:[point(5)]}));
          target.dispatchEvent(new TouchEvent('touchmove', {bubbles:true, cancelable:true, touches:[point(200)]}));
          target.dispatchEvent(new TouchEvent('touchend', {bubbles:true, touches:[], changedTouches:[point(200)]}));
        }, {selector,y});
        assert.equal(await selected(), 0, 'edge gestures stay with browser');
        await page.locator(selector).first().focus(); await page.keyboard.press('End');
        assert.equal(await selected(), await page.locator(selector).count()-1);
        await page.keyboard.press('Home'); assert.equal(await selected(), 0);
        await swipe(width*.5, y, width*.51, y+150); assert.equal(await selected(), 0, 'vertical scroll must not switch tabs');
        await page.locator(selector).nth(1).click(); assert.equal(await selected(), 1, 'tap alternative');
      }
      assert.deepEqual(errors, []); await context.close();
    }
    console.log('Swipe tabs passed: ES/EN phone/tablet, both planners, both directions, boundaries, vertical/edge gestures, tap and keyboard.');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
