package com.example.agendasacramental

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import com.google.firebase.Timestamp
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.activity.compose.BackHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

// Colores semáforo
val ColorVerde = Color(0xFF2E7D32)
val ColorAmarillo = Color(0xFFF9A825)
val ColorRojo = Color(0xFFC62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanificacionScreen(
    numeroUnidad: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AgendaRepository() }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(0) }
    var rankings by remember { mutableStateOf<List<HermanoRanking>>(emptyList()) }
    var agendas by remember { mutableStateOf<List<Agenda>>(emptyList()) }
    var config by remember { mutableStateOf(ConfiguracionPlanificacion(numeroUnidad = numeroUnidad)) }
    var isLoading by remember { mutableStateOf(true) }
    var showAgregarHermano by remember { mutableStateOf(false) }
    var showConfiguracion by remember { mutableStateOf(false) }
    var hermanoParaAsignar by remember { mutableStateOf<HermanoRanking?>(null) }
    var hermanoAEliminar by remember { mutableStateOf<HermanoRanking?>(null) }
    var hermanoAEditar by remember { mutableStateOf<HermanoRanking?>(null) }
    var modoSeleccion by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var seleccionados by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showConfirmarBulkDelete by remember { mutableStateOf(false) }

    var filtrosDiscurso by remember { mutableStateOf(setOf(ColorRanking.VERDE, ColorRanking.AMARILLO)) }
    var filtrosOracion by remember { mutableStateOf(setOf(ColorRanking.VERDE, ColorRanking.AMARILLO)) }

    LaunchedEffect(numeroUnidad) {
        isLoading = true
        val cfg = repository.getConfiguracion(numeroUnidad)
        if (cfg != null) config = cfg
        val hermanos = repository.getHermanos(numeroUnidad)
        val agResults = repository.getAgendas(numeroUnidad).getOrElse { emptyList() }
        agendas = agResults
        rankings = calcularRankings(hermanos, agResults, config)
        isLoading = false
    }

    fun recargar() {
        scope.launch {
            val hermanos = repository.getHermanos(numeroUnidad)
            val agResults = repository.getAgendas(numeroUnidad).getOrElse { emptyList() }
            agendas = agResults
            rankings = calcularRankings(hermanos, agResults, config)
        }
    }

    // Salir del modo selección con el botón atrás
    BackHandler(enabled = modoSeleccion) {
        modoSeleccion = false
        seleccionados = emptySet()
    }

    BackHandler(enabled = showSearch) {
        showSearch = false
        searchQuery = ""
    }

    // Diálogo confirmar bulk delete
    if (showConfirmarBulkDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmarBulkDelete = false },
            title = { Text(LocalContext.current.getString(R.string.plan_eliminar_hermanos_titulo)) },
            text = { Text(context.getString(R.string.plan_conf_eliminar_varios, seleccionados.size)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        seleccionados.forEach { id ->
                            val ranking = rankings.find { r ->
                                if (r.hermano.id.isNotBlank()) r.hermano.id == id
                                else r.hermano.nombre == id
                            }
                            if (ranking != null) {
                                if (ranking.hermano.id.isNotBlank()) {
                                    repository.eliminarHermano(ranking.hermano.id)
                                } else {
                                    repository.excluirHermanoHistorial(numeroUnidad, ranking.hermano.nombre)
                                }
                            }
                        }
                        recargar()
                        seleccionados = emptySet()
                        modoSeleccion = false
                        showConfirmarBulkDelete = false
                    }
                }) { Text(LocalContext.current.getString(R.string.btn_eliminar), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmarBulkDelete = false }) { Text(LocalContext.current.getString(R.string.btn_cancelar)) }
            }
        )
    }

    hermanoAEliminar?.let { ranking ->
        AlertDialog(
            onDismissRequest = { hermanoAEliminar = null },
            title = { Text(LocalContext.current.getString(R.string.plan_eliminar_hermano_titulo)) },
            text = { Text(LocalContext.current.getString(R.string.plan_conf_eliminar, ranking.hermano.nombre)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        if (ranking.hermano.id.isNotBlank()) {
                            repository.eliminarHermano(ranking.hermano.id)
                        } else {
                            repository.excluirHermanoHistorial(numeroUnidad, ranking.hermano.nombre)
                        }
                        recargar()
                        hermanoAEliminar = null
                    }
                }) { Text(LocalContext.current.getString(R.string.btn_eliminar), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { hermanoAEliminar = null }) { Text(LocalContext.current.getString(R.string.btn_cancelar)) }
            }
        )
    }

    hermanoAEditar?.let { ranking ->
        EditarHermanoDialog(
            nombreActual = ranking.hermano.nombre,
            fechaDiscursoActual = ranking.ultimaVezDiscurso ?: ranking.hermano.ultimaVezDiscursoManual,
            fechaOracionActual = ranking.ultimaVezOracion ?: ranking.hermano.ultimaVezOracionManual,
            nombresExistentes = rankings.map { it.hermano.nombre }.filter { it != ranking.hermano.nombre },
            onConfirm = { nuevoNombre, fechaDiscurso, fechaOracion, actualizarAgendas ->
                scope.launch {
                    val nombreAnterior = ranking.hermano.nombre
                    if (nuevoNombre != nombreAnterior) {
                        repository.editarNombreHermano(ranking.hermano.id, nuevoNombre)
                        if (actualizarAgendas) {
                            repository.renombrarEnAgendasFuturas(numeroUnidad, nombreAnterior, nuevoNombre)
                        }
                    }
                    repository.actualizarFechasManual(ranking.hermano.id, fechaDiscurso, fechaOracion)
                    recargar()
                    hermanoAEditar = null
                }
            },
            onDismiss = { hermanoAEditar = null }
        )
    }

    if (showAgregarHermano) {
        AgregarHermanoDialog(
            nombresExistentes = rankings.map { it.hermano.nombre },
            onConfirm = { nombre, fechaDiscurso, fechaOracion ->
                scope.launch {
                    repository.agregarHermano(Hermano(
                        numeroUnidad = numeroUnidad,
                        nombre = nombre,
                        agregadoManualmente = true,
                        ultimaVezDiscursoManual = fechaDiscurso,
                        ultimaVezOracionManual = fechaOracion
                    ))
                    recargar()
                    showAgregarHermano = false
                }
            },
            onDismiss = { showAgregarHermano = false }
        )
    }

    if (showConfiguracion) {
        ConfiguracionDialog(
            config = config,
            onConfirm = { nuevaConfig ->
                scope.launch {
                    repository.guardarConfiguracion(nuevaConfig)
                    config = nuevaConfig
                    val hermanos = repository.getHermanos(numeroUnidad)
                    rankings = calcularRankings(hermanos, agendas, nuevaConfig)
                    showConfiguracion = false
                }
            },
            onDismiss = { showConfiguracion = false }
        )
    }

    hermanoParaAsignar?.let { ranking ->
        AsignarAgendaDialog(
            ranking = ranking,
            tab = selectedTab,
            agendas = agendas.filter { it.estado == EstadoAgenda.BORRADOR },
            config = config,
            onConfirm = { agendaId, campo ->
                scope.launch {
                    repository.asignarHermanoAAgenda(agendaId, campo, ranking.hermano.nombre)
                    recargar()
                    hermanoParaAsignar = null
                }
            },
            onDismiss = { hermanoParaAsignar = null }
        )
    }

    Scaffold(
        topBar = {
            when {
                modoSeleccion -> TopAppBar(
                    title = { Text(context.getString(R.string.plan_seleccionados, seleccionados.size)) },
                    navigationIcon = {
                        IconButton(onClick = { modoSeleccion = false; seleccionados = emptySet() }) {
                            Icon(Icons.Default.Close, LocalContext.current.getString(R.string.plan_cancelar_seleccion))
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showConfirmarBulkDelete = true },
                            enabled = seleccionados.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, LocalContext.current.getString(R.string.plan_eliminar_seleccionados), tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
                showSearch -> {
                    val focusRequester = remember { FocusRequester() }
                    val keyboard = LocalSoftwareKeyboardController.current
                    LaunchedEffect(Unit) {
                        focusRequester.requestFocus()
                        keyboard?.show()
                    }
                    TopAppBar(
                        title = {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text(LocalContext.current.getString(R.string.plan_buscar)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { showSearch = false; searchQuery = "" }) {
                                Icon(Icons.Default.ArrowBack, LocalContext.current.getString(R.string.plan_cancelar_seleccion))
                            }
                        }
                    )
                }
                else -> TopAppBar(
                    title = { Text(LocalContext.current.getString(R.string.plan_titulo)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, LocalContext.current.getString(R.string.btn_volver))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, LocalContext.current.getString(R.string.plan_buscar))
                        }
                        IconButton(onClick = { showAgregarHermano = true }) {
                            Icon(Icons.Default.PersonAdd, LocalContext.current.getString(R.string.plan_agregar_hermano))
                        }
                        IconButton(onClick = { showConfiguracion = true }) {
                            Icon(Icons.Default.Settings, LocalContext.current.getString(R.string.plan_configuracion))
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text(LocalContext.current.getString(R.string.plan_discursos)) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(LocalContext.current.getString(R.string.plan_oraciones)) })
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val filtrosActivos = if (selectedTab == 0) filtrosDiscurso else filtrosOracion

                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ColorRanking.values().forEach { color ->
                        val seleccionado = color in filtrosActivos
                        FilterChip(
                            selected = seleccionado,
                            onClick = {
                                if (selectedTab == 0) {
                                    filtrosDiscurso = if (seleccionado) filtrosDiscurso - color else filtrosDiscurso + color
                                } else {
                                    filtrosOracion = if (seleccionado) filtrosOracion - color else filtrosOracion + color
                                }
                            },
                            label = { Text(context.getString(color.stringResId)) },
                            leadingIcon = if (seleccionado) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colorParaChip(color).copy(alpha = 0.2f),
                                selectedLabelColor = colorParaChip(color)
                            )
                        )
                    }
                }

                val inactivoParaTab: (HermanoRanking) -> Boolean = { r ->
                    if (selectedTab == 0) r.hermano.inactivoDiscurso else r.hermano.inactivoOracion
                }

                val listaFiltrada = rankings
                    .filter { ranking ->
                        // Si hay búsqueda activa, ignorar filtros de color y mostrar todos los que coincidan
                        if (searchQuery.isNotBlank()) {
                            return@filter normalizarNombre(ranking.hermano.nombre)
                                .contains(normalizarNombre(searchQuery))
                        }
                        if (inactivoParaTab(ranking)) return@filter true
                        val color = if (selectedTab == 0)
                            calcularColor(ranking.ultimaVezDiscurso, config.diasVerdeDiscurso, config.diasAmarilloDiscurso)
                        else
                            calcularColor(ranking.ultimaVezOracion, config.diasVerdeOracion, config.diasAmarilloOracion)
                        color in filtrosActivos
                    }
                    .sortedWith(Comparator { a, b ->
                        if (inactivoParaTab(a) && !inactivoParaTab(b)) return@Comparator 1
                        if (!inactivoParaTab(a) && inactivoParaTab(b)) return@Comparator -1
                        val diasA = if (selectedTab == 0) diasDesde(a.ultimaVezDiscurso) else diasDesde(a.ultimaVezOracion)
                        val diasB = if (selectedTab == 0) diasDesde(b.ultimaVezDiscurso) else diasDesde(b.ultimaVezOracion)
                        diasB.compareTo(diasA)
                    })

                if (listaFiltrada.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(LocalContext.current.getString(R.string.plan_sin_hermanos), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(listaFiltrada) { ranking ->
                            val itemId = ranking.hermano.id.ifBlank { ranking.hermano.nombre }
                            val estaSeleccionado = itemId in seleccionados
                            HermanoRankingCard(
                                ranking = ranking,
                                tab = selectedTab,
                                config = config,
                                modoSeleccion = modoSeleccion,
                                estaSeleccionado = estaSeleccionado,
                                onClick = {
                                    if (modoSeleccion) {
                                        seleccionados = if (estaSeleccionado)
                                            seleccionados - itemId
                                        else
                                            seleccionados + itemId
                                    } else {
                                        if (!inactivoParaTab(ranking)) hermanoParaAsignar = ranking
                                    }
                                },
                                onLongClick = {
                                    if (!modoSeleccion) {
                                        modoSeleccion = true
                                        seleccionados = setOf(itemId)
                                    }
                                },
                                onDelete = { hermanoAEliminar = ranking },
                                onEdit = {
                                    scope.launch {
                                        if (ranking.hermano.id.isNotBlank()) {
                                            hermanoAEditar = ranking
                                        } else {
                                            val result = repository.agregarHermano(
                                                Hermano(
                                                    numeroUnidad = numeroUnidad,
                                                    nombre = ranking.hermano.nombre,
                                                    agregadoManualmente = false
                                                )
                                            )
                                            if (result.isSuccess) {
                                                recargar()
                                                val hermanos = repository.getHermanos(numeroUnidad)
                                                val nuevo = hermanos.find {
                                                    normalizarNombre(it.nombre) == normalizarNombre(ranking.hermano.nombre) && it.id.isNotBlank()
                                                }
                                                if (nuevo != null) {
                                                    hermanoAEditar = ranking.copy(hermano = nuevo)
                                                }
                                            }
                                        }
                                    }
                                },
                                onToggleInactivo = {
                                    scope.launch {
                                        val campo = if (selectedTab == 0) "inactivoDiscurso" else "inactivoOracion"
                                        val valorActual = if (selectedTab == 0) ranking.hermano.inactivoDiscurso else ranking.hermano.inactivoOracion
                                        if (ranking.hermano.id.isNotBlank()) {
                                            repository.toggleInactivoHermano(ranking.hermano.id, campo, !valorActual)
                                        } else {
                                            val hermano = if (selectedTab == 0)
                                                Hermano(numeroUnidad = numeroUnidad, nombre = ranking.hermano.nombre, inactivoDiscurso = true)
                                            else
                                                Hermano(numeroUnidad = numeroUnidad, nombre = ranking.hermano.nombre, inactivoOracion = true)
                                            repository.agregarHermano(hermano)
                                        }
                                        recargar()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HermanoRankingCard(
    ranking: HermanoRanking,
    tab: Int,
    config: ConfiguracionPlanificacion,
    modoSeleccion: Boolean = false,
    estaSeleccionado: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onToggleInactivo: () -> Unit
) {
    val context = LocalContext.current
    val ultimaVez = if (tab == 0) ranking.ultimaVezDiscurso else ranking.ultimaVezOracion
    val dias = diasDesde(ultimaVez)
    val color = if (tab == 0)
        calcularColor(ultimaVez, config.diasVerdeDiscurso, config.diasAmarilloDiscurso)
    else
        calcularColor(ultimaVez, config.diasVerdeOracion, config.diasAmarilloOracion)
    val colorReal = colorParaChip(color)
    val inactivo = if (tab == 0) ranking.hermano.inactivoDiscurso else ranking.hermano.inactivoOracion
    val alpha = if (inactivo) 0.4f else 1f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                estaSeleccionado -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                inactivo -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            // Fila principal: indicador + nombre + badge color
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (modoSeleccion) {
                    Checkbox(
                        checked = estaSeleccionado,
                        onCheckedChange = { onClick() },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(colorReal.copy(alpha = alpha), shape = MaterialTheme.shapes.small)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Nombre e info — ocupa todo el espacio disponible
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            ranking.hermano.nombre,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (inactivo) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(
                                    LocalContext.current.getString(R.string.plan_inactivo),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        when {
                            ultimaVez == null -> LocalContext.current.getString(R.string.plan_sin_registros)
                            dias < 0 -> context.getString(R.string.plan_dentro_de, (-dias).toString())
                            dias == 0L -> context.getString(R.string.plan_hoy)
                            else -> context.getString(R.string.plan_hace_dias, dias.toString())
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                    )
                    if (tab == 0 && ranking.vecesDiscurso90Dias > 0) {
                        Text(
                            context.getString(R.string.plan_veces_90_dias, ranking.vecesDiscurso90Dias),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                        )
                    } else if (tab == 1 && ranking.vecesOracion90Dias > 0) {
                        Text(
                            context.getString(R.string.plan_veces_90_dias, ranking.vecesOracion90Dias),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                        )
                    }
                }

                // Chip de color — siempre visible a la derecha, tamaño fijo
                if (!inactivo) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = colorReal.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            context.getString(color.stringResId),
                            style = MaterialTheme.typography.labelSmall,
                            color = colorReal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            // Fila de botones — solo cuando no está en modo selección
            if (!modoSeleccion) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, LocalContext.current.getString(R.string.btn_agregar),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onToggleInactivo, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (inactivo) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (inactivo) LocalContext.current.getString(R.string.plan_reactivar) else LocalContext.current.getString(R.string.plan_desactivar),
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, LocalContext.current.getString(R.string.btn_eliminar),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun AgregarHermanoDialog(
    nombresExistentes: List<String>,
    onConfirm: (String, Timestamp?, Timestamp?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var nombre by remember { mutableStateOf("") }
    var showDuplicadoWarning by remember { mutableStateOf(false) }
    var nombreDuplicado by remember { mutableStateOf("") }
    var fechaDiscurso by remember { mutableStateOf<Timestamp?>(null) }
    var fechaOracion by remember { mutableStateOf<Timestamp?>(null) }
    var showPickerDiscurso by remember { mutableStateOf(false) }
    var showPickerOracion by remember { mutableStateOf(false) }

    val dateFormat = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()) }
    val nombreNorm = normalizarNombre(nombre)
    val duplicado = nombresExistentes.firstOrNull { normalizarNombre(it) == nombreNorm && it.isNotBlank() }

    // Date pickers como diálogos separados encima del diálogo principal
    if (showPickerDiscurso) {
        FechaSelectorDialog(
            onFechaSeleccionada = { fechaDiscurso = Timestamp(it); showPickerDiscurso = false },
            onDismiss = { showPickerDiscurso = false }
        )
    }

    if (showPickerOracion) {
        FechaSelectorDialog(
            onFechaSeleccionada = { fechaOracion = Timestamp(it); showPickerOracion = false },
            onDismiss = { showPickerOracion = false }
        )
    }

    if (showDuplicadoWarning) {
        AlertDialog(
            onDismissRequest = { showDuplicadoWarning = false },
            title = { Text(LocalContext.current.getString(R.string.editar_nombre_duplicado)) },
            text = { Text(context.getString(R.string.plan_nombre_similar, nombreDuplicado)) },
            confirmButton = {
                TextButton(onClick = { onConfirm(nombre.trim(), fechaDiscurso, fechaOracion); showDuplicadoWarning = false }) { Text(LocalContext.current.getString(R.string.btn_agregar_igual)) }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicadoWarning = false }) { Text(LocalContext.current.getString(R.string.btn_cancelar)) }
            }
        )
    }

    if (!showPickerDiscurso && !showPickerOracion && !showDuplicadoWarning) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(LocalContext.current.getString(R.string.plan_agregar_hermano_titulo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nombre, onValueChange = { nombre = it },
                        label = { Text(LocalContext.current.getString(R.string.editar_nombre_completo)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Text(
                        LocalContext.current.getString(R.string.plan_opcional_participacion),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = fechaDiscurso?.let { dateFormat.format(it.toDate()) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(LocalContext.current.getString(R.string.plan_ultimo_discurso)) },
                        placeholder = { Text(LocalContext.current.getString(R.string.editar_sin_datos)) },
                        trailingIcon = {
                            Row {
                                if (fechaDiscurso != null) {
                                    IconButton(onClick = { fechaDiscurso = null }) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                    }
                                }
                                IconButton(onClick = { showPickerDiscurso = true }) {
                                    Icon(Icons.Default.DateRange, LocalContext.current.getString(R.string.btn_aceptar))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fechaOracion?.let { dateFormat.format(it.toDate()) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(LocalContext.current.getString(R.string.plan_ultima_oracion)) },
                        placeholder = { Text(LocalContext.current.getString(R.string.editar_sin_datos)) },
                        trailingIcon = {
                            Row {
                                if (fechaOracion != null) {
                                    IconButton(onClick = { fechaOracion = null }) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                    }
                                }
                                IconButton(onClick = { showPickerOracion = true }) {
                                    Icon(Icons.Default.DateRange, LocalContext.current.getString(R.string.btn_aceptar))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nombre.isNotBlank()) {
                            if (duplicado != null) { nombreDuplicado = duplicado; showDuplicadoWarning = true }
                            else onConfirm(nombre.trim(), fechaDiscurso, fechaOracion)
                        }
                    },
                    enabled = nombre.isNotBlank()
                ) { Text(LocalContext.current.getString(R.string.btn_agregar)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(LocalContext.current.getString(R.string.btn_cancelar)) } }
        )
    }
}

@Composable
fun ConfiguracionDialog(
    config: ConfiguracionPlanificacion,
    onConfirm: (ConfiguracionPlanificacion) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var diasVerdeDiscurso by remember { mutableStateOf(config.diasVerdeDiscurso.toString()) }
    var diasAmarilloDiscurso by remember { mutableStateOf(config.diasAmarilloDiscurso.toString()) }
    var diasVerdeOracion by remember { mutableStateOf(config.diasVerdeOracion.toString()) }
    var diasAmarilloOracion by remember { mutableStateOf(config.diasAmarilloOracion.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(LocalContext.current.getString(R.string.plan_conf_colores)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(LocalContext.current.getString(R.string.plan_discursos), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = diasVerdeDiscurso, onValueChange = { diasVerdeDiscurso = it }, label = { Text(LocalContext.current.getString(R.string.plan_dias_verde)) }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = diasAmarilloDiscurso, onValueChange = { diasAmarilloDiscurso = it }, label = { Text(LocalContext.current.getString(R.string.plan_dias_amarillo)) }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Text(LocalContext.current.getString(R.string.plan_oraciones), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = diasVerdeOracion, onValueChange = { diasVerdeOracion = it }, label = { Text(LocalContext.current.getString(R.string.plan_dias_verde)) }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = diasAmarilloOracion, onValueChange = { diasAmarilloOracion = it }, label = { Text(LocalContext.current.getString(R.string.plan_dias_amarillo)) }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Text(LocalContext.current.getString(R.string.plan_leyenda_colores), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(config.copy(
                    diasVerdeDiscurso = diasVerdeDiscurso.toIntOrNull() ?: config.diasVerdeDiscurso,
                    diasAmarilloDiscurso = diasAmarilloDiscurso.toIntOrNull() ?: config.diasAmarilloDiscurso,
                    diasVerdeOracion = diasVerdeOracion.toIntOrNull() ?: config.diasVerdeOracion,
                    diasAmarilloOracion = diasAmarilloOracion.toIntOrNull() ?: config.diasAmarilloOracion
                ))
            }) { Text(LocalContext.current.getString(R.string.btn_guardar)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(LocalContext.current.getString(R.string.btn_cancelar)) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsignarAgendaDialog(
    ranking: HermanoRanking,
    tab: Int,
    agendas: List<Agenda>,
    config: ConfiguracionPlanificacion,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()) }
    var agendaSeleccionada by remember { mutableStateOf<Agenda?>(null) }
    var expandedAgenda by remember { mutableStateOf(false) }
    var campoOracion by remember { mutableStateOf("") }
    var expandedOracion by remember { mutableStateOf(false) }
    var showAdvertencia by remember { mutableStateOf(false) }

    val ultimaVez = if (tab == 0) ranking.ultimaVezDiscurso else ranking.ultimaVezOracion
    val color = if (tab == 0)
        calcularColor(ultimaVez, config.diasVerdeDiscurso, config.diasAmarilloDiscurso)
    else
        calcularColor(ultimaVez, config.diasVerdeOracion, config.diasAmarilloOracion)

    val camposOracion = listOf(LocalContext.current.getString(R.string.editar_primera_oracion), LocalContext.current.getString(R.string.editar_oracion_final))
    val campoFinal = if (tab == 0) LocalContext.current.getString(R.string.plan_nueva_discurso) else campoOracion
    val puedeConfirmar = agendaSeleccionada != null && (tab == 0 || campoOracion.isNotBlank())

    if (showAdvertencia) {
        AlertDialog(
            onDismissRequest = { showAdvertencia = false },
            title = { Text(LocalContext.current.getString(R.string.plan_hermano_reciente)) },
            text = { Text("${ranking.hermano.nombre} participó hace solo ${diasDesde(ultimaVez)} días. ¿Deseas asignarlo/a de todas formas?") },
            confirmButton = {
                TextButton(onClick = { agendaSeleccionada?.let { onConfirm(it.id, campoFinal) }; showAdvertencia = false }) { Text(LocalContext.current.getString(R.string.btn_si_asignar)) }
            },
            dismissButton = { TextButton(onClick = { showAdvertencia = false }) { Text(LocalContext.current.getString(R.string.btn_cancelar)) } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(LocalContext.current.getString(R.string.plan_asignar_titulo, ranking.hermano.nombre)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (tab == 0) LocalContext.current.getString(R.string.plan_se_asignara_discurso) else LocalContext.current.getString(R.string.plan_se_asignara_oracion),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExposedDropdownMenuBox(expanded = expandedAgenda, onExpandedChange = { expandedAgenda = it }) {
                    OutlinedTextField(
                        value = agendaSeleccionada?.let { dateFormat.format(it.fecha.toDate()) } ?: "",
                        onValueChange = {}, readOnly = true,
                        label = { Text(LocalContext.current.getString(R.string.plan_reunion_borrador)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAgenda) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        placeholder = { Text(LocalContext.current.getString(R.string.editar_seleccionar_fecha)) }
                    )
                    ExposedDropdownMenu(expanded = expandedAgenda, onDismissRequest = { expandedAgenda = false }) {
                        if (agendas.isEmpty()) {
                            DropdownMenuItem(text = { Text(LocalContext.current.getString(R.string.agendas_no_hay_borrador)) }, onClick = { expandedAgenda = false }, enabled = false)
                        } else {
                            agendas.sortedBy { it.fecha.toDate() }.forEach { agenda ->
                                DropdownMenuItem(text = { Text(dateFormat.format(agenda.fecha.toDate())) }, onClick = { agendaSeleccionada = agenda; expandedAgenda = false })
                            }
                        }
                    }
                }
                if (tab == 1) {
                    ExposedDropdownMenuBox(expanded = expandedOracion, onExpandedChange = { expandedOracion = it }) {
                        OutlinedTextField(
                            value = campoOracion, onValueChange = {}, readOnly = true,
                            label = { Text(LocalContext.current.getString(R.string.plan_tipo_oracion)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOracion) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            placeholder = { Text(LocalContext.current.getString(R.string.btn_aceptar)) }
                        )
                        ExposedDropdownMenu(expanded = expandedOracion, onDismissRequest = { expandedOracion = false }) {
                            camposOracion.forEach { campo ->
                                DropdownMenuItem(text = { Text(campo) }, onClick = { campoOracion = campo; expandedOracion = false })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (color == ColorRanking.ROJO) showAdvertencia = true
                    else agendaSeleccionada?.let { onConfirm(it.id, campoFinal) }
                },
                enabled = puedeConfirmar
            ) { Text(LocalContext.current.getString(R.string.btn_asignar)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(LocalContext.current.getString(R.string.btn_cancelar)) } }
    )
}

@Composable
fun EditarHermanoDialog(
    nombreActual: String,
    fechaDiscursoActual: Timestamp?,
    fechaOracionActual: Timestamp?,
    nombresExistentes: List<String>,
    onConfirm: (String, Timestamp?, Timestamp?, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var nombre by remember { mutableStateOf(nombreActual) }
    var showDuplicadoWarning by remember { mutableStateOf(false) }
    var actualizarAgendas by remember { mutableStateOf(false) }
    var nombreDuplicado by remember { mutableStateOf("") }
    var fechaDiscurso by remember { mutableStateOf(fechaDiscursoActual) }
    var fechaOracion by remember { mutableStateOf(fechaOracionActual) }
    var showPickerDiscurso by remember { mutableStateOf(false) }
    var showPickerOracion by remember { mutableStateOf(false) }

    val dateFormat = remember { java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()) }
    val nombreNorm = normalizarNombre(nombre)
    val duplicado = nombresExistentes.firstOrNull { normalizarNombre(it) == nombreNorm && it.isNotBlank() }

    if (showPickerDiscurso) {
        FechaSelectorDialog(
            onFechaSeleccionada = { fechaDiscurso = Timestamp(it); showPickerDiscurso = false },
            onDismiss = { showPickerDiscurso = false }
        )
    }

    if (showPickerOracion) {
        FechaSelectorDialog(
            onFechaSeleccionada = { fechaOracion = Timestamp(it); showPickerOracion = false },
            onDismiss = { showPickerOracion = false }
        )
    }

    if (showDuplicadoWarning) {
        AlertDialog(
            onDismissRequest = { showDuplicadoWarning = false },
            title = { Text(LocalContext.current.getString(R.string.editar_nombre_duplicado)) },
            text = { Text(context.getString(R.string.plan_nombre_similar, nombreDuplicado)) },
            confirmButton = {
                TextButton(onClick = { onConfirm(nombre.trim(), fechaDiscurso, fechaOracion, actualizarAgendas); showDuplicadoWarning = false }) { Text(LocalContext.current.getString(R.string.btn_guardar_igual)) }
            },
            dismissButton = { TextButton(onClick = { showDuplicadoWarning = false }) { Text(LocalContext.current.getString(R.string.btn_cancelar)) } }
        )
    }

    if (!showPickerDiscurso && !showPickerOracion && !showDuplicadoWarning) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(LocalContext.current.getString(R.string.plan_editar_hermano_titulo)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = nombre, onValueChange = { nombre = it },
                        label = { Text(LocalContext.current.getString(R.string.editar_nombre_completo)) },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    // Checkbox actualizar agendas — solo si cambió el nombre
                    if (nombre.trim() != nombreActual && nombre.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = actualizarAgendas,
                                onCheckedChange = { actualizarAgendas = it }
                            )
                            Text(
                                LocalContext.current.getString(R.string.editar_actualizar_agendas),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                    Text(
                        LocalContext.current.getString(R.string.plan_ultima_participacion),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = fechaDiscurso?.let { dateFormat.format(it.toDate()) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(LocalContext.current.getString(R.string.plan_ultimo_discurso)) },
                        placeholder = { Text(LocalContext.current.getString(R.string.editar_sin_datos)) },
                        trailingIcon = {
                            Row {
                                if (fechaDiscurso != null) {
                                    IconButton(onClick = { fechaDiscurso = null }) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                    }
                                }
                                IconButton(onClick = { showPickerDiscurso = true }) {
                                    Icon(Icons.Default.DateRange, LocalContext.current.getString(R.string.btn_aceptar))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fechaOracion?.let { dateFormat.format(it.toDate()) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(LocalContext.current.getString(R.string.plan_ultima_oracion)) },
                        placeholder = { Text(LocalContext.current.getString(R.string.editar_sin_datos)) },
                        trailingIcon = {
                            Row {
                                if (fechaOracion != null) {
                                    IconButton(onClick = { fechaOracion = null }) {
                                        Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                    }
                                }
                                IconButton(onClick = { showPickerOracion = true }) {
                                    Icon(Icons.Default.DateRange, LocalContext.current.getString(R.string.btn_aceptar))
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (nombre.isNotBlank()) {
                            if (nombre.trim() != nombreActual && duplicado != null) {
                                nombreDuplicado = duplicado; showDuplicadoWarning = true
                            } else {
                                onConfirm(nombre.trim(), fechaDiscurso, fechaOracion, actualizarAgendas)
                            }
                        }
                    },
                    enabled = nombre.isNotBlank()
                ) { Text(LocalContext.current.getString(R.string.btn_guardar)) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(LocalContext.current.getString(R.string.btn_cancelar)) } }
        )
    } // end if !showPicker && !showDuplicado
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FechaSelectorDialog(
    onFechaSeleccionada: (java.util.Date) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
            .apply { set(java.util.Calendar.HOUR_OF_DAY, 12) }.timeInMillis
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val calUtc = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).also { it.timeInMillis = millis }
                    val calLocal = java.util.Calendar.getInstance().apply {
                        set(calUtc.get(java.util.Calendar.YEAR), calUtc.get(java.util.Calendar.MONTH), calUtc.get(java.util.Calendar.DAY_OF_MONTH), 12, 0, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }
                    onFechaSeleccionada(calLocal.time)
                }
            }) { Text(LocalContext.current.getString(R.string.btn_aceptar)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(LocalContext.current.getString(R.string.btn_cancelar)) } }
    ) { DatePicker(state = datePickerState) }
}

// --- Funciones de cálculo ---

fun diasDesde(timestamp: Timestamp?): Long {
    if (timestamp == null) return Long.MAX_VALUE
    val hoy = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    val fecha = java.util.Calendar.getInstance().apply {
        time = timestamp.toDate()
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis
    // Negativo = fecha futura, positivo = fecha pasada
    return TimeUnit.MILLISECONDS.toDays(hoy - fecha)
}

fun calcularColor(ultimaVez: Timestamp?, diasVerde: Int, diasAmarillo: Int): ColorRanking {
    val dias = diasDesde(ultimaVez)
    return when {
        dias == Long.MAX_VALUE -> ColorRanking.VERDE  // nunca participó
        dias < 0 -> ColorRanking.ROJO                 // participación futura = muy reciente
        dias >= diasVerde -> ColorRanking.VERDE
        dias >= diasAmarillo -> ColorRanking.AMARILLO
        else -> ColorRanking.ROJO
    }
}

fun colorParaChip(color: ColorRanking): Color = when (color) {
    ColorRanking.VERDE -> ColorVerde
    ColorRanking.AMARILLO -> ColorAmarillo
    ColorRanking.ROJO -> ColorRojo
}

fun calcularRankings(
    hermanos: List<Hermano>,
    agendas: List<Agenda>,
    config: ConfiguracionPlanificacion
): List<HermanoRanking> {
    val hace90Dias = Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(90))

    return hermanos.map { hermano ->
        val nombre = hermano.nombre.trim().lowercase()

        var ultimaVezDiscurso: Timestamp? = null
        var vecesDiscurso90 = 0
        agendas.forEach { agenda ->
            agenda.mensajesEvangelio.forEach { msg ->
                if (msg.tipo != TipoMensaje.HIMNO_INTERMEDIO && msg.nombre.trim().lowercase() == nombre) {
                    if (ultimaVezDiscurso == null || agenda.fecha.toDate().after(ultimaVezDiscurso!!.toDate())) {
                        ultimaVezDiscurso = agenda.fecha
                    }
                    if (agenda.fecha.toDate().after(hace90Dias)) vecesDiscurso90++
                }
            }
        }
        // Usar fecha manual solo si es más reciente que la de agendas (o si no hay dato de agendas)
        hermano.ultimaVezDiscursoManual?.let { manual ->
            if (ultimaVezDiscurso == null || manual.toDate().after(ultimaVezDiscurso!!.toDate())) {
                ultimaVezDiscurso = manual
            }
        }

        var ultimaVezOracion: Timestamp? = null
        var vecesOracion90 = 0
        agendas.forEach { agenda ->
            listOf(agenda.primeraOracion, agenda.oracionFinal).forEach { oracion ->
                if (oracion.trim().lowercase() == nombre) {
                    if (ultimaVezOracion == null || agenda.fecha.toDate().after(ultimaVezOracion!!.toDate())) {
                        ultimaVezOracion = agenda.fecha
                    }
                    if (agenda.fecha.toDate().after(hace90Dias)) vecesOracion90++
                }
            }
        }
        // Usar fecha manual solo si es más reciente que la de agendas (o si no hay dato de agendas)
        hermano.ultimaVezOracionManual?.let { manual ->
            if (ultimaVezOracion == null || manual.toDate().after(ultimaVezOracion!!.toDate())) {
                ultimaVezOracion = manual
            }
        }

        HermanoRanking(
            hermano = hermano,
            ultimaVezDiscurso = ultimaVezDiscurso,
            ultimaVezOracion = ultimaVezOracion,
            vecesDiscurso90Dias = vecesDiscurso90,
            vecesOracion90Dias = vecesOracion90
        )
    }
}
