// Pure import/export logic. All dates are calendar dates, never UTC instants.
export const HISTORY_FIELDS = { id: 'ID', date: 'Fecha', name: 'Nombre', type: 'Participación', topic: 'Tema', opening: 'Primera oración', closing: 'Última oración', talk: 'Discursante' };
export const normalize = value => String(value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLowerCase().replace(/\s+/g, ' ');
export const exactName = value => String(value ?? '').trim().replace(/\s+/g, ' ');
export function parseCSV(text) {
  text = text.replace(/^\uFEFF/, '');
  const counts = new Map([[',', 0], [';', 0], ['\t', 0]]);
  let inQuotes = false;
  for (let i = 0; i < text.length; i++) {
    if (text[i] === '"') {
      if (inQuotes && text[i + 1] === '"') i++;
      else inQuotes = !inQuotes;
    } else if (!inQuotes) {
      if (/[\r\n]/.test(text[i])) break;
      if (counts.has(text[i])) counts.set(text[i], counts.get(text[i]) + 1);
    }
  }
  const delimiter = [...counts].sort((a, b) => b[1] - a[1])[0][0];
  const rows = []; let row = [], cell = '', quoted = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (c === '"') {
      if (quoted && text[i + 1] === '"') { cell += '"'; i++; }
      else if (!quoted && cell) throw new Error('Comillas inesperadas en el archivo CSV.');
      else quoted = !quoted;
    } else if (!quoted && (c === delimiter || c === '\n' || c === '\r')) {
      row.push(cell); cell = '';
      if (c !== delimiter) { if (row.some(v => v.trim())) rows.push(row); row = []; if (c === '\r' && text[i + 1] === '\n') i++; }
    } else cell += c;
  }
  if (quoted) throw new Error('Hay comillas sin cerrar en el CSV.');
  row.push(cell); if (row.some(v => v.trim())) rows.push(row);
  if (!rows.length) throw new Error('El archivo está vacío.');
  if (rows.length > 5001) throw new Error('Usá archivos de hasta 5000 filas.');
  return rows;
}
export function guessColumns(headers) {
  const aliases = { id: ['id', 'identificador'], date: ['fecha', 'date', 'domingo'], name: ['nombre', 'name', 'hermano', 'participante'], type: ['participacion', 'participation', 'tipo', 'type', 'funcion', 'role'], topic: ['tema', 'topic'], opening: ['primera oracion', 'oracion inicial', 'apertura', 'opening prayer'], closing: ['ultima oracion', 'oracion final', 'cierre', 'closing prayer'], talk: ['discursante', 'discurso', 'orador', 'speaker', 'talk'] };
  return Object.fromEntries(Object.entries(aliases).map(([key, values]) => [key, headers.findIndex(h => values.includes(normalize(h)))]));
}
export function parseDate(value, order = 'DMY') {
  const text = String(value ?? '').trim();
  let y, m, d;
  let match = /^(\d{4})-(\d{1,2})-(\d{1,2})$/.exec(text);
  if (match) [, y, m, d] = match;
  else {
    match = /^(\d{1,2})[/.\-](\d{1,2})[/.\-](\d{4})$/.exec(text);
    if (!match) return '';
    [, d, m, y] = match;
    if (order === 'MDY') [m, d] = [d, m];
  }
  const date = new Date(Number(y), Number(m) - 1, Number(d));
  if (date.getFullYear() !== +y || date.getMonth() !== +m - 1 || date.getDate() !== +d) return '';
  return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
}
export function participationType(value) {
  const n = normalize(value);
  if (['discurso', 'discursante', 'talk', 'orador'].includes(n)) return 'Discurso';
  if (['primera oracion', 'oracion inicial', 'apertura', 'opening prayer'].includes(n)) return 'Primera oración';
  if (['ultima oracion', 'oracion final', 'cierre', 'closing prayer'].includes(n)) return 'Última oración';
  if (['oracion', 'prayer'].includes(n)) return 'Oración';
  return '';
}
export function mapRows(table, mapping, order = 'DMY', wide = false) {
  if (mapping.date < 0 || (!wide && (mapping.name < 0 || mapping.type < 0))) throw new Error('Indicá las columnas de fecha, nombre y participación.');
  if (wide && !['talk', 'opening', 'closing'].some(key => mapping[key] >= 0)) throw new Error('Elegí al menos una columna de participantes.');
  const selected = (wide ? ['date', 'talk', 'opening', 'closing', 'topic'] : ['date', 'name', 'type', 'topic', 'id']).map(k => mapping[k]).filter(v => v >= 0);
  if (new Set(selected).size !== selected.length) throw new Error('Cada campo debe usar una columna diferente.');
  return table.slice(1).flatMap((row, index) => {
    if (row.every(value => !String(value ?? '').trim())) return [];
    // Reverse only the formula-protection prefix emitted by exportCSV.
    const get = key => {
      const value = String(row[mapping[key]] ?? '').trim();
      return table.sourceFormat === 'xlsx' ? value : value.replace(/^'(?=\s*[=+@\-\t\r])/, '');
    };
    const base = { date: parseDate(get('date'), order), topic: get('topic'), row: index + 1 + (table.sourceRow || 1) };
    const records = wide ? [['talk', 'Discurso'], ['opening', 'Primera oración'], ['closing', 'Última oración']].filter(([key]) => get(key)).map(([key, type]) => ({ ...base, id: '', name: get(key), type, topic: type === 'Discurso' ? base.topic : '' })) : [{ ...base, id: get('id'), name: get('name'), type: participationType(get('type')) }];
    if (!records.length) records.push({ ...base, id: '', name: '', type: '' });
    return records.map(record => ({ ...record, name: exactName(record.name), error: row.length !== table[0].length ? 'La cantidad de columnas no coincide con el encabezado.' : !record.date ? 'Fecha inválida.' : !record.name ? 'Falta el nombre.' : !record.type ? 'Participación no reconocida.' : record.name.length > 200 || record.topic.length > 2000 ? 'Nombre o tema demasiado largo.' : '' }));
  });
}
export const recordKey = r => JSON.stringify([exactName(r.name), r.date, r.type]);
export const sameRecord = (a, b) => recordKey(a) === recordKey(b) && (a.topic || '') === (b.topic || '');
export function previewRows(rows, existing) {
  const ids = new Map(existing.map(r => [r.id, r]));
  const accepted = [...existing]; const seenIds = new Set();
  return rows.map(row => {
    const result = { ...row, status: 'new' };
    if (row.error) return { ...result, status: 'invalid' };
    if (row.id && seenIds.has(row.id)) return { ...result, status: 'invalid', error: 'ID repetido dentro del archivo.' };
    if (row.id) seenIds.add(row.id);
    const original = row.id && ids.get(row.id);
    if (row.id && !original) return { ...result, status: 'invalid', error: 'ID desconocido para esta unidad. Dejá el ID vacío solo si es una participación nueva.' };
    if (original && sameRecord(row, original)) return { ...result, status: 'unchanged' };
    const duplicate = accepted.find(r => (!row.id || r.id !== row.id) && recordKey(r) === recordKey(row));
    if (duplicate) return { ...result, status: 'duplicate', original: duplicate };
    if (original) {
      if (original.source !== 'import') return { ...result, status: 'invalid', error: 'Este registro pertenece a una agenda o ficha. Corregilo allí y volvé a exportar.' };
      accepted.push(row);
      return { ...result, status: 'update', original };
    }
    const candidates = [...new Set(accepted.filter(r => normalize(r.name) === normalize(row.name) && exactName(r.name) !== row.name).map(r => r.name))];
    accepted.push(row);
    return { ...result, candidates };
  });
}
export function exportCSV(records, language = 'es') {
  const safe = v => {
    let s = String(v ?? '');
    if (/^[\s]*[=+@\-\t\r]/.test(s)) s = "'" + s;
    return '"' + s.replace(/"/g, '""') + '"';
  };
  const en = language === 'en';
  const types = { 'Discurso': 'Talk', 'Primera oración': 'Opening prayer', 'Última oración': 'Closing prayer', 'Oración': 'Prayer' };
  return '\uFEFF' + [en ? ['ID', 'Date', 'Name', 'Participation', 'Topic'] : ['ID', 'Fecha', 'Nombre', 'Participación', 'Tema'], ...records.map(r => [r.id, r.date, r.name, en ? types[r.type] || r.type : r.type, r.topic])].map(row => row.map(safe).join(';')).join('\r\n');
}
