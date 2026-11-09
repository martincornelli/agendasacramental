// UI-only fixture: no Firebase imports, credentials, network writes, or service worker.
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../docs');
function fixtureSource() {
  let source = fs.readFileSync(path.join(root, 'app.js'), 'utf8');
  source = source.replace('boot();', 'previewUnitAdministration();');
  source = source.replace('navigator.serviceWorker.register("./sw.js")', 'Promise.resolve()');
  source += `
async function previewUnitAdministration() {
  const scenario = new URLSearchParams(location.search).get('case') || 'access';
  const uid = scenario === 'invited' ? 'guest' : scenario === 'ordinary' ? 'reader' : 'owner';
  state.user = { uid, email: uid + '@example.com', emailVerified: true, providerData: [{ providerId: 'google.com' }] };
  state.isBooting = false; state.isReady = true; state.unitNumber = '';
  const units = { '123': { numeroUnidad: '123', creadoPor: 'owner@example.com', administradores: { owner: 'owner@example.com' }, invitacionesAdmin: ['guest@example.com'] } };
  if (scenario === 'legacy') delete units['123'].administradores;
  db = {}; doc = (_, collection, id) => id; serverTimestamp = () => new Date();
  const snapshot = ref => ({ exists: () => Boolean(units[ref]), data: () => structuredClone(units[ref]) });
  getDoc = async ref => snapshot(ref);
  runTransaction = async (_, fn) => fn({ get: async ref => snapshot(ref), update: (ref, patch) => Object.assign(units[ref], patch), set: (ref, data) => units[ref] = data });
  signOut = async () => {};
  renderUnitAccess();
  document.title = 'LOCAL TEST — Unit administration';
  if (scenario === 'review') openUnitReview('123');
  else if (scenario === 'exists') openUnitReview('123', true);
  else if (scenario !== 'access') await openUnitAdministrators('123');
}
`;
  return source;
}
http.createServer((req, res) => {
  const pathname = new URL(req.url, 'http://localhost').pathname;
  const file = path.resolve(root, '.' + decodeURIComponent(pathname === '/' ? '/index.html' : pathname));
  if (!file.startsWith(root + path.sep) || !fs.existsSync(file) || !fs.statSync(file).isFile()) { res.writeHead(404); res.end(); return; }
  const mime = { '.html': 'text/html', '.css': 'text/css', '.js': 'text/javascript', '.mjs': 'text/javascript', '.webp': 'image/webp', '.webmanifest': 'application/manifest+json' };
  res.setHeader('Content-Type', (mime[path.extname(file)] || 'application/octet-stream') + '; charset=utf-8');
  res.setHeader('Cache-Control', 'no-store');
  res.end(file === path.join(root, 'app.js') ? fixtureSource() : fs.readFileSync(file));
}).listen(4182, '127.0.0.1', () => console.log('Isolated UI fixture: http://127.0.0.1:4182/?case=access&lang=es'));
