import test from "node:test";
import assert from "node:assert/strict";
import {
  dateKey,
  duplicateHymnNumbers,
  generateHymnPlans,
  hymnHistory,
  hymnMetadata,
  planFromAgenda,
  sundaysOfYear
} from "../docs/hymn-planner.mjs";

const catalog = {
  1: "Ya rompe el alba",
  2: "El Espíritu de Dios",
  101: "Dios, escúchanos orar",
  102: "Hoy con humildad te pido",
  103: "En memoria de Tu muerte",
  1001: "Come, Thou Fount of Every Blessing"
};

function agenda(date, extra = {}) {
  return {
    id: dateKey(date),
    fecha: date,
    estado: "BORRADOR",
    mensajesEvangelio: [],
    ...extra
  };
}

test("calculates every Sunday without crossing the selected year", () => {
  const sundays = sundaysOfYear(2026);
  assert.equal(sundays.length, 52);
  assert.equal(sundays[0].getDay(), 0);
  assert.equal(sundays.at(-1).getFullYear(), 2026);
  assert.equal(new Set(sundays.map(dateKey)).size, sundays.length);
});

test("maps all agenda hymn positions and finds meeting duplicates", () => {
  const plan = planFromAgenda({
    primerHimnoNumero: 1,
    primerHimnoNombre: catalog[1],
    himnoSacramentalNumero: 101,
    himnoSacramentalNombre: catalog[101],
    himnoFinalNumero: 1,
    himnoFinalNombre: catalog[1],
    mensajesEvangelio: [{ tipo: "HIMNO_INTERMEDIO", himnoNumero: 2, himnoNombre: catalog[2] }]
  });
  assert.equal(plan.intermediate.number, 2);
  assert.deepEqual(duplicateHymnNumbers(plan), [1]);
});

test("recognizes Santa Cena hymn 101 and new-hymnbook numbers", () => {
  assert.equal(hymnMetadata(101, catalog[101], "es").sacramental, true);
  assert.equal(hymnMetadata(1001, catalog[1001], "es").newCollection, true);
});

test("history separates realized use from a future plan", () => {
  const history = hymnHistory(catalog, [
    agenda(new Date(2026, 0, 4), { estado: "REALIZADA", primerHimnoNumero: 1, primerHimnoNombre: catalog[1] }),
    agenda(new Date(2026, 11, 20), { estado: "BORRADOR", primerHimnoNumero: 1, primerHimnoNombre: catalog[1] })
  ], 2026, new Date(2026, 9, 21));
  const hymn = history.find((item) => item.number === 1);
  assert.equal(hymn.usesInYear, 1);
  assert.equal(hymn.totalUses, 1);
  assert.equal(dateKey(hymn.nextPlanned), "2026-12-20");
});

test("automatic generation preserves manual choices, exclusions and meeting uniqueness", () => {
  const date = new Date(2026, 10, 15);
  const existing = agenda(date, {
    primerHimnoNumero: 1,
    primerHimnoNombre: catalog[1],
    primerHimnoOrigen: "PLANIFICADOR_MANUAL"
  });
  const result = generateHymnPlans({
    catalog,
    agendas: [existing],
    dates: [date],
    language: "es",
    options: {
      onlyEmptyDates: false,
      includeIntermediate: true,
      excludedHymns: [2],
      minimumIntervalDays: 0,
      seed: 42
    }
  });
  assert.equal(result.proposals.length, 1);
  const plan = result.proposals[0].proposal;
  assert.equal(plan.opening.number, 1);
  assert.equal(Object.values(plan).some((item) => item.number === 2), false);
  assert.equal(duplicateHymnNumbers(plan).length, 0);
  assert.equal(hymnMetadata(plan.sacrament.number, plan.sacrament.name, "es").sacramental, true);
});
