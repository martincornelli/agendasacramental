// Shared wire contract with ConfirmacionAgenda.kt. IDs and fingerprints are language independent.
export const ORGANIZATIONS = {
  ELDERS_QUORUM: "Cuórum de Élderes", RELIEF_SOCIETY: "Sociedad de Socorro",
  YOUNG_MEN: "Hombres Jóvenes", YOUNG_WOMEN: "Mujeres Jóvenes", PRIMARY: "Primaria", BISHOPRIC: "Obispado"
};
const clean = value => String(value ?? "").trim();
const signature = values => values.map(v => `${clean(v).length}:${clean(v)}`).join("");
export function assignmentChecks(agenda) {
  const date = agenda.fecha?.toDate?.() || agenda.fecha;
  const day = date instanceof Date ? `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,"0")}-${String(date.getDate()).padStart(2,"0")}` : clean(date);
  const mode = agenda.presentacionPrimaria ? "PRIMARY" : agenda.reunionTestimonios ? "TESTIMONY" : "NORMAL";
  const rows = [];
  const add = (id, label, person, content = [], complete = true, detail = "") => rows.push({
    id, label, person: clean(person), detail,
    complete: Boolean(clean(person)) && complete,
    fingerprint: signature([day, mode, id, person, ...content])
  });
  [ ["preside","Preside"], ["dirige","Dirige"], ["primeraOracion","Primera oración"], ["oracionFinal","Oración final"] ].forEach(([key,label]) => add(key,label,agenda[key]));
  const hymn = (id,label,number,name) => {
    const detail = [Number(number)||"",clean(name)].filter(Boolean).join(" · ");
    [ ["pianista","Pianista"], ["directorMusica","Director/a de música"] ].forEach(([role,roleLabel]) =>
      add(`${id}_${role}`,label,agenda[role],[Number(number)||0,name],Boolean(Number(number)>0 || clean(name)),`${roleLabel}: ${detail}`));
  };
  hymn("primerHimno","Primer himno",agenda.primerHimnoNumero,agenda.primerHimnoNombre);
  hymn("himnoSacramental","Himno sacramental",agenda.himnoSacramentalNumero,agenda.himnoSacramentalNombre);
  hymn("himnoFinal","Himno final",agenda.himnoFinalNumero,agenda.himnoFinalNombre);
  if (agenda.presentacionPrimaria) add("responsablePrimaria","Responsable de Primaria",agenda.responsablePrimaria);
  else if (!agenda.reunionTestimonios) {
    const messages = agenda.mensajesEvangelio || [];
    if (!messages.some(m => m.tipo !== "HIMNO_INTERMEDIO")) add("missingTalk","Discurso","",[],false);
    messages.forEach((m,i) => {
      const id = m.id || `legacy-${i}`;
      if (m.tipo === "HIMNO_INTERMEDIO") hymn(`mensaje_${id}`,"Himno Intermedio",m.himnoNumero,m.himnoNombre);
      else add(`mensaje_${id}`,m.tipo === "TESTIMONIO" ? "Testimonio" : "Discurso",m.nombre,[m.tipo || "DISCURSO",m.tema],true,clean(m.tema));
    });
  }
  return rows;
}
export function changeAgendaStatus(agenda, status, user, now) {
  const next = reconcileConfirmation({...agenda, confirmacionVersion:1});
  if (status === "REALIZADA") return {...next,estado:status};
  if (status === "BORRADOR") return reconcileConfirmation({...next,estado:status});
  const checks = assignmentChecks(next);
  if (!checks.length || checks.some(row => !row.complete)) throw new Error("ASSIGNMENTS_INCOMPLETE");
  return {...next,estado:"CONFIRMADA",aceptaciones:Object.fromEntries(checks.map(row => [row.id,next.aceptaciones[row.id] || {firma:row.fingerprint,registradoPor:user,registradoEn:now}]))};
}
export function reconcileConfirmation(agenda) {
  const messages = (agenda.mensajesEvangelio || []).map((m,i) => ({...m,id:m.id || `legacy-${i}`}));
  const next = {...agenda,mensajesEvangelio:messages};
  if (!next.confirmacionVersion) return next; // Explicit migration only; never invent historical acceptances.
  const checks = assignmentChecks(next);
  next.aceptaciones = Object.fromEntries(checks.filter(r => r.complete && next.aceptaciones?.[r.id]?.firma === r.fingerprint).map(r => [r.id,next.aceptaciones[r.id]]));
  if (next.estado !== "REALIZADA") next.estado = checks.length > 0 && checks.every(r => next.aceptaciones[r.id]) ? "CONFIRMADA" : "BORRADOR";
  return next;
}
