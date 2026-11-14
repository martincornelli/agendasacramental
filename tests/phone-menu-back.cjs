// Physical-device regression for popup Back handling on Android 16.
// Uses only the isolated design QA package and its local Firestore emulator.
const fs = require('node:fs');
const assert = require('node:assert/strict');
const {execFileSync} = require('node:child_process');
const helper = 'tests/phone-confirmation.cjs';
function run(...args) { return execFileSync(process.execPath, [helper, ...args], {encoding:'utf8', timeout:45000}); }
function screen(name) { run('screen', name); return fs.readFileSync('output/confirmation-phone/latest.xml','utf8'); }
function hasText(xml, value) { return xml.includes(`text="${value}"`); }
function menuBack(route, language, trigger, item, screenText) {
  run('open', route, language);
  run('tap', trigger);
  const before = screen(`back-${route}-${language}-open`);
  assert.ok(hasText(before, item), `${route}: menu did not open`);
  run('back');
  const after = screen(`back-${route}-${language}-closed`);
  assert.ok(!hasText(after, item), `${route}: Back did not dismiss the popup`);
  assert.ok(hasText(after, screenText), `${route}: Back left the underlying screen`);
  console.log(`${route}/${language}: Back closes popup and preserves screen`);
}
// Seeding is restricted by the helper to the local demo emulator.
if (process.argv.includes('--with-data')) run('seed');
menuBack('home','es','Ajustes','Tema','Preparar con propósito');
menuBack('home','en','Settings','Theme','Prepare with purpose');
if (process.argv.includes('--with-data')) {
  menuBack('editor','es','Más opciones','Exportar PDF','Editar Agenda');
  menuBack('planning','en','Options for Ana QA','Delete','Planning');
  run('open','editor','es');
  run('replace','Presidente QA','Cambio QA');
  assert.ok(hasText(screen('back-editor-typed'),'Cambio QA'), 'Autocomplete interrupted text input');
  run('tap','Más opciones');
  assert.ok(hasText(screen('back-unsaved-open'),'Exportar PDF'));
  run('back');
  const unsaved = screen('back-unsaved-closed');
  assert.ok(hasText(unsaved,'Cambio QA'), 'Back discarded the unsaved form');
  assert.ok(!hasText(unsaved,'Exportar PDF'));
  run('tap','Aceptar todas');run('tap','Guardar');run('assert','CONFIRMADA','12');
  run('open','editor','en');
  const persisted = screen('back-editor-persisted');
  assert.ok(hasText(persisted,'Cambio QA'));
  assert.ok(hasText(persisted,'Confirmed'));
  console.log('Editor: unsaved changes survive menu Back; 12 acceptances persist after Save/reopen.');
}
