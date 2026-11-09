import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import { googleIdentity, isUnitAdmin } from '../docs/unit-admin.mjs';

const source = fs.readFileSync(new URL('../docs/app.js', import.meta.url), 'utf8');
function functionSource(name) {
  const start = source.indexOf(`function ${name}(`);
  return source.slice(source.slice(start - 6, start) === 'async ' ? start - 6 : start, source.indexOf('\n}', start) + 2);
}
const flush = () => new Promise(resolve => setImmediate(resolve));

test('password and decline success close the dialog; failures keep it available', async () => {
  for (const action of ['password', 'decline']) for (const fail of [false, true]) {
    let closes = 0, reads = 0;
    const notices = [], operations = [];
    const elements = new Map();
    const element = selector => {
      if (!elements.has(selector)) elements.set(selector, { value: 'test-only-password', dataset: {}, events: {}, addEventListener(name, fn) { this.events[name] = fn; } });
      return elements.get(selector);
    };
    const user = { uid: 'one', email: 'one@example.com', emailVerified: true, providerData: [{ providerId: 'google.com' }] };
    const unit = { administradores: action === 'password' ? { one: user.email } : { owner: 'owner@example.com' }, invitacionesAdmin: [user.email] };
    const context = vm.createContext({
      state: { user }, googleIdentity, isUnitAdmin,
      loadUnitAdministration: async () => { reads++; return unit; },
      changeUnitAdministration: async (_, operation) => { operations.push(operation); if (fail) throw Error('denied'); },
      adminText: key => key, escapeHtml: text => text, escapeAttr: text => text, field: () => '',
      openModal: ({ bind }) => bind({ querySelector: element, querySelectorAll: () => [] }),
      closeModal: () => closes++, toastMessage: message => notices.push(message), adminErrorMessage: () => 'error'
    });
    vm.runInContext(functionSource('openUnitAdministrators'), context);
    await context.openUnitAdministrators('123');
    if (action === 'password') element('#admin-reset-password').events.submit({ preventDefault() {} });
    else element('#decline-admin').events.click();
    await flush();
    assert.deepEqual(operations, [action]);
    assert.equal(closes, fail ? 0 : 1);
    assert.equal(reads, 1, 'a successful write must not depend on a second read to close');
    assert.deepEqual(notices, [fail ? 'error' : action === 'password' ? 'admin_password_success' : 'admin_declined']);
  }
});
