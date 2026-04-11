package com.example.agendasacramental

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaAgendasScreen(
    numeroUnidad: String,
    userEmail: String,
    onNuevaAgenda: () -> Unit,
    onEditarAgenda: (String) -> Unit,
    onLogout: () -> Unit
) {
    val repository = remember { AgendaRepository() }
    val scope = rememberCoroutineScope()

    var agendas by remember { mutableStateOf<List<Agenda>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var agendaAEliminar by remember { mutableStateOf<Agenda?>(null) }
    var showCrearDomingos by remember { mutableStateOf(false) }

    var filtrosActivos by remember { mutableStateOf(setOf(EstadoAgenda.BORRADOR, EstadoAgenda.CONFIRMADA)) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val hoy = remember { Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.time }

    LaunchedEffect(numeroUnidad) {
        isLoading = true
        // Auto-marcar pasadas como realizadas
        repository.marcarAgendasPasadasComoRealizadas(numeroUnidad)
        val result = repository.getAgendas(numeroUnidad)
        agendas = result.getOrElse { emptyList() }
        isLoading = false
    }

    // Separar próximo domingo de las demás
    val proximoDomingo = remember(agendas) {
        agendas
            .filter { !it.fecha.toDate().before(hoy) && it.estado != EstadoAgenda.REALIZADA }
            .minByOrNull { it.fecha.toDate() }
    }

    val agendasFiltradas = remember(agendas, searchQuery, filtrosActivos, proximoDomingo) {
        agendas
            .filter { it.id != proximoDomingo?.id } // excluir el próximo para mostrarlo separado
            .filter { agenda -> filtrosActivos.isEmpty() || agenda.estado in filtrosActivos }
            .filter { agenda ->
                if (searchQuery.isBlank()) true
                else {
                    val fechaStr = dateFormat.format(agenda.fecha.toDate())
                    fechaStr.contains(searchQuery, ignoreCase = true) ||
                            agenda.preside.contains(searchQuery, ignoreCase = true) ||
                            agenda.dirige.contains(searchQuery, ignoreCase = true) ||
                            agenda.primeraOracion.contains(searchQuery, ignoreCase = true) ||
                            agenda.oracionFinal.contains(searchQuery, ignoreCase = true) ||
                            agenda.primerHimnoNombre.contains(searchQuery, ignoreCase = true) ||
                            agenda.himnoSacramentalNombre.contains(searchQuery, ignoreCase = true) ||
                            agenda.himnoFinalNombre.contains(searchQuery, ignoreCase = true) ||
                            agenda.primerHimnoNumero.toString().contains(searchQuery) ||
                            agenda.himnoSacramentalNumero.toString().contains(searchQuery) ||
                            agenda.himnoFinalNumero.toString().contains(searchQuery) ||
                            agenda.mensajesEvangelio.any {
                                it.nombre.contains(searchQuery, ignoreCase = true) ||
                                        it.himnoNombre.contains(searchQuery, ignoreCase = true) ||
                                        it.himnoNumero.toString().contains(searchQuery)
                            }
                }
            }
            .sortedWith(compareBy(
                // Primero futuras/hoy (no realizadas), luego realizadas
                { if (it.estado == EstadoAgenda.REALIZADA) 1 else 0 },
                // Futuras: más próximas primero; realizadas: más recientes primero
                { if (it.estado == EstadoAgenda.REALIZADA) -it.fecha.seconds else it.fecha.seconds }
            ))
    }

    // Diálogo eliminar agenda
    agendaAEliminar?.let { agenda ->
        AlertDialog(
            onDismissRequest = { agendaAEliminar = null },
            title = { Text("Eliminar agenda") },
            text = { Text("¿Está seguro que desea eliminar la agenda del ${dateFormat.format(agenda.fecha.toDate())}?") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.eliminarAgenda(agenda.id)
                        agendas = agendas.filter { it.id != agenda.id }
                        agendaAEliminar = null
                    }
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { agendaAEliminar = null }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo crear domingos en blanco
    if (showCrearDomingos) {
        CrearDomingosDialog(
            onConfirm = { hastaFecha ->
                scope.launch {
                    val result = repository.crearAgendasDomingos(numeroUnidad, userEmail, hastaFecha)
                    showCrearDomingos = false
                    // Recargar
                    isLoading = true
                    repository.marcarAgendasPasadasComoRealizadas(numeroUnidad)
                    agendas = repository.getAgendas(numeroUnidad).getOrElse { emptyList() }
                    isLoading = false
                    result.onSuccess { n ->
                        // podría mostrarse un snackbar
                    }
                }
            },
            onDismiss = { showCrearDomingos = false }
        )
    }

    Scaffold(
        topBar = {
            if (showSearch) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar por fecha, nombre, himno...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { showSearch = false; searchQuery = "" }) {
                            Icon(Icons.Default.ArrowBack, "Volver")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Unidad $numeroUnidad") },
                    actions = {
                        IconButton(onClick = { showCrearDomingos = true }) {
                            Icon(Icons.Default.CalendarMonth, "Crear domingos")
                        }
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, "Buscar")
                        }
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Default.ArrowBack, "Volver al menú")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNuevaAgenda) {
                Icon(Icons.Default.Add, "Nueva agenda")
            }
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            // Chips de filtro
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                EstadoAgenda.values().forEach { estado ->
                    val seleccionado = estado in filtrosActivos
                    FilterChip(
                        selected = seleccionado,
                        onClick = {
                            filtrosActivos = if (seleccionado) filtrosActivos - estado else filtrosActivos + estado
                        },
                        label = { Text(estado.label) },
                        leadingIcon = if (seleccionado) {
                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Próximo domingo destacado
                            proximoDomingo?.let { agenda ->
                                if (filtrosActivos.contains(agenda.estado) || filtrosActivos.isEmpty()) {
                                    item {
                                        Text(
                                            "PRÓXIMO DOMINGO",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                    item {
                                        AgendaCard(
                                            agenda = agenda,
                                            dateFormat = dateFormat,
                                            destacada = true,
                                            onClick = { onEditarAgenda(agenda.id) },
                                            onDelete = { agendaAEliminar = agenda }
                                        )
                                    }
                                    item { Divider(modifier = Modifier.padding(vertical = 8.dp)) }
                                }
                            }

                            if (agendasFiltradas.isEmpty() && proximoDomingo == null) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            "No hay agendas aún.\nToque + para crear una.",
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Separador si hay agendas futuras/pasadas
                            val futuras = agendasFiltradas.filter { it.estado != EstadoAgenda.REALIZADA }
                            val realizadas = agendasFiltradas.filter { it.estado == EstadoAgenda.REALIZADA }

                            if (futuras.isNotEmpty()) {
                                items(futuras) { agenda ->
                                    AgendaCard(
                                        agenda = agenda,
                                        dateFormat = dateFormat,
                                        onClick = { onEditarAgenda(agenda.id) },
                                        onDelete = { agendaAEliminar = agenda }
                                    )
                                }
                            }

                            if (realizadas.isNotEmpty() && EstadoAgenda.REALIZADA in filtrosActivos) {
                                item {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    Text(
                                        "REALIZADAS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                                items(realizadas) { agenda ->
                                    AgendaCard(
                                        agenda = agenda,
                                        dateFormat = dateFormat,
                                        onClick = { onEditarAgenda(agenda.id) },
                                        onDelete = { agendaAEliminar = agenda }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearDomingosDialog(
    onConfirm: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDate by remember {
        // Default: 3 meses desde hoy
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, 3)
        // Avanzar al próximo domingo
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) cal.add(Calendar.DAY_OF_MONTH, 1)
        mutableStateOf(cal.time)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                time = selectedDate
                set(Calendar.HOUR_OF_DAY, 12); set(Calendar.MINUTE, 0)
            }.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).also { it.timeInMillis = millis }
                        val localCal = Calendar.getInstance().apply {
                            set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                        }
                        selectedDate = localCal.time
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = datePickerState) }
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear domingos en blanco") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Se crearán agendas en blanco para todos los domingos desde hoy hasta la fecha elegida. Las fechas que ya tengan agenda serán ignoradas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = dateFormat.format(selectedDate),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Hasta el domingo") },
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, "Seleccionar fecha")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedDate) }) { Text("Crear") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun AgendaCard(
    agenda: Agenda,
    dateFormat: SimpleDateFormat,
    destacada: Boolean = false,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val estadoColor = when (agenda.estado) {
        EstadoAgenda.BORRADOR -> MaterialTheme.colorScheme.outline
        EstadoAgenda.CONFIRMADA -> MaterialTheme.colorScheme.primary
        EstadoAgenda.REALIZADA -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (destacada)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateFormat.format(agenda.fecha.toDate()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (destacada) FontWeight.Bold else FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = estadoColor.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = agenda.estado.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = estadoColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    if (agenda.asistencia > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "👥 ${agenda.asistencia}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (agenda.preside.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Preside: ${agenda.preside}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                if (agenda.ultimaEdicionPor.isNotBlank()) {
                    Text(
                        text = "Editado por: ${agenda.ultimaEdicionPor}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}