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
const BUSINESS_TYPES = ["RELEVO", "SOSTENIMIENTO", "OTROS"];
const MESSAGE_TYPES = ["DISCURSO", "TESTIMONIO", "HIMNO_INTERMEDIO"];
const defaultConfig = {
  diasVerdeDiscurso: 90,
  diasAmarilloDiscurso: 30,
  diasVerdeOracion: 30,
  diasAmarilloOracion: 14
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
  theme: localStorage.getItem(THEME_STORAGE_KEY) || "system"
};

const appShell = document.querySelector("#app");
const screen = document.querySelector("#screen");
const modal = document.querySelector("#modal");
const toast = document.querySelector("#toast");
const sessionChip = document.querySelector("#session-chip");
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
    button.addEventListener("click", () => navigate(button.dataset.route));
  });
  document.querySelector("#change-unit").addEventListener("click", () => {
    state.unitNumber = "";
    localStorage.removeItem(UNIT_STORAGE_KEY);
    cleanupSubscriptions();
    render();
  });
  document.querySelector("#sign-out").addEventListener("click", async () => {
    await signOut(auth);
    state.unitNumber = "";
    localStorage.removeItem(UNIT_STORAGE_KEY);
    cleanupSubscriptions();
    render();
  });
  window.addEventListener("hashchange", () => {
    state.route = routeFromHash();
    state.activeAgendaId = null;
    render();
  });
}

function navigate(route) {
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
    reading: renderReadingMode
  };
  (routes[state.route] || renderAgendas)();
}

function renderChrome() {
  const setupMode = state.isBooting || !state.user || !state.unitNumber || Boolean(state.fatalError);
  appShell.classList.toggle("setup-mode", setupMode);
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
  topbarEyebrow.textContent = state.unitNumber ? `Unidad ${state.unitNumber}` : "Agenda Sacramental";
  topbarTitle.textContent = routeTitle();
}

function routeTitle() {
  if (state.route === "planning") return "Planificación";
  if (state.route === "settings") return "Ajustes";
  if (state.route === "edit") return state.activeAgendaId ? "Editar agenda" : "Nueva agenda";
  if (state.route === "reading") return "Modo lectura";
  return "Agendas";
}

function renderLogin() {
  appShell.classList.add("setup-mode");
  screen.innerHTML = `
    <div class="setup-wrap">
      <section class="setup-panel">
        <img class="login-mark" src="./assets/app-icon.webp" alt="">
        <p class="eyebrow">Agenda Sacramental Web</p>
        <h2>Entrar con Google</h2>
        <p class="muted">Usa la misma cuenta y la misma unidad que en Android para trabajar sobre los datos existentes.</p>
        <div class="button-row">
          <button id="google-login" class="primary-button" type="button">Iniciar sesión con Google</button>
        </div>
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
    <div class="setup-wrap">
      <section class="setup-panel">
        <p class="eyebrow">${escapeHtml(userEmail())}</p>
        <h2>Acceso a unidad</h2>
        <p class="muted">Ingresa el número de unidad y su contraseña. Si la unidad no existe, podrás crearla con esa contraseña.</p>
        <form id="unit-form" class="form-grid">
          <div class="inline-fields">
            <div class="field">
              <label for="unit-number">Número de unidad</label>
              <input id="unit-number" class="input" inputmode="numeric" autocomplete="off" required>
            </div>
            <div class="field">
              <label for="unit-password">Contraseña</label>
              <input id="unit-password" class="input" type="password" autocomplete="current-password" required>
            </div>
          </div>
          <div class="button-row">
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
      if (!unitNumber || !password) throw new Error("Ingresa unidad y contraseña.");
      const unitDoc = await getDoc(unitRef(unitNumber));
      const passwordHash = await hashPassword(password);
      if (!unitDoc.exists()) {
        if (!confirm(`La unidad ${unitNumber} no existe. ¿Deseas crearla?`)) return;
        await setDoc(unitRef(unitNumber), {
          numeroUnidad: unitNumber,
          passwordHash,
          creadoPor: userEmail(),
          creadoEn: serverTimestamp()
        });
      } else if (unitDoc.data().passwordHash !== passwordHash) {
        throw new Error("Contraseña incorrecta.");
      }
      activateUnit(unitNumber);
    });
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
  const nextAgenda = upcomingAgenda();
  const stats = agendaStats();
  screen.innerHTML = `
    <div class="toolbar">
      <div class="toolbar-left">
        <input id="agenda-search" class="input" type="search" placeholder="Buscar por fecha, nombre, himno..." style="width: min(420px, 100%);">
        ${stateFilter("all", "Todas", true)}
        ${stateFilter("BORRADOR", "Borrador")}
        ${stateFilter("CONFIRMADA", "Confirmada")}
        ${stateFilter("REALIZADA", "Realizada")}
      </div>
      <div class="toolbar-right">
        <button id="create-sundays" class="secondary-button" type="button">Crear domingos</button>
        <button id="new-agenda" class="primary-button" type="button">Nueva agenda</button>
      </div>
    </div>

    <div class="screen-grid">
      <section class="panel prominent">
        ${sectionTitle("Resumen", "A", "")}
        <div class="metric-grid">
          ${metricPill("Total", stats.total)}
          ${metricPill("Borrador", stats.draft)}
          ${metricPill("Confirmadas", stats.confirmed)}
        </div>
      </section>

      ${nextAgenda ? `
        <section class="panel">
          ${sectionTitle("Proximo domingo", "P", `<button class="text-button" data-open="${escapeAttr(nextAgenda.id)}" type="button">Abrir</button>`)}
          ${agendaCard(nextAgenda)}
        </section>
      ` : ""}

      <section class="panel">
        ${sectionTitle("Agendas", "L", "")}
        <div id="agenda-list" class="agenda-list">${renderAgendaList(searchValue, "all")}</div>
      </section>
    </div>
  `;
  screen.querySelector("#new-agenda").addEventListener("click", () => openEditor(null));
  screen.querySelector("#create-sundays").addEventListener("click", openCreateSundaysDialog);
  const searchInput = screen.querySelector("#agenda-search");
  const filters = [...screen.querySelectorAll("[data-filter-state]")];
  const rerender = () => {
    const active = filters.find((button) => button.classList.contains("active"))?.dataset.filterState || "all";
    screen.querySelector("#agenda-list").innerHTML = renderAgendaList(searchInput.value, active);
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

function stateFilter(value, label, active = false) {
  return `<button class="filter-chip ${active ? "active" : ""}" data-filter-state="${escapeAttr(value)}" type="button">${escapeHtml(label)}</button>`;
}

function renderAgendaList(searchValue, filterState) {
  const normalizedSearch = normalizeText(searchValue);
  const items = state.agendas.filter((agenda) => {
    const passesState = filterState === "all" || agenda.estado === filterState;
    const haystack = normalizeText([
      formatDateLong(agenda.fecha),
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
      agenda.mensajesEvangelio.map((item) => `${item.nombre} ${item.himnoNombre} ${item.himnoNumero}`).join(" ")
    ].join(" "));
    return passesState && (!normalizedSearch || haystack.includes(normalizedSearch));
  });
  if (!items.length) return emptyState("No hay agendas para mostrar.");
  return items.map(agendaCard).join("");
}

function agendaCard(agenda) {
  return `
    <article class="agenda-card">
      <div class="agenda-card-header">
        <div>
          <h3>${escapeHtml(formatDateLong(agenda.fecha))}</h3>
          <p class="item-meta">${agenda.reunionTestimonios ? "Reunion de ayuno y testimonios" : "Reunion sacramental"}</p>
        </div>
        <span class="status-pill status-${escapeAttr(agenda.estado)}">${escapeHtml(labelState(agenda.estado))}</span>
      </div>
      <div class="agenda-summary">
        <div><span>Preside</span><strong>${escapeHtml(agenda.preside || "Sin datos")}</strong></div>
        <div><span>Dirige</span><strong>${escapeHtml(agenda.dirige || "Sin datos")}</strong></div>
        <div><span>Mensajes</span><strong>${agenda.mensajesEvangelio.length || agenda.testimonios.length || 0}</strong></div>
      </div>
      <div class="agenda-card-footer">
        <span class="item-meta">Asistencia: ${Number(agenda.asistencia || 0)}</span>
        <div class="item-actions">
          <button class="secondary-button" data-read="${escapeAttr(agenda.id)}" type="button">Lectura</button>
          <button class="primary-button" data-open="${escapeAttr(agenda.id)}" type="button">Editar</button>
          <button class="icon-button" data-delete-agenda="${escapeAttr(agenda.id)}" type="button" title="Eliminar">X</button>
        </div>
      </div>
    </article>
  `;
}

function bindAgendaListActions() {
  screen.querySelectorAll("[data-open]").forEach((button) => {
    button.addEventListener("click", () => openEditor(button.dataset.open));
  });
  screen.querySelectorAll("[data-read]").forEach((button) => {
    button.addEventListener("click", () => openReading(button.dataset.read));
  });
  screen.querySelectorAll("[data-delete-agenda]").forEach((button) => {
    button.addEventListener("click", async () => {
      if (!confirm("Eliminar esta agenda?")) return;
      await withToastError(async () => {
        await deleteDoc(agendaRef(button.dataset.deleteAgenda));
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
    screen.innerHTML = emptyPanel("No se encontro la agenda.");
    return;
  }
  screen.innerHTML = `
    <form id="agenda-form" class="screen-grid">
      <section class="panel">
        ${sectionTitle(state.activeAgendaId ? "Editar agenda" : "Nueva agenda", "E", `
          <div class="button-row">
            <button class="secondary-button" data-action="back" type="button">Volver</button>
            <button class="secondary-button" data-action="reading-preview" type="button">Modo lectura</button>
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
            ${field("reconocimientos", "Reconocimientos", textarea("reconocimientos", agenda.reconocimientos))}
            ${field("anuncios", "Anuncios", textarea("anuncios", agenda.anuncios))}
          </div>
        </div>
      </section>

      <section class="panel">
        ${sectionTitle("Himnos y musica", "H", "")}
        <div class="form-grid">
          ${hymnFields("primerHimno", "Primer himno", agenda.primerHimnoNumero, agenda.primerHimnoNombre)}
          ${hymnFields("himnoSacramental", "Himno sacramental", agenda.himnoSacramentalNumero, agenda.himnoSacramentalNombre)}
          ${hymnFields("himnoFinal", "Himno final", agenda.himnoFinalNumero, agenda.himnoFinalNombre)}
          <div class="inline-fields">
            ${field("directorMusica", "Director/a de musica", textInput("directorMusica", agenda.directorMusica, "names-list"))}
            ${field("pianista", "Pianista", textInput("pianista", agenda.pianista, "names-list"))}
          </div>
          <div class="inline-fields">
            ${field("primeraOracion", "Primera oracion", textInput("primeraOracion", agenda.primeraOracion, "names-list"))}
            ${field("oracionFinal", "Oracion final", textInput("oracionFinal", agenda.oracionFinal, "names-list"))}
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
          Reunion de ayuno y testimonios
        </label>
        <div id="message-list" class="dynamic-list">${agenda.mensajesEvangelio.map(messageRow).join("")}</div>
        <div class="field">
          <label for="testimonios">Testimonios o nombres, uno por linea</label>
          <textarea id="testimonios" class="textarea">${escapeHtml((agenda.testimonios || []).join("\n"))}</textarea>
        </div>
      </section>

      <datalist id="names-list">${usedNames().map((name) => `<option value="${escapeAttr(name)}"></option>`).join("")}</datalist>
    </form>
  `;
  if (!agenda.asuntosEstacaBarrio.length) addBusinessRow();
  if (!agenda.mensajesEvangelio.length && !agenda.reunionTestimonios) addMessageRow();
  screen.querySelector("#agenda-form").addEventListener("submit", saveAgendaFromForm);
  screen.querySelector('[data-action="back"]').addEventListener("click", () => navigate("agendas"));
  screen.querySelector('[data-action="reading-preview"]').addEventListener("click", () => {
    state.activeAgendaId = state.activeAgendaId || "__draft__";
    state.route = "reading";
    state.draftReadingAgenda = readAgendaForm(agenda);
    render();
  });
  screen.querySelector("#add-business").addEventListener("click", addBusinessRow);
  screen.querySelector("#add-message").addEventListener("click", addMessageRow);
  screen.querySelectorAll("[data-remove-row]").forEach(bindRemoveRow);
  bindHymnAutoFill();
}

function field(id, label, control) {
  return `<div class="field"><label for="${escapeAttr(id)}">${escapeHtml(label)}</label>${control}</div>`;
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
  return `
    <div class="dynamic-row" data-business-row>
      <select class="select" data-business-type>${BUSINESS_TYPES.map((value) => option(value, labelBusiness(value), type)).join("")}</select>
      <input class="input" data-business-col2 value="${escapeAttr(item.columna2 || "")}" placeholder="Nombre o detalle">
      <input class="input" data-business-col3 value="${escapeAttr(item.columna3 || "")}" placeholder="Cargo / texto">
      <button class="icon-button" data-remove-row type="button" title="Eliminar">X</button>
    </div>
  `;
}

function messageRow(item = {}) {
  const type = item.tipo || "DISCURSO";
  return `
    <div class="dynamic-row message-row" data-message-row>
      <select class="select" data-message-type>${MESSAGE_TYPES.map((value) => option(value, labelMessage(value), type)).join("")}</select>
      <input class="input" data-message-name value="${escapeAttr(item.nombre || "")}" list="names-list" placeholder="Nombre">
      <input class="input" data-message-hymn-number type="number" min="0" value="${Number(item.himnoNumero || 0) || ""}" placeholder="Nro.">
      <input class="input" data-message-hymn-name value="${escapeAttr(item.himnoNombre || "")}" placeholder="Nombre del himno">
      <button class="icon-button" data-remove-row type="button" title="Eliminar">X</button>
    </div>
  `;
}

function addBusinessRow() {
  screen.querySelector("#business-list").insertAdjacentHTML("beforeend", businessRow());
  bindRemoveRow(screen.querySelector("#business-list [data-business-row]:last-child [data-remove-row]"));
}

function addMessageRow() {
  screen.querySelector("#message-list").insertAdjacentHTML("beforeend", messageRow());
  const row = screen.querySelector("#message-list [data-message-row]:last-child");
  bindRemoveRow(row.querySelector("[data-remove-row]"));
  bindMessageHymnRow(row);
}

function bindRemoveRow(button) {
  button.addEventListener("click", () => button.closest(".dynamic-row")?.remove());
}

function bindHymnAutoFill() {
  screen.querySelectorAll("[data-hymn-number]").forEach((input) => {
    input.addEventListener("change", () => {
      const target = screen.querySelector(`#${input.dataset.hymnNumber}`);
      fillHymnName(input.value, target);
    });
  });
  screen.querySelectorAll("[data-message-row]").forEach(bindMessageHymnRow);
}

function bindMessageHymnRow(row) {
  row.querySelector("[data-message-hymn-number]")?.addEventListener("change", (event) => {
    fillHymnName(event.target.value, row.querySelector("[data-message-hymn-name]"));
  });
}

function fillHymnName(number, target) {
  if (!target) return;
  const hymn = hymnName(number);
  if (hymn && (!target.value || confirm("Reemplazar el nombre del himno con el sugerido?"))) {
    target.value = hymn;
  }
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
    if (agenda.id) {
      await setDoc(agendaRef(agenda.id), data);
      toastMessage("Agenda guardada");
    } else {
      const ref = await addDoc(collection(db, "agendas"), data);
      state.activeAgendaId = ref.id;
      toastMessage("Agenda creada");
    }
    navigate("agendas");
  });
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
    asuntosEstacaBarrio: [...screen.querySelectorAll("[data-business-row]")].map((row) => ({
      tipo: row.querySelector("[data-business-type]").value,
      columna2: row.querySelector("[data-business-col2]").value.trim(),
      columna3: row.querySelector("[data-business-col3]").value.trim()
    })).filter((item) => item.columna2 || item.columna3),
    mensajesEvangelio: [...screen.querySelectorAll("[data-message-row]")].map((row) => ({
      tipo: row.querySelector("[data-message-type]").value,
      nombre: row.querySelector("[data-message-name]").value.trim(),
      himnoNumero: Number(row.querySelector("[data-message-hymn-number]").value || 0),
      himnoNombre: row.querySelector("[data-message-hymn-name]").value.trim()
    })).filter((item) => item.nombre || item.himnoNumero || item.himnoNombre),
    reunionTestimonios: screen.querySelector("#reunionTestimonios").checked,
    testimonios
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

function renderReadingMode() {
  const agenda = state.draftReadingAgenda || state.agendas.find((item) => item.id === state.activeAgendaId);
  state.draftReadingAgenda = null;
  if (!agenda) {
    screen.innerHTML = emptyPanel("No se encontro la agenda.");
    return;
  }
  screen.innerHTML = `
    <div class="toolbar">
      <div class="toolbar-left">
        <button class="secondary-button" data-action="back" type="button">Volver</button>
      </div>
      <div class="toolbar-right">
        <button class="secondary-button" data-action="copy" type="button">Copiar texto</button>
        <button class="primary-button" data-action="print" type="button">Imprimir / PDF</button>
      </div>
    </div>
    <article class="reading-page">
      ${readingHtml(agenda)}
    </article>
  `;
  screen.querySelector('[data-action="back"]').addEventListener("click", () => navigate("agendas"));
  screen.querySelector('[data-action="print"]').addEventListener("click", () => window.print());
  screen.querySelector('[data-action="copy"]').addEventListener("click", async () => {
    await navigator.clipboard.writeText(agendaText(agenda));
    toastMessage("Texto copiado");
  });
}

function readingHtml(agenda) {
  const messages = agenda.reunionTestimonios
    ? agenda.testimonios.map((name) => `<li>Testimonio: ${escapeHtml(name)}</li>`).join("")
    : agenda.mensajesEvangelio.map((message) => {
      if (message.tipo === "HIMNO_INTERMEDIO") return `<li>Himno intermedio: ${escapeHtml(hymnLabel(message.himnoNumero, message.himnoNombre))}</li>`;
      return `<li>${escapeHtml(labelMessage(message.tipo))}: ${escapeHtml(message.nombre || "Sin datos")}</li>`;
    }).join("");
  return `
    <div class="reading-title">
      <h2>Agenda Reunion Sacramental</h2>
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
    ${readingLine("Primera oracion", agenda.primeraOracion)}
    ${agenda.asuntosEstacaBarrio.length ? `<section class="reading-section"><strong>Asuntos Estaca/Barrio</strong><ul>${agenda.asuntosEstacaBarrio.map((item) => `<li>${escapeHtml(labelBusiness(item.tipo))}: ${escapeHtml([item.columna2, item.columna3].filter(Boolean).join(" - "))}</li>`).join("")}</ul></section>` : ""}
    ${readingLine("Himno Sacramental", hymnLabel(agenda.himnoSacramentalNumero, agenda.himnoSacramentalNombre))}
    <section class="reading-section"><strong>${agenda.reunionTestimonios ? "Reunion de testimonios" : "Mensajes del Evangelio"}</strong>${messages ? `<ul>${messages}</ul>` : "<p>Sin datos</p>"}</section>
    ${readingLine("Himno final", hymnLabel(agenda.himnoFinalNumero, agenda.himnoFinalNombre))}
    ${readingLine("Oracion final", agenda.oracionFinal)}
  `;
}

function readingLine(label, value) {
  return `<section class="reading-section"><strong>${escapeHtml(label)}:</strong> ${escapeHtml(value || "Sin datos")}</section>`;
}

function renderPlanning() {
  const rankings = planningRankings();
  const tab = state.planningTab;
  const list = rankings
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
        <input id="planning-search" class="input" type="search" placeholder="Buscar hermano/a..." style="width: min(340px, 100%);">
      </div>
      <div class="toolbar-right">
        <button id="planning-config" class="secondary-button" type="button">Configuración</button>
        <button id="add-brother" class="primary-button" type="button">Agregar hermano</button>
      </div>
    </div>
    <section class="panel">
      ${sectionTitle(tab === "talks" ? "Discursos" : "Oraciones", "P", "")}
      <div id="brother-list" class="agenda-list">${renderBrotherList(list, tab, "")}</div>
    </section>
  `;
  screen.querySelectorAll("[data-planning-tab]").forEach((button) => {
    button.addEventListener("click", () => {
      state.planningTab = button.dataset.planningTab;
      renderPlanning();
    });
  });
  screen.querySelector("#planning-search").addEventListener("input", (event) => {
    screen.querySelector("#brother-list").innerHTML = renderBrotherList(list, tab, event.target.value);
    bindBrotherActions();
  });
  screen.querySelector("#planning-config").addEventListener("click", openPlanningConfigDialog);
  screen.querySelector("#add-brother").addEventListener("click", () => openBrotherDialog());
  bindBrotherActions();
}

function renderBrotherList(items, tab, searchValue) {
  const q = normalizeText(searchValue);
  const filtered = items.filter((item) => !q || normalizeText(item.hermano.nombre).includes(q));
  if (!filtered.length) return emptyState("No hay hermanos en esta categoria.");
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
        <p class="item-meta">${inactive ? "Inactivo · " : ""}${last ? `Ultima participacion: ${formatDateShort(last)} (${daysSince(last)} dias)` : "Sin registros"}${count ? ` · ${count} vez/veces en 90 dias` : ""}</p>
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
          ${field("last-talk", "Ultimo discurso conocido", `<input id="last-talk" class="input" type="date" value="${dateInputValue(toDate(hermano.ultimaVezDiscursoManual))}">`)}
          ${field("last-prayer", "Ultima oracion conocida", `<input id="last-prayer" class="input" type="date" value="${dateInputValue(toDate(hermano.ultimaVezOracionManual))}">`)}
        </div>
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
        ${tab === "prayers" ? field("assign-field", "Tipo de oracion", `<select id="assign-field" class="select"><option value="primeraOracion">Primera oracion</option><option value="oracionFinal">Oracion final</option></select>`) : `<input id="assign-field" type="hidden" value="NUEVO_DISCURSO">`}
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
          if (last && rankColor(last, tab) === "rank-red" && !confirm(`${ranking.hermano.nombre} participo hace solo ${daysSince(last)} dias. Asignar igual?`)) return;
          const agendaId = dialog.querySelector("#assign-agenda").value;
          const fieldName = dialog.querySelector("#assign-field").value;
          if (fieldName === "NUEVO_DISCURSO") {
            const agenda = state.agendas.find((item) => item.id === agendaId);
            await updateDoc(agendaRef(agendaId), {
              mensajesEvangelio: [...agenda.mensajesEvangelio, { tipo: "DISCURSO", nombre: ranking.hermano.nombre, himnoNumero: 0, himnoNombre: "" }],
              ultimaEdicionPor: userEmail(),
              ultimaEdicionEn: serverTimestamp()
            });
          } else {
            await updateDoc(agendaRef(agendaId), {
              [fieldName]: ranking.hermano.nombre,
              ultimaEdicionPor: userEmail(),
              ultimaEdicionEn: serverTimestamp()
            });
          }
          closeModal();
          toastMessage("Asignado");
        });
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
  if (!ranking || !confirm(`Eliminar a ${ranking.hermano.nombre} del planificador?`)) return;
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
          ${field("green-talk", "Discursos: verde desde dias", `<input id="green-talk" class="input" type="number" min="1" value="${Number(state.config.diasVerdeDiscurso)}">`)}
          ${field("yellow-talk", "Discursos: amarillo desde dias", `<input id="yellow-talk" class="input" type="number" min="1" value="${Number(state.config.diasAmarilloDiscurso)}">`)}
        </div>
        <div class="inline-fields">
          ${field("green-prayer", "Oraciones: verde desde dias", `<input id="green-prayer" class="input" type="number" min="1" value="${Number(state.config.diasVerdeOracion)}">`)}
          ${field("yellow-prayer", "Oraciones: amarillo desde dias", `<input id="yellow-prayer" class="input" type="number" min="1" value="${Number(state.config.diasAmarilloOracion)}">`)}
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
          const data = {
            numeroUnidad: state.unitNumber,
            diasVerdeDiscurso: Number(dialog.querySelector("#green-talk").value || 90),
            diasAmarilloDiscurso: Number(dialog.querySelector("#yellow-talk").value || 30),
            diasVerdeOracion: Number(dialog.querySelector("#green-prayer").value || 30),
            diasAmarilloOracion: Number(dialog.querySelector("#yellow-prayer").value || 14)
          };
          if (state.configId) await setDoc(configRef(state.configId), data);
          else await addDoc(collection(db, "configuracion"), data);
          closeModal();
          toastMessage("Configuración guardada");
        });
      });
    }
  });
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
    state.agendas.forEach((agenda) => {
      agenda.mensajesEvangelio.forEach((message) => {
        if (message.tipo !== "HIMNO_INTERMEDIO" && normalizeName(message.nombre) === key) talkDates.push(agenda.fecha);
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
      <section class="panel">
        ${sectionTitle("Publicacion web", "W", "")}
        <p class="muted">Si Google bloquea el inicio de sesión, agrega el dominio de GitHub Pages en Firebase Authentication > Authorized domains.</p>
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
        <span class="soft-badge">${escapeHtml(icon)}</span>
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

function agendaText(agenda) {
  const lines = [
    `Agenda Reunion Sacramental - ${formatDateLong(agenda.fecha)}`,
    `Preside: ${agenda.preside || "Sin datos"}`,
    `Dirige: ${agenda.dirige || "Sin datos"}`,
    agenda.reconocimientos ? `Reconocimientos: ${agenda.reconocimientos}` : "",
    agenda.anuncios ? `Anuncios: ${agenda.anuncios}` : "",
    `Himno de apertura: ${hymnLabel(agenda.primerHimnoNumero, agenda.primerHimnoNombre)}`,
    `Primera oracion: ${agenda.primeraOracion || "Sin datos"}`,
    `Himno Sacramental: ${hymnLabel(agenda.himnoSacramentalNumero, agenda.himnoSacramentalNombre)}`,
    agenda.reunionTestimonios
      ? `Testimonios: ${(agenda.testimonios || []).join(", ") || "Sin datos"}`
      : `Mensajes: ${agenda.mensajesEvangelio.map((item) => item.tipo === "HIMNO_INTERMEDIO" ? hymnLabel(item.himnoNumero, item.himnoNombre) : `${labelMessage(item.tipo)} - ${item.nombre}`).join("; ") || "Sin datos"}`,
    `Himno final: ${hymnLabel(agenda.himnoFinalNumero, agenda.himnoFinalNombre)}`,
    `Oracion final: ${agenda.oracionFinal || "Sin datos"}`
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
  return Math.max(0, Math.floor((startOfDay(new Date()) - startOfDay(toDate(date))) / 86400000));
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
