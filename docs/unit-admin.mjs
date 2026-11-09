export const REVIEW_EMAIL = 'martin.cornelli.lds@gmail.com';
export const normalizeEmail = value => String(value || '').trim().toLowerCase();

export function googleIdentity(user) {
  if (!user?.uid || !user.emailVerified || !user.providerData?.some(p => p.providerId === 'google.com')) throw Error('ADMIN_IDENTITY');
  return { uid: user.uid, email: normalizeEmail(user.email) };
}

export function isUnitAdmin(unit, identity) {
  return Boolean(unit?.administradores?.[identity.uid]);
}

export function adminTransition(unit, identity, action, value = '') {
  const admins = { ...(unit.administradores || {}) };
  const invitations = [...(unit.invitacionesAdmin || [])];
  if (action === 'claim') {
    if (unit.administradores !== undefined || normalizeEmail(unit.creadoPor) !== identity.email) throw Error('ADMIN_FORBIDDEN');
    return { administradores: { [identity.uid]: identity.email }, invitacionesAdmin: [] };
  }
  if (action === 'decline') {
    if (!invitations.includes(identity.email)) throw Error('ADMIN_FORBIDDEN');
    return { administradores: admins, invitacionesAdmin: invitations.filter(e => e !== identity.email) };
  }
  if (action === 'accept') {
    if (!invitations.includes(identity.email)) throw Error('ADMIN_FORBIDDEN');
    if (Object.keys(admins).length >= 30 && !admins[identity.uid]) throw Error('ADMIN_LIMIT');
    admins[identity.uid] = identity.email;
    return { administradores: admins, invitacionesAdmin: invitations.filter(e => e !== identity.email) };
  }
  if (!isUnitAdmin(unit, identity)) throw Error('ADMIN_FORBIDDEN');
  if (action === 'invite') {
    const emails = [...new Set(String(value).split(/[,;\n\r]+/).map(normalizeEmail).filter(Boolean))];
    if (!emails.length || emails.some(email => !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))) throw Error('ADMIN_EMAIL');
    if (emails.some(email => Object.values(admins).includes(email))) throw Error('ADMIN_ALREADY');
    for (const email of emails) if (!invitations.includes(email)) invitations.push(email);
    if (invitations.length > 30) throw Error('ADMIN_LIMIT');
  } else if (action === 'cancel') {
    return { administradores: admins, invitacionesAdmin: invitations.filter(e => e !== value) };
  } else if (action === 'remove') {
    delete admins[value];
    if (!Object.keys(admins).length) throw Error('ADMIN_LAST');
  } else throw Error('ADMIN_FORBIDDEN');
  return { administradores: admins, invitacionesAdmin: invitations };
}

export function reviewMessage(unit, email, language) {
  const en = language === 'en';
  const subject = en ? `Review request — Unit ${unit}` : `Solicitud de revisión — Unidad ${unit}`;
  const body = en
    ? `Unit: ${unit}\nGoogle account: ${email}\n\nName:\nCalling:\nReason for requesting a review:\n`
    : `Unidad: ${unit}\nCuenta Google: ${email}\n\nNombre:\nLlamamiento:\nMotivo de la solicitud de revisión:\n`;
  const query = `subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}`;
  return { subject, body, mailto: `mailto:${REVIEW_EMAIL}?${query}`, gmail: `https://mail.google.com/mail/?view=cm&fs=1&to=${encodeURIComponent(REVIEW_EMAIL)}&su=${encodeURIComponent(subject)}&body=${encodeURIComponent(body)}` };
}
