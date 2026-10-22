const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync('docs/app.js', 'utf8');
function load(names, globals = {}) {
  const context = vm.createContext(globals);
  const translations = source.slice(source.indexOf('const MEETING_TEXT'), source.indexOf('const UNIT_STORAGE_KEY'));
  const functions = names.map(name => {
    const start = source.indexOf(`function ${name}(`);
    assert.ok(start >= 0, name);
    return source.slice(start, source.indexOf('\n}', start) + 2);
  });
  vm.runInContext(translations + functions.join('\n'), context);
  return context;
}
class Element {
  constructor() { this.dataset = {}; this.events = {}; this.children = []; this.attrs = {}; this.value = ''; this.checked = false; this.hidden = false; this.classList = { toggle: (_, flag) => { this.hidden = flag; } }; }
  addEventListener(name, fn) { this.events[name] = fn; }
  append(el) { this.children.push(el); }
  replaceChildren() { this.children = []; }
  after(el) { this.hint = el; }
  setAttribute(key, value) { this.attrs[key] = value; }
  getAttribute(key) { return this.attrs[key]; }
}

test('imported history feeds planning dates, frequency and topics without creating agendas or people', () => {
  const state = {
    unitNumber: '123', agendas: [], hermanos: [],
    importedHistory: [
      { id: 'one', name: 'Ana', date: '2026-01-04', type: 'Discurso', topic: 'Fe' },
      { id: 'two', name: 'Ana', date: '2026-01-11', type: 'Primera oración', topic: '' }
    ]
  };
  const c = load(['planningRankings', 'collectHistoryNames', 'normalizeName', 'normalizeText', 'topicRecords'], {
    state, navigator: { language: 'es' },
    latestDate: dates => dates.length ? dates.reduce((a, b) => a > b ? a : b) : null,
    countWithinDays: dates => dates.length
  });
  const [ranking] = c.planningRankings();
  assert.equal(ranking.hermano.nombre, 'Ana');
  assert.equal(ranking.vecesDiscurso90Dias, 1);
  assert.equal(ranking.vecesOracion90Dias, 1);
  assert.equal(ranking.ultimoTemaDiscurso, 'Fe');
  assert.equal(ranking.ultimaVezDiscurso.getDate(), 4);
  assert.equal(ranking.ultimaVezOracion.getDate(), 11);
  assert.equal(c.topicRecords()[0].tema, 'Fe');
  assert.deepEqual(state.agendas, []);
  assert.deepEqual(state.hermanos, []);
});
function nameField(value, names) {
  const input = new Element(); input.value = value;
  const context = load(['bindNameSuggestions', 'normalizeName', 'normalizeText'], {
    navigator: { language: 'es' }, usedNames: () => names,
    document: { createElement: () => new Element() }
  });
  context.bindNameSuggestions({ querySelectorAll: () => [input] });
  return input;
}
test('trailing spaces are removed and an exact name never prompts', () => {
  const input = nameField('José Pérez   ', ['José Pérez', 'Jose Perez']);
  assert.equal(input.value, 'José Pérez');
  assert.equal(input.hint.children.length, 0);
});
test('accept and reject dismiss suggestions, editing enables a fresh decision', () => {
  for (const accept of [true, false]) {
    const input = nameField('jose perez', ['José Pérez']);
    assert.equal(input.hint.children.length, 3);
    input.hint.children[accept ? 1 : 2].events.click();
    assert.equal(input.value, accept ? 'José Pérez' : 'jose perez');
    assert.equal(input.hint.children.length, 0);
    input.events.blur();
    assert.equal(input.hint.children.length, 0);
    input.value = 'JOSE PEREZ'; input.events.input();
    assert.equal(input.hint.children.length, 3);
  }
});
test('name suggestions ignore accents, case and repeated whitespace', () => {
  const input = nameField('  JOSE   PEREZ  ', ['José Pérez']);
  assert.equal(input.hint.children.length, 3);
  assert.equal(input.hint.children[0].textContent, '¿Quisiste decir José Pérez?');
});
test('history suggestions collapse whitespace-only variants', () => {
  const c = load(['usedNames'], { collectHistoryNames: () => ['Ana ', 'Ana', 'Ana\t'], state: { hermanos: [{ nombre: 'Ana  ' }] } });
  assert.deepEqual(Array.from(c.usedNames()), ['Ana']);
});
test('agenda cards keep the mobile hierarchy compact and fully actionable', () => {
  const c = load(['agendaCard'], {
    escapeAttr: value => String(value), escapeHtml: value => String(value), formatDateLong: () => 'domingo, 15 de noviembre de 2026',
    formatAgendaDateCompact: () => 'dom, 15 nov 2026', labelState: () => 'Borrador', meetingText: () => 'Presentación de la Primaria',
    icon: name => `<i>${name}</i>`, buttonIcon: name => `<i>${name}</i>`
  });
  const html = c.agendaCard({
    id: 'agenda-1', estado: 'BORRADOR', fecha: new Date(2026, 10, 15), preside: 'José Pérez', dirige: '', asistencia: 0,
    mensajesEvangelio: [], testimonios: [], presentacionPrimaria: false, reunionTestimonios: false
  }, true);
  assert.ok(html.includes('agenda-card-main'));
  assert.ok(html.includes('agenda-date-compact'));
  assert.ok(html.includes('agenda-card-highlighted'));
  assert.ok(html.includes('data-read="agenda-1"'));
  assert.ok(html.includes('data-print-agenda="agenda-1"'));
  assert.ok(html.includes('data-delete-agenda="agenda-1"'));
  assert.ok(!html.includes('agenda-summary'));
});
test('special meeting options are exclusive and restore regular messages', () => {
  const elements = Object.fromEntries(['#presentacionPrimaria', '#reunionTestimonios', '[data-testimonies-field]', '[data-primary-announcement]', '#message-list', '#add-message', '[data-messages-heading]'].map(key => [key, new Element()]));
  const c = load(['syncTestimoniesFieldVisibility'], { screen: { querySelector: key => elements[key] } });
  const primary = elements['#presentacionPrimaria'], testimony = elements['#reunionTestimonios'];
  primary.checked = testimony.checked = true;
  c.syncTestimoniesFieldVisibility({ target: primary });
  assert.equal(testimony.checked, false);
  assert.equal(elements['#message-list'].hidden, true);
  assert.equal(elements['[data-primary-announcement]'].hidden, false);
  testimony.checked = true; c.syncTestimoniesFieldVisibility({ target: testimony });
  assert.equal(primary.checked, false);
  assert.equal(elements['[data-testimonies-field]'].hidden, false);
  testimony.checked = false; c.syncTestimoniesFieldVisibility({ target: testimony });
  assert.equal(elements['#message-list'].hidden, false);
});
test('Primary presentation persists, old agendas default to regular, both languages export', () => {
  for (const language of ['es', 'en']) {
    const c = load(['normalizeAgenda', 'agendaToFirestore', 'pdfMessagesSection', 'agendaText', 'readingHtml', 'escapeHtml'], {
      navigator: { language }, state: { unitNumber: '123' }, userEmail: () => 'test@example.com', serverTimestamp: () => 'now',
      Timestamp: { fromDate: date => date }, toDate: value => value || new Date(), formatDateLong: () => 'date', formatDate: () => 'date',
      hymnLabel: () => 'Hymn', readingLine: () => '', labelState: () => 'Draft'
    });
    assert.equal(c.normalizeAgenda('old', {}).presentacionPrimaria, false);
    const agenda = c.normalizeAgenda('new', { presentacionPrimaria: true, reunionTestimonios: true });
    assert.equal(agenda.reunionTestimonios, false);
    assert.equal(c.agendaToFirestore(agenda).presentacionPrimaria, true);
    const announcement = c.meetingText('announcement');
    for (const output of [c.pdfMessagesSection(agenda), c.agendaText(agenda), c.readingHtml(agenda)]) {
      assert.ok(output.includes(announcement));
      assert.ok(!output.includes('Mensajes del Evangelio'));
    }
  }
});
test('internal navigation keeps Agendas below every module and agenda view', () => {
  const c = load(['navigationAction']);
  for (const route of ['planning', 'hymns', 'settings', 'edit', 'reading', 'pdf']) {
    assert.equal(c.navigationAction('agendas', route), 'push');
    assert.equal(c.navigationAction(route, 'agendas'), 'back');
  }
  assert.equal(c.navigationAction('edit', 'reading'), 'replace');
  assert.equal(c.navigationAction('reading', 'pdf'), 'replace');
  assert.equal(c.navigationAction('planning', 'hymns'), 'replace');
  assert.equal(c.navigationAction('agendas', 'agendas'), 'stay');
});
test('module switches replace the child entry and Back returns to Agendas', () => {
  const calls = [];
  const state = { route: 'agendas', activeAgendaId: null };
  const history = {
    pushState: (entry, _title, url) => calls.push(['push', entry.route, url]),
    replaceState: (entry, _title, url) => calls.push(['replace', entry.route, url]),
    back: () => calls.push(['back'])
  };
  const c = load(['navigate', 'navigationAction', 'commitNavigation', 'navigationEntry', 'navigationUrl'], {
    state, history, APP_HISTORY_KEY: 'agenda-sacramental-route', encodeURIComponent,
    closeMobileMenu: () => {},
    applyNavigation: entry => { state.route = entry.route; state.activeAgendaId = entry.agendaId; }
  });
  c.navigate('planning');
  c.navigate('hymns');
  c.navigate('agendas');
  assert.deepEqual(calls, [
    ['push', 'planning', '#planning'],
    ['replace', 'hymns', '#hymns'],
    ['back']
  ]);
});
test('agenda routes are encoded in the URL and restored safely', () => {
  const globals = { location: { hash: '#agendas' }, encodeURIComponent, decodeURIComponent };
  const c = load(['navigationFromHash', 'navigationUrl'], globals);
  assert.equal(c.navigationUrl('edit', 'agenda 15/11'), '#edit/agenda%2015%2F11');
  assert.deepEqual(
    JSON.parse(JSON.stringify(c.navigationFromHash('#reading/agenda%2015%2F11'))),
    { route: 'reading', agendaId: 'agenda 15/11' }
  );
  assert.deepEqual(
    JSON.parse(JSON.stringify(c.navigationFromHash('#pdf'))),
    { route: 'agendas', agendaId: null }
  );
  assert.deepEqual(
    JSON.parse(JSON.stringify(c.navigationFromHash('#outside'))),
    { route: 'agendas', agendaId: null }
  );
});
test('planning cards use the whole card for assigning and compact secondary actions', () => {
  const c = load(['brotherRow', 'planningRankTone', 'planningRankLabel'], {
    isInactiveForTab: () => false,
    rankColor: () => 'rank-yellow',
    formatDateShort: () => '15 nov 2026',
    daysSince: () => 45,
    escapeAttr: value => String(value),
    escapeHtml: value => String(value),
    buttonIcon: name => `<i>${name}</i>`,
    icon: name => `<i>${name}</i>`
  });
  const html = c.brotherRow({
    key: 'jose-perez',
    hermano: { nombre: 'José Pérez' },
    ultimaVezDiscurso: new Date(2026, 8, 1),
    ultimaVezOracion: null,
    vecesDiscurso90Dias: 1,
    vecesOracion90Dias: 0,
    ultimaEtiquetaDiscurso: 'Fe',
    ultimoTemaDiscurso: ''
  }, 'talks');
  assert.ok(html.includes('class="brother-item-main" data-assign="jose-perez"'));
  assert.ok(html.includes('class="rank-status tone-yellow">Posible</span>'));
  assert.ok(html.includes('planning-action-button'));
  assert.ok(html.includes('Último tema: Fe'));
});
test('current hymn month is first and its next Sunday is the first date', () => {
  const c = load(['orderHymnMonths']);
  const item = day => ({ date: new Date(2026, 9, day) });
  const entries = [
    [0, [{ date: new Date(2026, 0, 4) }]],
    [9, [item(4), item(11), item(18), item(25)]],
    [10, [{ date: new Date(2026, 10, 1) }]]
  ];
  const ordered = c.orderHymnMonths(entries, 2026, new Date(2026, 9, 22));
  assert.deepEqual(Array.from(ordered, ([month]) => month), [9, 10, 0]);
  assert.deepEqual(Array.from(ordered[0][1], value => value.date.getDate()), [25, 18, 11, 4]);
});
test('mobile hymn library keeps search and topic filter in a compact grid', () => {
  const css = fs.readFileSync('docs/styles.css', 'utf8');
  assert.match(css, /\.library-toolbar \{\s*display: grid;\s*grid-template-columns:/);
  assert.match(css, /\.library-toolbar \.search-field \{\s*flex: 0 0 auto;/);
});
