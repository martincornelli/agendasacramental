import { HYMN_TOPICS_DATA } from './hymn-topics-data.mjs';

// The source currently returns these English labels in its Spanish catalog.
const SPANISH_LABELS = { Easter: 'Pascua', "God's love": 'Amor de Dios', 'New Testament': 'Nuevo Testamento', blessings: 'Bendiciones', commandments: 'Mandamientos', 'family of God': 'Familia de Dios', peace: 'Paz', prayer: 'Oración', repentance: 'Arrepentimiento' };
export function hymnTopicKey(number, language) {
  return `${String(language).startsWith('en') ? 'en' : 'es'}-${Number(number)}`;
}
export function officialHymnTopics(number, language) {
  const lang = String(language).startsWith('en') ? 'en' : 'es';
  return (HYMN_TOPICS_DATA.hymns[lang][Number(number)]?.topics || []).map(topic => lang === 'es' ? SPANISH_LABELS[topic] || topic : topic);
}
export function effectiveHymnTopics(number, language, overrides = {}) {
  const override = overrides[hymnTopicKey(number, language)];
  return Array.isArray(override) ? [...override] : officialHymnTopics(number, language);
}
export function parseHymnTopics(text) {
  const topics = [...new Set(String(text).split('\n').map(topic => topic.trim()).filter(Boolean))];
  if (topics.length > 30 || topics.some(topic => topic.length > 80)) throw new Error('TOPIC_LIMIT');
  return topics;
}
