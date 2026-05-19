const translations = {
  es: {
    navFeatures: "Funciones",
    navFlow: "Flujo",
    navDownload: "Descargar",
    eyebrow: "Android · v1.2.3",
    heroLede:
      "Prepará reuniones sacramentales completas, coordiná asignaciones y compartí la agenda con el obispado sin perder el hilo del domingo.",
    primaryCta: "Descargar en Google Play",
    secondaryCta: "Ver proyecto",
    mockUnit: "Unidad 123456",
    mockNext: "PRÓXIMO DOMINGO",
    mockDate: "24 de mayo",
    mockAgenda: "Agenda confirmada",
    mockPreside: "Preside",
    mockHymn: "Himno sacramental",
    mockMessages: "Mensajes",
    mockAgendas: "Agendas",
    mockPlanning: "Planificación",
    statOne: "Agendas",
    statOneText: "borrador, confirmadas y realizadas",
    statTwo: "PDF",
    statTwoText: "listo para imprimir o compartir",
    statThree: "ES / EN",
    statThreeText: "interfaz bilingüe",
    featuresEyebrow: "Pensada para el domingo",
    featuresTitle: "Todo lo que el obispado necesita para preparar la reunión.",
    featureAgendaTitle: "Agendas completas",
    featureAgendaText:
      "Fecha, asistencia, autoridades, anuncios, himnos, oraciones, asuntos y mensajes en un solo lugar.",
    featurePlanTitle: "Planificación justa",
    featurePlanText:
      "Ranking de discursos y oraciones para ver participaciones recientes y evitar repetir asignaciones sin querer.",
    featureShareTitle: "Compartir sin fricción",
    featureShareText:
      "Exportá la agenda en PDF o pedí sugerencias por mensaje con himnos, oraciones y discursantes pendientes.",
    featureRemindersTitle: "Recordatorios",
    featureRemindersText:
      "Avisos semanales configurables para revisar si la reunión está confirmada antes de llegar al domingo.",
    flowEyebrow: "Flujo simple",
    flowTitle: "Crear, revisar, compartir.",
    flowText:
      "La app acompaña el trabajo real: empezar con una agenda en blanco, completar asignaciones con contexto histórico y terminar con una versión lista para dirigir la reunión.",
    flowOne: "Creá los domingos próximos en blanco.",
    flowTwo: "Completá himnos, oraciones, mensajes y asuntos.",
    flowThree: "Confirmá, exportá PDF y compartí con quienes corresponda.",
    downloadEyebrow: "Android",
    downloadTitle: "Agenda Sacramental está lista para usar.",
    downloadText:
      "Disponible para unidades que quieran coordinar la reunión sacramental con una herramienta sencilla y enfocada.",
    privacyCta: "Política de privacidad",
    disclaimer:
      "Esta es una aplicación no oficial. No está afiliada ni respaldada por La Iglesia de Jesucristo de los Santos de los Últimos Días.",
    footerRepo: "Repositorio en GitHub",
  },
  en: {
    navFeatures: "Features",
    navFlow: "Flow",
    navDownload: "Download",
    eyebrow: "Android · v1.2.3",
    heroLede:
      "Prepare complete sacrament meeting agendas, coordinate assignments, and share the plan with the bishopric without losing Sunday's thread.",
    primaryCta: "Download on Google Play",
    secondaryCta: "View project",
    mockUnit: "Unit 123456",
    mockNext: "NEXT SUNDAY",
    mockDate: "May 24",
    mockAgenda: "Agenda confirmed",
    mockPreside: "Presiding",
    mockHymn: "Sacrament hymn",
    mockMessages: "Messages",
    mockAgendas: "Agendas",
    mockPlanning: "Planning",
    statOne: "Agendas",
    statOneText: "draft, confirmed, and completed",
    statTwo: "PDF",
    statTwoText: "ready to print or share",
    statThree: "ES / EN",
    statThreeText: "bilingual interface",
    featuresEyebrow: "Built for Sunday",
    featuresTitle: "Everything a bishopric needs to prepare the meeting.",
    featureAgendaTitle: "Complete agendas",
    featureAgendaText:
      "Date, attendance, authorities, announcements, hymns, prayers, business, and messages in one focused place.",
    featurePlanTitle: "Fair planning",
    featurePlanText:
      "Talk and prayer rankings help you see recent participation and avoid repeating assignments by accident.",
    featureShareTitle: "Easy sharing",
    featureShareText:
      "Export the agenda as PDF or request suggestions by message for pending hymns, prayers, and speakers.",
    featureRemindersTitle: "Reminders",
    featureRemindersText:
      "Configurable weekly reminders help you review whether the meeting is confirmed before Sunday arrives.",
    flowEyebrow: "Simple flow",
    flowTitle: "Create, review, share.",
    flowText:
      "The app follows the real work: start with a blank agenda, complete assignments with historical context, and finish with a version ready to conduct the meeting.",
    flowOne: "Create upcoming blank Sundays.",
    flowTwo: "Complete hymns, prayers, messages, and business.",
    flowThree: "Confirm, export PDF, and share with the right people.",
    downloadEyebrow: "Android",
    downloadTitle: "Sacrament Meeting Agenda is ready to use.",
    downloadText:
      "Available for units that want to coordinate sacrament meeting with a simple, focused tool.",
    privacyCta: "Privacy policy",
    disclaimer:
      "This is an unofficial app. It is not affiliated with or endorsed by The Church of Jesus Christ of Latter-day Saints.",
    footerRepo: "GitHub repository",
  },
};

const setLanguage = (language) => {
  const dictionary = translations[language] ?? translations.es;
  document.documentElement.lang = language;
  document.querySelectorAll("[data-i18n]").forEach((element) => {
    const key = element.dataset.i18n;
    if (dictionary[key]) {
      element.textContent = dictionary[key];
    }
  });
  document.querySelectorAll("[data-lang]").forEach((button) => {
    const isActive = button.dataset.lang === language;
    button.classList.toggle("is-active", isActive);
    button.setAttribute("aria-pressed", String(isActive));
  });
  localStorage.setItem("agenda-sacramental-language", language);
};

document.querySelectorAll("[data-lang]").forEach((button) => {
  button.addEventListener("click", () => setLanguage(button.dataset.lang));
});

const savedLanguage = localStorage.getItem("agenda-sacramental-language");
const browserLanguage = navigator.language?.toLowerCase().startsWith("en") ? "en" : "es";
setLanguage(savedLanguage || browserLanguage);
