export function normalizeDuplicateText(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/\s+/g, " ")
    .trim()
    .toLowerCase();
}

export function analyzeDuplicates(agendas, peopleRecords) {
  const duplicateDates = groupBy(agendas, (agenda) => localDateKey(toDate(agenda.fecha)))
    .filter((group) => group.key && group.items.length > 1);
  const duplicatePeople = groupBy(
    peopleRecords.filter((person) => person?.nombre && person.excluido !== true),
    (person) => normalizeDuplicateText(person.nombre)
  ).filter((group) => group.key && group.items.length > 1);

  const repeatedPeople = [];
  const repeatedHymns = [];
  agendas.forEach((agenda) => {
    const people = meetingPeople(agenda);
    const peopleGroups = groupBy(people, (item) => normalizeDuplicateText(item.name))
      .filter((group) => group.key && group.items.length > 1);
    peopleGroups.forEach((group) => repeatedPeople.push({ agenda, name: group.items[0].name, roles: group.items.map((item) => item.role) }));

    const hymns = meetingHymns(agenda).filter((item) => item.number > 0);
    const hymnGroups = groupBy(hymns, (item) => String(item.number)).filter((group) => group.items.length > 1);
    hymnGroups.forEach((group) => repeatedHymns.push({ agenda, number: group.items[0].number, name: group.items[0].name, positions: group.items.map((item) => item.position) }));
  });

  return {
    duplicateDates,
    duplicatePeople,
    repeatedPeople,
    repeatedHymns,
    total: duplicateDates.length + duplicatePeople.length + repeatedPeople.length + repeatedHymns.length
  };
}

function meetingPeople(agenda) {
  const people = [
    [agenda.preside, "Preside"],
    [agenda.dirige, "Dirige"],
    [agenda.directorMusica, "Dirección de música"],
    [agenda.pianista, "Piano"],
    [agenda.primeraOracion, "Primera oración"],
    [agenda.oracionFinal, "Oración final"]
  ].map(([name, role]) => ({ name: String(name || "").trim(), role })).filter((item) => item.name);
  (agenda.mensajesEvangelio || []).forEach((message) => {
    if (message.tipo === "HIMNO_INTERMEDIO" || !String(message.nombre || "").trim()) return;
    people.push({ name: String(message.nombre).trim(), role: message.tipo === "TESTIMONIO" ? "Testimonio" : "Discurso" });
  });
  (agenda.testimonios || []).forEach((name) => {
    if (String(name || "").trim()) people.push({ name: String(name).trim(), role: "Testimonio" });
  });
  return people;
}

function meetingHymns(agenda) {
  const intermediate = (agenda.mensajesEvangelio || []).find((message) => message.tipo === "HIMNO_INTERMEDIO");
  return [
    { number: Number(agenda.primerHimnoNumero || 0), name: agenda.primerHimnoNombre || "", position: "Apertura" },
    { number: Number(agenda.himnoSacramentalNumero || 0), name: agenda.himnoSacramentalNombre || "", position: "Santa Cena" },
    { number: Number(intermediate?.himnoNumero || 0), name: intermediate?.himnoNombre || "", position: "Intermedio" },
    { number: Number(agenda.himnoFinalNumero || 0), name: agenda.himnoFinalNombre || "", position: "Final" }
  ];
}

function groupBy(items, keyFor) {
  const groups = new Map();
  items.forEach((item) => {
    const key = keyFor(item);
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(item);
  });
  return [...groups.entries()].map(([key, groupedItems]) => ({ key, items: groupedItems }));
}

function localDateKey(value) {
  if (!value || Number.isNaN(value.getTime())) return "";
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`;
}

function toDate(value) {
  if (value instanceof Date) return value;
  if (value?.toDate) return value.toDate();
  return new Date(value);
}
