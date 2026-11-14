const labels = {
  ELDERS_QUORUM: ['Cuórum de Élderes', 'Elders Quorum'],
  RELIEF_SOCIETY: ['Sociedad de Socorro', 'Relief Society'],
  YOUNG_MEN: ['Hombres Jóvenes', 'Young Men'],
  YOUNG_WOMEN: ['Mujeres Jóvenes', 'Young Women'],
  PRIMARY: ['Primaria', 'Primary'],
  BISHOPRIC: ['Obispado', 'Bishopric']
};
const normalize = value => String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLowerCase().replace(/[’']/g, '').replace(/\s+/g, ' ');
export const memberName = value => String(value ?? '').trim().replace(/\s+/g, ' ');
export function parseOrganization(value) {
  const text = normalize(value);
  if (!text) return '';
  return Object.keys(labels).find(key => [key, ...labels[key]].some(label => normalize(label) === text)) || null;
}
export const organizationLabel = (value, language = 'es') => labels[value]?.[language === 'en' ? 1 : 0] || '';
// Never guess among namesakes or silently choose among contradictory rows.
export function organizationUpdates(rows, members) {
  const desired = new Map();
  for (const row of rows) {
    if (!row.organization) continue;
    const name = memberName(row.name);
    if (!labels[row.organization]) throw Error('Organización no reconocida.');
    if (desired.has(name) && desired.get(name) !== row.organization) throw Error('Hay organizaciones distintas para la misma persona.');
    desired.set(name, row.organization);
  }
  return [...desired].flatMap(([name, organization]) => {
    const matches = members.filter(m => memberName(m.nombre) === name);
    if (matches.length > 1) throw Error('Hay más de una ficha con ese nombre. Revisá las fichas antes de importar.');
    if (!matches.length) return [{ member: { nombre: name }, organization, create: true }];
    return matches.length === 1 && (matches[0].organizacion || '') !== organization
      ? [{ member: matches[0], organization }] : [];
  });
}
