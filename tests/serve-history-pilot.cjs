// Real Firebase pilot, restricted to the explicitly authorized disposable unit.
// No credentials are stored here. Enter the unit password in the browser.
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../docs');
const unit = '123456';
const account = 'martin.cornelli.lds@gmail.com';
function pilotSource() {
  let source = fs.readFileSync(path.join(root, 'app.js'), 'utf8');
  source = source.replace('unitNumber: localStorage.getItem(UNIT_STORAGE_KEY) || "",', 'unitNumber: "",');
  source = source.replace('state.user = user;', `if (user && user.email !== ${JSON.stringify(account)}) { state.user = null; state.isBooting = false; state.fatalError = new Error('Esta prueba requiere la cuenta autorizada.'); render(); return; }\n      state.user = user;`);
  source = source.replace('function subscribeUnitData(unitNumber) {', `function subscribeUnitData(unitNumber) {\n  if (unitNumber !== '${unit}') throw new Error('Prueba limitada a la unidad ${unit}.');`);
  source = source.replace('async function ensureUnitAccess(unitNumber, password) {', `async function ensureUnitAccess(unitNumber, password) {\n  if (unitNumber !== '${unit}') throw new Error('Prueba limitada a la unidad ${unit}.');`);
  source = source.replace('function renderChrome() {', `function renderChrome() {\n  document.title = 'PRUEBA — Unidad ${unit}';`);
  // This isolated local origin must never install a cached production shell.
  source = source.replace('navigator.serviceWorker.register("./sw.js")', 'Promise.resolve()');
  return source;
}
http.createServer((req, res) => {
  const pathname = new URL(req.url, 'http://localhost').pathname;
  const file = path.resolve(root, '.' + decodeURIComponent(pathname === '/' ? '/index.html' : pathname));
  if (!file.startsWith(root + path.sep) || !fs.existsSync(file) || !fs.statSync(file).isFile()) { res.writeHead(404); res.end(); return; }
  const mime = { '.html': 'text/html', '.css': 'text/css', '.js': 'text/javascript', '.mjs': 'text/javascript', '.webp': 'image/webp', '.webmanifest': 'application/manifest+json' };
  res.setHeader('Content-Type', (mime[path.extname(file)] || 'application/octet-stream') + '; charset=utf-8');
  res.setHeader('Cache-Control', 'no-store');
  res.end(file === path.join(root, 'app.js') ? pilotSource() : fs.readFileSync(file));
}).listen(4180, '127.0.0.1', () => console.log('Real Firebase pilot restricted to unit 123456: http://localhost:4180'));
