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
