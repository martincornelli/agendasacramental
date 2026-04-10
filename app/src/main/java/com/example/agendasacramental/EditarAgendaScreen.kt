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
    var errorGuardado by remember { mutableStateOf("") }
    var nombresUsados by remember { mutableStateOf<List<String>>(emptyList()) }

    var fechaDate by remember { mutableStateOf(Date()) }
    var estado by remember { mutableStateOf(EstadoAgenda.BORRADOR) }
    var asistencia by remember { mutableStateOf("") }
    var preside by remember { mutableStateOf("") }
    var dirige by remember { mutableStateOf("") }
    var reconocimientos by remember { mutableStateOf("") }
    var anuncios by remember { mutableStateOf("") }
    var primerHimnoNumero by remember { mutableStateOf("") }
    var primerHimnoNombre by remember { mutableStateOf("") }
    var directorMusica by remember { mutableStateOf("") }
    var pianista by remember { mutableStateOf("") }
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

    LaunchedEffect(agendaId) {
        nombresUsados = repository.getNombresUsados(numeroUnidad)
        if (agendaId != null) {
            val result = repository.getAgenda(agendaId)
            result.onSuccess { agenda ->
                fechaDate = agenda.fecha.toDate()
                estado = agenda.estado
                asistencia = if (agenda.asistencia > 0) agenda.asistencia.toString() else ""
                preside = agenda.preside
                dirige = agenda.dirige
                reconocimientos = agenda.reconocimientos
                anuncios = agenda.anuncios
                primerHimnoNumero = if (agenda.primerHimnoNumero > 0) agenda.primerHimnoNumero.toString() else ""
                primerHimnoNombre = agenda.primerHimnoNombre
                directorMusica = agenda.directorMusica
                pianista = agenda.pianista
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
            asistencia = asistencia.toIntOrNull() ?: 0,
            preside = preside,
            dirige = dirige,
            reconocimientos = reconocimientos,
            anuncios = anuncios,
            primerHimnoNumero = primerHimnoNumero.toIntOrNull() ?: 0,
            primerHimnoNombre = primerHimnoNombre,
            directorMusica = directorMusica,
            pianista = pianista,
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
                        Icon(Icons.Default.Share, "Compartir WhatsApp")
                    }
                    IconButton(onClick = { GeneradorPDF.generarYCompartir(context, buildAgenda()) }) {
                        Icon(Icons.Default.PictureAsPdf, "Exportar PDF")
                    }
                    IconButton(
                        onClick = {
                            isSaving = true
                            scope.launch {
                                val result = repository.guardarAgenda(buildAgenda(), userEmail)
                                isSaving = false
                                if (result.isSuccess) {
                                    errorGuardado = ""
                                    // Recargar el agendaId si es nueva para habilitar PDF
                                    if (agendaId == null) onBack()
                                    else errorGuardado = "✓ Guardado correctamente."
                                } else if (result.exceptionOrNull()?.message == "FECHA_DUPLICADA") {
                                    errorGuardado = "Ya existe una agenda para esa fecha."
                                } else {
                                    errorGuardado = "Error al guardar. Intente nuevamente."
                                }
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (errorGuardado.isNotBlank()) {
                val esExito = errorGuardado.startsWith("✓")
                androidx.compose.material3.Surface(
                    color = if (esExito) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        errorGuardado,
                        color = if (esExito) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    Text(
                        "Agenda de Reunión Sacramental",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

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

                // Fecha y Asistencia en la misma fila
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
                        Box(modifier = Modifier.weight(1f)) {
                            FechaSelector(
                                fecha = fechaDate,
                                onFechaChange = { fechaDate = it },
                                dateFormat = dateFormat
                            )
                        }
                        OutlinedTextField(
                            value = asistencia,
                            onValueChange = { asistencia = it },
                            label = { Text("Asist.") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(80.dp),
                            singleLine = true
                        )
                    }
                }

                item { EstadoSelector(estado = estado, onEstadoChange = { estado = it }) }

                item {
                    CampoConAutocompletado(
                        valor = preside, onValorChange = { preside = it },
                        label = "Preside", sugerencias = nombresUsados
                    )
                }

                item {
                    CampoConAutocompletado(
                        valor = dirige, onValorChange = { dirige = it },
                        label = "Dirige", sugerencias = nombresUsados
                    )
                }

                item {
                    OutlinedTextField(
                        value = reconocimientos, onValueChange = { reconocimientos = it },
                        label = { Text("Reconocimientos") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2
                    )
                }

                item {
                    OutlinedTextField(
                        value = anuncios, onValueChange = { anuncios = it },
                        label = { Text("Anuncios") },
                        modifier = Modifier.fillMaxWidth(), minLines = 2
                    )
                }

                item {
                    CampoHimno(
                        label = "Primer Himno", numero = primerHimnoNumero, nombre = primerHimnoNombre,
                        onNumeroChange = { num ->
                            primerHimnoNumero = num
                            val n = num.toIntOrNull()
                            if (num.isBlank()) primerHimnoNombre = ""
                            else if (n != null) primerHimnoNombre = Himnos.getNombre(n).ifEmpty { primerHimnoNombre }
                        },
                        onNombreChange = { primerHimnoNombre = it }
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            CampoConAutocompletado(
                                valor = directorMusica,
                                onValorChange = { directorMusica = it },
                                label = "Director/a de música",
                                sugerencias = nombresUsados
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CampoConAutocompletado(
                                valor = pianista,
                                onValorChange = { pianista = it },
                                label = "Pianista",
                                sugerencias = nombresUsados
                            )
                        }
                    }
                }

                item {
                    CampoConAutocompletado(
                        valor = primeraOracion, onValorChange = { primeraOracion = it },
                        label = "Primera Oración", sugerencias = nombresUsados
                    )
                }

                item {
                    TablaAsuntos(asuntos = asuntos, onAsuntosChange = { asuntos = it })
                }

                item {
                    CampoHimno(
                        label = "Himno Sacramental", numero = himnoSacramentalNumero, nombre = himnoSacramentalNombre,
                        onNumeroChange = { num ->
                            himnoSacramentalNumero = num
                            val n = num.toIntOrNull()
                            if (num.isBlank()) himnoSacramentalNombre = ""
                            else if (n != null) himnoSacramentalNombre = Himnos.getNombre(n).ifEmpty { himnoSacramentalNombre }
                        },
                        onNombreChange = { himnoSacramentalNombre = it }
                    )
                }

                item {
                    TablaMensajes(
                        mensajes = mensajes, onMensajesChange = { mensajes = it },
                        nombresUsados = nombresUsados
                    )
                }

                item {
                    CampoHimno(
                        label = "Himno Final", numero = himnoFinalNumero, nombre = himnoFinalNombre,
                        onNumeroChange = { num ->
                            himnoFinalNumero = num
                            val n = num.toIntOrNull()
                            if (num.isBlank()) himnoFinalNombre = ""
                            else if (n != null) himnoFinalNombre = Himnos.getNombre(n).ifEmpty { himnoFinalNombre }
                        },
                        onNombreChange = { himnoFinalNombre = it }
                    )
                }

                item {
                    CampoConAutocompletado(
                        valor = oracionFinal, onValorChange = { oracionFinal = it },
                        label = "Oración Final", sugerencias = nombresUsados
                    )
                }

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
        } // close Column
    }
}

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
                DropdownMenuItem(text = { Text(e.label) }, onClick = { onEstadoChange(e); expanded = false })
            }
        }
    }
}

@Composable
fun CampoHimno(
    label: String, numero: String, nombre: String,
    onNumeroChange: (String) -> Unit, onNombreChange: (String) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = numero, onValueChange = onNumeroChange,
                label = { Text("Nº") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp), singleLine = true
            )
            OutlinedTextField(
                value = nombre, onValueChange = onNombreChange,
                label = { Text("Nombre del himno") },
                modifier = Modifier.weight(1f), singleLine = true
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampoConAutocompletado(
    valor: String, onValorChange: (String) -> Unit,
    label: String, sugerencias: List<String>
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
            value = valor, onValueChange = { onValorChange(it); expanded = true },
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true
        )
        if (filtradas.isNotEmpty()) {
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                filtradas.take(5).forEach { sugerencia ->
                    DropdownMenuItem(text = { Text(sugerencia) }, onClick = { onValorChange(sugerencia); expanded = false })
                }
            }
        }
    }
}

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
                IconButton(onClick = { onAsuntosChange(asuntos + AsuntoEstacaBarrio()) }) {
                    Icon(Icons.Default.Add, "Agregar fila")
                }
            }
        }
        asuntos.forEachIndexed { index, asunto ->
            AsuntoRow(
                asunto = asunto,
                onAsuntoChange = { nuevo -> onAsuntosChange(asuntos.toMutableList().also { it[index] = nuevo }) },
                onDelete = { onAsuntosChange(asuntos.toMutableList().also { it.removeAt(index) }) },
                onMoveUp = { if (index > 0) onAsuntosChange(asuntos.toMutableList().also { it.add(index - 1, it.removeAt(index)) }) },
                onMoveDown = { if (index < asuntos.size - 1) onAsuntosChange(asuntos.toMutableList().also { it.add(index + 1, it.removeAt(index)) }) },
                isFirst = index == 0,
                isLast = index == asuntos.size - 1
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
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    isFirst: Boolean,
    isLast: Boolean
) {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExposedDropdownMenuBox(
                    expanded = expanded, onExpandedChange = { expanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = asunto.tipo.label, onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true
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
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(Icons.Default.KeyboardArrowUp, "Subir", tint = if (!isFirst) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(Icons.Default.KeyboardArrowDown, "Bajar", tint = if (!isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Close, "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
            OutlinedTextField(
                value = asunto.columna2, onValueChange = { onAsuntoChange(asunto.copy(columna2 = it)) },
                modifier = Modifier.fillMaxWidth(), placeholder = { Text("Nombre") }, minLines = 1
            )
            OutlinedTextField(
                value = asunto.columna3, onValueChange = { onAsuntoChange(asunto.copy(columna3 = it)) },
                modifier = Modifier.fillMaxWidth(), placeholder = { Text("Cargo / Detalle") }, minLines = 1
            )
        }
    }
}

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
            IconButton(onClick = { onMensajesChange(mensajes + MensajeEvangelio()) }) {
                Icon(Icons.Default.Add, "Agregar")
            }
        }
        mensajes.forEachIndexed { index, mensaje ->
            MensajeRow(
                mensaje = mensaje,
                onMensajeChange = { nuevo -> onMensajesChange(mensajes.toMutableList().also { it[index] = nuevo }) },
                onDelete = { onMensajesChange(mensajes.toMutableList().also { it.removeAt(index) }) },
                onMoveUp = { if (index > 0) onMensajesChange(mensajes.toMutableList().also { it.add(index - 1, it.removeAt(index)) }) },
                onMoveDown = { if (index < mensajes.size - 1) onMensajesChange(mensajes.toMutableList().also { it.add(index + 1, it.removeAt(index)) }) },
                isFirst = index == 0,
                isLast = index == mensajes.size - 1,
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
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    isFirst: Boolean,
    isLast: Boolean,
    nombresUsados: List<String>
) {
    var expanded by remember { mutableStateOf(false) }
    var himnoNumero by remember { mutableStateOf(if (mensaje.himnoNumero > 0) mensaje.himnoNumero.toString() else "") }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExposedDropdownMenuBox(
                    expanded = expanded, onExpandedChange = { expanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = mensaje.tipo.label, onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true
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
                // Botones de reordenar
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(Icons.Default.KeyboardArrowUp, "Subir", tint = if (!isFirst) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(Icons.Default.KeyboardArrowDown, "Bajar", tint = if (!isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
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
                            val nombre = if (num.isBlank()) "" else if (n != null) Himnos.getNombre(n).ifEmpty { mensaje.himnoNombre } else mensaje.himnoNombre
                            onMensajeChange(mensaje.copy(himnoNumero = n ?: 0, himnoNombre = nombre))
                        },
                        label = { Text("Nº") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp), singleLine = true
                    )
                    OutlinedTextField(
                        value = mensaje.himnoNombre,
                        onValueChange = { onMensajeChange(mensaje.copy(himnoNombre = it)) },
                        label = { Text("Nombre del himno") },
                        modifier = Modifier.weight(1f), singleLine = true
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

fun generarTextoAgenda(agenda: Agenda, dateFormat: SimpleDateFormat): String {
    val sb = StringBuilder()
    sb.appendLine("📋 *AGENDA DE REUNIÓN SACRAMENTAL*")
    sb.appendLine("_D&C 46:2_")
    sb.appendLine()
    sb.appendLine("📅 Fecha: ${dateFormat.format(agenda.fecha.toDate())}")
    if (agenda.asistencia > 0) sb.appendLine("👥 Asistencia: ${agenda.asistencia}")
    if (agenda.preside.isNotBlank()) sb.appendLine("👤 Preside: ${agenda.preside}")
    if (agenda.dirige.isNotBlank()) sb.appendLine("🎙 Dirige: ${agenda.dirige}")
    if (agenda.reconocimientos.isNotBlank()) sb.appendLine("⭐ Reconocimientos: ${agenda.reconocimientos}")
    if (agenda.anuncios.isNotBlank()) sb.appendLine("📣 Anuncios: ${agenda.anuncios}")
    sb.appendLine()
    if (agenda.primerHimnoNumero > 0) sb.appendLine("🎵 Primer Himno: ${agenda.primerHimnoNumero} - ${agenda.primerHimnoNombre}")
    if (agenda.directorMusica.isNotBlank()) sb.appendLine("🎼 Director/a: ${agenda.directorMusica}")
    if (agenda.pianista.isNotBlank()) sb.appendLine("🎹 Pianista: ${agenda.pianista}")
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
        sb.appendLine("📖 Mensajes del Evangelio:")
        agenda.mensajesEvangelio.forEach {
            when (it.tipo) {
                TipoMensaje.HIMNO_INTERMEDIO -> sb.appendLine("  🎵 Himno Intermedio: ${it.himnoNumero} - ${it.himnoNombre}")
                TipoMensaje.TESTIMONIO -> sb.appendLine("  👁️‍🗨️ Testimonio: ${it.nombre}")
                else -> sb.appendLine("  📖 ${it.tipo.label}: ${it.nombre}")
            }
        }
    }

    sb.appendLine()
    if (agenda.himnoFinalNumero > 0) sb.appendLine("🎵 Himno Final: ${agenda.himnoFinalNumero} - ${agenda.himnoFinalNombre}")
    if (agenda.oracionFinal.isNotBlank()) sb.appendLine("🙏 Oración Final: ${agenda.oracionFinal}")

    return sb.toString()
}