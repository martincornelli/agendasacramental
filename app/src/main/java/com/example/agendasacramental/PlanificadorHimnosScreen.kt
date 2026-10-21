package com.example.agendasacramental

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class FechaPlanificador(
    val fecha: Date,
    val agenda: Agenda?
)

private enum class FiltroBiblioteca { TODOS, NUEVOS, NO_USADOS, PLANIFICADOS, EXCLUIDOS }

private fun indiceCabeceraMesActual(fechas: List<FechaPlanificador>): Int {
    if (fechas.isEmpty()) return 0

    val hoy = Calendar.getInstance()
    val primeraFecha = Calendar.getInstance().apply { time = fechas.first().fecha }
    if (primeraFecha.get(Calendar.YEAR) != hoy.get(Calendar.YEAR)) return 0

    val mesObjetivo = hoy.get(Calendar.MONTH)
    var indiceLista = 0
    var mesAnterior = -1
    for (item in fechas) {
        val mesItem = Calendar.getInstance().apply { time = item.fecha }.get(Calendar.MONTH)
        if (mesItem != mesAnterior) {
            if (mesItem == mesObjetivo) return indiceLista
            indiceLista++ // Cabecera del mes.
            mesAnterior = mesItem
        }
        indiceLista++ // Tarjeta del domingo.
    }
    return 0
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanificadorHimnosScreen(
    numeroUnidad: String,
    userEmail: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AgendaRepository() }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val catalogo = remember(context.resources.configuration.locales[0].language) {
        if (context.resources.configuration.locales[0].language == "en") HimnosEn.himnos else Himnos.lista
    }
    var agendas by remember { mutableStateOf<List<Agenda>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var anio by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var tab by remember { mutableStateOf(0) }
    var fechaEnEdicion by remember { mutableStateOf<FechaPlanificador?>(null) }
    var configuracion by remember(numeroUnidad) {
        mutableStateOf(ConfiguracionPlanificacion(numeroUnidad = numeroUnidad))
    }
    var mostrarGenerador by remember { mutableStateOf(false) }
    var previsualizacion by remember { mutableStateOf<ResultadoGeneracionHimnos?>(null) }
    var guardandoGeneracion by remember { mutableStateOf(false) }

    suspend fun recargar(mostrarIndicador: Boolean = true) {
        if (mostrarIndicador) cargando = true
        agendas = repository.getAgendas(numeroUnidad).getOrElse { emptyList() }
        if (mostrarIndicador) cargando = false
    }

    LaunchedEffect(numeroUnidad) {
        cargando = true
        agendas = repository.getAgendas(numeroUnidad).getOrElse { emptyList() }
        configuracion = repository.getConfiguracion(numeroUnidad)
            ?: ConfiguracionPlanificacion(numeroUnidad = numeroUnidad)
        cargando = false
    }

    val agendasPorFecha = remember(agendas) { agendas.associateBy { claveFecha(it.fecha.toDate()) } }
    val fechas = remember(anio, agendasPorFecha) {
        domingosDelAnio(anio).map { FechaPlanificador(it, agendasPorFecha[claveFecha(it)]) }
    }
    val estadoListaFechas = remember(anio) { LazyListState() }
    var listaFechasInicializada by remember(anio) { mutableStateOf(false) }

    LaunchedEffect(cargando, fechas, anio) {
        if (!cargando && !listaFechasInicializada && fechas.isNotEmpty()) {
            estadoListaFechas.scrollToItem(indiceCabeceraMesActual(fechas))
            listaFechasInicializada = true
        }
    }

    if (mostrarGenerador) {
        ConfigurarGeneracionHimnosDialog(
            fechas = fechas,
            configuracion = configuracion,
            onDismiss = { mostrarGenerador = false },
            onPreview = { fechasElegidas, nuevaConfiguracion ->
                configuracion = nuevaConfiguracion
                mostrarGenerador = false
                previsualizacion = generarPlanificacionHimnos(
                    catalogo = catalogo,
                    agendas = agendas,
                    fechas = fechasElegidas,
                    opciones = OpcionesGeneracionHimnos(
                        intervaloMinimoDias = nuevaConfiguracion.intervaloMinimoHimnosDias,
                        soloFechasSinPlanificacion = nuevaConfiguracion.soloFechasSinHimnos,
                        reemplazarAutomaticas = nuevaConfiguracion.reemplazarHimnosAutomaticos,
                        priorizarTemas = nuevaConfiguracion.priorizarTemasHimnos,
                        forzarHimnoNuevo = nuevaConfiguracion.forzarHimnoNuevo,
                        incluirIntermedio = nuevaConfiguracion.generarHimnoIntermedio,
                        himnosExcluidos = nuevaConfiguracion.himnosExcluidos.toSet(),
                        semilla = numeroUnidad.hashCode().toLong() * 31L +
                                (fechasElegidas.firstOrNull()?.time ?: 0L)
                    )
                )
                scope.launch {
                    if (repository.guardarConfiguracion(nuevaConfiguracion).isFailure) {
                        snackbar.showSnackbar(context.getString(R.string.hymn_config_save_error))
                    }
                }
            }
        )
    }

    previsualizacion?.let { resultado ->
        PrevisualizacionGeneracionHimnosDialog(
            resultado = resultado,
            guardando = guardandoGeneracion,
            onDismiss = { previsualizacion = null },
            onConfirm = {
                scope.launch {
                    guardandoGeneracion = true
                    val result = repository.guardarPlanificacionesHimnos(
                        numeroUnidad = numeroUnidad,
                        planificaciones = resultado.propuestas.map { it.fecha to it.propuesta },
                        userEmail = userEmail
                    )
                    guardandoGeneracion = false
                    if (result.isSuccess) {
                        previsualizacion = null
                        recargar(mostrarIndicador = false)
                        snackbar.showSnackbar(
                            context.getString(R.string.hymn_preview_saved, result.getOrDefault(0))
                        )
                    } else {
                        snackbar.showSnackbar(context.getString(R.string.hymn_preview_save_error))
                    }
                }
            }
        )
    }

    fechaEnEdicion?.let { fechaPlanificador ->
        EditarPlanificacionHimnosDialog(
            fecha = fechaPlanificador.fecha,
            agenda = fechaPlanificador.agenda,
            catalogo = catalogo,
            onDismiss = { fechaEnEdicion = null },
            onSave = { planificacion ->
                scope.launch {
                    val result = repository.guardarPlanificacionHimnos(
                        numeroUnidad = numeroUnidad,
                        fecha = fechaPlanificador.fecha,
                        planificacion = planificacion,
                        userEmail = userEmail
                    )
                    if (result.isSuccess) {
                        fechaEnEdicion = null
                        recargar(mostrarIndicador = false)
                        snackbar.showSnackbar(context.getString(R.string.hymn_planner_saved))
                    } else {
                        snackbar.showSnackbar(context.getString(R.string.hymn_planner_save_error))
                    }
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hymn_planner_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.btn_volver))
                    }
                },
                actions = {
                    if (tab == 0) {
                        IconButton(onClick = { mostrarGenerador = true }) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                stringResource(R.string.hymn_generate_action)
                            )
                        }
                    }
                    IconButton(onClick = { anio-- }) {
                        Icon(Icons.Default.ChevronLeft, stringResource(R.string.hymn_planner_previous_year))
                    }
                    Text(anio.toString(), fontWeight = FontWeight.Bold)
                    IconButton(onClick = { anio++ }) {
                        Icon(Icons.Default.ChevronRight, stringResource(R.string.hymn_planner_next_year))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text(stringResource(R.string.hymn_planner_dates_tab)) },
                    icon = { Icon(Icons.Default.EventNote, null) }
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text(stringResource(R.string.hymn_planner_library_tab)) },
                    icon = { Icon(Icons.Default.LibraryMusic, null) }
                )
            }
            if (cargando) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (tab == 0) {
                PlanificacionPorFechas(
                    fechas = fechas,
                    estadoLista = estadoListaFechas,
                    onEditar = { fechaEnEdicion = it }
                )
            } else {
                BibliotecaHimnos(
                    catalogo = catalogo,
                    agendas = agendas,
                    anio = anio,
                    configuracion = configuracion,
                    onCambiarExclusion = { numero ->
                        val anterior = configuracion
                        val excluidos = configuracion.himnosExcluidos.toMutableSet().apply {
                            if (!add(numero)) remove(numero)
                        }.sorted()
                        val nueva = configuracion.copy(himnosExcluidos = excluidos)
                        configuracion = nueva
                        scope.launch {
                            if (repository.guardarConfiguracion(nueva).isSuccess) {
                                snackbar.showSnackbar(context.getString(R.string.hymn_exclusions_saved))
                            } else {
                                configuracion = anterior
                                snackbar.showSnackbar(context.getString(R.string.hymn_config_save_error))
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun PlanificacionPorFechas(
    fechas: List<FechaPlanificador>,
    estadoLista: LazyListState,
    onEditar: (FechaPlanificador) -> Unit
) {
    val locale = LocalContext.current.resources.configuration.locales[0]
    val mes = remember(locale) { SimpleDateFormat("MMMM yyyy", locale) }
    val fecha = remember(locale) { SimpleDateFormat("EEEE d", locale) }
    LazyColumn(
        state = estadoLista,
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        fechas.forEachIndexed { index, item ->
            val mesActual = mes.format(item.fecha)
            val mesAnterior = fechas.getOrNull(index - 1)?.let { mes.format(it.fecha) }
            if (mesActual != mesAnterior) {
                item(key = "month-$mesActual") {
                    Text(
                        mesActual.replaceFirstChar { it.titlecase(locale) },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = if (index == 0) 0.dp else 10.dp)
                    )
                }
            }
            item(key = claveFecha(item.fecha)) {
                FechaHimnosCard(item, fecha) { onEditar(item) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FechaHimnosCard(
    item: FechaPlanificador,
    formatoFecha: SimpleDateFormat,
    onClick: () -> Unit
) {
    val locale = LocalContext.current.resources.configuration.locales[0]
    val agenda = item.agenda
    val plan = planificacionDesdeAgenda(agenda)
    val selecciones = listOf(
        stringResource(R.string.hymn_position_opening) to plan.apertura,
        stringResource(R.string.hymn_position_sacrament) to plan.sacramental,
        stringResource(R.string.hymn_position_intermediate) to plan.intermedio,
        stringResource(R.string.hymn_position_closing) to plan.final
    ).filter { it.second.numero > 0 || it.second.nombre.isNotBlank() }
    val temas = temasDeAgenda(agenda)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selecciones.isEmpty()) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        formatoFecha.format(item.fecha).replaceFirstChar { it.titlecase(locale) },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        agenda?.let { stringResource(it.estado.stringResId) }
                            ?: stringResource(R.string.hymn_planner_no_agenda),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(Icons.Default.MusicNote, null, tint = MaterialTheme.colorScheme.primary)
            }
            if (selecciones.isEmpty()) {
                Text(
                    stringResource(R.string.hymn_planner_unplanned),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                selecciones.forEach { (label, seleccion) ->
                    Text(
                        "$label: ${formatearHimno(seleccion.numero, seleccion.nombre)}",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (temas.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    temas.take(4).forEach { tema ->
                        AssistChip(onClick = {}, label = { Text(tema, maxLines = 1) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BibliotecaHimnos(
    catalogo: Map<Int, String>,
    agendas: List<Agenda>,
    anio: Int,
    configuracion: ConfiguracionPlanificacion,
    onCambiarExclusion: (Int) -> Unit
) {
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    var busqueda by remember { mutableStateOf("") }
    var filtro by remember { mutableStateOf(FiltroBiblioteca.TODOS) }
    var categoria by remember { mutableStateOf<CategoriaHimno?>(null) }
    var menuCategorias by remember { mutableStateOf(false) }
    val formatoFecha = remember(locale) { SimpleDateFormat("dd/MM/yyyy", locale) }
    val historial = remember(catalogo, agendas, anio) { calcularHistorialHimnos(catalogo, agendas, anio) }
    val visibles = remember(historial, busqueda, filtro, categoria, configuracion.himnosExcluidos) {
        historial.filter { resumen ->
            val coincide = busqueda.isBlank() || resumen.numero.toString().contains(busqueda.trim()) ||
                    resumen.nombre.contains(busqueda.trim(), ignoreCase = true)
            val pasaFiltro = when (filtro) {
                FiltroBiblioteca.TODOS -> true
                FiltroBiblioteca.NUEVOS -> resumen.numero.toString().length == 4
                FiltroBiblioteca.NO_USADOS -> resumen.usosEnAnio == 0
                FiltroBiblioteca.PLANIFICADOS -> resumen.proximaPlanificacion != null
                FiltroBiblioteca.EXCLUIDOS -> resumen.numero in configuracion.himnosExcluidos
            }
            val pasaCategoria = categoria == null ||
                    categoria in metadataHimno(resumen.numero, resumen.nombre).categorias
            coincide && pasaFiltro && pasaCategoria
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(
            value = busqueda,
            onValueChange = { busqueda = it },
            label = { Text(stringResource(R.string.hymn_library_search)) },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            singleLine = true,
            trailingIcon = {
                if (busqueda.isNotBlank()) IconButton(onClick = { busqueda = "" }) {
                    Icon(Icons.Default.Close, stringResource(R.string.btn_cerrar))
                }
            }
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                FiltroBiblioteca.TODOS to R.string.hymn_filter_all,
                FiltroBiblioteca.NUEVOS to R.string.hymn_filter_new_collection,
                FiltroBiblioteca.NO_USADOS to R.string.hymn_filter_unused,
                FiltroBiblioteca.PLANIFICADOS to R.string.hymn_filter_planned,
                FiltroBiblioteca.EXCLUIDOS to R.string.hymn_filter_excluded
            ).forEach { (valor, label) ->
                FilterChip(
                    selected = filtro == valor,
                    onClick = { filtro = valor },
                    label = { Text(stringResource(label)) }
                )
            }
        }
        ExposedDropdownMenuBox(
            expanded = menuCategorias,
            onExpandedChange = { menuCategorias = it }
        ) {
            OutlinedTextField(
                value = categoria?.let { stringResource(it.stringResId) }
                    ?: stringResource(R.string.hymn_category_all),
                onValueChange = {},
                label = { Text(stringResource(R.string.hymn_category_filter)) },
                readOnly = true,
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menuCategorias) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = menuCategorias,
                onDismissRequest = { menuCategorias = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.hymn_category_all)) },
                    onClick = {
                        categoria = null
                        menuCategorias = false
                    }
                )
                CategoriaHimno.entries.forEach { opcion ->
                    DropdownMenuItem(
                        text = { Text(stringResource(opcion.stringResId)) },
                        onClick = {
                            categoria = opcion
                            menuCategorias = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
        ) {
            items(visibles, key = { it.numero }) { resumen ->
                val metadata = metadataHimno(resumen.numero, resumen.nombre)
                val excluido = resumen.numero in configuracion.himnosExcluidos
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                resumen.numero.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                resumen.nombre,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            if (resumen.numero.toString().length == 4) {
                                Icon(Icons.Default.AutoStories, stringResource(R.string.hymn_new_collection))
                            }
                        }
                        val ultimo = resumen.ultimaUtilizacion?.let { formatoFecha.format(it.toDate()) }
                            ?: context.getString(R.string.hymn_never_used)
                        Text(
                            context.resources.getQuantityString(
                                R.plurals.hymn_usage_summary,
                                resumen.usosEnAnio,
                                resumen.usosEnAnio,
                                anio,
                                resumen.usosTotales
                            ),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            context.getString(R.string.hymn_last_used, ultimo),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        resumen.proximaPlanificacion?.let {
                            Text(
                                context.getString(R.string.hymn_next_planned, formatoFecha.format(it.toDate())),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (metadata.categorias.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                metadata.categorias.take(3).forEach { tema ->
                                    AssistChip(
                                        onClick = { categoria = tema },
                                        label = { Text(stringResource(tema.stringResId)) }
                                    )
                                }
                            }
                        }
                        if (excluido) {
                            Text(
                                stringResource(R.string.hymn_excluded_status),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        TextButton(
                            onClick = { onCambiarExclusion(resumen.numero) },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(
                                if (excluido) Icons.Default.CheckCircle else Icons.Default.Block,
                                null,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                            Text(
                                stringResource(
                                    if (excluido) R.string.hymn_include_action
                                    else R.string.hymn_exclude_action
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditarPlanificacionHimnosDialog(
    fecha: Date,
    agenda: Agenda?,
    catalogo: Map<Int, String>,
    onDismiss: () -> Unit,
    onSave: (PlanificacionHimnos) -> Unit
) {
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    val inicial = remember(agenda) { planificacionDesdeAgenda(agenda) }
    var apertura by remember(agenda) { mutableStateOf(inicial.apertura) }
    var sacramental by remember(agenda) { mutableStateOf(inicial.sacramental) }
    var intermedio by remember(agenda) { mutableStateOf(inicial.intermedio) }
    var final by remember(agenda) { mutableStateOf(inicial.final) }
    val plan = PlanificacionHimnos(apertura, sacramental, intermedio, final)
    val duplicados = numerosDuplicados(plan)
    val cambios = plan != inicial
    val formato = remember(locale) {
        SimpleDateFormat(context.getString(R.string.hymn_planner_date_pattern), locale)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    stringResource(R.string.hymn_planner_edit_date),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    formato.format(fecha).replaceFirstChar { it.titlecase(locale) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        SelectorHimno(
                            label = stringResource(R.string.hymn_position_opening),
                            seleccion = apertura,
                            catalogo = catalogo,
                            onChange = { apertura = it }
                        )
                    }
                    item {
                        SelectorHimno(
                            label = stringResource(R.string.hymn_position_sacrament),
                            seleccion = sacramental,
                            catalogo = catalogo,
                            onChange = { sacramental = it }
                        )
                    }
                    item {
                        SelectorHimno(
                            label = stringResource(R.string.hymn_position_intermediate),
                            seleccion = intermedio,
                            catalogo = catalogo,
                            onChange = { intermedio = it }
                        )
                    }
                    item {
                        SelectorHimno(
                            label = stringResource(R.string.hymn_position_closing),
                            seleccion = final,
                            catalogo = catalogo,
                            onChange = { final = it }
                        )
                    }
                    if (duplicados.isNotEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.hymn_planner_duplicate_warning),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    if (cambios) {
                        item {
                            Text(
                                stringResource(R.string.hymn_planner_unsaved),
                                color = MaterialTheme.colorScheme.tertiary,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancelar)) }
                    TextButton(
                        onClick = { onSave(plan) },
                        enabled = cambios && duplicados.isEmpty()
                    ) { Text(stringResource(R.string.btn_guardar)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorHimno(
    label: String,
    seleccion: SeleccionHimno,
    catalogo: Map<Int, String>,
    onChange: (SeleccionHimno) -> Unit
) {
    var consulta by remember(seleccion.numero, seleccion.nombre) {
        mutableStateOf(formatearHimno(seleccion.numero, seleccion.nombre))
    }
    var expanded by remember { mutableStateOf(false) }
    val resultados = remember(consulta, catalogo) {
        val termino = consulta.trim()
        if (termino.isBlank()) emptyList()
        else catalogo.entries.filter {
            it.key.toString().contains(termino, ignoreCase = true) ||
                    it.value.contains(termino, ignoreCase = true)
        }.take(8)
    }

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        ExposedDropdownMenuBox(
            expanded = expanded && resultados.isNotEmpty(),
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = consulta,
                onValueChange = {
                    consulta = it
                    expanded = true
                    if (it.isBlank()) onChange(SeleccionHimno())
                },
                label = { Text(label) },
                placeholder = { Text(stringResource(R.string.hymn_picker_placeholder)) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                singleLine = true,
                trailingIcon = {
                    if (seleccion.numero > 0 || seleccion.nombre.isNotBlank()) {
                        IconButton(onClick = {
                            consulta = ""
                            expanded = false
                            onChange(SeleccionHimno())
                        }) { Icon(Icons.Default.Close, stringResource(R.string.btn_eliminar)) }
                    } else {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                    }
                }
            )
            ExposedDropdownMenu(
                expanded = expanded && resultados.isNotEmpty(),
                onDismissRequest = { expanded = false }
            ) {
                resultados.forEach { (numero, nombre) ->
                    DropdownMenuItem(
                        text = { Text("$numero - $nombre") },
                        onClick = {
                            val nueva = SeleccionHimno(numero, nombre, OrigenHimno.PLANIFICADOR_MANUAL)
                            consulta = formatearHimno(numero, nombre)
                            expanded = false
                            onChange(nueva)
                        }
                    )
                }
            }
        }
        if (seleccion.origen.isNotBlank()) {
            Text(
                when (seleccion.origen) {
                    OrigenHimno.PLANIFICADOR_MANUAL -> stringResource(R.string.hymn_origin_planner_manual)
                    OrigenHimno.AUTOMATICA -> stringResource(R.string.hymn_origin_automatic)
                    else -> stringResource(R.string.hymn_origin_agenda)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
