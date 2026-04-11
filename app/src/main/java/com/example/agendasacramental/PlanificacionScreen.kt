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

    // Diálogo confirmar bulk delete
    if (showConfirmarBulkDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmarBulkDelete = false },
            title = { Text("Eliminar hermanos") },
            text = { Text("¿Estás seguro que querés eliminar ${seleccionados.size} hermano${if (seleccionados.size == 1) "" else "s"} del planificador?") },
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
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmarBulkDelete = false }) { Text("Cancelar") }
            }
        )
    }

    hermanoAEliminar?.let { ranking ->
        AlertDialog(
            onDismissRequest = { hermanoAEliminar = null },
            title = { Text("Eliminar hermano/a") },
            text = { Text("¿Estás seguro que querés eliminar a ${ranking.hermano.nombre} del planificador?") },
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
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { hermanoAEliminar = null }) { Text("Cancelar") }
            }
        )
    }

    hermanoAEditar?.let { ranking ->
        EditarHermanoDialog(
            nombreActual = ranking.hermano.nombre,
            fechaDiscursoActual = ranking.hermano.ultimaVezDiscursoManual,
            fechaOracionActual = ranking.hermano.ultimaVezOracionManual,
            nombresExistentes = rankings.map { it.hermano.nombre }.filter { it != ranking.hermano.nombre },
            onConfirm = { nuevoNombre, fechaDiscurso, fechaOracion ->
                scope.launch {
                    if (nuevoNombre != ranking.hermano.nombre) {
                        repository.editarNombreHermano(ranking.hermano.id, nuevoNombre)
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
            if (modoSeleccion) {
                TopAppBar(
                    title = { Text("${seleccionados.size} seleccionado${if (seleccionados.size == 1) "" else "s"}") },
                    navigationIcon = {
                        IconButton(onClick = { modoSeleccion = false; seleccionados = emptySet() }) {
                            Icon(Icons.Default.Close, "Cancelar selección")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showConfirmarBulkDelete = true },
                            enabled = seleccionados.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Delete, "Eliminar seleccionados", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = { Text("Planificación") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, "Volver")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showAgregarHermano = true }) {
                            Icon(Icons.Default.PersonAdd, "Agregar hermano")
                        }
                        IconButton(onClick = { showConfiguracion = true }) {
                            Icon(Icons.Default.Settings, "Configuración")
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Discursos") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Oraciones") })
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
                            label = { Text(color.label) },
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
                        Text("No hay hermanos en esta categoría.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                                    hermanoAEditar = HermanoRanking(hermano = nuevo)
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
                                    "Inactivo",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        when {
                            ultimaVez == null -> "Sin registros"
                            dias < 0 -> "Dentro de ${-dias} día${if (-dias == 1L) "" else "s"}"
                            dias == 0L -> "Hoy"
                            else -> "Hace $dias día${if (dias == 1L) "" else "s"}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                    )
                    if (tab == 0 && ranking.vecesDiscurso90Dias > 0) {
                        Text(
                            "${ranking.vecesDiscurso90Dias} vez/veces en 90 días",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                        )
                    } else if (tab == 1 && ranking.vecesOracion90Dias > 0) {
                        Text(
                            "${ranking.vecesOracion90Dias} vez/veces en 90 días",
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
                            color.label,
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
                        Icon(Icons.Default.Edit, "Editar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onToggleInactivo, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (inactivo) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (inactivo) "Reactivar" else "Desactivar",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, "Eliminar",
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

    if (showPickerDiscurso) {
        FechaSelectorDialog(
            onFechaSeleccionada = { fechaDiscurso = Timestamp(it); showPickerDiscurso = false },
            onDismiss = { showPickerDiscurso = false }
        )
        return
    }

    if (showPickerOracion) {
        FechaSelectorDialog(
            onFechaSeleccionada = { fechaOracion = Timestamp(it); showPickerOracion = false },
            onDismiss = { showPickerOracion = false }
        )
        return
    }

    if (showDuplicadoWarning) {
        AlertDialog(
            onDismissRequest = { showDuplicadoWarning = false },
            title = { Text("¿Nombre duplicado?") },
            text = { Text("Ya existe \"$nombreDuplicado\" que parece el mismo nombre. ¿Querés agregarlo igual?") },
            confirmButton = {
                TextButton(onClick = { onConfirm(nombre.trim(), fechaDiscurso, fechaOracion); showDuplicadoWarning = false }) { Text("Agregar igual") }
            },
            dismissButton = {
                TextButton(onClick = { showDuplicadoWarning = false }) { Text("Cancelar") }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar hermano/a") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre completo") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Text(
                    "Opcional: última participación conocida",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = fechaDiscurso?.let { dateFormat.format(it.toDate()) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Último discurso") },
                    placeholder = { Text("Sin datos") },
                    trailingIcon = {
                        Row {
                            if (fechaDiscurso != null) {
                                IconButton(onClick = { fechaDiscurso = null }) {
                                    Icon(Icons.Default.Close, "Borrar", modifier = Modifier.size(16.dp))
                                }
                            }
                            IconButton(onClick = { showPickerDiscurso = true }) {
                                Icon(Icons.Default.DateRange, "Seleccionar")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fechaOracion?.let { dateFormat.format(it.toDate()) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Última oración") },
                    placeholder = { Text("Sin datos") },
                    trailingIcon = {
                        Row {
                            if (fechaOracion != null) {
                                IconButton(onClick = { fechaOracion = null }) {
                                    Icon(Icons.Default.Close, "Borrar", modifier = Modifier.size(16.dp))
                                }
                            }
                            IconButton(onClick = { showPickerOracion = true }) {
                                Icon(Icons.Default.DateRange, "Seleccionar")
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
            ) { Text("Agregar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun ConfiguracionDialog(
    config: ConfiguracionPlanificacion,
    onConfirm: (ConfiguracionPlanificacion) -> Unit,
    onDismiss: () -> Unit
) {
    var diasVerdeDiscurso by remember { mutableStateOf(config.diasVerdeDiscurso.toString()) }
    var diasAmarilloDiscurso by remember { mutableStateOf(config.diasAmarilloDiscurso.toString()) }
    var diasVerdeOracion by remember { mutableStateOf(config.diasVerdeOracion.toString()) }
    var diasAmarilloOracion by remember { mutableStateOf(config.diasAmarilloOracion.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configuración de colores") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Discursos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = diasVerdeDiscurso, onValueChange = { diasVerdeDiscurso = it }, label = { Text("🟢 días") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = diasAmarilloDiscurso, onValueChange = { diasAmarilloDiscurso = it }, label = { Text("🟡 días") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Text("Oraciones", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = diasVerdeOracion, onValueChange = { diasVerdeOracion = it }, label = { Text("🟢 días") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = diasAmarilloOracion, onValueChange = { diasAmarilloOracion = it }, label = { Text("🟡 días") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Text("🟢 = más de X días  🟡 = entre X y Y días  🔴 = menos de Y días", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
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

    val camposOracion = listOf("Primera Oración", "Oración Final")
    val campoFinal = if (tab == 0) "NUEVO_DISCURSO" else campoOracion
    val puedeConfirmar = agendaSeleccionada != null && (tab == 0 || campoOracion.isNotBlank())

    if (showAdvertencia) {
        AlertDialog(
            onDismissRequest = { showAdvertencia = false },
            title = { Text("⚠️ Hermano/a reciente") },
            text = { Text("${ranking.hermano.nombre} participó hace solo ${diasDesde(ultimaVez)} días. ¿Deseas asignarlo/a de todas formas?") },
            confirmButton = {
                TextButton(onClick = { agendaSeleccionada?.let { onConfirm(it.id, campoFinal) }; showAdvertencia = false }) { Text("Sí, asignar") }
            },
            dismissButton = { TextButton(onClick = { showAdvertencia = false }) { Text("Cancelar") } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Asignar ${ranking.hermano.nombre}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (tab == 0) "Se agregará como discursante" else "Se asignará como oración",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ExposedDropdownMenuBox(expanded = expandedAgenda, onExpandedChange = { expandedAgenda = it }) {
                    OutlinedTextField(
                        value = agendaSeleccionada?.let { dateFormat.format(it.fecha.toDate()) } ?: "",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Reunión (borrador)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAgenda) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        placeholder = { Text("Seleccionar fecha") }
                    )
                    ExposedDropdownMenu(expanded = expandedAgenda, onDismissRequest = { expandedAgenda = false }) {
                        if (agendas.isEmpty()) {
                            DropdownMenuItem(text = { Text("No hay agendas borrador") }, onClick = { expandedAgenda = false }, enabled = false)
                        } else {
                            agendas.forEach { agenda ->
                                DropdownMenuItem(text = { Text(dateFormat.format(agenda.fecha.toDate())) }, onClick = { agendaSeleccionada = agenda; expandedAgenda = false })
                            }
                        }
                    }
                }
                if (tab == 1) {
                    ExposedDropdownMenuBox(expanded = expandedOracion, onExpandedChange = { expandedOracion = it }) {
                        OutlinedTextField(
                            value = campoOracion, onValueChange = {}, readOnly = true,
                            label = { Text("Tipo de oración") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedOracion) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            placeholder = { Text("Seleccionar") }
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
            ) { Text("Asignar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun EditarHermanoDialog(
    nombreActual: String,
    fechaDiscursoActual: Timestamp?,
    fechaOracionActual: Timestamp?,
    nombresExistentes: List<String>,
    onConfirm: (String, Timestamp?, Timestamp?) -> Unit,
    onDismiss: () -> Unit
) {
    var nombre by remember { mutableStateOf(nombreActual) }
    var showDuplicadoWarning by remember { mutableStateOf(false) }
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
        return
    }

    if (showPickerOracion) {
        FechaSelectorDialog(
            onFechaSeleccionada = { fechaOracion = Timestamp(it); showPickerOracion = false },
            onDismiss = { showPickerOracion = false }
        )
        return
    }

    if (showDuplicadoWarning) {
        AlertDialog(
            onDismissRequest = { showDuplicadoWarning = false },
            title = { Text("¿Nombre duplicado?") },
            text = { Text("Ya existe \"$nombreDuplicado\" que parece el mismo nombre. ¿Querés guardarlo igual?") },
            confirmButton = {
                TextButton(onClick = { onConfirm(nombre.trim(), fechaDiscurso, fechaOracion); showDuplicadoWarning = false }) { Text("Guardar igual") }
            },
            dismissButton = { TextButton(onClick = { showDuplicadoWarning = false }) { Text("Cancelar") } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar hermano/a") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre, onValueChange = { nombre = it },
                    label = { Text("Nombre completo") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true
                )
                Text(
                    "Última participación conocida",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = fechaDiscurso?.let { dateFormat.format(it.toDate()) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Último discurso") },
                    placeholder = { Text("Sin datos") },
                    trailingIcon = {
                        Row {
                            if (fechaDiscurso != null) {
                                IconButton(onClick = { fechaDiscurso = null }) {
                                    Icon(Icons.Default.Close, "Borrar", modifier = Modifier.size(16.dp))
                                }
                            }
                            IconButton(onClick = { showPickerDiscurso = true }) {
                                Icon(Icons.Default.DateRange, "Seleccionar")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fechaOracion?.let { dateFormat.format(it.toDate()) } ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Última oración") },
                    placeholder = { Text("Sin datos") },
                    trailingIcon = {
                        Row {
                            if (fechaOracion != null) {
                                IconButton(onClick = { fechaOracion = null }) {
                                    Icon(Icons.Default.Close, "Borrar", modifier = Modifier.size(16.dp))
                                }
                            }
                            IconButton(onClick = { showPickerOracion = true }) {
                                Icon(Icons.Default.DateRange, "Seleccionar")
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
                            onConfirm(nombre.trim(), fechaDiscurso, fechaOracion)
                        }
                    }
                },
                enabled = nombre.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FechaSelectorDialog(
    onFechaSeleccionada: (java.util.Date) -> Unit,
    onDismiss: () -> Unit
) {
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
            }) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
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