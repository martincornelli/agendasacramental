import { test } from 'node:test';
import assert from 'node:assert/strict';
import { adminTransition, googleIdentity, reviewMessage, REVIEW_EMAIL } from '../docs/unit-admin.mjs';
import { ADMIN_TEXT } from '../docs/unit-admin-text.mjs';

test('multiple invitations are normalized, deduplicated and validated as a single change', () => {
  const identity = { uid: 'one', email: 'one@example.com' };
  const unit = { administradores: { one: identity.email }, invitacionesAdmin: ['existing@example.com'] };
  const result = adminTransition(unit, identity, 'invite', ' Two@example.com; THREE@example.org\nfour@example.net, two@example.com;existing@example.com');
  assert.deepEqual(result.invitacionesAdmin, ['existing@example.com', 'two@example.com', 'three@example.org', 'four@example.net']);
  assert.throws(() => adminTransition(unit, identity, 'invite', 'valid@example.com;invalid'), /EMAIL/);
  assert.throws(() => adminTransition(unit, identity, 'invite', 'valid@example.com;one@example.com'), /ALREADY/);
  assert.deepEqual(unit.invitacionesAdmin, ['existing@example.com']);
  assert.throws(() => adminTransition(unit, identity, 'invite', Array.from({ length: 30 }, (_, i) => `person${i}@example.com`).join(',')), /LIMIT/);
});

test('invitee can decline only their invitation without changing administrators', () => {
  const unit = { administradores: { one: 'one@example.com' }, invitacionesAdmin: ['two@example.com', 'three@example.com'] };
  const identity = { uid: 'two', email: 'two@example.com' };
  const result = adminTransition(unit, identity, 'decline');
  assert.deepEqual(result.administradores, unit.administradores);
  assert.deepEqual(result.invitacionesAdmin, ['three@example.com']);
  assert.throws(() => adminTransition(result, identity, 'accept'), /FORBIDDEN/);
  assert.throws(() => adminTransition(unit, { uid: 'stranger', email: 'stranger@example.com' }, 'decline'), /FORBIDDEN/);
});

test('legacy migration, invitation and transfer preserve at least one administrator', () => {
  const owner = { uid: 'one', email: 'one@example.com' };
  const other = { uid: 'two', email: 'two@example.com' };
  let unit = { creadoPor: 'One@Example.com', passwordHash: 'unchanged' };
  unit = { ...unit, ...adminTransition(unit, owner, 'claim') };
  assert.throws(() => adminTransition(unit, owner, 'claim'), /FORBIDDEN/);
  assert.throws(() => adminTransition(unit, owner, 'remove', owner.uid), /LAST/);
  unit = { ...unit, ...adminTransition(unit, owner, 'invite', ' TWO@example.com ') };
  assert.throws(() => adminTransition(unit, { uid: 'three', email: 'three@example.com' }, 'accept'), /FORBIDDEN/);
  unit = { ...unit, ...adminTransition(unit, other, 'accept') };
  unit = { ...unit, ...adminTransition(unit, owner, 'remove', owner.uid) };
  assert.deepEqual(unit.administradores, { two: 'two@example.com' });
  assert.deepEqual(unit.invitacionesAdmin, []);
  assert.equal(unit.passwordHash, 'unchanged');
  assert.throws(() => adminTransition(unit, owner, 'invite', 'new@example.com'), /FORBIDDEN/);
});

test('identity requires Google verification and invitation validation supports non-Gmail accounts', () => {
  assert.throws(() => googleIdentity({ uid: 'one', email: 'one@example.com' }), /IDENTITY/);
  assert.deepEqual(googleIdentity({ uid: 'one', email: 'ONE@example.com', emailVerified: true, providerData: [{ providerId: 'google.com' }] }), { uid: 'one', email: 'one@example.com' });
  const identity = { uid: 'one', email: 'one@example.com' };
  const unit = { administradores: { one: identity.email }, invitacionesAdmin: [] };
  assert.throws(() => adminTransition(unit, identity, 'invite', 'invalid'), /EMAIL/);
  assert.deepEqual(adminTransition(unit, identity, 'invite', 'other@organization.org').invitacionesAdmin, ['other@organization.org']);
});

test('review links encode unit and identity safely in both languages without sending mail', () => {
  for (const language of ['es', 'en']) {
    const message = reviewMessage('123', 'name+test@gmail.com', language);
    const gmail = new URL(message.gmail);
    assert.equal(gmail.hostname, 'mail.google.com');
    assert.equal(gmail.searchParams.get('to'), REVIEW_EMAIL);
    assert.equal(gmail.searchParams.get('body'), message.body);
    assert.ok(message.body.includes('name+test@gmail.com'));
    assert.ok(message.subject.includes('123'));
    assert.equal(new URL(message.mailto).searchParams.get('subject'), message.subject);
  }
  assert.deepEqual(Object.keys(ADMIN_TEXT.es), Object.keys(ADMIN_TEXT.en));
  assert.ok(Object.values(ADMIN_TEXT.en).every(Boolean));
});
