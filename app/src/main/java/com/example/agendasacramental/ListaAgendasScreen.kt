package com.example.agendasacramental

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    // Por defecto BORRADOR y CONFIRMADA seleccionados, REALIZADA no
    var filtrosActivos by remember { mutableStateOf(setOf(EstadoAgenda.BORRADOR, EstadoAgenda.CONFIRMADA)) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    LaunchedEffect(numeroUnidad) {
        isLoading = true
        val result = repository.getAgendas(numeroUnidad)
        agendas = result.getOrElse { emptyList() }
        isLoading = false
    }

    val agendasFiltradas = remember(agendas, searchQuery, filtrosActivos) {
        agendas
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
    }

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

            // Chips de filtro por estado
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EstadoAgenda.values().forEach { estado ->
                    val seleccionado = estado in filtrosActivos
                    FilterChip(
                        selected = seleccionado,
                        onClick = {
                            filtrosActivos = if (seleccionado) {
                                filtrosActivos - estado
                            } else {
                                filtrosActivos + estado
                            }
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
                    agendasFiltradas.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                if (searchQuery.isBlank() && filtrosActivos.size == EstadoAgenda.values().size)
                                    "No hay agendas aún.\nToque + para crear una."
                                else "No se encontraron resultados.",
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(agendasFiltradas) { agenda ->
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

@Composable
fun AgendaCard(
    agenda: Agenda,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val estadoColor = when (agenda.estado) {
        EstadoAgenda.BORRADOR -> MaterialTheme.colorScheme.outline
        EstadoAgenda.CONFIRMADA -> MaterialTheme.colorScheme.primary
        EstadoAgenda.REALIZADA -> MaterialTheme.colorScheme.secondary
    }

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = dateFormat.format(agenda.fecha.toDate()), style = MaterialTheme.typography.titleMedium)
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