import { HYMNS_EN, HYMNS_ES } from "./hymns.js";

const firebaseConfig = {
  apiKey: "AIzaSyDHm4h7REdvYQ0PgKv168wZ82MJaLqGrko",
  authDomain: "agendasacramental-333f2.firebaseapp.com",
  projectId: "agendasacramental-333f2",
  storageBucket: "agendasacramental-333f2.firebasestorage.app",
  messagingSenderId: "833115584713"
};

let GoogleAuthProvider;
let Timestamp;
let addDoc;
let auth;
let browserLocalPersistence;
let collection;
let db;
let deleteDoc;
let doc;
let getAuth;
let getDoc;
let getFirestore;
let googleProvider;
let initializeApp;
let limit;
let onAuthStateChanged;
let onSnapshot;
let orderBy;
let query;
let serverTimestamp;
let setDoc;
let setPersistence;
let signInWithPopup;
let signInWithRedirect;
let signOut;
let updateDoc;
let where;

const UNIT_STORAGE_KEY = "agenda_sacramental_web_unit";
const THEME_STORAGE_KEY = "agenda_sacramental_web_theme";
const GITHUB_PAGES_DOMAIN = "martincornelli.github.io";
const AGENDA_STATES = ["BORRADOR", "CONFIRMADA", "REALIZADA"];
const AGENDA_GROUPS = [
  { state: "BORRADOR", label: "Borrador", defaultOpen: true },
  { state: "CONFIRMADA", label: "Confirmadas", defaultOpen: true },
  { state: "REALIZADA", label: "Realizadas", defaultOpen: false }
];
const SVG_ICONS = {
  search: `<path d="m21 21-4.35-4.35"></path><circle cx="11" cy="11" r="7"></circle>`,
  calendarPlus: `<path d="M8 2v4"></path><path d="M16 2v4"></path><path d="M3 10h18"></path><rect x="3" y="4" width="18" height="18" rx="2"></rect><path d="M12 14v4"></path><path d="M10 16h4"></path>`,
  plus: `<path d="M12 5v14"></path><path d="M5 12h14"></path>`,
  chevronRight: `<path d="m9 18 6-6-6-6"></path>`,
  bookOpen: `<path d="M12 7v14"></path><path d="M3 5.5A2.5 2.5 0 0 1 5.5 3H12v18H5.5A2.5 2.5 0 0 0 3 23z"></path><path d="M21 5.5A2.5 2.5 0 0 0 18.5 3H12v18h6.5A2.5 2.5 0 0 1 21 23z"></path>`,
  fileText: `<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><path d="M14 2v6h6"></path><path d="M8 13h8"></path><path d="M8 17h6"></path>`,
  pencil: `<path d="M12 20h9"></path><path d="M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"></path>`,
  x: `<path d="M18 6 6 18"></path><path d="m6 6 12 12"></path>`,
  arrowUp: `<path d="m18 15-6-6-6 6"></path>`,
  arrowDown: `<path d="m6 9 6 6 6-6"></path>`,
  mic: `<path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3z"></path><path d="M19 10v2a7 7 0 0 1-14 0v-2"></path><path d="M12 19v3"></path>`,
  users: `<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M22 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path>`,
  messageSquare: `<path d="M21 15a4 4 0 0 1-4 4H8l-5 3V7a4 4 0 0 1 4-4h10a4 4 0 0 1 4 4z"></path>`
};
const BUSINESS_TYPES = ["RELEVO", "SOSTENIMIENTO", "SOSTENIMIENTO_OFICIALES", "ESTACA", "ORDENACION_AARONICA", "OTROS"];
const MESSAGE_TYPES = ["DISCURSO", "TESTIMONIO", "HIMNO_INTERMEDIO"];
const AARONIC_OFFICES = ["Diacono", "Maestro", "Presbitero"];
const CALLING_PLURALS = {
  presidente: "presidentes",
  presidenta: "presidentas",
  "presidente/a": "presidentes/as",
  consejero: "consejeros",
  consejera: "consejeras",
  "consejero/a": "consejeros/as",
  maestro: "maestros",
  maestra: "maestras",
  "maestro/a": "maestros/as",
  lider: "líderes",
  asesor: "asesores",
  asesora: "asesoras",
  "asesor/a": "asesores/as",
  ayudante: "ayudantes",
  secretario: "secretarios",
  secretaria: "secretarias",
  "secretario/a": "secretarios/as",
  especialista: "especialistas",
  president: "presidents",
  counselor: "counselors",
  counsellor: "counsellors",
  teacher: "teachers",
  leader: "leaders",
  advisor: "advisors",
  adviser: "advisers",
  assistant: "assistants",
  secretary: "secretaries",
  specialist: "specialists"
};
const BASE_TOPIC_TAGS = [
  "Jesucristo",
  "Expiacion",
  "Fe",
  "Arrepentimiento",
  "Perdon",
  "Santa Cena",
  "Convenios",
  "Templo",
  "Espiritu Santo",
  "Oracion",
  "Escrituras",
  "Servicio",
  "Familia",
  "Obediencia"
];
const defaultConfig = {
  diasVerdeDiscurso: 90,
  diasAmarilloDiscurso: 30,
  diasVerdeOracion: 30,
  diasAmarilloOracion: 14,
  diasVerdeTema: 180,
  diasAmarilloTema: 90,
  etiquetasTema: []
};

const state = {
  user: null,
  unitNumber: localStorage.getItem(UNIT_STORAGE_KEY) || "",
  route: routeFromHash(),
  activeAgendaId: null,
  planningTab: "talks",
  agendas: [],
  hermanos: [],
  config: { ...defaultConfig },
  configId: "",
  isBooting: true,
  isReady: false,
  fatalError: null,
  unsubscribers: [],
  theme: localStorage.getItem(THEME_STORAGE_KEY) || "system",
  draftReadingAgenda: null,
  draftPdfAgenda: null,
  pdfReturnRoute: "agendas",
  pdfReturnAgendaId: null
};

const appShell = document.querySelector("#app");
const screen = document.querySelector("#screen");
const modal = document.querySelector("#modal");
const toast = document.querySelector("#toast");
const sessionChip = document.querySelector("#session-chip");
const sidebarFooterUnit = document.querySelector("#sidebar-footer-unit");
const topbarEyebrow = document.querySelector("#topbar-eyebrow");
const topbarTitle = document.querySelector("#topbar-title");

bindChrome();
applyTheme();
boot();

setTimeout(() => {
  if (state.isBooting) {
    state.isBooting = false;
    state.fatalError = new Error("Firebase no respondió a tiempo. Revisa la conexión o intenta recargar la página.");
    render();
  }
}, 9000);

async function boot() {
  try {
    await loadFirebase();
    const firebaseApp = initializeApp(firebaseConfig);
    auth = getAuth(firebaseApp);
    db = getFirestore(firebaseApp);
    googleProvider = new GoogleAuthProvider();
    await setPersistence(auth, browserLocalPersistence);
    onAuthStateChanged(auth, (user) => {
      state.user = user;
      state.isBooting = false;
      state.fatalError = null;
      if (user && state.unitNumber) subscribeUnitData(state.unitNumber);
      else {
        cleanupSubscriptions();
        state.isReady = false;
      }
      render();
    });
  } catch (error) {
    state.fatalError = error;
    state.isBooting = false;
    render();
  }
}

async function loadFirebase() {
  const [appModule, authModule, firestoreModule] = await Promise.all([
    import("https://www.gstatic.com/firebasejs/10.12.5/firebase-app.js"),
    import("https://www.gstatic.com/firebasejs/10.12.5/firebase-auth.js"),
    import("https://www.gstatic.com/firebasejs/10.12.5/firebase-firestore.js")
  ]);
  initializeApp = appModule.initializeApp;
  GoogleAuthProvider = authModule.GoogleAuthProvider;
  browserLocalPersistence = authModule.browserLocalPersistence;
  getAuth = authModule.getAuth;
  onAuthStateChanged = authModule.onAuthStateChanged;
  setPersistence = authModule.setPersistence;
  signInWithPopup = authModule.signInWithPopup;
  signInWithRedirect = authModule.signInWithRedirect;
  signOut = authModule.signOut;
  Timestamp = firestoreModule.Timestamp;
  addDoc = firestoreModule.addDoc;
  collection = firestoreModule.collection;
  deleteDoc = firestoreModule.deleteDoc;
  doc = firestoreModule.doc;
  getDoc = firestoreModule.getDoc;
  getFirestore = firestoreModule.getFirestore;
  limit = firestoreModule.limit;
  onSnapshot = firestoreModule.onSnapshot;
  orderBy = firestoreModule.orderBy;
  query = firestoreModule.query;
  serverTimestamp = firestoreModule.serverTimestamp;
  setDoc = firestoreModule.setDoc;
  updateDoc = firestoreModule.updateDoc;
  where = firestoreModule.where;
}

function bindChrome() {
  document.querySelector("#brand-home").addEventListener("click", () => navigate("agendas"));
  document.querySelectorAll(".nav-item").forEach((button) => {
    button.addEventListener("click", () => {
      closeMobileMenu();
      navigate(button.dataset.route);
    });
  });
  document.querySelector("#mobile-menu-toggle")?.addEventListener("click", () => {
    const isOpen = appShell.classList.toggle("mobile-menu-open");
    document.querySelector("#mobile-menu-toggle")?.setAttribute("aria-expanded", String(isOpen));
  });
  document.querySelector("#change-unit").addEventListener("click", () => {
    closeMobileMenu();
    openChangeUnitDialog();
  });
  document.querySelector("#sign-out").addEventListener("click", async () => {
    closeMobileMenu();
    await signOut(auth);
    state.unitNumber = "";
    localStorage.removeItem(UNIT_STORAGE_KEY);
    cleanupSubscriptions();
    render();
  });
  window.addEventListener("hashchange", () => {
    closeMobileMenu();
    state.route = routeFromHash();
    state.activeAgendaId = null;
    render();
  });
}

function closeMobileMenu() {
  appShell.classList.remove("mobile-menu-open");
  document.querySelector("#mobile-menu-toggle")?.setAttribute("aria-expanded", "false");
}

function navigate(route) {
  closeMobileMenu();
  state.route = route;
  state.activeAgendaId = null;
  if (location.hash !== `#${route}`) location.hash = route;
  render();
}

function routeFromHash() {
  const route = (location.hash || "#agendas").replace("#", "");
  return ["agendas", "planning", "settings"].includes(route) ? route : "agendas";
}

function cleanupSubscriptions() {
  state.unsubscribers.forEach((unsubscribe) => unsubscribe?.());
  state.unsubscribers = [];
  state.agendas = [];
  state.hermanos = [];
  state.config = { ...defaultConfig };
  state.configId = "";
}

function subscribeUnitData(unitNumber) {
  cleanupSubscriptions();
  state.isReady = false;
  const agendasQuery = query(
    collection(db, "agendas"),
    where("numeroUnidad", "==", unitNumber),
    orderBy("fecha", "desc")
  );
  state.unsubscribers.push(onSnapshot(agendasQuery, (snapshot) => {
    state.agendas = snapshot.docs.map((item) => normalizeAgenda(item.id, item.data()));
    state.isReady = true;
    render();
  }, handleFatal));

  const hermanosQuery = query(collection(db, "hermanos"), where("numeroUnidad", "==", unitNumber), orderBy("nombre"));
  state.unsubscribers.push(onSnapshot(hermanosQuery, (snapshot) => {
    state.hermanos = snapshot.docs.map((item) => ({ id: item.id, ...item.data() }));
    render();
  }, () => {
    state.hermanos = [];
    render();
  }));

  const configQuery = query(collection(db, "configuracion"), where("numeroUnidad", "==", unitNumber), limit(1));
  state.unsubscribers.push(onSnapshot(configQuery, (snapshot) => {
    const docSnap = snapshot.docs[0];
    state.configId = docSnap?.id || "";
    state.config = docSnap ? { ...defaultConfig, ...docSnap.data() } : { ...defaultConfig };
    render();
  }, () => {
    state.config = { ...defaultConfig };
    render();
  }));
}

function handleFatal(error) {
  state.fatalError = error;
  render();
}

function render() {
  renderChrome();
  if (state.isBooting) {
    screen.innerHTML = loadingPanel("Conectando...");
    return;
  }
  if (state.fatalError) {
    renderFatalError(state.fatalError);
    return;
  }
  if (!state.user) {
    renderLogin();
    return;
  }
  if (!state.unitNumber) {
    renderUnitAccess();
    return;
  }
  if (!state.isReady && state.route !== "settings") {
    screen.innerHTML = loadingPanel("Cargando agendas...");
    return;
  }

  appShell.classList.remove("setup-mode");
  const routes = {
    agendas: renderAgendas,
    planning: renderPlanning,
    settings: renderSettings,
    edit: renderAgendaEditor,
    reading: renderReadingMode,
    pdf: renderPdfMode
  };
  (routes[state.route] || renderAgendas)();
}

function renderChrome() {
  const setupMode = state.isBooting || !state.user || !state.unitNumber || Boolean(state.fatalError);
  appShell.classList.toggle("setup-mode", setupMode);
  appShell.classList.toggle("pdf-mode", state.route === "pdf");
  document.querySelectorAll(".nav-item").forEach((button) => {
    button.classList.toggle("active", button.dataset.route === state.route);
    button.disabled = !state.user || !state.unitNumber;
  });
  sessionChip.classList.toggle("skeleton", state.isBooting);
  sessionChip.textContent = state.unitNumber
    ? `Unidad ${state.unitNumber}`
    : state.user
      ? "Sin unidad activa"
      : state.isBooting
        ? "Conectando..."
        : "Sin sesión";
  if (sidebarFooterUnit) {
    sidebarFooterUnit.textContent = `\u00a9 ${new Date().getFullYear()} \u00b7 ${state.unitNumber ? `Unidad ${state.unitNumber}` : "Sin unidad"}`;
  }
  topbarEyebrow.textContent = state.unitNumber ? `Unidad ${state.unitNumber}` : "Agenda Sacramental";
  topbarTitle.textContent = routeTitle();
}

function routeTitle() {
  if (state.route === "planning") return "Planificación";
  if (state.route === "settings") return "Ajustes";
  if (state.route === "edit") return state.activeAgendaId ? "Editar agenda" : "Nueva agenda";
  if (state.route === "reading") return "Modo lectura";
  if (state.route === "pdf") return "Exportar PDF";
  return "Agendas";
}

function renderLogin() {
  appShell.classList.add("setup-mode");
  screen.innerHTML = `
    <div class="setup-wrap auth-wrap">
      <section class="auth-intro">
        ${authMark()}
        <p class="eyebrow">Agenda Sacramental Web</p>
        <h2>Entrar con Google</h2>
        <p class="muted">Usa la misma cuenta y la misma unidad que en Android para trabajar sobre los datos existentes.</p>
      </section>
      <section class="setup-panel auth-card">
        <button id="google-login" class="google-button" type="button">
          <span class="google-mark" aria-hidden="true">G</span>
          Iniciar sesión con Google
        </button>
      </section>
    </div>
  `;
  screen.querySelector("#google-login").addEventListener("click", async () => {
    try {
      await signInWithPopup(auth, googleProvider);
    } catch (error) {
      if (error?.code === "auth/popup-blocked" || error?.code === "auth/operation-not-supported-in-this-environment") {
        await signInWithRedirect(auth, googleProvider);
      } else {
        toastMessage(formatErrorMessage(error) || "No se pudo iniciar sesión.", 7000);
      }
    }
  });
}

function renderUnitAccess() {
  appShell.classList.add("setup-mode");
  screen.innerHTML = `
    <div class="setup-wrap auth-wrap">
      <section class="auth-intro">
        ${authMark()}
        <p class="eyebrow">${escapeHtml(userEmail())}</p>
        <h2>Acceso a unidad</h2>
        <p class="muted">Ingresa el número de unidad y su contraseña. Si la unidad no existe, podrás crearla con esa contraseña.</p>
      </section>
      <section class="setup-panel auth-card">
        <form id="unit-form" class="form-grid">
          <div class="field">
            <label for="unit-number">Número de unidad</label>
            <input id="unit-number" class="input" inputmode="numeric" autocomplete="off" required>
          </div>
          <div class="field">
            <label for="unit-password">Contraseña</label>
            <input id="unit-password" class="input" type="password" autocomplete="current-password" required>
          </div>
          <div class="auth-actions">
            <button class="primary-button" type="submit">Ingresar</button>
            <button id="logout-setup" class="secondary-button" type="button">Salir</button>
          </div>
        </form>
      </section>
    </div>
  `;
  screen.querySelector("#logout-setup").addEventListener("click", () => signOut(auth));
  screen.querySelector("#unit-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    await withToastError(async () => {
      const unitNumber = screen.querySelector("#unit-number").value.trim();
      const password = screen.querySelector("#unit-password").value;
      const canAccess = await ensureUnitAccess(unitNumber, password);
      if (!canAccess) return;
      activateUnit(unitNumber);
    });
  });
}

function authMark() {
  return `
    <div class="auth-mark" aria-hidden="true">
      <svg viewBox="0 0 24 24" focusable="false">
        <path d="M4.75 5.5c0-.69.56-1.25 1.25-1.25h4.2c1.02 0 1.8.33 2.3.86.5-.53 1.28-.86 2.3-.86H19c.69 0 1.25.56 1.25 1.25v12.75c0 .41-.34.75-.75.75h-4.7c-.86 0-1.4.2-1.78.54-.29.25-.75.25-1.04 0-.38-.34-.92-.54-1.78-.54H5.5a.75.75 0 0 1-.75-.75V5.5Z"/>
        <path d="M12.5 6.1v12.5"/>
      </svg>
    </div>
  `;
}

async function ensureUnitAccess(unitNumber, password) {
  if (!unitNumber || !password) throw new Error("Ingresa unidad y contraseña.");
  const unitDoc = await getDoc(unitRef(unitNumber));
  const passwordHash = await hashPassword(password);
  if (!unitDoc.exists()) {
    if (!confirm(`La unidad ${unitNumber} no existe. ¿Deseas crearla?`)) return false;
    await setDoc(unitRef(unitNumber), {
      numeroUnidad: unitNumber,
      passwordHash,
      creadoPor: userEmail(),
      creadoEn: serverTimestamp()
    });
  } else if (unitDoc.data().passwordHash !== passwordHash) {
    throw new Error("Contraseña incorrecta.");
  }
  return true;
}

function openChangeUnitDialog() {
  openModal({
    title: "Cambiar unidad",
    body: `
      <form id="change-unit-form" class="form-grid">
        <p class="muted">Unidad actual: <strong>${escapeHtml(state.unitNumber)}</strong></p>
        <div class="inline-fields">
          ${field("change-unit-number", "Número de unidad", `<input id="change-unit-number" class="input" inputmode="numeric" autocomplete="off" value="${escapeAttr(state.unitNumber)}" required>`)}
          ${field("change-unit-password", "Contraseña", `<input id="change-unit-password" class="input" type="password" autocomplete="current-password" required>`)}
        </div>
      </form>
    `,
    footer: `
      <button class="secondary-button" data-action="cancel" type="button">Cancelar</button>
      <button class="primary-button" form="change-unit-form" type="submit">Entrar</button>
    `,
    bind: (dialog) => {
      dialog.querySelector("#change-unit-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        await withToastError(async () => {
          const unitNumber = dialog.querySelector("#change-unit-number").value.trim();
          const password = dialog.querySelector("#change-unit-password").value;
          const canAccess = await ensureUnitAccess(unitNumber, password);
          if (!canAccess) return;
          closeModal();
          activateUnit(unitNumber);
        });
      });
    }
  });
}

function activateUnit(unitNumber) {
  state.unitNumber = unitNumber;
  localStorage.setItem(UNIT_STORAGE_KEY, unitNumber);
  subscribeUnitData(unitNumber);
  navigate("agendas");
}

function renderAgendas() {
  const searchValue = "";
  screen.innerHTML = `
    <div class="toolbar">
      <div class="toolbar-left">
        <div class="search-field">
          <span class="search-icon" aria-hidden="true">${icon("search")}</span>
          <input id="agenda-search" class="input" type="search" placeholder="Buscar por fecha, nombre, himno..." value="${escapeAttr(searchValue)}">
        </div>
        ${stateFilter("all", "Todas", true)}
        ${stateFilter("BORRADOR", "Borrador")}
        ${stateFilter("CONFIRMADA", "Confirmada")}
        ${stateFilter("REALIZADA", "Realizada")}
      </div>
      <div class="toolbar-right">
        <button id="create-sundays" class="secondary-button" type="button">${buttonIcon("calendarPlus")}Crear domingos</button>
        <button id="new-agenda" class="primary-button" type="button">${buttonIcon("plus")}Nueva agenda</button>
      </div>
    </div>

    <div id="agenda-content" class="screen-grid">${renderAgendaDashboard(searchValue, "all")}</div>
  `;
  screen.querySelector("#new-agenda").addEventListener("click", () => openEditor(null));
  screen.querySelector("#create-sundays").addEventListener("click", openCreateSundaysDialog);
  const searchInput = screen.querySelector("#agenda-search");
  const filters = [...screen.querySelectorAll("[data-filter-state]")];
  const rerender = () => {
    const active = filters.find((button) => button.classList.contains("active"))?.dataset.filterState || "all";
    screen.querySelector("#agenda-content").innerHTML = renderAgendaDashboard(searchInput.value, active);
    bindAgendaListActions();
  };
  searchInput.addEventListener("input", rerender);
  filters.forEach((button) => {
    button.addEventListener("click", () => {
      filters.forEach((item) => item.classList.remove("active"));
      button.classList.add("active");
      rerender();
    });
  });
  bindAgendaListActions();
}

function renderAgendaDashboard(searchValue, filterState) {
  const stats = agendaStats();
  const nextAgenda = upcomingAgenda();
  const showNextAgenda = nextAgenda &&
    agendaMatchesState(nextAgenda, filterState) &&
    agendaMatchesSearch(nextAgenda, searchValue);
  return `
    <section class="summary-section">
      ${sectionTitle("Resumen", "", "")}
      <div class="metric-grid">
        ${metricPill("Total", stats.total)}
        ${metricPill("Borrador", stats.draft)}
        ${metricPill("Confirmadas", stats.confirmed)}
      </div>
    </section>

    ${showNextAgenda ? `
      <section class="panel next-agenda-panel">
        ${sectionTitle("Próximo domingo", "", `<button class="text-button" data-open="${escapeAttr(nextAgenda.id)}" type="button">Abrir ${buttonIcon("chevronRight")}</button>`)}
        ${agendaCard(nextAgenda)}
      </section>
    ` : ""}

    <section class="panel">
      ${sectionTitle("Agendas", "", "")}
      <div id="agenda-list" class="agenda-list grouped">${renderAgendaList(searchValue, filterState, showNextAgenda ? nextAgenda.id : "")}</div>
    </section>
  `;
}

function stateFilter(value, label, active = false) {
  return `<button class="filter-chip ${active ? "active" : ""}" data-filter-state="${escapeAttr(value)}" type="button">${escapeHtml(label)}</button>`;
}

function renderAgendaList(searchValue, filterState, excludedAgendaId = "") {
  const items = state.agendas.filter((agenda) =>
    agenda.id !== excludedAgendaId &&
    agendaMatchesState(agenda, filterState) &&
    agendaMatchesSearch(agenda, searchValue)
  );
  if (!items.length) return emptyState("No hay agendas para mostrar.");

  return AGENDA_GROUPS
    .filter((group) => filterState === "all" || group.state === filterState)
    .map((group) => {
      const groupItems = items
        .filter((agenda) => agenda.estado === group.state)
        .sort((a, b) => compareAgendasForGroup(a, b, group.state));
      if (!groupItems.length) return "";
      const shouldOpen = group.defaultOpen || Boolean(normalizeText(searchValue)) || filterState !== "all";
      return `
        <details class="agenda-group" ${shouldOpen ? "open" : ""}>
          <summary>
            <span>${escapeHtml(group.label)}</span>
            <strong>${groupItems.length}</strong>
          </summary>
          <div class="agenda-list">${groupItems.map(agendaCard).join("")}</div>
        </details>
      `;
    })
    .join("");
}

function agendaMatchesState(agenda, filterState) {
  return filterState === "all" || agenda.estado === filterState;
}

function agendaMatchesSearch(agenda, searchValue) {
  const normalizedSearch = normalizeText(searchValue);
  if (!normalizedSearch) return true;
  const haystack = normalizeText([
    formatDateLong(agenda.fecha),
    labelState(agenda.estado),
    agenda.preside,
    agenda.dirige,
    agenda.primeraOracion,
    agenda.oracionFinal,
    agenda.primerHimnoNombre,
    agenda.himnoSacramentalNombre,
    agenda.himnoFinalNombre,
    agenda.primerHimnoNumero,
    agenda.himnoSacramentalNumero,
    agenda.himnoFinalNumero,
    agenda.asuntosEstacaBarrio.map((item) => `${labelBusiness(item.tipo)} ${item.columna2} ${item.columna3}`).join(" "),
    agenda.mensajesEvangelio.map((item) => `${item.nombre} ${item.tema} ${item.etiquetaTema} ${item.himnoNombre} ${item.himnoNumero}`).join(" ")
  ].join(" "));
  return haystack.includes(normalizedSearch);
}

function compareAgendasForGroup(a, b, groupState) {
  return groupState === "REALIZADA" ? b.fecha - a.fecha : a.fecha - b.fecha;
}

function agendaCard(agenda) {
  return `
    <article class="agenda-card">
      <div class="agenda-card-header">
        <div>
          <h3>${escapeHtml(formatDateLong(agenda.fecha))}</h3>
          <p class="item-meta">${agenda.reunionTestimonios ? "Reunión de ayuno y testimonios" : "Reunión sacramental"}</p>
        </div>
        <span class="status-pill status-${escapeAttr(agenda.estado)}">${escapeHtml(labelState(agenda.estado))}</span>
      </div>
      <div class="agenda-summary">
        <div>${summaryLabel("users", "Preside")}<strong>${escapeHtml(agenda.preside || "Sin datos")}</strong></div>
        <div>${summaryLabel("mic", "Dirige")}<strong>${escapeHtml(agenda.dirige || "Sin datos")}</strong></div>
        <div>${summaryLabel("messageSquare", "Mensajes")}<strong>${agenda.mensajesEvangelio.length || agenda.testimonios.length || 0}</strong></div>
      </div>
      <div class="agenda-card-footer">
        <span class="item-meta attendance-meta">👥 Asistencia: ${Number(agenda.asistencia || 0)}</span>
        <div class="item-actions">
          <button class="secondary-button" data-read="${escapeAttr(agenda.id)}" type="button">${buttonIcon("bookOpen")}Lectura</button>
          <button class="secondary-button" data-print-agenda="${escapeAttr(agenda.id)}" type="button">${buttonIcon("fileText")}PDF</button>
          <button class="primary-button" data-open="${escapeAttr(agenda.id)}" type="button">${buttonIcon("pencil")}Editar</button>
          <button class="icon-button" data-delete-agenda="${escapeAttr(agenda.id)}" type="button" title="Eliminar" aria-label="Eliminar agenda">${icon("x")}</button>
        </div>
      </div>
    </article>
  `;
}

function icon(name, className = "svg-icon") {
  return `<svg class="${className}" viewBox="0 0 24 24" aria-hidden="true">${SVG_ICONS[name] || ""}</svg>`;
}

function buttonIcon(name) {
  return `<span class="button-icon" aria-hidden="true">${icon(name)}</span>`;
}

function summaryLabel(iconName, label) {
  return `<span>${icon(iconName, "summary-icon")} ${escapeHtml(label)}</span>`;
}

function bindAgendaListActions() {
  screen.querySelectorAll("[data-open]").forEach((button) => {
    button.addEventListener("click", () => openEditor(button.dataset.open));
  });
  screen.querySelectorAll("[data-read]").forEach((button) => {
    button.addEventListener("click", () => openReading(button.dataset.read));
  });
  screen.querySelectorAll("[data-print-agenda]").forEach((button) => {
    button.addEventListener("click", () => {
      const agenda = state.agendas.find((item) => item.id === button.dataset.printAgenda);
      if (agenda) exportAgendaPdf(agenda);
    });
  });
  screen.querySelectorAll("[data-delete-agenda]").forEach((button) => {
    button.addEventListener("click", async () => {
      if (!confirm("¿Eliminar esta agenda?")) return;
      await withToastError(async () => {
        const agenda = state.agendas.find((item) => item.id === button.dataset.deleteAgenda);
        await deleteDoc(agendaRef(button.dataset.deleteAgenda));
        await syncRemovedAgendaParticipants(
          agenda,
          null,
          state.agendas.filter((item) => item.id !== button.dataset.deleteAgenda)
        );
        toastMessage("Agenda eliminada");
      });
    });
  });
}

function openEditor(agendaId) {
  state.route = "edit";
  state.activeAgendaId = agendaId;
  render();
}

function openReading(agendaId) {
  state.route = "reading";
  state.activeAgendaId = agendaId;
  render();
}

function renderAgendaEditor() {
  const agenda = state.activeAgendaId
    ? state.agendas.find((item) => item.id === state.activeAgendaId)
    : createBlankAgenda(nextSunday());
  if (!agenda) {
      screen.innerHTML = emptyPanel("No se encontró la agenda.");
    return;
  }
  screen.innerHTML = `
    <form id="agenda-form" class="screen-grid">
      <section class="panel">
        ${sectionTitle(state.activeAgendaId ? "Editar agenda" : "Nueva agenda", "E", `
          <div class="button-row">
            <button class="secondary-button" data-action="back" type="button">Volver</button>
            <button class="secondary-button" data-action="reading-preview" type="button">Modo lectura</button>
            <button class="secondary-button" data-action="export-pdf" type="button">Exportar PDF</button>
            <button class="primary-button" type="submit">Guardar</button>
          </div>
        `)}
        <div class="form-grid">
          <div class="inline-fields">
            ${field("fecha", "Fecha", `<input id="fecha" class="input" type="date" value="${escapeAttr(dateInputValue(agenda.fecha))}" required>`)}
            ${field("estado", "Estado", `<select id="estado" class="select">${AGENDA_STATES.map((item) => option(item, labelState(item), agenda.estado)).join("")}</select>`)}
            ${field("asistencia", "Asist.", `<input id="asistencia" class="input" type="number" min="0" value="${Number(agenda.asistencia || 0)}">`)}
          </div>
          <div class="inline-fields">
            ${field("preside", "Preside", textInput("preside", agenda.preside, "names-list"))}
            ${field("dirige", "Dirige", textInput("dirige", agenda.dirige, "names-list"))}
          </div>
          <div class="inline-fields">
            ${field("reconocimientos", "Reconocimientos", textarea("reconocimientos", agenda.reconocimientos), "Si vas a cargar varios, separalos con una coma.")}
            ${field("anuncios", "Anuncios", textarea("anuncios", agenda.anuncios), "Si vas a cargar varios, separalos con una coma.")}
          </div>
        </div>
      </section>

      <section class="panel">
        ${sectionTitle("Himnos y música", "H", "")}
        <div class="form-grid">
          ${hymnFields("primerHimno", "Primer himno", agenda.primerHimnoNumero, agenda.primerHimnoNombre)}
          ${hymnFields("himnoSacramental", "Himno sacramental", agenda.himnoSacramentalNumero, agenda.himnoSacramentalNombre)}
          ${hymnFields("himnoFinal", "Himno final", agenda.himnoFinalNumero, agenda.himnoFinalNombre)}
          <div class="inline-fields">
            ${field("directorMusica", "Director/a de música", textInput("directorMusica", agenda.directorMusica, "names-list"))}
            ${field("pianista", "Pianista", textInput("pianista", agenda.pianista, "names-list"))}
          </div>
          <div class="inline-fields">
            ${field("primeraOracion", "Primera oración", textInput("primeraOracion", agenda.primeraOracion, "names-list"))}
            ${field("oracionFinal", "Oración final", textInput("oracionFinal", agenda.oracionFinal, "names-list"))}
          </div>
        </div>
      </section>

      <section class="panel">
        ${sectionTitle("Asuntos Estaca/Barrio", "S", `<button class="secondary-button" id="add-business" type="button">Agregar</button>`)}
        <div id="business-list" class="dynamic-list">${agenda.asuntosEstacaBarrio.map(businessRow).join("")}</div>
      </section>

      <section class="panel">
        ${sectionTitle("Mensajes del Evangelio", "M", `<button class="secondary-button" id="add-message" type="button">Agregar</button>`)}
        <label class="checkbox-line">
          <input id="reunionTestimonios" type="checkbox" ${agenda.reunionTestimonios ? "checked" : ""}>
          Reunión de ayuno y testimonios
        </label>
        <div id="message-list" class="dynamic-list">${agenda.mensajesEvangelio.map(messageRow).join("")}</div>
        <div class="field">
          <label for="testimonios">Testimonios o nombres, uno por línea</label>
          <textarea id="testimonios" class="textarea">${escapeHtml((agenda.testimonios || []).join("\n"))}</textarea>
        </div>
      </section>

      <datalist id="names-list">${usedNames().map((name) => `<option value="${escapeAttr(name)}"></option>`).join("")}</datalist>
    </form>
  `;
  if (!agenda.asuntosEstacaBarrio.length) screen.querySelector("#business-list").insertAdjacentHTML("beforeend", businessRow());
  if (!agenda.mensajesEvangelio.length && !agenda.reunionTestimonios) screen.querySelector("#message-list").insertAdjacentHTML("beforeend", messageRow());
  screen.querySelector("#agenda-form").addEventListener("submit", saveAgendaFromForm);
  screen.querySelector('[data-action="back"]').addEventListener("click", () => navigate("agendas"));
  screen.querySelector('[data-action="reading-preview"]').addEventListener("click", () => {
    state.activeAgendaId = state.activeAgendaId || "__draft__";
    state.route = "reading";
    state.draftReadingAgenda = readAgendaForm(agenda);
    render();
  });
  screen.querySelector('[data-action="export-pdf"]').addEventListener("click", () => {
    exportAgendaPdf(readAgendaForm(agenda));
  });
  screen.querySelector("#add-business").addEventListener("click", addBusinessRow);
  screen.querySelector("#add-message").addEventListener("click", addMessageRow);
  bindHymnAutoFill();
  bindBusinessRows();
  bindMessageRows();
}

function field(id, label, control, hint = "") {
  return `
    <div class="field">
      <label for="${escapeAttr(id)}">${escapeHtml(label)}</label>
      ${control}
      ${hint ? `<p class="field-hint">${escapeHtml(hint)}</p>` : ""}
    </div>
  `;
}

function configColorField(id, label, colorClass, value) {
  return `
    <div class="field">
      <label class="config-color-label" for="${escapeAttr(id)}">
        <span class="config-swatch ${escapeAttr(colorClass)}"></span>
        ${escapeHtml(label)}
      </label>
      <input id="${escapeAttr(id)}" class="input" type="number" min="1" value="${Number(value)}">
    </div>
  `;
}

function textInput(id, value, list = "") {
  return `<input id="${escapeAttr(id)}" class="input" value="${escapeAttr(value || "")}" ${list ? `list="${escapeAttr(list)}"` : ""}>`;
}

function textarea(id, value) {
  return `<textarea id="${escapeAttr(id)}" class="textarea">${escapeHtml(value || "")}</textarea>`;
}

function option(value, label, selectedValue) {
  return `<option value="${escapeAttr(value)}" ${value === selectedValue ? "selected" : ""}>${escapeHtml(label)}</option>`;
}

function hymnFields(prefix, label, number, name) {
  return `
    <div class="inline-fields">
      ${field(`${prefix}Numero`, `${label} Nro.`, `<input id="${prefix}Numero" class="input" type="number" min="0" value="${Number(number || 0) || ""}" data-hymn-number="${prefix}Nombre">`)}
      ${field(`${prefix}Nombre`, "Nombre del himno", `<input id="${prefix}Nombre" class="input" value="${escapeAttr(name || "")}">`)}
    </div>
  `;
}

function businessRow(item = {}) {
  const type = item.tipo || "SOSTENIMIENTO";
  const col2 = item.columna2 || "";
  const col3 = item.columna3 || "";
  const fields = {
    RELEVO: `
      <input class="input" data-business-col2 value="${escapeAttr(col2)}" placeholder="Nombre">
      <input class="input" data-business-col3 value="${escapeAttr(col3)}" placeholder="Cargo">
    `,
    SOSTENIMIENTO: `
      <input class="input" data-business-col2 value="${escapeAttr(col2)}" placeholder="Nombre">
      <input class="input" data-business-col3 value="${escapeAttr(col3)}" placeholder="Cargo">
    `,
    ESTACA: `
      <div class="field compact-field">
        <input class="input" data-business-col2 value="${escapeAttr(col2)}" placeholder="Ej.: Presidente Garcia">
        <p class="field-hint">${escapeHtml(descripcionAsuntoEstaca({ columna2: col2 }))}</p>
      </div>
      <input type="hidden" data-business-col3 value="">
    `,
    SOSTENIMIENTO_OFICIALES: `
      <div class="field compact-field">
        <input class="input" data-business-col2 value="${escapeAttr(col2)}" placeholder="Ej.: Obispo Garcia">
        <p class="field-hint">${escapeHtml(descripcionSostenimientoOficiales({ columna2: col2 }))}</p>
      </div>
      <input type="hidden" data-business-col3 value="">
    `,
    ORDENACION_AARONICA: `
      <input class="input" data-business-col2 value="${escapeAttr(col2)}" placeholder="Nombre">
      <select class="select" data-business-col3>
        <option value="">Oficio</option>
        ${AARONIC_OFFICES.map((office) => option(office, office, col3)).join("")}
      </select>
    `,
    OTROS: `
      <textarea class="textarea compact-textarea" data-business-col2 placeholder="Escribi el texto tal como debe aparecer">${escapeHtml(col2)}</textarea>
      <input type="hidden" data-business-col3 value="">
    `
  }[type] || "";
  return `
    <div class="dynamic-row business-row" data-business-row>
      <select class="select" data-business-type>${BUSINESS_TYPES.map((value) => option(value, labelBusiness(value), type)).join("")}</select>
      <div class="dynamic-row-fields">${fields}</div>
      ${dynamicRowActions()}
    </div>
  `;
}

function messageRow(item = {}) {
  const type = item.tipo || "DISCURSO";
  const personLabel = type === "TESTIMONIO" ? "Nombre" : "Discursante";
  return `
    <div class="dynamic-row message-row" data-message-row>
      <select class="select" data-message-type>${MESSAGE_TYPES.map((value) => option(value, labelMessage(value), type)).join("")}</select>
      <div class="dynamic-row-fields">
        ${type === "HIMNO_INTERMEDIO" ? `
          <div class="inline-fields compact-inline">
            <input class="input" data-message-hymn-number type="number" min="0" value="${Number(item.himnoNumero || 0) || ""}" placeholder="Nro.">
            <input class="input" data-message-hymn-name value="${escapeAttr(item.himnoNombre || "")}" placeholder="Nombre del himno">
          </div>
          <input type="hidden" data-message-name value="">
          <input type="hidden" data-message-topic value="">
          <input type="hidden" data-message-topic-tags value="">
        ` : `
          <input class="input" data-message-name value="${escapeAttr(item.nombre || "")}" list="names-list" placeholder="${escapeAttr(personLabel)}">
          <input type="hidden" data-message-hymn-number value="0">
          <input type="hidden" data-message-hymn-name value="">
          ${type === "DISCURSO" ? `
            <input class="input" data-message-topic value="${escapeAttr(item.tema || "")}" placeholder="Tema exacto">
            <input class="input" data-message-topic-tags value="${escapeAttr(item.etiquetaTema || "")}" placeholder="Etiquetas separadas con coma">
          ` : `
            <input type="hidden" data-message-topic value="">
            <input type="hidden" data-message-topic-tags value="">
          `}
        `}
      </div>
      ${dynamicRowActions()}
    </div>
  `;
}

function dynamicRowActions() {
  return `
    <div class="dynamic-row-actions" role="group" aria-label="Orden de fila">
      <button class="icon-button" data-move-row="up" type="button" title="Subir" aria-label="Subir fila">${icon("arrowUp")}</button>
      <button class="icon-button" data-move-row="down" type="button" title="Bajar" aria-label="Bajar fila">${icon("arrowDown")}</button>
      <button class="icon-button danger-icon" data-remove-row type="button" title="Eliminar" aria-label="Eliminar fila">${icon("x")}</button>
    </div>
  `;
}

function addBusinessRow() {
  const list = screen.querySelector("#business-list");
  list.insertAdjacentHTML("beforeend", businessRow());
  bindBusinessRow(list.querySelector("[data-business-row]:last-child"));
  updateDynamicRowButtons(list);
}

function addMessageRow() {
  const list = screen.querySelector("#message-list");
  list.insertAdjacentHTML("beforeend", messageRow());
  const row = list.querySelector("[data-message-row]:last-child");
  bindMessageRow(row);
  updateDynamicRowButtons(list);
}

function bindDynamicRowControls(row) {
  row.querySelector("[data-remove-row]")?.addEventListener("click", () => {
    const list = row.parentElement;
    row.remove();
    updateDynamicRowButtons(list);
  });
  row.querySelectorAll("[data-move-row]").forEach((button) => {
    button.addEventListener("click", () => moveDynamicRow(row, button.dataset.moveRow));
  });
}

function bindBusinessRows() {
  screen.querySelectorAll("[data-business-row]").forEach(bindBusinessRow);
  updateDynamicRowButtons(screen.querySelector("#business-list"));
}

function bindBusinessRow(row) {
  bindDynamicRowControls(row);
  row.querySelector("[data-business-type]")?.addEventListener("change", (event) => {
    const nextItem = normalizeBusinessForType(readBusinessRow(row), event.target.value);
    replaceDynamicRow(row, businessRow(nextItem), bindBusinessRow);
  });
  row.querySelector("[data-business-col2]")?.addEventListener("input", () => {
    const type = row.querySelector("[data-business-type]")?.value;
    if (!["ESTACA", "SOSTENIMIENTO_OFICIALES"].includes(type)) return;
    const hint = row.querySelector(".field-hint");
    if (hint) {
      const item = readBusinessRow(row);
      hint.textContent = type === "SOSTENIMIENTO_OFICIALES"
        ? descripcionSostenimientoOficiales(item)
        : descripcionAsuntoEstaca(item);
    }
  });
}

function bindMessageRows() {
  screen.querySelectorAll("[data-message-row]").forEach(bindMessageRow);
  updateDynamicRowButtons(screen.querySelector("#message-list"));
}

function bindMessageRow(row) {
  bindDynamicRowControls(row);
  bindMessageHymnRow(row);
  row.querySelector("[data-message-type]")?.addEventListener("change", (event) => {
    const nextItem = normalizeMessageForType(readMessageRow(row), event.target.value);
    replaceDynamicRow(row, messageRow(nextItem), bindMessageRow);
  });
}

function replaceDynamicRow(row, html, bind) {
  row.insertAdjacentHTML("afterend", html);
  const nextRow = row.nextElementSibling;
  const list = row.parentElement;
  row.remove();
  bind(nextRow);
  updateDynamicRowButtons(list);
}

function moveDynamicRow(row, direction) {
  const list = row?.parentElement;
  if (!list) return;
  if (direction === "up" && row.previousElementSibling) {
    list.insertBefore(row, row.previousElementSibling);
  }
  if (direction === "down" && row.nextElementSibling) {
    list.insertBefore(row.nextElementSibling, row);
  }
  updateDynamicRowButtons(list);
  row.querySelector(`[data-move-row="${direction}"]`)?.focus();
}

function updateDynamicRowButtons(list) {
  if (!list) return;
  const rows = [...list.children].filter((child) => child.classList.contains("dynamic-row"));
  rows.forEach((row, index) => {
    const up = row.querySelector('[data-move-row="up"]');
    const down = row.querySelector('[data-move-row="down"]');
    if (up) up.disabled = index === 0;
    if (down) down.disabled = index === rows.length - 1;
  });
}

function bindHymnAutoFill() {
  screen.querySelectorAll("[data-hymn-number]").forEach((input) => {
    input.addEventListener("change", () => {
      const target = screen.querySelector(`#${input.dataset.hymnNumber}`);
      fillHymnName(input.value, target);
    });
  });
}

function bindMessageHymnRow(row) {
  row.querySelector("[data-message-hymn-number]")?.addEventListener("change", (event) => {
    fillHymnName(event.target.value, row.querySelector("[data-message-hymn-name]"));
  });
}

function fillHymnName(number, target) {
  if (!target) return;
  const hymn = hymnName(number);
  if (hymn) target.value = hymn;
}

async function saveAgendaFromForm(event) {
  event.preventDefault();
  await withToastError(async () => {
    const oldAgenda = state.activeAgendaId ? state.agendas.find((item) => item.id === state.activeAgendaId) : null;
    const agenda = readAgendaForm(oldAgenda || createBlankAgenda(nextSunday()));
    const duplicate = state.agendas.some((item) =>
      item.id !== agenda.id && sameDay(item.fecha, agenda.fecha)
    );
    if (duplicate) throw new Error("Ya existe una agenda para esa fecha.");
    const data = agendaToFirestore(agenda, oldAgenda);
    let savedAgenda = agenda;
    let message = "Agenda guardada";
    if (agenda.id) {
      await setDoc(agendaRef(agenda.id), data);
    } else {
      const ref = await addDoc(collection(db, "agendas"), data);
      state.activeAgendaId = ref.id;
      savedAgenda = { ...agenda, id: ref.id };
      message = "Agenda creada";
    }
    await syncAgendaParticipants(savedAgenda);
    await syncRemovedAgendaParticipants(oldAgenda, savedAgenda, agendasAfterSave(savedAgenda));
    upsertLocalAgenda(savedAgenda);
    state.activeAgendaId = savedAgenda.id;
    state.route = "edit";
    render();
    toastMessage(message);
  });
}

function upsertLocalAgenda(agenda) {
  const index = state.agendas.findIndex((item) => item.id === agenda.id);
  if (index >= 0) state.agendas[index] = { ...state.agendas[index], ...agenda };
  else state.agendas = [{ ...agenda }, ...state.agendas];
}

function readAgendaForm(baseAgenda) {
  const testimonios = screen.querySelector("#testimonios").value
    .split("\n")
    .map((item) => item.trim())
    .filter(Boolean);
  return {
    ...baseAgenda,
    id: baseAgenda.id || "",
    numeroUnidad: state.unitNumber,
    fecha: dateFromInput(screen.querySelector("#fecha").value),
    estado: screen.querySelector("#estado").value,
    asistencia: numberValue("#asistencia"),
    preside: valueOf("#preside"),
    dirige: valueOf("#dirige"),
    reconocimientos: valueOf("#reconocimientos"),
    anuncios: valueOf("#anuncios"),
    primerHimnoNumero: numberValue("#primerHimnoNumero"),
    primerHimnoNombre: valueOf("#primerHimnoNombre"),
    directorMusica: valueOf("#directorMusica"),
    pianista: valueOf("#pianista"),
    himnoSacramentalNumero: numberValue("#himnoSacramentalNumero"),
    himnoSacramentalNombre: valueOf("#himnoSacramentalNombre"),
    himnoFinalNumero: numberValue("#himnoFinalNumero"),
    himnoFinalNombre: valueOf("#himnoFinalNombre"),
    primeraOracion: valueOf("#primeraOracion"),
    oracionFinal: valueOf("#oracionFinal"),
    asuntosEstacaBarrio: [...screen.querySelectorAll("[data-business-row]")]
      .map(readBusinessRow)
      .filter((item) => item.columna2 || item.columna3),
    mensajesEvangelio: [...screen.querySelectorAll("[data-message-row]")]
      .map(readMessageRow)
      .filter((item) => item.nombre || item.himnoNumero || item.himnoNombre || item.tema || item.etiquetaTema),
    reunionTestimonios: screen.querySelector("#reunionTestimonios").checked,
    testimonios
  };
}

function readBusinessRow(row) {
  return normalizeBusinessForType({
    tipo: row.querySelector("[data-business-type]")?.value || "SOSTENIMIENTO",
    columna2: row.querySelector("[data-business-col2]")?.value.trim() || "",
    columna3: row.querySelector("[data-business-col3]")?.value.trim() || ""
  });
}

function normalizeBusinessForType(item, nextType = item.tipo) {
  const type = nextType || "SOSTENIMIENTO";
  const col2 = item.columna2 || "";
  const col3 = item.columna3 || "";
  return {
    tipo: type,
    columna2: col2,
    columna3: ["ESTACA", "SOSTENIMIENTO_OFICIALES", "OTROS"].includes(type) ? "" : col3
  };
}

function readMessageRow(row) {
  return normalizeMessageForType({
    tipo: row.querySelector("[data-message-type]")?.value || "DISCURSO",
    nombre: row.querySelector("[data-message-name]")?.value.trim() || "",
    himnoNumero: Number(row.querySelector("[data-message-hymn-number]")?.value || 0),
    himnoNombre: row.querySelector("[data-message-hymn-name]")?.value.trim() || "",
    tema: row.querySelector("[data-message-topic]")?.value.trim() || "",
    etiquetaTema: row.querySelector("[data-message-topic-tags]")?.value.trim() || ""
  });
}

function normalizeMessageForType(item, nextType = item.tipo) {
  const type = nextType || "DISCURSO";
  if (type === "HIMNO_INTERMEDIO") {
    return {
      tipo: type,
      nombre: "",
      himnoNumero: Number(item.himnoNumero || 0),
      himnoNombre: item.himnoNombre || "",
      tema: "",
      etiquetaTema: ""
    };
  }
  if (type === "TESTIMONIO") {
    return {
      tipo: type,
      nombre: item.nombre || "",
      himnoNumero: 0,
      himnoNombre: "",
      tema: "",
      etiquetaTema: ""
    };
  }
  return {
    tipo: type,
    nombre: item.nombre || "",
    himnoNumero: 0,
    himnoNombre: "",
    tema: item.tema || "",
    etiquetaTema: item.etiquetaTema || ""
  };
}

function valueOf(selector) {
  return screen.querySelector(selector)?.value.trim() || "";
}

function numberValue(selector) {
  return Number(screen.querySelector(selector)?.value || 0);
}

function agendaToFirestore(agenda, oldAgenda = null) {
  const createdBy = oldAgenda?.creadoPor || agenda.creadoPor || userEmail();
  const createdAt = oldAgenda?.creadoEn || agenda.creadoEn || serverTimestamp();
  return {
    numeroUnidad: agenda.numeroUnidad,
    fecha: Timestamp.fromDate(agenda.fecha),
    estado: agenda.estado,
    asistencia: Number(agenda.asistencia || 0),
    preside: agenda.preside || "",
    dirige: agenda.dirige || "",
    reconocimientos: agenda.reconocimientos || "",
    anuncios: agenda.anuncios || "",
    primerHimnoNumero: Number(agenda.primerHimnoNumero || 0),
    primerHimnoNombre: agenda.primerHimnoNombre || "",
    directorMusica: agenda.directorMusica || "",
    pianista: agenda.pianista || "",
    himnoSacramentalNumero: Number(agenda.himnoSacramentalNumero || 0),
    himnoSacramentalNombre: agenda.himnoSacramentalNombre || "",
    himnoFinalNumero: Number(agenda.himnoFinalNumero || 0),
    himnoFinalNombre: agenda.himnoFinalNombre || "",
    primeraOracion: agenda.primeraOracion || "",
    oracionFinal: agenda.oracionFinal || "",
    asuntosEstacaBarrio: agenda.asuntosEstacaBarrio || [],
    mensajesEvangelio: agenda.mensajesEvangelio || [],
    reunionTestimonios: Boolean(agenda.reunionTestimonios),
    testimonios: agenda.testimonios || [],
    creadoPor: createdBy,
    creadoEn: createdAt,
    ultimaEdicionPor: userEmail(),
    ultimaEdicionEn: serverTimestamp()
  };
}

async function syncAgendaParticipants(agenda) {
  const cache = hermanoCache();
  const talkNames = agenda.mensajesEvangelio
    .filter((item) => item.tipo !== "HIMNO_INTERMEDIO")
    .map((item) => item.nombre?.trim())
    .filter(Boolean);
  const prayerNames = [agenda.primeraOracion, agenda.oracionFinal]
    .map((item) => item?.trim())
    .filter(Boolean);

  for (const name of talkNames) {
    await syncHermanoParticipation(name, agenda.fecha, "talks", cache);
  }
  for (const name of prayerNames) {
    await syncHermanoParticipation(name, agenda.fecha, "prayers", cache);
  }
}

async function syncRemovedAgendaParticipants(oldAgenda, newAgenda = null, agendasAfter = state.agendas) {
  if (!oldAgenda) return;
  const dateChanged = !newAgenda || !sameDay(oldAgenda.fecha, newAgenda.fecha);
  const oldTalks = agendaTalkParticipants(oldAgenda);
  const newTalks = agendaTalkParticipants(newAgenda);
  const oldPrayers = agendaPrayerParticipants(oldAgenda);
  const newPrayers = agendaPrayerParticipants(newAgenda);
  const cache = hermanoCache();

  const affectedTalks = dateChanged
    ? [...oldTalks.keys()]
    : [...oldTalks.keys()].filter((key) => !newTalks.has(key));
  const affectedPrayers = dateChanged
    ? [...oldPrayers.keys()]
    : [...oldPrayers.keys()].filter((key) => !newPrayers.has(key));

  for (const key of affectedTalks) {
    await rebuildHermanoParticipation(oldTalks.get(key), oldAgenda.fecha, "talks", agendasAfter, cache);
  }
  for (const key of affectedPrayers) {
    await rebuildHermanoParticipation(oldPrayers.get(key), oldAgenda.fecha, "prayers", agendasAfter, cache);
  }
}

function agendasAfterSave(savedAgenda) {
  const exists = state.agendas.some((agenda) => agenda.id === savedAgenda.id);
  if (exists) {
    return state.agendas.map((agenda) => agenda.id === savedAgenda.id ? savedAgenda : agenda);
  }
  return [...state.agendas, savedAgenda];
}

function agendaTalkParticipants(agenda) {
  const names = new Map();
  agenda?.mensajesEvangelio
    ?.filter((item) => item.tipo !== "HIMNO_INTERMEDIO")
    .forEach((item) => addParticipantName(names, item.nombre));
  return names;
}

function agendaPrayerParticipants(agenda) {
  const names = new Map();
  addParticipantName(names, agenda?.primeraOracion);
  addParticipantName(names, agenda?.oracionFinal);
  return names;
}

function addParticipantName(map, name) {
  const cleanName = name?.trim();
  const key = normalizeName(cleanName);
  if (key) map.set(key, cleanName);
}

function hermanoCache() {
  return new Map(
    state.hermanos
      .filter((item) => item.excluido !== true)
      .map((item) => [normalizeName(item.nombre), { ...item }])
  );
}

async function syncHermanoParticipation(name, date, tab, cache = hermanoCache()) {
  const cleanName = name?.trim();
  if (!cleanName) return;
  const participationDate = toDate(date);
  if (!participationDate) return;

  const key = normalizeName(cleanName);
  const fieldName = tab === "talks" ? "ultimaVezDiscursoManual" : "ultimaVezOracionManual";
  const syncFieldName = tab === "talks" ? "ultimaVezDiscursoAutoSync" : "ultimaVezOracionAutoSync";
  const beforeSyncFieldName = tab === "talks" ? "ultimaVezDiscursoAntesAutoSync" : "ultimaVezOracionAntesAutoSync";
  const timestamp = Timestamp.fromDate(participationDate);
  const existing = cache.get(key);
  const currentDate = toDate(existing?.[fieldName]);
  if (currentDate && participationDate <= currentDate) return;

  if (existing?.id) {
    const currentSyncDate = toDate(existing?.[syncFieldName]);
    const previousValue = sameMoment(currentDate, currentSyncDate)
      ? (existing?.[beforeSyncFieldName] || null)
      : (currentDate ? Timestamp.fromDate(currentDate) : null);
    await updateDoc(hermanoRef(existing.id), {
      [fieldName]: timestamp,
      [syncFieldName]: timestamp,
      [beforeSyncFieldName]: previousValue
    });
    cache.set(key, {
      ...existing,
      [fieldName]: timestamp,
      [syncFieldName]: timestamp,
      [beforeSyncFieldName]: previousValue
    });
    return;
  }

  const data = {
    numeroUnidad: state.unitNumber,
    nombre: cleanName,
    agregadoManualmente: false,
    excluido: false,
    inactivoDiscurso: false,
    inactivoOracion: false,
    ultimaVezDiscursoManual: tab === "talks" ? timestamp : null,
    ultimaVezOracionManual: tab === "prayers" ? timestamp : null,
    ultimaVezDiscursoAutoSync: tab === "talks" ? timestamp : null,
    ultimaVezOracionAutoSync: tab === "prayers" ? timestamp : null,
    ultimaVezDiscursoAntesAutoSync: null,
    ultimaVezOracionAntesAutoSync: null,
    creadoEn: serverTimestamp()
  };
  const ref = await addDoc(collection(db, "hermanos"), data);
  cache.set(key, { ...data, id: ref.id });
}

async function rebuildHermanoParticipation(name, removedDate, tab, agendasAfter = state.agendas, cache = hermanoCache()) {
  const cleanName = name?.trim();
  if (!cleanName) return;
  const key = normalizeName(cleanName);
  const existing = cache.get(key);
  if (!existing?.id) return;

  const fieldName = tab === "talks" ? "ultimaVezDiscursoManual" : "ultimaVezOracionManual";
  const syncFieldName = tab === "talks" ? "ultimaVezDiscursoAutoSync" : "ultimaVezOracionAutoSync";
  const beforeSyncFieldName = tab === "talks" ? "ultimaVezDiscursoAntesAutoSync" : "ultimaVezOracionAntesAutoSync";
  const currentDate = toDate(existing[fieldName]);
  const syncDate = toDate(existing[syncFieldName]);
  if (!sameMoment(currentDate, removedDate) && !(sameMoment(syncDate, removedDate) && sameMoment(currentDate, syncDate))) return;

  const latest = latestParticipationDate(cleanName, tab, agendasAfter);
  const previousDate = toDate(existing[beforeSyncFieldName]);
  const restored = latestDate([latest, previousDate]);
  const timestamp = restored ? Timestamp.fromDate(restored) : null;
  const syncTimestamp = sameMoment(restored, latest) ? timestamp : null;
  await updateDoc(hermanoRef(existing.id), {
    [fieldName]: timestamp,
    [syncFieldName]: syncTimestamp,
    [beforeSyncFieldName]: null
  });
  cache.set(key, {
    ...existing,
    [fieldName]: timestamp,
    [syncFieldName]: syncTimestamp,
    [beforeSyncFieldName]: null
  });
}

function latestParticipationDate(name, tab, agendas = state.agendas) {
  const key = normalizeName(name);
  const dates = [];
  agendas.forEach((agenda) => {
    if (tab === "talks") {
      agenda.mensajesEvangelio.forEach((message) => {
        if (message.tipo !== "HIMNO_INTERMEDIO" && normalizeName(message.nombre) === key) {
          dates.push(agenda.fecha);
        }
      });
    } else {
      if (normalizeName(agenda.primeraOracion) === key) dates.push(agenda.fecha);
      if (normalizeName(agenda.oracionFinal) === key) dates.push(agenda.fecha);
    }
  });
  return latestDate(dates);
}

function renderReadingMode() {
  const agenda = state.draftReadingAgenda || state.agendas.find((item) => item.id === state.activeAgendaId);
  state.draftReadingAgenda = null;
  if (!agenda) {
    screen.innerHTML = emptyPanel("No se encontró la agenda.");
    return;
  }
  screen.innerHTML = `
    <div class="toolbar">
      <div class="toolbar-left">
        <button class="secondary-button" data-action="back" type="button">Volver</button>
      </div>
      <div class="toolbar-right">
        <button class="secondary-button" data-action="copy" type="button">Copiar texto</button>
        <button class="secondary-button" data-action="print-reading" type="button">Imprimir lectura</button>
        <button class="primary-button" data-action="pdf-agenda" type="button">PDF agenda</button>
      </div>
    </div>
    <article class="reading-page">
      ${readingHtml(agenda)}
    </article>
  `;
  screen.querySelector('[data-action="back"]').addEventListener("click", () => navigate("agendas"));
  screen.querySelector('[data-action="print-reading"]').addEventListener("click", () => window.print());
  screen.querySelector('[data-action="pdf-agenda"]').addEventListener("click", () => exportAgendaPdf(agenda));
  screen.querySelector('[data-action="copy"]').addEventListener("click", async () => {
    await navigator.clipboard.writeText(agendaText(agenda));
    toastMessage("Texto copiado");
  });
}

function exportAgendaPdf(agenda) {
  state.pdfReturnRoute = state.route === "edit" ? "edit" : "agendas";
  state.pdfReturnAgendaId = state.route === "edit" ? state.activeAgendaId : null;
  state.draftPdfAgenda = agenda;
  state.activeAgendaId = agenda.id || "__draft__";
  state.route = "pdf";
  render();
  window.setTimeout(() => window.print(), 220);
}

function readingHtml(agenda) {
  const messages = agenda.reunionTestimonios
    ? agenda.testimonios.map((name) => `<li>Testimonio: ${escapeHtml(name)}</li>`).join("")
    : agenda.mensajesEvangelio.map((message) => {
      if (message.tipo === "HIMNO_INTERMEDIO") return `<li>Himno intermedio: ${escapeHtml(hymnLabel(message.himnoNumero, message.himnoNombre))}</li>`;
      const topic = message.tipo === "DISCURSO" && message.tema
        ? `<br><span class="reading-detail">Tema: ${escapeHtml(message.tema)}</span>`
        : "";
      const tags = message.tipo === "DISCURSO" && message.etiquetaTema
        ? `<br><span class="reading-detail">Etiquetas: ${escapeHtml(message.etiquetaTema)}</span>`
        : "";
      return `<li>${escapeHtml(labelMessage(message.tipo))}: ${escapeHtml(message.nombre || "Sin datos")}${topic}${tags}</li>`;
    }).join("");
  return `
    <div class="reading-title">
      <h2>Agenda Reunión Sacramental</h2>
      <p>${escapeHtml(formatDateLong(agenda.fecha))}</p>
    </div>
    ${readingLine("Preside", agenda.preside)}
    ${readingLine("Dirige", agenda.dirige)}
    ${readingLine("Asistencia", agenda.asistencia ? String(agenda.asistencia) : "")}
    ${readingLine("Reconocimientos", agenda.reconocimientos)}
    ${readingLine("Anuncios", agenda.anuncios)}
    ${readingLine("Himno de apertura", hymnLabel(agenda.primerHimnoNumero, agenda.primerHimnoNombre))}
    ${readingLine("Director/a", agenda.directorMusica)}
    ${readingLine("Pianista", agenda.pianista)}
    ${readingLine("Primera oración", agenda.primeraOracion)}
    ${agenda.asuntosEstacaBarrio.length ? `<section class="reading-section"><strong>Asuntos Estaca/Barrio</strong><ul>${agenda.asuntosEstacaBarrio.map((item) => `<li>${escapeHtml(labelBusiness(item.tipo))}: ${escapeHtml(businessDescription(item) || "Sin datos")}</li>`).join("")}</ul></section>` : ""}
    ${readingLine("Himno Sacramental", hymnLabel(agenda.himnoSacramentalNumero, agenda.himnoSacramentalNombre))}
    <section class="reading-section"><strong>${agenda.reunionTestimonios ? "Reunión de testimonios" : "Mensajes del Evangelio"}</strong>${messages ? `<ul>${messages}</ul>` : "<p>Sin datos</p>"}</section>
    ${readingLine("Himno final", hymnLabel(agenda.himnoFinalNumero, agenda.himnoFinalNombre))}
    ${readingLine("Oración final", agenda.oracionFinal)}
  `;
}

function readingLine(label, value) {
  return `<section class="reading-section"><strong>${escapeHtml(label)}:</strong> ${escapeHtml(value || "Sin datos")}</section>`;
}

function renderPdfMode() {
  const agenda = state.draftPdfAgenda || state.agendas.find((item) => item.id === state.activeAgendaId);
  state.draftPdfAgenda = null;
  if (!agenda) {
    screen.innerHTML = emptyPanel("No se encontro la agenda.");
    return;
  }
  const returnLabel = state.pdfReturnRoute === "edit" ? "Volver a editar" : "Salir del PDF";
  screen.innerHTML = `
    <div class="toolbar">
      <div class="toolbar-left">
        <button class="secondary-button" data-action="back" type="button">${escapeHtml(returnLabel)}</button>
      </div>
      <div class="toolbar-right">
        <button class="primary-button" data-action="print" type="button">Imprimir / PDF</button>
      </div>
    </div>
    <div class="pdf-document">
      ${pdfHtml(agenda)}
    </div>
  `;
  screen.querySelector('[data-action="back"]').addEventListener("click", closePdfMode);
  screen.querySelector('[data-action="print"]').addEventListener("click", () => window.print());
  window.requestAnimationFrame(fitPdfPages);
}

function closePdfMode() {
  state.route = state.pdfReturnRoute || "agendas";
  state.activeAgendaId = state.pdfReturnAgendaId;
  state.draftPdfAgenda = null;
  render();
}

function pdfHtml(agenda) {
  return `
    <article class="pdf-page pdf-main-page ${escapeAttr(pdfDensityClass(agenda))}">
    <div class="pdf-title">
      <h2>AGENDA REUNIÓN SACRAMENTAL</h2>
      <p>Una Experiencia Espiritual</p>
      <div class="pdf-attendance">
        <span>Asistencia</span>
        <strong>${agenda.asistencia ? escapeHtml(String(agenda.asistencia)) : ""}</strong>
      </div>
    </div>
    <div class="pdf-row">
      ${pdfField("Fecha:", formatDateShort(agenda.fecha))}
      ${pdfField("Dirige:", agenda.dirige)}
    </div>
    ${pdfField("Preside:", agenda.preside)}
    <p class="pdf-muted">Preludio (10-15' antes del inicio de la reunión)</p>
    ${pdfListSection("Bienvenida y reconocimiento de autoridades:", splitCommaItems(agenda.reconocimientos), 1)}
    ${pdfListSection("Anuncios (Solamente los más importantes y urgentes):", splitCommaItems(agenda.anuncios), 2, true)}
    ${pdfField("Himno de apertura:", hymnLabel(agenda.primerHimnoNumero, agenda.primerHimnoNombre))}
    <div class="pdf-row">
      ${pdfField("Director/a:", agenda.directorMusica)}
      ${pdfField("Pianista:", agenda.pianista)}
    </div>
    ${pdfField("Primera oración:", agenda.primeraOracion)}
    ${pdfBusinessSection(agenda)}
    ${pdfField("Himno Sacramental:", hymnLabel(agenda.himnoSacramentalNumero, agenda.himnoSacramentalNombre))}
    <p class="pdf-muted">Bendición y Reparto de la Santa Cena</p>
    ${pdfMessagesSection(agenda)}
    ${pdfField("Himno final:", hymnLabel(agenda.himnoFinalNumero, agenda.himnoFinalNombre))}
    ${pdfField("Oración final:", agenda.oracionFinal)}
    <p class="pdf-muted">Postludio (10 minutos - sólo música)</p>
    <p class="pdf-quote">"Pero a pesar de las cosas que están escritas, siempre se ha concedido a los élderes de mi iglesia desde el principio, y siempre será así, dirigir todas las reuniones conforme los oriente y los guíe el Santo Espíritu." D y C 46:2</p>
    <p class="pdf-area">Área Sudamérica Sur</p>
    </article>
    ${agenda.reunionTestimonios ? pdfTestimonyPage(agenda) : ""}
  `;
}

function pdfField(label, value) {
  return `
    <div class="pdf-field">
      <span>${escapeHtml(label)}</span>
      <strong>${escapeHtml(value || "")}</strong>
    </div>
  `;
}

function pdfListSection(title, items, emptyLines = 1, muted = false) {
  const content = items.length
    ? `<ul class="pdf-list">${items.map((item) => `<li>${escapeHtml(item)}</li>`).join("")}</ul>`
    : pdfEmptyLines(emptyLines);
  return `
    <section class="pdf-section ${muted ? "pdf-section-muted" : ""}">
      <h3>${escapeHtml(title)}</h3>
      ${content}
    </section>
  `;
}

function pdfBusinessSection(agenda) {
  const blocks = businessBlocksInOrder(agenda.asuntosEstacaBarrio || []);
  const content = blocks.length
    ? blocks.map(pdfBusinessBlock).join("")
    : pdfEmptyLines(3);
  return `
    <section class="pdf-section">
      <h3>Asuntos</h3>
      <div class="pdf-box">${content}</div>
    </section>
  `;
}

function pdfBusinessBlock(block) {
  const type = block[0]?.tipo || "OTROS";
  const text = liturgicalBusinessText(type, block);
  return `
    <div class="pdf-business-block">
      <strong>${escapeHtml(businessFormulaLabel(type))}</strong>
      <p>${escapeHtml(text).replaceAll("\n", "<br>")}</p>
    </div>
  `;
}

function pdfMessagesSection(agenda) {
  if (agenda.reunionTestimonios) {
    return `
      <section class="pdf-section">
        <h3>Reunión de Testimonios</h3>
        <div class="pdf-box">
          <p class="pdf-box-lead">Tiempo de testimonios de la congregación</p>
        </div>
      </section>
    `;
  }

  const messages = agenda.mensajesEvangelio || [];
  return `
    <section class="pdf-section">
      <h3>Mensajes del evangelio, canto de la congregación y números musicales especiales</h3>
      <div class="pdf-box pdf-message-box">
        ${messages.length ? messages.map(pdfMessageItem).join("") : pdfEmptyLines(3)}
      </div>
    </section>
  `;
}

function pdfMessageItem(message) {
  if (message.tipo === "HIMNO_INTERMEDIO") {
    return `<div class="pdf-message-item"><div class="pdf-message-line"><strong>Himno:</strong> ${escapeHtml(hymnLabel(message.himnoNumero, message.himnoNombre) || "-")}</div></div>`;
  }
  if (message.tipo === "TESTIMONIO") {
    return `<div class="pdf-message-item"><div class="pdf-message-line"><strong>Testimonio:</strong> ${escapeHtml(message.nombre || "-")}</div></div>`;
  }
  const details = [
    `<div class="pdf-message-line"><strong>Discurso:</strong> ${escapeHtml(message.nombre || "-")}</div>`,
    message.tema ? `<div class="pdf-message-detail">Tema: ${escapeHtml(message.tema)}</div>` : ""
  ].filter(Boolean);
  return `<div class="pdf-message-item">${details.join("")}</div>`;
}

function pdfTestimonyPage(agenda) {
  const names = (agenda.testimonios || []).filter(Boolean);
  const lineCount = Math.max(28, names.length);
  return `
    <article class="pdf-page pdf-testimony-page">
      <div class="pdf-testimony-title">
        <h2>REUNIÓN DE TESTIMONIOS</h2>
        <p>${escapeHtml(formatDateShort(agenda.fecha))}</p>
      </div>
      <div class="pdf-testimony-list">
        ${Array.from({ length: lineCount }, (_, index) => `
          <div class="pdf-testimony-row">
            <span>${index + 1}.</span>
            <strong>${escapeHtml(names[index] || "")}</strong>
          </div>
        `).join("")}
      </div>
    </article>
  `;
}

function pdfEmptyLines(count) {
  return Array.from({ length: count }, () => `<div class="pdf-empty-line"></div>`).join("");
}

function splitCommaItems(value) {
  return String(value || "")
    .split(",")
    .map((item) => item.trim())
    .filter(Boolean);
}

function fitPdfPages() {
  const fitClasses = ["pdf-fit-normal", "pdf-fit-compact", "pdf-fit-dense", "pdf-fit-tiny"];
  document.querySelectorAll(".pdf-main-page").forEach((page) => {
    fitClasses.forEach((className) => page.classList.remove(className));
    for (const className of fitClasses) {
      page.classList.add(className);
      const maxHeight = Math.max(1123, page.getBoundingClientRect().width * 1.4142);
      if (page.scrollHeight <= maxHeight + 2 || className === fitClasses[fitClasses.length - 1]) break;
      page.classList.remove(className);
    }
  });
}

function pdfDensityClass(agenda) {
  const textLength = [
    agenda.reconocimientos,
    agenda.anuncios,
    ...(agenda.asuntosEstacaBarrio || []).map((item) => liturgicalBusinessText(item.tipo, [item])),
    ...(agenda.mensajesEvangelio || []).map(messageText)
  ].join(" ").length;
  const score = 28 +
    Math.ceil(textLength / 115) +
    (agenda.asuntosEstacaBarrio?.length || 0) * 3 +
    (agenda.mensajesEvangelio?.length || 0) * 4;
  if (agenda.reunionTestimonios) return score > 58 ? "pdf-fit-compact" : "pdf-fit-normal";
  if (score > 78) return "pdf-fit-tiny";
  if (score > 64) return "pdf-fit-dense";
  if (score > 50) return "pdf-fit-compact";
  return "pdf-fit-normal";
}

function renderPlanning() {
  const rankings = planningRankings();
  const tab = state.planningTab;
  const isTopics = tab === "topics";
  const list = isTopics ? [] : rankings
    .sort((a, b) => {
      const inactiveA = isInactiveForTab(a, tab);
      const inactiveB = isInactiveForTab(b, tab);
      if (inactiveA !== inactiveB) return inactiveA ? 1 : -1;
      return daysFor(b, tab) - daysFor(a, tab);
    });
  screen.innerHTML = `
    <div class="toolbar">
      <div class="toolbar-left">
        <button class="filter-chip ${tab === "talks" ? "active" : ""}" data-planning-tab="talks" type="button">Discursos</button>
        <button class="filter-chip ${tab === "prayers" ? "active" : ""}" data-planning-tab="prayers" type="button">Oraciones</button>
        <button class="filter-chip ${tab === "topics" ? "active" : ""}" data-planning-tab="topics" type="button">Temas</button>
        <input id="planning-search" class="input" type="search" placeholder="${isTopics ? "Buscar tema o etiquetas..." : "Buscar hermano/a..."}" style="width: min(340px, 100%);">
      </div>
      <div class="toolbar-right">
        <button id="planning-config" class="secondary-button" type="button">Configuración</button>
        ${isTopics ? `<button id="topic-tags" class="secondary-button" type="button">Etiquetas</button>` : `<button id="add-brother" class="primary-button" type="button">Agregar hermano</button>`}
      </div>
    </div>
    ${isTopics ? renderTopicsPlanning("") : `
      <section class="panel">
        ${sectionTitle(tab === "talks" ? "Discursos" : "Oraciones", "P", "")}
        <div id="brother-list" class="agenda-list">${renderBrotherList(list, tab, "")}</div>
      </section>
    `}
  `;
  screen.querySelectorAll("[data-planning-tab]").forEach((button) => {
    button.addEventListener("click", () => {
      state.planningTab = button.dataset.planningTab;
      renderPlanning();
    });
  });
  screen.querySelector("#planning-search").addEventListener("input", (event) => {
    if (isTopics) {
      screen.querySelector("#topics-content").innerHTML = renderTopicsPlanningContent(event.target.value);
      bindTopicActions();
    } else {
      screen.querySelector("#brother-list").innerHTML = renderBrotherList(list, tab, event.target.value);
      bindBrotherActions();
    }
  });
  screen.querySelector("#planning-config").addEventListener("click", openPlanningConfigDialog);
  if (isTopics) {
    screen.querySelector("#topic-tags").addEventListener("click", openTopicTagsDialog);
    bindTopicActions();
  } else {
    screen.querySelector("#add-brother").addEventListener("click", () => openBrotherDialog());
    bindBrotherActions();
  }
}

function renderTopicsPlanning(searchValue) {
  return `<div id="topics-content" class="screen-grid">${renderTopicsPlanningContent(searchValue)}</div>`;
}

function renderTopicsPlanningContent(searchValue) {
  const summaries = topicSummaries();
  const suggestions = topicSuggestions(summaries, topicTagsAvailable());
  const query = normalizeText(searchValue);
  const filteredSuggestions = suggestions.filter((item) =>
    !query || normalizeText(item.etiqueta).includes(query) || normalizeText(item.resumen?.ultimoTema).includes(query)
  );
  const filteredSummaries = summaries.filter((item) =>
    !query ||
    normalizeText(item.etiqueta).includes(query) ||
    normalizeText(item.ultimoTema).includes(query) ||
    normalizeText(item.ultimoDiscursante).includes(query) ||
    item.registros.some((record) => normalizeText(`${record.tema} ${record.discursante}`).includes(query))
  );

  return `
    <section class="panel">
      ${sectionTitle("Temas sugeridos", "T", "")}
      <div class="topic-grid">
        ${filteredSuggestions.length ? filteredSuggestions.slice(0, 12).map(topicSuggestionCard).join("") : emptyState("No hay sugerencias que coincidan.")}
      </div>
    </section>
    <section class="panel">
      ${sectionTitle("Historial de temas", "H", "")}
      <div class="agenda-list">
        ${filteredSummaries.length ? filteredSummaries.map(topicSummaryCard).join("") : emptyState("Todavia no hay temas guardados en discursos.")}
      </div>
    </section>
  `;
}

function topicSuggestionCard(item) {
  const color = topicColor(item.resumen);
  const meta = item.resumen
    ? `${topicDistanceText(item.resumen.ultimaFecha)} - ${item.resumen.veces180Dias} en 180 dias`
    : "Sin registros en el historial.";
  return `
    <article class="topic-card ${escapeAttr(color)}">
      <div>
        <p class="item-title"><span class="rank-dot ${escapeAttr(color)}"></span>${escapeHtml(item.etiqueta)}</p>
        <p class="item-meta">${escapeHtml(meta)}</p>
      </div>
      <button class="secondary-button" data-topic-assign="${escapeAttr(item.etiqueta)}" type="button">Asignar</button>
    </article>
  `;
}

function topicSummaryCard(summary) {
  const color = topicColor(summary);
  return `
    <article class="topic-card">
      <div>
        <p class="item-title"><span class="rank-dot ${escapeAttr(color)}"></span>${escapeHtml(summary.etiqueta)}</p>
        <p class="item-meta">${escapeHtml(topicDistanceText(summary.ultimaFecha))} - ${summary.veces90Dias} en 90 dias</p>
        ${summary.ultimoTema ? `<p class="topic-detail">${escapeHtml(summary.ultimoTema)}</p>` : ""}
        <p class="item-meta">Ultimo: ${escapeHtml(summary.ultimoDiscursante || "Sin datos")} - ${summary.veces180Dias} en 180 dias - ${summary.total} total</p>
      </div>
    </article>
  `;
}

function bindTopicActions() {
  const suggestions = topicSuggestions(topicSummaries(), topicTagsAvailable());
  const byLabel = new Map(suggestions.map((item) => [normalizeText(item.etiqueta), item]));
  screen.querySelectorAll("[data-topic-assign]").forEach((button) => {
    button.addEventListener("click", () => openAssignTopicDialog(byLabel.get(normalizeText(button.dataset.topicAssign))));
  });
}

function topicTagsAvailable() {
  const source = Array.isArray(state.config.etiquetasTema) && state.config.etiquetasTema.length
    ? state.config.etiquetasTema
    : BASE_TOPIC_TAGS;
  return uniqueCleanList(source);
}

function topicLabelsFromText(text, fallback = "") {
  const labels = uniqueCleanList(String(text || "").split(","));
  if (labels.length) return labels;
  const cleanFallback = fallback.trim();
  return cleanFallback ? [cleanFallback] : [];
}

function topicRecords() {
  return state.agendas.flatMap((agenda) =>
    agenda.mensajesEvangelio.flatMap((message) => {
      if (message.tipo !== "DISCURSO") return [];
      const topic = (message.tema || "").trim();
      const labels = topicLabelsFromText(message.etiquetaTema, topic);
      if (!topic && !labels.length) return [];
      return labels.map((label) => ({
        etiqueta: label,
        tema: topic,
        discursante: (message.nombre || "").trim(),
        fecha: agenda.fecha
      }));
    })
  );
}

function topicSummaries() {
  const groups = new Map();
  topicRecords().forEach((record) => {
    const key = normalizeText(record.etiqueta);
    if (!key) return;
    if (!groups.has(key)) groups.set(key, []);
    groups.get(key).push(record);
  });

  return [...groups.values()]
    .map((group) => {
      const ordered = group.sort((a, b) => b.fecha - a.fecha);
      const latest = ordered[0];
      return {
        etiqueta: latest.etiqueta,
        ultimoTema: latest.tema,
        ultimoDiscursante: latest.discursante,
        ultimaFecha: latest.fecha,
        veces90Dias: countRecordsWithinDays(group, 90),
        veces180Dias: countRecordsWithinDays(group, 180),
        total: group.length,
        registros: ordered
      };
    })
    .sort((a, b) => b.ultimaFecha - a.ultimaFecha);
}

function topicSuggestions(summaries, baseTags) {
  const byTag = new Map(summaries.map((item) => [normalizeText(item.etiqueta), item]));
  const baseKeys = new Set(baseTags.map(normalizeText));
  const fromBase = baseTags.map((etiqueta) => ({ etiqueta, resumen: byTag.get(normalizeText(etiqueta)) || null }));
  const historical = summaries
    .filter((item) => item.veces90Dias === 0 && !baseKeys.has(normalizeText(item.etiqueta)))
    .map((item) => ({ etiqueta: item.etiqueta, resumen: item }));
  return [...fromBase, ...historical]
    .filter((item, index, list) => list.findIndex((other) => normalizeText(other.etiqueta) === normalizeText(item.etiqueta)) === index)
    .sort((a, b) => {
      const countDiff = (a.resumen?.veces90Dias || 0) - (b.resumen?.veces90Dias || 0);
      if (countDiff) return countDiff;
      const dateDiff = (a.resumen?.ultimaFecha?.getTime?.() || 0) - (b.resumen?.ultimaFecha?.getTime?.() || 0);
      if (dateDiff) return dateDiff;
      return normalizeText(a.etiqueta).localeCompare(normalizeText(b.etiqueta));
    });
}

function countRecordsWithinDays(records, days) {
  const now = startOfDay(new Date());
  return records.filter((record) => (now - startOfDay(record.fecha)) / 86400000 <= days).length;
}

function topicColor(summary) {
  if (!summary) return "rank-green";
  const days = daysSince(summary.ultimaFecha);
  if (days >= Number(state.config.diasVerdeTema || 180)) return "rank-green";
  if (days >= Number(state.config.diasAmarilloTema || 90)) return "rank-yellow";
  return "rank-red";
}

function topicDistanceText(date) {
  const days = daysFromToday(date);
  if (!Number.isFinite(days)) return "Sin registros";
  if (days === 0) return "Hoy";
  return days > 0 ? `Hace ${days} dias` : `Dentro de ${Math.abs(days)} dias`;
}

function uniqueCleanList(items) {
  const seen = new Set();
  const clean = [];
  items.forEach((item) => {
    const value = String(item || "").trim();
    const key = normalizeText(value);
    if (!value || seen.has(key)) return;
    seen.add(key);
    clean.push(value);
  });
  return clean;
}

function renderBrotherList(items, tab, searchValue) {
  const q = normalizeText(searchValue);
  const filtered = items.filter((item) => !q || normalizeText(item.hermano.nombre).includes(q));
  if (!filtered.length) return emptyState("No hay hermanos en esta categoría.");
  return filtered.map((item) => brotherRow(item, tab)).join("");
}

function brotherRow(item, tab) {
  const last = tab === "talks" ? item.ultimaVezDiscurso : item.ultimaVezOracion;
  const count = tab === "talks" ? item.vecesDiscurso90Dias : item.vecesOracion90Dias;
  const inactive = isInactiveForTab(item, tab);
  const color = inactive ? "rank-muted" : rankColor(last, tab);
  return `
    <article class="brother-item">
      <div>
        <p class="item-title"><span class="rank-dot ${escapeAttr(color)}"></span>${escapeHtml(item.hermano.nombre)}</p>
        <p class="item-meta">${inactive ? "Inactivo · " : ""}${last ? `Última participación: ${formatDateShort(last)} (${daysSince(last)} días)` : "Sin registros"}${count ? ` · ${count} vez/veces en 90 días` : ""}</p>
      </div>
      <div class="item-actions">
        <button class="secondary-button" data-assign="${escapeAttr(item.key)}" type="button">Asignar</button>
        <button class="secondary-button" data-edit-brother="${escapeAttr(item.key)}" type="button">Editar</button>
        <button class="secondary-button" data-toggle-inactive="${escapeAttr(item.key)}" type="button">${inactive ? "Reactivar" : "Inactivar"}</button>
        <button class="icon-button" data-delete-brother="${escapeAttr(item.key)}" type="button" title="Eliminar">X</button>
      </div>
    </article>
  `;
}

function bindBrotherActions() {
  const rankings = planningRankings();
  const byKey = new Map(rankings.map((item) => [item.key, item]));
  screen.querySelectorAll("[data-assign]").forEach((button) => {
    button.addEventListener("click", () => openAssignDialog(byKey.get(button.dataset.assign)));
  });
  screen.querySelectorAll("[data-edit-brother]").forEach((button) => {
    button.addEventListener("click", () => openBrotherDialog(byKey.get(button.dataset.editBrother)));
  });
  screen.querySelectorAll("[data-toggle-inactive]").forEach((button) => {
    button.addEventListener("click", () => toggleInactive(byKey.get(button.dataset.toggleInactive)));
  });
  screen.querySelectorAll("[data-delete-brother]").forEach((button) => {
    button.addEventListener("click", () => deleteBrother(byKey.get(button.dataset.deleteBrother)));
  });
}

function openBrotherDialog(ranking = null) {
  const hermano = ranking?.hermano || {};
  openModal({
    title: ranking ? "Editar hermano/a" : "Agregar hermano/a",
    body: `
      <form id="brother-form" class="form-grid">
        ${field("brother-name", "Nombre", `<input id="brother-name" class="input" value="${escapeAttr(hermano.nombre || "")}" required>`)}
        <div class="inline-fields">
          ${field("last-talk", "Último discurso conocido", `<input id="last-talk" class="input" type="date" value="${dateInputValue(ranking?.ultimaVezDiscurso || toDate(hermano.ultimaVezDiscursoManual))}">`)}
          ${field("last-prayer", "Última oración conocida", `<input id="last-prayer" class="input" type="date" value="${dateInputValue(ranking?.ultimaVezOracion || toDate(hermano.ultimaVezOracionManual))}">`)}
        </div>
        ${ranking?.ultimaEtiquetaDiscurso ? field("last-topic-tags", "Etiquetas del último discurso", `<input id="last-topic-tags" class="input" value="${escapeAttr(ranking.ultimaEtiquetaDiscurso)}" readonly>`) : ""}
        ${ranking?.ultimoTemaDiscurso ? field("last-topic", "Tema del último discurso", `<textarea id="last-topic" class="textarea" readonly>${escapeHtml(ranking.ultimoTemaDiscurso)}</textarea>`) : ""}
      </form>
    `,
    footer: `
      <button class="secondary-button" data-action="cancel" type="button">Cancelar</button>
      <button class="primary-button" form="brother-form" type="submit">Guardar</button>
    `,
    bind: (dialog) => {
      dialog.querySelector("#brother-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        await withToastError(async () => {
          const data = {
            numeroUnidad: state.unitNumber,
            nombre: dialog.querySelector("#brother-name").value.trim(),
            agregadoManualmente: true,
            excluido: false,
            inactivoDiscurso: Boolean(hermano.inactivoDiscurso),
            inactivoOracion: Boolean(hermano.inactivoOracion),
            ultimaVezDiscursoManual: timestampOrNull(dialog.querySelector("#last-talk").value),
            ultimaVezOracionManual: timestampOrNull(dialog.querySelector("#last-prayer").value),
            creadoEn: hermano.creadoEn || serverTimestamp()
          };
          if (!data.nombre) throw new Error("Ingresa un nombre.");
          if (hermano.id) await setDoc(hermanoRef(hermano.id), data);
          else await addDoc(collection(db, "hermanos"), data);
          closeModal();
          toastMessage("Hermano guardado");
        });
      });
    }
  });
}

function openAssignDialog(ranking) {
  if (!ranking) return;
  const candidateAgendas = state.agendas
    .filter((agenda) => agenda.estado !== "REALIZADA")
    .sort((a, b) => a.fecha - b.fecha);
  if (!candidateAgendas.length) {
    toastMessage("No hay agendas borrador o confirmadas.");
    return;
  }
  const tab = state.planningTab;
  openModal({
    title: `Asignar ${ranking.hermano.nombre}`,
    body: `
      <form id="assign-form" class="form-grid">
        ${field("assign-agenda", "Agenda", `<select id="assign-agenda" class="select">${candidateAgendas.map((agenda) => option(agenda.id, formatDateLong(agenda.fecha), candidateAgendas[0].id)).join("")}</select>`)}
        ${tab === "prayers" ? field("assign-field", "Tipo de oración", `<select id="assign-field" class="select"><option value="primeraOracion">Primera oración</option><option value="oracionFinal">Oración final</option></select>`) : `<input id="assign-field" type="hidden" value="NUEVO_DISCURSO">`}
      </form>
    `,
    footer: `
      <button class="secondary-button" data-action="cancel" type="button">Cancelar</button>
      <button class="primary-button" form="assign-form" type="submit">Asignar</button>
    `,
    bind: (dialog) => {
      dialog.querySelector("#assign-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        await withToastError(async () => {
          const last = tab === "talks" ? ranking.ultimaVezDiscurso : ranking.ultimaVezOracion;
          if (last && rankColor(last, tab) === "rank-red" && !confirm(`${ranking.hermano.nombre} participó hace solo ${daysSince(last)} días. ¿Asignar igual?`)) return;
          const agendaId = dialog.querySelector("#assign-agenda").value;
          const fieldName = dialog.querySelector("#assign-field").value;
          const agenda = state.agendas.find((item) => item.id === agendaId);
          let previousName = "";
          let agendasAfterAssignment = state.agendas;
          if (!agenda) throw new Error("No se encontró la agenda.");
          if (fieldName === "NUEVO_DISCURSO") {
            const mensajesEvangelio = [
              ...agenda.mensajesEvangelio,
              { tipo: "DISCURSO", nombre: ranking.hermano.nombre, himnoNumero: 0, himnoNombre: "" }
            ];
            await updateDoc(agendaRef(agendaId), {
              mensajesEvangelio,
              ultimaEdicionPor: userEmail(),
              ultimaEdicionEn: serverTimestamp()
            });
            agendasAfterAssignment = state.agendas.map((item) =>
              item.id === agendaId ? { ...item, mensajesEvangelio } : item
            );
          } else {
            previousName = agenda[fieldName] || "";
            await updateDoc(agendaRef(agendaId), {
              [fieldName]: ranking.hermano.nombre,
              ultimaEdicionPor: userEmail(),
              ultimaEdicionEn: serverTimestamp()
            });
            agendasAfterAssignment = state.agendas.map((item) =>
              item.id === agendaId ? { ...item, [fieldName]: ranking.hermano.nombre } : item
            );
          }
          await syncHermanoParticipation(ranking.hermano.nombre, agenda.fecha, tab);
          if (fieldName !== "NUEVO_DISCURSO" && normalizeName(previousName) !== normalizeName(ranking.hermano.nombre)) {
            await rebuildHermanoParticipation(previousName, agenda.fecha, tab, agendasAfterAssignment);
          }
          closeModal();
          toastMessage("Asignado");
        });
      });
    }
  });
}

function openAssignTopicDialog(suggestion) {
  if (!suggestion) return;
  const candidateAgendas = state.agendas
    .filter((agenda) => agenda.estado !== "REALIZADA")
    .sort((a, b) => a.fecha - b.fecha);
  if (!candidateAgendas.length) {
    toastMessage("No hay agendas borrador o confirmadas.");
    return;
  }

  openModal({
    title: `Asignar tema: ${suggestion.etiqueta}`,
    body: `
      <form id="assign-topic-form" class="form-grid">
        ${field("topic-agenda", "Agenda", `<select id="topic-agenda" class="select">${candidateAgendas.map((agenda) => option(agenda.id, formatDateLong(agenda.fecha), candidateAgendas[0].id)).join("")}</select>`)}
        ${field("topic-discourse", "Discurso asignado", `<select id="topic-discourse" class="select"></select>`)}
        ${field("topic-exact", "Tema exacto", `<input id="topic-exact" class="input" value="${escapeAttr(suggestion.etiqueta)}">`)}
        ${field("topic-tags-input", "Etiquetas", `<input id="topic-tags-input" class="input" value="${escapeAttr(suggestion.etiqueta)}">`, "Separá varias etiquetas con comas.")}
      </form>
    `,
    footer: `
      <button class="secondary-button" data-action="cancel" type="button">Cancelar</button>
      <button class="primary-button" form="assign-topic-form" type="submit">Asignar</button>
    `,
    bind: (dialog) => {
      const agendaSelect = dialog.querySelector("#topic-agenda");
      const discourseSelect = dialog.querySelector("#topic-discourse");
      const topicInput = dialog.querySelector("#topic-exact");
      const tagsInput = dialog.querySelector("#topic-tags-input");

      const refreshDiscourses = () => {
        const agenda = candidateAgendas.find((item) => item.id === agendaSelect.value);
        const discourses = agenda?.mensajesEvangelio
          ?.map((message, index) => ({ message, index }))
          .filter((item) => item.message.tipo === "DISCURSO" && item.message.nombre)
          || [];
        discourseSelect.innerHTML = discourses.length
          ? discourses.map((item) => option(String(item.index), discourseOptionLabel(item.message), discourseSelect.value)).join("")
          : `<option value="">No hay discursos asignados</option>`;
        discourseSelect.disabled = !discourses.length;
        const selected = discourses.find((item) => String(item.index) === discourseSelect.value) || discourses[0];
        if (selected?.message?.tema) topicInput.value = selected.message.tema;
        if (selected?.message?.etiquetaTema) tagsInput.value = selected.message.etiquetaTema;
      };

      agendaSelect.addEventListener("change", refreshDiscourses);
      discourseSelect.addEventListener("change", refreshDiscourses);
      refreshDiscourses();

      dialog.querySelector("#assign-topic-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        await withToastError(async () => {
          const agenda = candidateAgendas.find((item) => item.id === agendaSelect.value);
          const index = Number(discourseSelect.value);
          if (!agenda || !Number.isInteger(index)) throw new Error("Selecciona una agenda y un discurso.");
          const messages = [...agenda.mensajesEvangelio];
          const message = messages[index];
          if (!message || message.tipo !== "DISCURSO") throw new Error("El mensaje seleccionado no es un discurso.");
          const tema = topicInput.value.trim();
          const etiquetaTema = tagsInput.value.trim() || suggestion.etiqueta || tema;
          messages[index] = { ...message, tema, etiquetaTema };
          await updateDoc(agendaRef(agenda.id), {
            mensajesEvangelio: messages,
            ultimaEdicionPor: userEmail(),
            ultimaEdicionEn: serverTimestamp()
          });
          closeModal();
          toastMessage("Tema asignado");
        });
      });
    }
  });
}

function discourseOptionLabel(message) {
  return message.tema ? `${message.nombre} - ${message.tema}` : message.nombre;
}

function openTopicTagsDialog() {
  openModal({
    title: "Etiquetas de temas",
    body: `
      <form id="topic-tags-form" class="form-grid">
        ${field("topic-tags-list", "Etiquetas", `<textarea id="topic-tags-list" class="textarea">${escapeHtml(topicTagsAvailable().join("\n"))}</textarea>`, "Escribi una etiqueta por linea o separalas con comas.")}
      </form>
    `,
    footer: `
      <button class="secondary-button" data-action="cancel" type="button">Cancelar</button>
      <button class="primary-button" form="topic-tags-form" type="submit">Guardar</button>
    `,
    bind: (dialog) => {
      dialog.querySelector("#topic-tags-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        const tags = uniqueCleanList(dialog.querySelector("#topic-tags-list").value.split(/[\n,]/));
        await savePlanningConfig({ etiquetasTema: tags });
        closeModal();
        toastMessage("Etiquetas guardadas");
        renderPlanning();
      });
    }
  });
}

async function toggleInactive(ranking) {
  if (!ranking) return;
  await withToastError(async () => {
    const tab = state.planningTab;
    const fieldName = tab === "talks" ? "inactivoDiscurso" : "inactivoOracion";
    const nextValue = !ranking.hermano[fieldName];
    if (ranking.hermano.id) {
      await updateDoc(hermanoRef(ranking.hermano.id), { [fieldName]: nextValue });
    } else {
      await addDoc(collection(db, "hermanos"), {
        numeroUnidad: state.unitNumber,
        nombre: ranking.hermano.nombre,
        agregadoManualmente: true,
        excluido: false,
        inactivoDiscurso: fieldName === "inactivoDiscurso" ? nextValue : false,
        inactivoOracion: fieldName === "inactivoOracion" ? nextValue : false,
        creadoEn: serverTimestamp()
      });
    }
  });
}

async function deleteBrother(ranking) {
  if (!ranking || !confirm(`¿Eliminar a ${ranking.hermano.nombre} del planificador?`)) return;
  await withToastError(async () => {
    if (ranking.hermano.id) {
      await deleteDoc(hermanoRef(ranking.hermano.id));
    } else {
      await addDoc(collection(db, "hermanos"), {
        numeroUnidad: state.unitNumber,
        nombre: ranking.hermano.nombre,
        agregadoManualmente: false,
        excluido: true,
        creadoEn: serverTimestamp()
      });
    }
    toastMessage("Eliminado");
  });
}

function openPlanningConfigDialog() {
  openModal({
    title: "Configuración de colores",
    body: `
      <form id="planning-config-form" class="form-grid">
        <div class="inline-fields">
          ${configColorField("green-topic", "Temas: verde desde dias", "rank-green", state.config.diasVerdeTema || 180)}
          ${configColorField("yellow-topic", "Temas: amarillo desde dias", "rank-yellow", state.config.diasAmarilloTema || 90)}
        </div>
        <div class="inline-fields">
          ${configColorField("green-talk", "Discursos: verde desde días", "rank-green", state.config.diasVerdeDiscurso)}
          ${configColorField("yellow-talk", "Discursos: amarillo desde días", "rank-yellow", state.config.diasAmarilloDiscurso)}
        </div>
        <div class="inline-fields">
          ${configColorField("green-prayer", "Oraciones: verde desde días", "rank-green", state.config.diasVerdeOracion)}
          ${configColorField("yellow-prayer", "Oraciones: amarillo desde días", "rank-yellow", state.config.diasAmarilloOracion)}
        </div>
      </form>
    `,
    footer: `
      <button class="secondary-button" data-action="cancel" type="button">Cancelar</button>
      <button class="primary-button" form="planning-config-form" type="submit">Guardar</button>
    `,
    bind: (dialog) => {
      dialog.querySelector("#planning-config-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        await withToastError(async () => {
          await savePlanningConfig({
            diasVerdeDiscurso: Number(dialog.querySelector("#green-talk").value || 90),
            diasAmarilloDiscurso: Number(dialog.querySelector("#yellow-talk").value || 30),
            diasVerdeOracion: Number(dialog.querySelector("#green-prayer").value || 30),
            diasAmarilloOracion: Number(dialog.querySelector("#yellow-prayer").value || 14),
            diasVerdeTema: Number(dialog.querySelector("#green-topic").value || 180),
            diasAmarilloTema: Number(dialog.querySelector("#yellow-topic").value || 90)
          });
          closeModal();
          toastMessage("Configuración guardada");
        });
      });
    }
  });
}

async function savePlanningConfig(partial) {
  const data = {
    ...defaultConfig,
    ...state.config,
    ...partial,
    numeroUnidad: state.unitNumber
  };
  if (state.configId) await setDoc(configRef(state.configId), data);
  else await addDoc(collection(db, "configuracion"), data);
}

function planningRankings() {
  const excluded = new Set(
    state.hermanos
      .filter((item) => item.excluido === true)
      .map((item) => normalizeName(item.nombre))
  );
  const map = new Map();
  state.hermanos
    .filter((item) => item.excluido !== true)
    .forEach((item) => map.set(normalizeName(item.nombre), { ...item }));
  collectHistoryNames().forEach((name) => {
    const key = normalizeName(name);
    if (key && !excluded.has(key) && !map.has(key)) {
      map.set(key, { id: "", numeroUnidad: state.unitNumber, nombre: name });
    }
  });
  return [...map.entries()].map(([key, hermano]) => {
    const talkDates = [];
    const prayerDates = [];
    let ultimoTemaDiscurso = "";
    let ultimaEtiquetaDiscurso = "";
    let ultimaFechaTemaDiscurso = null;
    state.agendas.forEach((agenda) => {
      const agendaDate = toDate(agenda.fecha);
      agenda.mensajesEvangelio.forEach((message) => {
        if (message.tipo !== "HIMNO_INTERMEDIO" && normalizeName(message.nombre) === key) talkDates.push(agenda.fecha);
        if (message.tipo !== "DISCURSO" || normalizeName(message.nombre) !== key || !agendaDate) return;
        const topic = (message.tema || "").trim();
        const labels = topicLabelsFromText(message.etiquetaTema, topic);
        if (!topic && !labels.length) return;
        if (!ultimaFechaTemaDiscurso || agendaDate > ultimaFechaTemaDiscurso) {
          ultimaFechaTemaDiscurso = agendaDate;
          ultimoTemaDiscurso = topic;
          ultimaEtiquetaDiscurso = labels.join(", ");
        }
      });
      if (normalizeName(agenda.primeraOracion) === key) prayerDates.push(agenda.fecha);
      if (normalizeName(agenda.oracionFinal) === key) prayerDates.push(agenda.fecha);
    });
    if (hermano.ultimaVezDiscursoManual) talkDates.push(toDate(hermano.ultimaVezDiscursoManual));
    if (hermano.ultimaVezOracionManual) prayerDates.push(toDate(hermano.ultimaVezOracionManual));
    return {
      key,
      hermano,
      ultimaVezDiscurso: latestDate(talkDates),
      ultimaVezOracion: latestDate(prayerDates),
      ultimoTemaDiscurso,
      ultimaEtiquetaDiscurso,
      vecesDiscurso90Dias: countWithinDays(talkDates, 90),
      vecesOracion90Dias: countWithinDays(prayerDates, 90)
    };
  }).sort((a, b) => a.hermano.nombre.localeCompare(b.hermano.nombre));
}

function collectHistoryNames() {
  const names = new Set();
  state.agendas.forEach((agenda) => {
    [agenda.preside, agenda.dirige, agenda.primeraOracion, agenda.oracionFinal, agenda.directorMusica, agenda.pianista].forEach((name) => {
      if (name) names.add(name.trim());
    });
    agenda.mensajesEvangelio.forEach((message) => {
      if (message.tipo !== "HIMNO_INTERMEDIO" && message.nombre) names.add(message.nombre.trim());
    });
    agenda.testimonios.forEach((name) => {
      if (name) names.add(name.trim());
    });
  });
  return [...names];
}

function usedNames() {
  return [...new Set([...collectHistoryNames(), ...state.hermanos.map((item) => item.nombre).filter(Boolean)])].sort((a, b) => a.localeCompare(b));
}

function isInactiveForTab(item, tab) {
  return tab === "talks" ? Boolean(item.hermano.inactivoDiscurso) : Boolean(item.hermano.inactivoOracion);
}

function daysFor(item, tab) {
  const date = tab === "talks" ? item.ultimaVezDiscurso : item.ultimaVezOracion;
  return date ? daysSince(date) : Number.POSITIVE_INFINITY;
}

function rankColor(date, tab) {
  if (!date) return "rank-muted";
  const days = daysSince(date);
  const green = tab === "talks" ? state.config.diasVerdeDiscurso : state.config.diasVerdeOracion;
  const yellow = tab === "talks" ? state.config.diasAmarilloDiscurso : state.config.diasAmarilloOracion;
  if (days >= green) return "";
  if (days >= yellow) return "rank-yellow";
  return "rank-red";
}

function renderSettings() {
  screen.innerHTML = `
    <div class="screen-grid two-col">
      <section class="panel">
        ${sectionTitle("Unidad", "U", "")}
        <p class="muted">Unidad activa: <strong>${escapeHtml(state.unitNumber)}</strong></p>
        <form id="password-form" class="form-grid">
          ${field("new-password", "Nueva contraseña", `<input id="new-password" class="input" type="password" minlength="4" required>`)}
          <button class="primary-button" type="submit">Cambiar contraseña</button>
        </form>
      </section>
      <section class="panel">
        ${sectionTitle("Apariencia", "T", "")}
        <div class="segmented">
          <button class="filter-chip ${state.theme === "system" ? "active" : ""}" data-theme="system" type="button">Sistema</button>
          <button class="filter-chip ${state.theme === "light" ? "active" : ""}" data-theme="light" type="button">Claro</button>
          <button class="filter-chip ${state.theme === "dark" ? "active" : ""}" data-theme="dark" type="button">Oscuro</button>
        </div>
      </section>
    </div>
  `;
  screen.querySelector("#password-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    await withToastError(async () => {
      const password = screen.querySelector("#new-password").value;
      if (password.length < 4) throw new Error("La contraseña debe tener al menos 4 caracteres.");
      await updateDoc(unitRef(state.unitNumber), { passwordHash: await hashPassword(password) });
      toastMessage("Contraseña actualizada");
      event.target.reset();
    });
  });
  screen.querySelectorAll("[data-theme]").forEach((button) => {
    button.addEventListener("click", () => {
      state.theme = button.dataset.theme;
      localStorage.setItem(THEME_STORAGE_KEY, state.theme);
      applyTheme();
      renderSettings();
    });
  });
}

function openCreateSundaysDialog() {
  openModal({
    title: "Crear domingos en blanco",
    body: `
      <form id="sundays-form" class="form-grid">
        ${field("until-date", "Hasta el domingo", `<input id="until-date" class="input" type="date" value="${dateInputValue(addDays(nextSunday(), 28))}" required>`)}
      </form>
    `,
    footer: `
      <button class="secondary-button" data-action="cancel" type="button">Cancelar</button>
      <button class="primary-button" form="sundays-form" type="submit">Crear</button>
    `,
    bind: (dialog) => {
      dialog.querySelector("#sundays-form").addEventListener("submit", async (event) => {
        event.preventDefault();
        await withToastError(async () => {
          const until = dateFromInput(dialog.querySelector("#until-date").value);
          const created = await createSundaysUntil(until);
          closeModal();
          toastMessage(`${created} agenda(s) creada(s)`);
        });
      });
    }
  });
}

async function createSundaysUntil(untilDate) {
  const existing = new Set(state.agendas.map((agenda) => dateInputValue(agenda.fecha)));
  let current = nextSunday();
  let created = 0;
  while (current <= endOfDay(untilDate)) {
    const key = dateInputValue(current);
    if (!existing.has(key)) {
      await addDoc(collection(db, "agendas"), agendaToFirestore(createBlankAgenda(current), null));
      created += 1;
    }
    current = addDays(current, 7);
  }
  return created;
}

function openModal({ title, body, footer, bind }) {
  modal.className = "modal";
  modal.innerHTML = `
    <div class="modal-content">
      <div class="modal-header">
        <div>
          <p class="eyebrow">Agenda Sacramental</p>
          <h2>${escapeHtml(title)}</h2>
        </div>
        <button class="icon-button" data-action="cancel" type="button" title="Cerrar">X</button>
      </div>
      ${body}
      <div class="button-row">${footer}</div>
    </div>
  `;
  modal.querySelectorAll('[data-action="cancel"]').forEach((button) => button.addEventListener("click", closeModal));
  bind?.(modal);
  modal.showModal();
}

function closeModal() {
  modal.close();
  modal.innerHTML = "";
}

function sectionTitle(title, icon, trailing) {
  return `
    <div class="section-title">
      <div class="section-title-left">
        <h3>${escapeHtml(title)}</h3>
      </div>
      <div>${trailing || ""}</div>
    </div>
  `;
}

function metricPill(label, value) {
  return `
    <div class="metric-pill">
      <span class="metric-label">${escapeHtml(label)}</span>
      <strong class="metric-value">${escapeHtml(value)}</strong>
    </div>
  `;
}

function emptyState(text) {
  return `<p class="muted">${escapeHtml(text)}</p>`;
}

function emptyPanel(text) {
  return `<div class="empty-panel"><strong>${escapeHtml(text)}</strong></div>`;
}

function loadingPanel(text) {
  return `<div class="loading-panel"><div class="spinner"></div><span>${escapeHtml(text)}</span></div>`;
}

function renderFatalError(error) {
  const message = formatErrorMessage(error) || "Revisa Firebase y los permisos de Firestore.";
  appShell.classList.add("setup-mode");
  screen.innerHTML = `
    <div class="setup-wrap">
      <section class="setup-panel">
        <h2>No se pudo conectar</h2>
        <p class="muted">${escapeHtml(message)}</p>
      </section>
    </div>
  `;
}

async function withToastError(task) {
  try {
    await task();
  } catch (error) {
    console.error(error);
    toastMessage(formatErrorMessage(error) || "No se pudo completar la acción.", 7000);
  }
}

function formatErrorMessage(error) {
  if (error?.code === "auth/unauthorized-domain") {
    return `Este dominio no está autorizado en Firebase. Agrega ${GITHUB_PAGES_DOMAIN} en Authentication > Settings > Authorized domains.`;
  }
  if (error?.code === "auth/popup-closed-by-user") return "Inicio de sesión cancelado.";
  if (error?.code === "permission-denied") return "Firebase rechazó la operación. Revisa las reglas de Firestore.";
  return error?.message || "";
}

function toastMessage(message, duration = 2800) {
  toast.textContent = message;
  toast.classList.add("visible");
  clearTimeout(toastMessage.timeoutId);
  toastMessage.timeoutId = setTimeout(() => toast.classList.remove("visible"), duration);
}

function normalizeAgenda(id, data) {
  return {
    id,
    numeroUnidad: data.numeroUnidad || "",
    fecha: toDate(data.fecha) || new Date(),
    estado: data.estado || "BORRADOR",
    asistencia: Number(data.asistencia || 0),
    preside: data.preside || "",
    dirige: data.dirige || "",
    reconocimientos: data.reconocimientos || "",
    anuncios: data.anuncios || "",
    primerHimnoNumero: Number(data.primerHimnoNumero || 0),
    primerHimnoNombre: data.primerHimnoNombre || "",
    directorMusica: data.directorMusica || "",
    pianista: data.pianista || "",
    himnoSacramentalNumero: Number(data.himnoSacramentalNumero || 0),
    himnoSacramentalNombre: data.himnoSacramentalNombre || "",
    himnoFinalNumero: Number(data.himnoFinalNumero || 0),
    himnoFinalNombre: data.himnoFinalNombre || "",
    primeraOracion: data.primeraOracion || "",
    oracionFinal: data.oracionFinal || "",
    asuntosEstacaBarrio: data.asuntosEstacaBarrio || [],
    mensajesEvangelio: data.mensajesEvangelio || [],
    reunionTestimonios: Boolean(data.reunionTestimonios),
    testimonios: data.testimonios || [],
    creadoPor: data.creadoPor || "",
    creadoEn: data.creadoEn || null,
    ultimaEdicionPor: data.ultimaEdicionPor || "",
    ultimaEdicionEn: data.ultimaEdicionEn || null
  };
}

function createBlankAgenda(date) {
  return {
    id: "",
    numeroUnidad: state.unitNumber,
    fecha: new Date(date),
    estado: "BORRADOR",
    asistencia: 0,
    preside: "",
    dirige: "",
    reconocimientos: "",
    anuncios: "",
    primerHimnoNumero: 0,
    primerHimnoNombre: "",
    directorMusica: "",
    pianista: "",
    himnoSacramentalNumero: 0,
    himnoSacramentalNombre: "",
    himnoFinalNumero: 0,
    himnoFinalNombre: "",
    primeraOracion: "",
    oracionFinal: "",
    asuntosEstacaBarrio: [],
    mensajesEvangelio: [],
    reunionTestimonios: isFirstSunday(date),
    testimonios: [],
    creadoPor: userEmail(),
    creadoEn: null,
    ultimaEdicionPor: userEmail(),
    ultimaEdicionEn: null
  };
}

function upcomingAgenda() {
  const today = startOfDay(new Date());
  return state.agendas
    .filter((agenda) => agenda.fecha >= today && agenda.estado !== "REALIZADA")
    .sort((a, b) => a.fecha - b.fecha)[0] || null;
}

function agendaStats() {
  return {
    total: state.agendas.length,
    draft: state.agendas.filter((item) => item.estado === "BORRADOR").length,
    confirmed: state.agendas.filter((item) => item.estado === "CONFIRMADA").length
  };
}

function labelState(value) {
  return {
    BORRADOR: "Borrador",
    CONFIRMADA: "Confirmada",
    REALIZADA: "Realizada"
  }[value] || value;
}

function labelBusiness(value) {
  return {
    RELEVO: "Relevo",
    SOSTENIMIENTO: "Sostenimiento",
    SOSTENIMIENTO_OFICIALES: "Sostenimiento de Oficiales",
    ESTACA: "Estaca",
    ORDENACION_AARONICA: "Ordenacion",
    OTROS: "Otros"
  }[value] || value;
}

function labelMessage(value) {
  return {
    DISCURSO: "Discurso",
    TESTIMONIO: "Testimonio",
    HIMNO_INTERMEDIO: "Himno intermedio"
  }[value] || value;
}

function businessBlocksInOrder(items, allowedTypes = null) {
  const allowed = allowedTypes ? new Set(allowedTypes) : null;
  const groupAcrossAgenda = new Set(["RELEVO", "SOSTENIMIENTO"]);
  const blocksByType = new Map();
  const blocks = [];
  (items || []).forEach((item) => {
    if (!item || allowed?.has(item.tipo) === false) return;
    if (item.tipo === "OTROS" && !item.columna2?.trim()) return;
    if (!item.columna2?.trim() && !item.columna3?.trim()) return;

    if (groupAcrossAgenda.has(item.tipo)) {
      if (!blocksByType.has(item.tipo)) {
        const block = [];
        blocksByType.set(item.tipo, block);
        blocks.push(block);
      }
      blocksByType.get(item.tipo).push(item);
      return;
    }

    const last = blocks[blocks.length - 1];
    if (last && last[0]?.tipo === item.tipo) last.push(item);
    else blocks.push([item]);
  });
  return blocks;
}

function businessFormulaLabel(type) {
  return {
    RELEVO: "RELEVOS",
    SOSTENIMIENTO: "SOSTENIMIENTOS",
    SOSTENIMIENTO_OFICIALES: "SOSTENIMIENTO DE OFICIALES",
    ESTACA: "ESTACA",
    ORDENACION_AARONICA: "ORDENACIÓN AL SACERDOCIO AARÓNICO",
    OTROS: "OTROS"
  }[type] || labelBusiness(type);
}

function liturgicalBusinessText(type, items) {
  const clean = (items || []).filter((item) => item?.columna2?.trim() || item?.columna3?.trim());
  if (!clean.length) return "";
  if (type === "OTROS") return clean.map((item) => item.columna2?.trim()).filter(Boolean).join("\n");
  if (type === "ESTACA") return clean.map(descripcionAsuntoEstaca).join("\n");
  if (type === "SOSTENIMIENTO_OFICIALES") return clean.map(descripcionSostenimientoOficiales).join("\n");
  if (type === "ORDENACION_AARONICA") return clean.map(aaronicOrdinationFormula).join("\n");
  if (type === "RELEVO") return releaseFormula(clean);
  if (type === "SOSTENIMIENTO") return sustainingFormula(clean);
  return clean.map(businessDescription).filter(Boolean).join("\n");
}

function releaseFormula(items) {
  if (items.length === 1) {
    const item = items[0];
    return `"${businessName(item)} ha sido relevado como ${businessRole(item)}. Quienes deseen expresar agradecimiento por su servicio, sírvanse hacerlo levantando la mano."`;
  }
  return `Los siguientes hermanos/as han sido relevados de sus llamamientos: "${businessList(items)}. Quienes deseen expresar agradecimiento por su servicio, sírvanse hacerlo levantando la mano."`;
}

function sustainingFormula(items) {
  if (items.length === 1) {
    const item = items[0];
    return `"${businessName(item)} ha sido llamado como ${businessRole(item)}. Los que estén a favor de sostenerlo, sírvanse hacerlo levantando la mano. [Breve pausa]. Opuestos, si los hay, también pueden manifestarlo. [Breve pausa]."`;
  }
  return `Los siguientes hermanos/as han sido llamados a los siguientes llamamientos: "${businessList(items)}. Los que estén a favor de sostenerlos, sírvanse hacerlo levantando la mano. [Breve pausa]. Opuestos, si los hay, también pueden manifestarlo. [Breve pausa]."`;
}

function aaronicOrdinationFormula(item) {
  return `Pedirle al hermano que se ponga de pie.\n"Proponemos que ${businessName(item)} reciba el Sacerdocio de Aarón y que sea ordenado ${oficioParaFormula(item.columna3)}. Los que estén a favor, sírvanse indicarlo levantando la mano. [Breve pausa]. Opuestos, si los hay, también pueden manifestarlo. [Breve pausa]"`;
}

function businessList(items) {
  const groups = [];
  const groupsByRole = new Map();
  (items || []).forEach((item) => {
    const name = businessName(item);
    const role = businessRole(item);
    const roleKey = comparableText(role);
    if (!groupsByRole.has(roleKey)) {
      const group = { role, names: [], nameKeys: new Set() };
      groupsByRole.set(roleKey, group);
      groups.push(group);
    }
    const group = groupsByRole.get(roleKey);
    const nameKey = comparableText(name);
    if (!group.nameKeys.has(nameKey)) {
      group.nameKeys.add(nameKey);
      group.names.push(name);
    }
  });
  return groups.map((group) => {
    const role = group.names.length > 1 ? pluralizeCallingRole(group.role) : group.role;
    return `${joinFormulaNames(group.names)} como ${role}`;
  }).join("; ");
}

function businessName(item) {
  return item?.columna2?.trim() || "[Nombre]";
}

function businessRole(item) {
  return item?.columna3?.trim() || "[Llamamiento]";
}

function comparableText(value) {
  return (value || "")
    .trim()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/\s+/g, " ");
}

function joinFormulaNames(names) {
  if (!names.length) return "[Nombre]";
  if (names.length === 1) return names[0];
  if (names.length === 2) return `${names[0]} y ${names[1]}`;
  return `${names.slice(0, -1).join(", ")} y ${names[names.length - 1]}`;
}

function pluralizeCallingRole(role) {
  const firstWord = role.match(/^\S+/)?.[0];
  if (!firstWord) return role;
  const pluralBase = CALLING_PLURALS[comparableText(firstWord)];
  if (!pluralBase) return role;
  return applyCapitalization(pluralBase, firstWord) + role.slice(firstWord.length);
}

function applyCapitalization(value, reference) {
  const letters = reference.replace(/[^A-Za-zÁÉÍÓÚÜÑáéíóúüñ]/g, "");
  if (letters && letters === letters.toUpperCase()) return value.toUpperCase();
  if (reference[0] === reference[0]?.toUpperCase()) {
    return value.charAt(0).toUpperCase() + value.slice(1);
  }
  return value;
}

function descripcionAsuntoEstaca(item) {
  const nombre = item?.columna2?.trim() || "[nombre]";
  return `El ${nombre} tomará tiempo para asuntos de la estaca.`;
}

function descripcionSostenimientoOficiales(item) {
  const nombre = item?.columna2?.trim() || "[Nombre del líder]";
  return `${nombre} tomará un tiempo para el sostenimiento de oficiales.`;
}

function oficioParaFormula(oficio) {
  const clean = oficio?.trim();
  const labels = {
    Diacono: "diácono",
    Maestro: "maestro",
    Presbitero: "presbítero"
  };
  return labels[clean] || (clean ? clean.charAt(0).toLowerCase() + clean.slice(1) : "[Oficio]");
}

function businessDescription(item) {
  if (!item) return "";
  if (item.tipo === "ESTACA") return descripcionAsuntoEstaca(item);
  if (item.tipo === "SOSTENIMIENTO_OFICIALES") return descripcionSostenimientoOficiales(item);
  if (item.tipo === "ORDENACION_AARONICA") return [item.columna2, item.columna3].filter(Boolean).join(" - ");
  if (item.tipo === "OTROS") return item.columna2 || "";
  return [item.columna2, item.columna3].filter(Boolean).join(" - ");
}

function messageText(item) {
  if (item.tipo === "HIMNO_INTERMEDIO") return `Himno intermedio - ${hymnLabel(item.himnoNumero, item.himnoNombre)}`;
  if (item.tipo === "DISCURSO") {
    const details = [
      item.nombre,
      item.tema ? `Tema: ${item.tema}` : "",
      item.etiquetaTema ? `Etiquetas: ${item.etiquetaTema}` : ""
    ].filter(Boolean).join(" - ");
    return `${labelMessage(item.tipo)} - ${details || "Sin datos"}`;
  }
  return `${labelMessage(item.tipo)} - ${item.nombre || "Sin datos"}`;
}

function agendaText(agenda) {
  const lines = [
    `Agenda Reunión Sacramental - ${formatDateLong(agenda.fecha)}`,
    `Preside: ${agenda.preside || "Sin datos"}`,
    `Dirige: ${agenda.dirige || "Sin datos"}`,
    agenda.reconocimientos ? `Reconocimientos: ${agenda.reconocimientos}` : "",
    agenda.anuncios ? `Anuncios: ${agenda.anuncios}` : "",
    `Himno de apertura: ${hymnLabel(agenda.primerHimnoNumero, agenda.primerHimnoNombre)}`,
    `Primera oración: ${agenda.primeraOracion || "Sin datos"}`,
    agenda.asuntosEstacaBarrio?.length
      ? `Asuntos: ${agenda.asuntosEstacaBarrio.map((item) => `${labelBusiness(item.tipo)} - ${businessDescription(item)}`).join("; ")}`
      : "",
    `Himno Sacramental: ${hymnLabel(agenda.himnoSacramentalNumero, agenda.himnoSacramentalNombre)}`,
    agenda.reunionTestimonios
      ? `Testimonios: ${(agenda.testimonios || []).join(", ") || "Sin datos"}`
      : `Mensajes: ${agenda.mensajesEvangelio.map((item) => messageText(item)).join("; ") || "Sin datos"}`,
    `Himno final: ${hymnLabel(agenda.himnoFinalNumero, agenda.himnoFinalNombre)}`,
    `Oración final: ${agenda.oracionFinal || "Sin datos"}`
  ].filter(Boolean);
  return lines.join("\n");
}

function hymnLabel(number, name) {
  if (!number && !name) return "Sin datos";
  return `${number ? `${number} - ` : ""}${name || hymnName(number) || ""}`.trim();
}

function hymnName(number) {
  const key = Number(number || 0);
  return HYMNS_ES[key] || HYMNS_EN[key] || "";
}

function unitRef(unitNumber) {
  return doc(db, "unidades", unitNumber);
}

function agendaRef(agendaId) {
  return doc(db, "agendas", agendaId);
}

function hermanoRef(hermanoId) {
  return doc(db, "hermanos", hermanoId);
}

function configRef(configId) {
  return doc(db, "configuracion", configId);
}

function userEmail() {
  return state.user?.email || state.user?.displayName || "";
}

async function hashPassword(password) {
  const bytes = new TextEncoder().encode(password);
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return [...new Uint8Array(digest)].map((byte) => byte.toString(16).padStart(2, "0")).join("");
}

function applyTheme() {
  const prefersDark = window.matchMedia?.("(prefers-color-scheme: dark)")?.matches;
  const resolved = state.theme === "system" ? (prefersDark ? "dark" : "light") : state.theme;
  document.documentElement.dataset.theme = resolved;
}

function latestDate(dates) {
  const clean = dates.map(toDate).filter(Boolean);
  return clean.length ? new Date(Math.max(...clean.map((date) => date.getTime()))) : null;
}

function countWithinDays(dates, days) {
  const now = startOfDay(new Date());
  return dates.map(toDate).filter((date) => date && (now - startOfDay(date)) / 86400000 <= days).length;
}

function daysSince(date) {
  if (!date) return Number.POSITIVE_INFINITY;
  return Math.max(0, daysFromToday(date));
}

function daysFromToday(date) {
  const value = toDate(date);
  if (!value) return Number.POSITIVE_INFINITY;
  return calendarDayIndex(new Date()) - calendarDayIndex(value);
}

function calendarDayIndex(date) {
  const value = toDate(date);
  return Math.floor(Date.UTC(value.getFullYear(), value.getMonth(), value.getDate()) / 86400000);
}

function toDate(value) {
  if (!value) return null;
  if (value instanceof Date) return value;
  if (typeof value.toDate === "function") return value.toDate();
  if (value.seconds) return new Date(value.seconds * 1000);
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

function timestampOrNull(value) {
  return value ? Timestamp.fromDate(dateFromInput(value)) : null;
}

function dateFromInput(value) {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day, 12, 0, 0);
}

function dateInputValue(date) {
  const value = toDate(date);
  if (!value) return "";
  return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, "0")}-${String(value.getDate()).padStart(2, "0")}`;
}

function formatDateLong(date) {
  return new Intl.DateTimeFormat("es-UY", { weekday: "long", day: "numeric", month: "long", year: "numeric" }).format(toDate(date));
}

function formatDateShort(date) {
  return new Intl.DateTimeFormat("es-UY", { day: "2-digit", month: "2-digit", year: "numeric" }).format(toDate(date));
}

function startOfDay(date) {
  const value = toDate(date);
  return new Date(value.getFullYear(), value.getMonth(), value.getDate());
}

function endOfDay(date) {
  const value = toDate(date);
  return new Date(value.getFullYear(), value.getMonth(), value.getDate(), 23, 59, 59);
}

function addDays(date, days) {
  const value = new Date(date);
  value.setDate(value.getDate() + days);
  return value;
}

function nextSunday() {
  const date = startOfDay(new Date());
  while (date.getDay() !== 0) date.setDate(date.getDate() + 1);
  date.setHours(12, 0, 0, 0);
  return date;
}

function isFirstSunday(date) {
  const value = toDate(date);
  return value.getDay() === 0 && value.getDate() <= 7;
}

function sameDay(a, b) {
  return dateInputValue(a) === dateInputValue(b);
}

function sameMoment(a, b) {
  const first = toDate(a);
  const second = toDate(b);
  return Boolean(first && second && first.getTime() === second.getTime());
}

function normalizeName(name) {
  return normalizeText(name).trim();
}

function normalizeText(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function escapeAttr(value) {
  return escapeHtml(value);
}
