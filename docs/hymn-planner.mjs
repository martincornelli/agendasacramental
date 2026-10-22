const CATEGORY_DEFINITIONS = {
  praise: ["alabanza", "adoracion", "gloria", "praise", "worship", "glory"],
  jesus_christ: ["jesucristo", "cristo", "jesus", "salvador", "redentor", "christ", "savior", "redeemer"],
  sacrament: ["sacramento", "santa cena", "expiacion", "cruz", "sacrament", "atonement", "cross"],
  faith: ["fe", "testimonio", "confianza", "faith", "testimony", "trust"],
  prayer: ["oracion", "orar", "plegaria", "prayer", "pray"],
  holy_ghost: ["espiritu santo", "espiritu", "holy ghost", "holy spirit"],
  gratitude: ["gratitud", "gracias", "bendiciones", "gratitude", "thanks", "blessings"],
  service: ["servicio", "servir", "obra", "ministrar", "service", "serve", "minister"],
  family: ["familia", "hogar", "padres", "hijos", "family", "home", "parents", "children"],
  temple: ["templo", "convenios", "temple", "covenants"],
  scriptures: ["escrituras", "libro de mormon", "palabra de dios", "scriptures", "book of mormon", "word of god"],
  restoration: ["restauracion", "jose smith", "profeta", "restoration", "joseph smith", "prophet"],
  missionary: ["obra misional", "mision", "predicar", "missionary", "mission", "preach"],
  christmas: ["navidad", "belen", "pesebre", "nacio", "christmas", "bethlehem", "manger", "born"],
  easter: ["pascua", "resurreccion", "resucitado", "easter", "resurrection", "risen"],
  peace: ["paz", "consuelo", "peace", "comfort"],
  love: ["amor", "caridad", "love", "charity"],
  commandments: ["mandamientos", "obediencia", "obedecer", "commandments", "obedience", "obey"],
  zion: ["sion", "santos", "zion", "saints"]
};

const SACRAMENTAL_NEW_HYMNS = new Set([1007, 1008, 1009, 1015, 1016, 1017, 1041, 1043, 1072, 1206, 1210]);
export const HYMN_POSITIONS = ["opening", "sacrament", "intermediate", "closing"];

export function normalizePlannerText(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .trim()
    .toLowerCase();
}

export function dateKey(value) {
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) return "";
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function sundaysOfYear(year) {
  const current = new Date(Number(year), 0, 1, 12, 0, 0, 0);
  while (current.getDay() !== 0) current.setDate(current.getDate() + 1);
  const dates = [];
  while (current.getFullYear() === Number(year)) {
    dates.push(new Date(current));
    current.setDate(current.getDate() + 7);
  }
  return dates;
}

function addCategories(target, values) {
  values.forEach((value) => target.add(value));
}

export function hymnMetadata(numberValue, name, language = "es") {
  const number = Number(numberValue || 0);
  const categories = new Set();
  const english = String(language).toLowerCase().startsWith("en");

  if (english) {
    if (number >= 1 && number <= 61) addCategories(categories, ["restoration", "zion"]);
    else if (number <= 96) categories.add("praise");
    else if (number <= 168) addCategories(categories, ["faith", "prayer"]);
    else if (number <= 196) addCategories(categories, ["sacrament", "jesus_christ"]);
    else if (number <= 200) addCategories(categories, ["easter", "jesus_christ"]);
    else if (number <= 215) addCategories(categories, ["christmas", "jesus_christ"]);
    else if (number <= 252) addCategories(categories, ["service", "gratitude"]);
    else if (number <= 280) addCategories(categories, ["missionary", "scriptures"]);
    else if (number <= 291) categories.add("temple");
    else if (number <= 318) addCategories(categories, ["family", "love"]);
    else if (number <= 337) categories.add("service");
  } else {
    if (number >= 1 && number <= 29) addCategories(categories, ["restoration", "zion"]);
    else if (number <= 47) categories.add("praise");
    else if (number <= 82) addCategories(categories, ["faith", "prayer"]);
    else if (number <= 101) categories.add("praise");
    else if (number <= 120) addCategories(categories, ["sacrament", "jesus_christ"]);
    else if (number <= 122) addCategories(categories, ["easter", "jesus_christ"]);
    else if (number <= 134) addCategories(categories, ["christmas", "jesus_christ"]);
    else if (number <= 160) addCategories(categories, ["service", "gratitude"]);
    else if (number <= 180) addCategories(categories, ["missionary", "scriptures"]);
    else if (number <= 186) categories.add("temple");
    else if (number <= 204) addCategories(categories, ["family", "love"]);
    else if (number <= 209) categories.add("service");
  }

  const normalizedName = normalizePlannerText(name);
  Object.entries(CATEGORY_DEFINITIONS).forEach(([category, aliases]) => {
    if (aliases.some((alias) => normalizedName.includes(normalizePlannerText(alias)))) categories.add(category);
  });

  if (number >= 1000 && !categories.size) addCategories(categories, ["jesus_christ", "faith"]);
  const sacramental = categories.has("sacrament") || (!english && number === 101) || SACRAMENTAL_NEW_HYMNS.has(number);
  if (sacramental) addCategories(categories, ["sacrament", "jesus_christ"]);

  return {
    categories: [...categories],
    newCollection: String(number).length === 4,
    sacramental
  };
}

export function planFromAgenda(agenda) {
  const intermediate = agenda?.mensajesEvangelio?.find((message) => message.tipo === "HIMNO_INTERMEDIO");
  return {
    opening: selection(
      agenda?.primerHimnoNumero,
      agenda?.primerHimnoNombre,
      agenda?.primerHimnoOrigen,
      "AGENDA_MANUAL"
    ),
    sacrament: selection(
      agenda?.himnoSacramentalNumero,
      agenda?.himnoSacramentalNombre,
      agenda?.himnoSacramentalOrigen,
      "AGENDA_MANUAL"
    ),
    intermediate: selection(
      intermediate?.himnoNumero,
      intermediate?.himnoNombre,
      agenda?.himnoIntermedioOrigen,
      "AGENDA_MANUAL"
    ),
    closing: selection(
      agenda?.himnoFinalNumero,
      agenda?.himnoFinalNombre,
      agenda?.himnoFinalOrigen,
      "AGENDA_MANUAL"
    )
  };
}

function selection(number, name, origin, fallbackOrigin = "") {
  const selected = Number(number || 0) > 0 || String(name || "").trim();
  return {
    number: Number(number || 0),
    name: String(name || "").trim(),
    origin: String(origin || "").trim() || (selected ? fallbackOrigin : "")
  };
}

export function duplicateHymnNumbers(plan) {
  const counts = new Map();
  HYMN_POSITIONS.forEach((position) => {
    const number = Number(plan?.[position]?.number || 0);
    if (number > 0) counts.set(number, (counts.get(number) || 0) + 1);
  });
  return [...counts.entries()].filter(([, count]) => count > 1).map(([number]) => number);
}

export function topicsFromAgenda(agenda) {
  const topics = [];
  (agenda?.mensajesEvangelio || []).forEach((message) => {
    if (message.tipo !== "DISCURSO") return;
    const labels = String(message.etiquetaTema || "")
      .split(",")
      .map((value) => value.trim())
      .filter(Boolean);
    if (labels.length) topics.push(...labels);
    else if (String(message.tema || "").trim()) topics.push(String(message.tema).trim());
  });
  return uniqueByNormalized(topics);
}

export function hymnHistory(catalog, agendas, year, now = new Date()) {
  const summaries = new Map(
    Object.entries(catalog).map(([number, name]) => [Number(number), {
      number: Number(number),
      name,
      usesInYear: 0,
      totalUses: 0,
      lastUsed: null,
      nextPlanned: null
    }])
  );
  const today = startOfDay(now);

  agendas.forEach((agenda) => {
    const date = toValidDate(agenda.fecha);
    if (!date) return;
    const realized = agenda.estado === "REALIZADA";
    const futurePlan = !realized && startOfDay(date) >= today;
    HYMN_POSITIONS.map((position) => planFromAgenda(agenda)[position]).forEach((item) => {
      if (!item.number) return;
      const summary = summaries.get(item.number) || {
        number: item.number,
        name: item.name || String(item.number),
        usesInYear: 0,
        totalUses: 0,
        lastUsed: null,
        nextPlanned: null
      };
      if (!summary.name && item.name) summary.name = item.name;
      if (realized) {
        summary.totalUses += 1;
        if (date.getFullYear() === Number(year)) summary.usesInYear += 1;
        if (!summary.lastUsed || date > summary.lastUsed) summary.lastUsed = date;
      } else if (futurePlan && (!summary.nextPlanned || date < summary.nextPlanned)) {
        summary.nextPlanned = date;
      }
      summaries.set(item.number, summary);
    });
  });
  return [...summaries.values()].sort((a, b) => a.number - b.number);
}

export function generateHymnPlans({ catalog, agendas, dates, options = {}, language = "es" }) {
  const settings = {
    minimumIntervalDays: 180,
    onlyEmptyDates: true,
    replaceAutomatic: true,
    prioritizeTopics: true,
    forceNewCollection: false,
    includeIntermediate: false,
    excludedHymns: [],
    seed: 0,
    ...options
  };
  const candidates = Object.entries(catalog).map(([number, name]) => ({
    number: Number(number),
    name,
    metadata: hymnMetadata(number, name, language)
  }));
  const agendaByDate = new Map(agendas.map((agenda) => [dateKey(toValidDate(agenda.fecha)), agenda]));
  const targetDates = [...dates]
    .map(toValidDate)
    .filter(Boolean)
    .filter((date, index, all) => all.findIndex((item) => dateKey(item) === dateKey(date)) === index)
    .sort((a, b) => a - b)
    .filter((date) => !settings.onlyEmptyDates || !planHasSelections(planFromAgenda(agendaByDate.get(dateKey(date)))));
  const targetKeys = new Set(targetDates.map(dateKey));
  const historicalUses = new Map();
  const occupiedDates = new Map();
  const excluded = new Set(settings.excludedHymns.map(Number));
  const random = seededRandom(settings.seed);

  agendas.forEach((agenda) => {
    const date = toValidDate(agenda.fecha);
    if (!date) return;
    const plan = planFromAgenda(agenda);
    if (agenda.estado === "REALIZADA") {
      HYMN_POSITIONS.map((position) => plan[position]).filter((item) => item.number).forEach((item) => addMapDate(historicalUses, item.number, date));
    }
    HYMN_POSITIONS.forEach((position) => {
      const item = plan[position];
      const keep = !targetKeys.has(dateKey(date)) || agenda.estado === "REALIZADA" || isManual(item) || !settings.replaceAutomatic;
      if (keep && item.number) addMapDate(occupiedDates, item.number, date);
    });
  });

  const proposals = [];
  let unchangedDates = 0;

  targetDates.forEach((date) => {
    const agenda = agendaByDate.get(dateKey(date));
    const original = planFromAgenda(agenda);
    let proposal = clonePlan(original);
    const topics = topicsFromAgenda(agenda);
    const warnings = [];
    const positions = ["opening", "sacrament", ...(settings.includeIntermediate ? ["intermediate"] : []), "closing"];

    positions.forEach((position) => {
      const current = proposal[position];
      if (current.number && (isManual(current) || !settings.replaceAutomatic)) return;
      const result = chooseHymn({
        candidates,
        position,
        date,
        topics,
        alreadyChosen: new Set(selectedNumbers(proposal).filter((number) => number !== current.number)),
        occupiedDates,
        historicalUses,
        excluded,
        settings,
        onlyNewCollection: false,
        random
      });
      warnings.push(...result.warnings);
      if (result.selection) proposal[position] = result.selection;
    });

    if (settings.forceNewCollection && !selectedNumbers(proposal).some((number) => String(number).length === 4)) {
      const replaceable = ["opening", "closing", "intermediate", "sacrament"]
        .filter((position) => position !== "intermediate" || settings.includeIntermediate || original.intermediate.number)
        .filter((position) => !isManual(original[position]));
      let included = false;
      for (const position of replaceable) {
        const current = proposal[position];
        const result = chooseHymn({
          candidates,
          position,
          date,
          topics,
          alreadyChosen: new Set(selectedNumbers(proposal).filter((number) => number !== current.number)),
          occupiedDates,
          historicalUses,
          excluded,
          settings,
          onlyNewCollection: true,
          random
        });
        if (result.selection) {
          proposal[position] = result.selection;
          warnings.push(...result.warnings);
          included = true;
          break;
        }
      }
      if (!included) warnings.push({ code: "NO_NEW_COLLECTION" });
    }

    HYMN_POSITIONS.map((position) => proposal[position]).filter((item) => item.number).forEach((item) => addMapDate(occupiedDates, item.number, date));
    const changedPositions = HYMN_POSITIONS.filter((position) => !sameSelection(original[position], proposal[position])).length;
    const preservedManualSelections = HYMN_POSITIONS.filter((position) => isManual(original[position]) && sameSelection(original[position], proposal[position])).length;
    if (changedPositions) {
      proposals.push({
        date,
        original,
        proposal,
        topics,
        preservedManualSelections,
        changedPositions,
        warnings: uniqueWarnings(warnings)
      });
    } else unchangedDates += 1;
  });

  return { proposals, evaluatedDates: targetDates.length, unchangedDates };
}

function chooseHymn({ candidates, position, date, topics, alreadyChosen, occupiedDates, historicalUses, excluded, settings, onlyNewCollection, random }) {
  const available = candidates.filter((candidate) =>
    !excluded.has(candidate.number) &&
    !alreadyChosen.has(candidate.number) &&
    (!onlyNewCollection || candidate.metadata.newCollection) &&
    (position === "sacrament" ? candidate.metadata.sacramental : !candidate.metadata.sacramental)
  );
  if (!available.length) return { selection: null, warnings: [{ code: "NO_CANDIDATE", position }] };

  const interval = Math.max(0, Number(settings.minimumIntervalDays || 0));
  const withinInterval = available.filter((candidate) =>
    (occupiedDates.get(candidate.number) || []).every((other) => differenceInDays(date, other) >= interval)
  );
  const relaxed = !withinInterval.length;
  const pool = relaxed ? available : withinInterval;
  const selected = pool.reduce((best, candidate) => {
    const uses = historicalUses.get(candidate.number) || [];
    const previous = uses.filter((item) => item <= date).sort((a, b) => b - a)[0];
    const historyScore = !uses.length ? 1000 : Math.min(previous ? differenceInDays(date, previous) : 365, 730) - uses.length * 35;
    const plannedDistance = Math.min(...(occupiedDates.get(candidate.number) || []).map((other) => differenceInDays(date, other)), 365);
    const distanceScore = Number.isFinite(plannedDistance) ? plannedDistance * 0.5 : 180;
    const topicScore = settings.prioritizeTopics ? thematicMatches(candidate.metadata, topics) * 350 : 0;
    const score = historyScore + distanceScore + topicScore + random() * 10;
    return !best || score > best.score ? { candidate, score } : best;
  }, null)?.candidate;

  return {
    selection: selected ? { number: selected.number, name: selected.name, origin: "AUTOMATICA" } : null,
    warnings: relaxed ? [{ code: "INTERVAL_RELAXED", position }] : []
  };
}

function thematicMatches(metadata, topics) {
  if (!topics.length) return 0;
  const normalizedTopics = topics.map(normalizePlannerText);
  return metadata.categories.filter((category) => {
    const terms = [...(CATEGORY_DEFINITIONS[category] || []), category.replaceAll("_", " ")].map(normalizePlannerText);
    return terms.some((term) => normalizedTopics.some((topic) => topic.includes(term) || term.includes(topic)));
  }).length;
}

function uniqueByNormalized(values) {
  const seen = new Set();
  return values.filter((value) => {
    const key = normalizePlannerText(value);
    if (!key || seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}

function selectedNumbers(plan) {
  return HYMN_POSITIONS.map((position) => Number(plan[position]?.number || 0)).filter(Boolean);
}

function planHasSelections(plan) {
  return HYMN_POSITIONS.some((position) => plan[position].number || plan[position].name);
}

function isManual(item) {
  return Boolean(item?.number || item?.name) && item?.origin !== "AUTOMATICA";
}

function clonePlan(plan) {
  return Object.fromEntries(HYMN_POSITIONS.map((position) => [position, { ...plan[position] }]));
}

function sameSelection(a, b) {
  return Number(a?.number || 0) === Number(b?.number || 0) && String(a?.name || "").trim() === String(b?.name || "").trim() && String(a?.origin || "") === String(b?.origin || "");
}

function addMapDate(map, key, date) {
  if (!map.has(key)) map.set(key, []);
  map.get(key).push(date);
}

function differenceInDays(a, b) {
  const first = Date.UTC(a.getFullYear(), a.getMonth(), a.getDate());
  const second = Date.UTC(b.getFullYear(), b.getMonth(), b.getDate());
  return Math.abs(Math.round((first - second) / 86400000));
}

function startOfDay(value) {
  const date = new Date(value);
  date.setHours(0, 0, 0, 0);
  return date;
}

function toValidDate(value) {
  if (value instanceof Date) return Number.isNaN(value.getTime()) ? null : new Date(value);
  if (value?.toDate) return toValidDate(value.toDate());
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

function seededRandom(seedValue) {
  let seed = String(seedValue ?? "0").split("").reduce((value, character) => Math.imul(value ^ character.charCodeAt(0), 2654435761), 2166136261) >>> 0;
  return () => {
    seed += 0x6D2B79F5;
    let value = seed;
    value = Math.imul(value ^ (value >>> 15), value | 1);
    value ^= value + Math.imul(value ^ (value >>> 7), value | 61);
    return ((value ^ (value >>> 14)) >>> 0) / 4294967296;
  };
}

function uniqueWarnings(warnings) {
  const seen = new Set();
  return warnings.filter((warning) => {
    const key = `${warning.code}:${warning.position || ""}`;
    if (seen.has(key)) return false;
    seen.add(key);
    return true;
  });
}
