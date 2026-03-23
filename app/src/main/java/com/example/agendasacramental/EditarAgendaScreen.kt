package com.example.agendasacramental

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarAgendaScreen(
    numeroUnidad: String,
    agendaId: String?,
    userEmail: String,
    onBack: () -> Unit
) {
    val repository = remember { AgendaRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(agendaId != null) }
    var isSaving by remember { mutableStateOf(false) }
    var nombresUsados by remember { mutableStateOf<List<String>>(emptyList()) }

    // Campos de la agenda
    var fechaDate by remember { mutableStateOf(Date()) }
    var estado by remember { mutableStateOf(EstadoAgenda.BORRADOR) }
    var preside by remember { mutableStateOf("") }
    var dirige by remember { mutableStateOf("") }
    var reconocimientos by remember { mutableStateOf("") }
    var anuncios by remember { mutableStateOf("") }
    var primerHimnoNumero by remember { mutableStateOf("") }
    var primerHimnoNombre by remember { mutableStateOf("") }
    var himnoSacramentalNumero by remember { mutableStateOf("") }
    var himnoSacramentalNombre by remember { mutableStateOf("") }
    var himnoFinalNumero by remember { mutableStateOf("") }
    var himnoFinalNombre by remember { mutableStateOf("") }
    var primeraOracion by remember { mutableStateOf("") }
    var oracionFinal by remember { mutableStateOf("") }
    var asuntos by remember { mutableStateOf<List<AsuntoEstacaBarrio>>(emptyList()) }
    var mensajes by remember { mutableStateOf<List<MensajeEvangelio>>(emptyList()) }
    var metadataInfo by remember { mutableStateOf("") }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // Cargar agenda existente o nombres usados
    LaunchedEffect(agendaId) {
        nombresUsados = repository.getNombresUsados(numeroUnidad)
        if (agendaId != null) {
            val result = repository.getAgenda(agendaId)
            result.onSuccess { agenda ->
                fechaDate = agenda.fecha.toDate()
                estado = agenda.estado
                preside = agenda.preside
                dirige = agenda.dirige
                reconocimientos = agenda.reconocimientos
                anuncios = agenda.anuncios
                primerHimnoNumero = if (agenda.primerHimnoNumero > 0) agenda.primerHimnoNumero.toString() else ""
                primerHimnoNombre = agenda.primerHimnoNombre
                himnoSacramentalNumero = if (agenda.himnoSacramentalNumero > 0) agenda.himnoSacramentalNumero.toString() else ""
                himnoSacramentalNombre = agenda.himnoSacramentalNombre
                himnoFinalNumero = if (agenda.himnoFinalNumero > 0) agenda.himnoFinalNumero.toString() else ""
                himnoFinalNombre = agenda.himnoFinalNombre
                primeraOracion = agenda.primeraOracion
                oracionFinal = agenda.oracionFinal
                asuntos = agenda.asuntosEstacaBarrio
                mensajes = agenda.mensajesEvangelio
                metadataInfo = if (agenda.ultimaEdicionPor.isNotBlank())
                    "Última edición: ${agenda.ultimaEdicionPor} — ${dateFormat.format(agenda.ultimaEdicionEn.toDate())}"
                else ""
            }
            isLoading = false
        }
    }

    fun buildAgenda(): Agenda {
        return Agenda(
            id = agendaId ?: "",
            numeroUnidad = numeroUnidad,
            fecha = Timestamp(fechaDate),
            estado = estado,
            preside = preside,
            dirige = dirige,
            reconocimientos = reconocimientos,
            anuncios = anuncios,
            primerHimnoNumero = primerHimnoNumero.toIntOrNull() ?: 0,
            primerHimnoNombre = primerHimnoNombre,
            himnoSacramentalNumero = himnoSacramentalNumero.toIntOrNull() ?: 0,
            himnoSacramentalNombre = himnoSacramentalNombre,
            himnoFinalNumero = himnoFinalNumero.toIntOrNull() ?: 0,
            himnoFinalNombre = himnoFinalNombre,
            primeraOracion = primeraOracion,
            oracionFinal = oracionFinal,
            asuntosEstacaBarrio = asuntos,
            mensajesEvangelio = mensajes,
            creadoPor = userEmail,
            creadoEn = Timestamp.now(),
            ultimaEdicionPor = userEmail,
            ultimaEdicionEn = Timestamp.now()
        )
    }

    fun compartirWhatsApp(context: Context) {
        val texto = generarTextoAgenda(buildAgenda(), dateFormat)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
            setPackage("com.whatsapp")
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // WhatsApp no instalado, compartir general
            val intentGeneral = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, texto)
            }
            context.startActivity(Intent.createChooser(intentGeneral, "Compartir agenda"))
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (agendaId == null) "Nueva Agenda" else "Editar Agenda") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { compartirWhatsApp(context) }) {
                        Icon(Icons.Default.Share, "Compartir")
                    }
                    IconButton(
                        onClick = {
                            isSaving = true
                            scope.launch {
                                repository.guardarAgenda(buildAgenda(), userEmail)
                                isSaving = false
                                onBack()
                            }
                        },
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Check, "Guardar")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Título fijo
            item {
                Text(
                    "Agenda de Reunión Sacramental",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Cita fija
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text(
                        text = "D&C 46:2: Pero a pesar de las cosas que están escritas, siempre se ha concedido a los élderes de mi iglesia desde el principio, y siempre será así, dirigir todas las reuniones conforme los oriente y los guíe el Santo Espíritu.",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Fecha
            item {
                FechaSelector(
                    fecha = fechaDate,
                    onFechaChange = { fechaDate = it },
                    dateFormat = dateFormat
                )
            }

            // Estado
            item {
                EstadoSelector(estado = estado, onEstadoChange = { estado = it })
            }

            // Preside
            item {
                CampoConAutocompletado(
                    valor = preside,
                    onValorChange = { preside = it },
                    label = "Preside",
                    sugerencias = nombresUsados
                )
            }

            // Dirige
            item {
                CampoConAutocompletado(
                    valor = dirige,
                    onValorChange = { dirige = it },
                    label = "Dirige",
                    sugerencias = nombresUsados
                )
            }

            // Reconocimientos
            item {
                OutlinedTextField(
                    value = reconocimientos,
                    onValueChange = { reconocimientos = it },
                    label = { Text("Reconocimientos") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            // Anuncios
            item {
                OutlinedTextField(
                    value = anuncios,
                    onValueChange = { anuncios = it },
                    label = { Text("Anuncios") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }

            // Primer Himno
            item {
                CampoHimno(
                    label = "Primer Himno",
                    numero = primerHimnoNumero,
                    nombre = primerHimnoNombre,
                    onNumeroChange = { num ->
                        primerHimnoNumero = num
                        val n = num.toIntOrNull()
                        if (n != null) primerHimnoNombre = Himnos.getNombre(n).ifEmpty { primerHimnoNombre }
                    },
                    onNombreChange = { primerHimnoNombre = it }
                )
            }

            // Primera Oración
            item {
                CampoConAutocompletado(
                    valor = primeraOracion,
                    onValorChange = { primeraOracion = it },
                    label = "Primera Oración",
                    sugerencias = nombresUsados
                )
            }

            // Asuntos Estaca/Barrio
            item {
                TablaAsuntos(
                    asuntos = asuntos,
                    onAsuntosChange = { asuntos = it }
                )
            }

            // Himno Sacramental
            item {
                CampoHimno(
                    label = "Himno Sacramental",
                    numero = himnoSacramentalNumero,
                    nombre = himnoSacramentalNombre,
                    onNumeroChange = { num ->
                        himnoSacramentalNumero = num
                        val n = num.toIntOrNull()
                        if (n != null) himnoSacramentalNombre = Himnos.getNombre(n).ifEmpty { himnoSacramentalNombre }
                    },
                    onNombreChange = { himnoSacramentalNombre = it }
                )
            }

            // Mensajes del Evangelio
            item {
                TablaMensajes(
                    mensajes = mensajes,
                    onMensajesChange = { mensajes = it },
                    nombresUsados = nombresUsados
                )
            }

            // Himno Final
            item {
                CampoHimno(
                    label = "Himno Final",
                    numero = himnoFinalNumero,
                    nombre = himnoFinalNombre,
                    onNumeroChange = { num ->
                        himnoFinalNumero = num
                        val n = num.toIntOrNull()
                        if (n != null) himnoFinalNombre = Himnos.getNombre(n).ifEmpty { himnoFinalNombre }
                    },
                    onNombreChange = { himnoFinalNombre = it }
                )
            }

            // Oración Final
            item {
                CampoConAutocompletado(
                    valor = oracionFinal,
                    onValorChange = { oracionFinal = it },
                    label = "Oración Final",
                    sugerencias = nombresUsados
                )
            }

            // Metadata
            if (metadataInfo.isNotBlank()) {
                item {
                    Text(
                        text = metadataInfo,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

// --- Componente: Selector de fecha ---
@Composable
fun FechaSelector(fecha: Date, onFechaChange: (Date) -> Unit, dateFormat: SimpleDateFormat) {
    var showPicker by remember { mutableStateOf(false) }
    val calendar = Calendar.getInstance().also { it.time = fecha }

    OutlinedTextField(
        value = dateFormat.format(fecha),
        onValueChange = {},
        label = { Text("Fecha") },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.DateRange, "Seleccionar fecha")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )

    if (showPicker) {
        DatePickerDialog(
            onDismiss = { showPicker = false },
            onDateSelected = { year, month, day ->
                calendar.set(year, month, day)
                onFechaChange(calendar.time)
                showPicker = false
            },
            initialYear = calendar.get(Calendar.YEAR),
            initialMonth = calendar.get(Calendar.MONTH),
            initialDay = calendar.get(Calendar.DAY_OF_MONTH)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (Int, Int, Int) -> Unit,
    initialYear: Int,
    initialMonth: Int,
    initialDay: Int
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
            set(initialYear, initialMonth, initialDay, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    // Use UTC calendar to avoid timezone day shift
                    val cal = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).also { it.timeInMillis = millis }
                    onDateSelected(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                }
            }) { Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    ) {
        DatePicker(state = datePickerState)
    }
}

// --- Componente: Selector de estado ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadoSelector(estado: EstadoAgenda, onEstadoChange: (EstadoAgenda) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = estado.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Estado") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            EstadoAgenda.values().forEach { e ->
                DropdownMenuItem(
                    text = { Text(e.label) },
                    onClick = { onEstadoChange(e); expanded = false }
                )
            }
        }
    }
}

// --- Componente: Campo himno ---
@Composable
fun CampoHimno(
    label: String,
    numero: String,
    nombre: String,
    onNumeroChange: (String) -> Unit,
    onNombreChange: (String) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = numero,
                onValueChange = onNumeroChange,
                label = { Text("Nº") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = nombre,
                onValueChange = onNombreChange,
                label = { Text("Nombre del himno") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }
    }
}

// --- Componente: Campo con autocompletado ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoConAutocompletado(
    valor: String,
    onValorChange: (String) -> Unit,
    label: String,
    sugerencias: List<String>
) {
    var expanded by remember { mutableStateOf(false) }
    val filtradas = remember(valor, sugerencias) {
        if (valor.isBlank()) emptyList()
        else sugerencias.filter { it.contains(valor, ignoreCase = true) && it != valor }
    }

    ExposedDropdownMenuBox(
        expanded = expanded && filtradas.isNotEmpty(),
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = valor,
            onValueChange = { onValorChange(it); expanded = true },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            singleLine = true
        )
        if (filtradas.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filtradas.take(5).forEach { sugerencia ->
                    DropdownMenuItem(
                        text = { Text(sugerencia) },
                        onClick = { onValorChange(sugerencia); expanded = false }
                    )
                }
            }
        }
    }
}

// --- Componente: Tabla asuntos estaca/barrio ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablaAsuntos(
    asuntos: List<AsuntoEstacaBarrio>,
    onAsuntosChange: (List<AsuntoEstacaBarrio>) -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Asuntos Estaca/Barrio",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            if (asuntos.size < 20) {
                IconButton(onClick = {
                    onAsuntosChange(asuntos + AsuntoEstacaBarrio())
                }) {
                    Icon(Icons.Default.Add, "Agregar fila")
                }
            }
        }
        asuntos.forEachIndexed { index, asunto ->
            AsuntoRow(
                asunto = asunto,
                onAsuntoChange = { nuevo ->
                    onAsuntosChange(asuntos.toMutableList().also { it[index] = nuevo })
                },
                onDelete = {
                    onAsuntosChange(asuntos.toMutableList().also { it.removeAt(index) })
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsuntoRow(
    asunto: AsuntoEstacaBarrio,
    onAsuntoChange: (AsuntoEstacaBarrio) -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = asunto.tipo.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        TipoAsunto.values().forEach { tipo ->
                            DropdownMenuItem(
                                text = { Text(tipo.label) },
                                onClick = { onAsuntoChange(asunto.copy(tipo = tipo)); expanded = false }
                            )
                        }
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Close, "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
            OutlinedTextField(
                value = asunto.columna2,
                onValueChange = { onAsuntoChange(asunto.copy(columna2 = it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Nombre") },
                minLines = 1
            )
            OutlinedTextField(
                value = asunto.columna3,
                onValueChange = { onAsuntoChange(asunto.copy(columna3 = it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Cargo / Detalle") },
                minLines = 1
            )
        }
    }
}

// --- Componente: Tabla mensajes del evangelio ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TablaMensajes(
    mensajes: List<MensajeEvangelio>,
    onMensajesChange: (List<MensajeEvangelio>) -> Unit,
    nombresUsados: List<String>
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Mensajes del Evangelio",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                onMensajesChange(mensajes + MensajeEvangelio())
            }) {
                Icon(Icons.Default.Add, "Agregar")
            }
        }
        mensajes.forEachIndexed { index, mensaje ->
            MensajeRow(
                mensaje = mensaje,
                onMensajeChange = { nuevo ->
                    onMensajesChange(mensajes.toMutableList().also { it[index] = nuevo })
                },
                onDelete = {
                    onMensajesChange(mensajes.toMutableList().also { it.removeAt(index) })
                },
                nombresUsados = nombresUsados
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MensajeRow(
    mensaje: MensajeEvangelio,
    onMensajeChange: (MensajeEvangelio) -> Unit,
    onDelete: () -> Unit,
    nombresUsados: List<String>
) {
    var expanded by remember { mutableStateOf(false) }
    var himnoNumero by remember { mutableStateOf(if (mensaje.himnoNumero > 0) mensaje.himnoNumero.toString() else "") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it },
                    modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = mensaje.tipo.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        TipoMensaje.values().forEach { tipo ->
                            DropdownMenuItem(
                                text = { Text(tipo.label) },
                                onClick = { onMensajeChange(mensaje.copy(tipo = tipo)); expanded = false }
                            )
                        }
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Close, "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }

            if (mensaje.tipo == TipoMensaje.HIMNO_INTERMEDIO) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = himnoNumero,
                        onValueChange = { num ->
                            himnoNumero = num
                            val n = num.toIntOrNull()
                            val nombre = if (n != null) Himnos.getNombre(n).ifEmpty { mensaje.himnoNombre } else mensaje.himnoNombre
                            onMensajeChange(mensaje.copy(himnoNumero = n ?: 0, himnoNombre = nombre))
                        },
                        label = { Text("Nº") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = mensaje.himnoNombre,
                        onValueChange = { onMensajeChange(mensaje.copy(himnoNombre = it)) },
                        label = { Text("Nombre del himno") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            } else {
                CampoConAutocompletado(
                    valor = mensaje.nombre,
                    onValorChange = { onMensajeChange(mensaje.copy(nombre = it)) },
                    label = if (mensaje.tipo == TipoMensaje.TESTIMONIO) "Nombre" else "Discursante",
                    sugerencias = nombresUsados
                )
            }
        }
    }
}

// --- Función para generar texto para compartir ---
fun generarTextoAgenda(agenda: Agenda, dateFormat: SimpleDateFormat): String {
    val sb = StringBuilder()
    sb.appendLine("📋 *AGENDA DE REUNIÓN SACRAMENTAL*")
    sb.appendLine("_D&C 46:2_")
    sb.appendLine()
    sb.appendLine("📅 Fecha: ${dateFormat.format(agenda.fecha.toDate())}")
    if (agenda.preside.isNotBlank()) sb.appendLine("👤 Preside: ${agenda.preside}")
    if (agenda.dirige.isNotBlank()) sb.appendLine("🎙 Dirige: ${agenda.dirige}")
    if (agenda.reconocimientos.isNotBlank()) sb.appendLine("⭐ Reconocimientos: ${agenda.reconocimientos}")
    if (agenda.anuncios.isNotBlank()) sb.appendLine("📣 Anuncios: ${agenda.anuncios}")
    sb.appendLine()
    if (agenda.primerHimnoNumero > 0) sb.appendLine("🎵 Primer Himno: ${agenda.primerHimnoNumero} - ${agenda.primerHimnoNombre}")
    if (agenda.primeraOracion.isNotBlank()) sb.appendLine("🙏 Primera Oración: ${agenda.primeraOracion}")

    if (agenda.asuntosEstacaBarrio.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("📌 Asuntos Estaca/Barrio:")
        agenda.asuntosEstacaBarrio.forEach {
            sb.appendLine("  • ${it.tipo.label}: ${it.columna2} ${it.columna3}")
        }
    }

    sb.appendLine()
    if (agenda.himnoSacramentalNumero > 0) sb.appendLine("🎵 Himno Sacramental: ${agenda.himnoSacramentalNumero} - ${agenda.himnoSacramentalNombre}")

    if (agenda.mensajesEvangelio.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("✝️ Mensajes del Evangelio:")
        agenda.mensajesEvangelio.forEach {
            when (it.tipo) {
                TipoMensaje.HIMNO_INTERMEDIO -> sb.appendLine("  • Himno Intermedio: ${it.himnoNumero} - ${it.himnoNombre}")
                else -> sb.appendLine("  • ${it.tipo.label}: ${it.nombre}")
            }
        }
    }

    sb.appendLine()
    if (agenda.himnoFinalNumero > 0) sb.appendLine("🎵 Himno Final: ${agenda.himnoFinalNumero} - ${agenda.himnoFinalNombre}")
    if (agenda.oracionFinal.isNotBlank()) sb.appendLine("🙏 Oración Final: ${agenda.oracionFinal}")

    return sb.toString()
}