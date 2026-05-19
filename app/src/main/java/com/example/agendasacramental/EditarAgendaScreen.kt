package com.example.agendasacramental

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
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


// Devuelve el nombre del himno según el idioma del contexto
fun getNombreHimno(numero: Int, context: android.content.Context): String {
    val lang = context.resources.configuration.locales[0].language
    return if (lang == "en") HimnosEn.getNombre(numero) else Himnos.getNombre(numero)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarAgendaScreen(
    numeroUnidad: String,
    agendaId: String?,
    userEmail: String,
    onBack: () -> Unit,
    onModoLectura: (Agenda) -> Unit = {}
) {
    val repository = remember { AgendaRepository() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var isLoading by remember { mutableStateOf(agendaId != null) }
    var isSaving by remember { mutableStateOf(false) }
    var errorGuardado by remember { mutableStateOf("") }
    var showSolicitudDialog by remember { mutableStateOf(false) }
    var nombresUsados by remember { mutableStateOf<List<String>>(emptyList()) }

    var fechaDate by remember { mutableStateOf(Date()) }
    var estado by remember { mutableStateOf(EstadoAgenda.BORRADOR) }
    var asistencia by remember { mutableStateOf("") }
    var preside by remember { mutableStateOf("") }
    var dirige by remember { mutableStateOf("") }
    var reconocimientos by remember { mutableStateOf<List<String>>(emptyList()) }
    var anuncios by remember { mutableStateOf<List<String>>(emptyList()) }
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
    var reunionTestimonios by remember { mutableStateOf(esPrimerDomingoDelMes(fechaDate)) }
    var testimonios by remember { mutableStateOf<List<String>>(emptyList()) }
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
                    .split(",").map { it.trim() }.filter { it.isNotBlank() }
                anuncios = agenda.anuncios
                    .split(",").map { it.trim() }.filter { it.isNotBlank() }
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
                reunionTestimonios = agenda.reunionTestimonios
                testimonios = agenda.testimonios
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
            reconocimientos = reconocimientos.filter { it.isNotBlank() }.joinToString(", "),
            anuncios = anuncios.filter { it.isNotBlank() }.joinToString(", "),
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
            reunionTestimonios = reunionTestimonios,
            testimonios = testimonios.filter { it.isNotBlank() },
            creadoPor = userEmail,
            creadoEn = Timestamp.now(),
            ultimaEdicionPor = userEmail,
            ultimaEdicionEn = Timestamp.now()
        )
    }

    fun compartirWhatsApp(context: Context) {
        val texto = generarTextoAgenda(buildAgenda(), dateFormat, context)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            val intentGeneral = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, texto)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intentGeneral, context.getString(R.string.editar_enviar_agenda)))
        }
    }

    if (showSolicitudDialog) {
        SolicitudRecomendacionesDialog(
            agenda = buildAgenda(),
            dateFormat = dateFormat,
            context = context,
            onDismiss = { showSolicitudDialog = false }
        )
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
                title = { Text(if (agendaId == null) stringResource(R.string.editar_agenda_nueva) else stringResource(R.string.editar_agenda_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { onModoLectura(buildAgenda()) }) {
                        Icon(Icons.Default.MenuBook, stringResource(R.string.editar_modo_lectura))
                    }
                    Box {
                        var showCompartirMenu by remember { mutableStateOf(false) }
                        IconButton(onClick = { showCompartirMenu = true }) {
                            Icon(Icons.Default.Share, "Compartir")
                        }
                        DropdownMenu(
                            expanded = showCompartirMenu,
                            onDismissRequest = { showCompartirMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.editar_enviar_agenda)) },
                                leadingIcon = { Icon(Icons.Default.Share, null) },
                                onClick = { compartirWhatsApp(context); showCompartirMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.editar_exportar_pdf)) },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, null) },
                                onClick = { GeneradorPDF.generarYCompartir(context, buildAgenda()); showCompartirMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.editar_pedir_sugerencias)) },
                                leadingIcon = { Icon(Icons.Default.GroupAdd, null) },
                                onClick = { showSolicitudDialog = true; showCompartirMenu = false }
                            )
                        }
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
                                    else errorGuardado = "✓ ${context.getString(R.string.editar_guardado_ok)}"
                                } else if (result.exceptionOrNull()?.message == "FECHA_DUPLICADA") {
                                    errorGuardado = context.getString(R.string.editar_fecha_duplicada)
                                } else {
                                    errorGuardado = context.getString(R.string.editar_error_guardar)
                                }
                            }
                        },
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Check, stringResource(R.string.btn_guardar))
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
                        stringResource(R.string.editar_agenda_titulo_card),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Text(
                            text = stringResource(R.string.editar_cita_dc),
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
                                onFechaChange = { nuevaFecha ->
                                    val fechaAnteriorEraPrimerDomingo = esPrimerDomingoDelMes(fechaDate)
                                    fechaDate = nuevaFecha
                                    when {
                                        esPrimerDomingoDelMes(nuevaFecha) -> reunionTestimonios = true
                                        fechaAnteriorEraPrimerDomingo && testimonios.all { it.isBlank() } -> reunionTestimonios = false
                                    }
                                },
                                dateFormat = dateFormat
                            )
                        }
                        OutlinedTextField(
                            value = asistencia,
                            onValueChange = { asistencia = it },
                            label = { Text(stringResource(R.string.editar_asistencia)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(80.dp),
                            singleLine = true
                        )
                    }
                }

                item { EstadoSelector(estado = estado, onEstadoChange = { estado = it }) }

                item {
                    SeccionTestimonios(
                        activa = reunionTestimonios,
                        testimonios = testimonios,
                        onActivaChange = { reunionTestimonios = it },
                        onTestimoniosChange = { testimonios = it }
                    )
                }

                item {
                    CampoConAutocompletado(
                        valor = preside, onValorChange = { preside = it },
                        label = stringResource(R.string.editar_preside), sugerencias = nombresUsados
                    )
                }

                item {
                    CampoConAutocompletado(
                        valor = dirige, onValorChange = { dirige = it },
                        label = stringResource(R.string.editar_dirige), sugerencias = nombresUsados
                    )
                }

                item {
                    ListaItemsEditor(
                        titulo = stringResource(R.string.editar_reconocimientos),
                        items = reconocimientos,
                        onItemsChange = { reconocimientos = it },
                        placeholder = stringResource(R.string.editar_agregar_reconocimiento)
                    )
                }

                item {
                    ListaItemsEditor(
                        titulo = stringResource(R.string.editar_anuncios),
                        items = anuncios,
                        onItemsChange = { anuncios = it },
                        placeholder = stringResource(R.string.editar_agregar_anuncio)
                    )
                }

                item {
                    CampoHimno(
                        label = stringResource(R.string.editar_primer_himno), numero = primerHimnoNumero, nombre = primerHimnoNombre,
                        onNumeroChange = { num ->
                            primerHimnoNumero = num
                            val n = num.toIntOrNull()
                            if (num.isBlank()) primerHimnoNombre = ""
                            else if (n != null) primerHimnoNombre = getNombreHimno(n, context).ifEmpty { primerHimnoNombre }
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
                                label = stringResource(R.string.editar_director_musica),
                                sugerencias = nombresUsados
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            CampoConAutocompletado(
                                valor = pianista,
                                onValorChange = { pianista = it },
                                label = stringResource(R.string.editar_pianista),
                                sugerencias = nombresUsados
                            )
                        }
                    }
                }

                item {
                    CampoConAutocompletado(
                        valor = primeraOracion, onValorChange = { primeraOracion = it },
                        label = stringResource(R.string.editar_primera_oracion), sugerencias = nombresUsados
                    )
                }

                item {
                    TablaAsuntos(asuntos = asuntos, onAsuntosChange = { asuntos = it })
                }

                item {
                    CampoHimno(
                        label = stringResource(R.string.editar_himno_sacramental), numero = himnoSacramentalNumero, nombre = himnoSacramentalNombre,
                        onNumeroChange = { num ->
                            himnoSacramentalNumero = num
                            val n = num.toIntOrNull()
                            if (num.isBlank()) himnoSacramentalNombre = ""
                            else if (n != null) himnoSacramentalNombre = getNombreHimno(n, context).ifEmpty { himnoSacramentalNombre }
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
                        label = stringResource(R.string.editar_himno_final), numero = himnoFinalNumero, nombre = himnoFinalNombre,
                        onNumeroChange = { num ->
                            himnoFinalNumero = num
                            val n = num.toIntOrNull()
                            if (num.isBlank()) himnoFinalNombre = ""
                            else if (n != null) himnoFinalNombre = getNombreHimno(n, context).ifEmpty { himnoFinalNombre }
                        },
                        onNombreChange = { himnoFinalNombre = it }
                    )
                }

                item {
                    CampoConAutocompletado(
                        valor = oracionFinal, onValorChange = { oracionFinal = it },
                        label = stringResource(R.string.editar_oracion_final), sugerencias = nombresUsados
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
    val context = LocalContext.current
    var showPicker by remember { mutableStateOf(false) }
    val calendar = Calendar.getInstance().also { it.time = fecha }

    OutlinedTextField(
        value = dateFormat.format(fecha),
        onValueChange = {},
        label = { Text(stringResource(R.string.editar_fecha)) },
        readOnly = true,
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.DateRange, stringResource(R.string.editar_seleccionar_fecha))
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
    val context = LocalContext.current
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
            }) { Text(stringResource(R.string.btn_aceptar)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancelar)) } }
    ) {
        DatePicker(state = datePickerState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EstadoSelector(estado: EstadoAgenda, onEstadoChange: (EstadoAgenda) -> Unit) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = context.getString(estado.stringResId),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.editar_estado)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            EstadoAgenda.values().forEach { e ->
                DropdownMenuItem(text = { Text(context.getString(e.stringResId)) }, onClick = { onEstadoChange(e); expanded = false })
            }
        }
    }
}

@Composable
fun SeccionTestimonios(
    activa: Boolean,
    testimonios: List<String>,
    onActivaChange: (Boolean) -> Unit,
    onTestimoniosChange: (List<String>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.editar_reunion_testimonios),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = activa, onCheckedChange = onActivaChange)
        }
        if (activa) {
            ListaItemsEditor(
                titulo = stringResource(R.string.pdf_reunion_testimonios),
                items = testimonios,
                onItemsChange = onTestimoniosChange,
                placeholder = stringResource(R.string.editar_agregar_testimonio)
            )
        }
    }
}

@Composable
fun CampoHimno(
    label: String, numero: String, nombre: String,
    onNumeroChange: (String) -> Unit, onNombreChange: (String) -> Unit
) {
    val context = LocalContext.current
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = numero, onValueChange = onNumeroChange,
                label = { Text(stringResource(R.string.editar_himno_numero)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp), singleLine = true
            )
            OutlinedTextField(
                value = nombre, onValueChange = onNombreChange,
                label = { Text(stringResource(R.string.editar_himno_nombre)) },
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
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val filtradas = remember(valor, sugerencias) {
        if (valor.isBlank()) emptyList()
        else sugerencias.filter { it.contains(valor, ignoreCase = true) && it != valor }
    }

    // Detectar nombre similar (misma normalización pero texto diferente)
    val nombreSimilar = remember(valor, sugerencias) {
        if (valor.isBlank()) null
        else sugerencias.firstOrNull { s ->
            normalizarNombre(s) == normalizarNombre(valor) && s != valor
        }
    }

    Column {
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
        if (nombreSimilar != null) {
            Text(
                "¿Quisiste decir \"$nombreSimilar\"?",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .padding(start = 4.dp, top = 2.dp)
                    .clickable { onValorChange(nombreSimilar); expanded = false }
            )
        }
    }
}

@Composable
fun ListaItemsEditor(
    titulo: String,
    items: List<String>,
    onItemsChange: (List<String>) -> Unit,
    placeholder: String = stringResource(R.string.editar_agregar_reconocimiento)
) {
    val context = LocalContext.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                titulo,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onItemsChange(items + "") }) {
                Icon(Icons.Default.Add, stringResource(R.string.btn_agregar))
            }
        }
        items.forEachIndexed { index, item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("•", modifier = Modifier.padding(end = 6.dp, top = 4.dp))
                OutlinedTextField(
                    value = item,
                    onValueChange = { nuevo ->
                        onItemsChange(items.toMutableList().also { it[index] = nuevo })
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(placeholder) },
                    singleLine = true
                )
                IconButton(onClick = {
                    onItemsChange(items.toMutableList().also { it.removeAt(index) })
                }) {
                    Icon(Icons.Default.Close, stringResource(R.string.btn_eliminar), tint = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        if (items.isEmpty()) {
            Text(
                "Sin $titulo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
fun TablaAsuntos(
    asuntos: List<AsuntoEstacaBarrio>,
    onAsuntosChange: (List<AsuntoEstacaBarrio>) -> Unit
) {
    val context = LocalContext.current
    var asuntoFormula by remember { mutableStateOf<AsuntoEstacaBarrio?>(null) }

    // Diálogo fórmula litúrgica — agrupa todos los del mismo tipo
    asuntoFormula?.let { asunto ->
        val delMismoTipo = asuntos.filter { it.tipo == asunto.tipo }
        val titulo = "Fórmula — ${context.getString(asunto.tipo.stringResId)}"
        val texto = generarFormulaLiturgica(asunto.tipo, delMismoTipo, context)
        AlertDialog(
            onDismissRequest = { asuntoFormula = null },
            title = { Text(titulo) },
            text = {
                Text(texto, style = MaterialTheme.typography.bodyMedium)
            },
            confirmButton = {
                TextButton(onClick = { asuntoFormula = null }) { Text(stringResource(R.string.btn_cerrar)) }
            }
        )
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.editar_asuntos),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            if (asuntos.size < 20) {
                IconButton(onClick = { onAsuntosChange(asuntos + AsuntoEstacaBarrio()) }) {
                    Icon(Icons.Default.Add, stringResource(R.string.editar_agregar_reconocimiento))
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
                isLast = index == asuntos.size - 1,
                onVerFormula = { asuntoFormula = asunto }
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
    isLast: Boolean,
    onVerFormula: () -> Unit = {}
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExposedDropdownMenuBox(
                    expanded = expanded, onExpandedChange = { expanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = context.getString(asunto.tipo.stringResId), onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        TipoAsunto.values().forEach { tipo ->
                            DropdownMenuItem(
                                text = { Text(context.getString(tipo.stringResId)) },
                                onClick = {
                                    onAsuntoChange(
                                        if (tipo == TipoAsunto.OTROS) asunto.copy(tipo = tipo, columna3 = "")
                                        else asunto.copy(tipo = tipo)
                                    )
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                // Botón ver fórmula litúrgica
                if (asunto.tipo != TipoAsunto.OTROS) {
                    IconButton(onClick = onVerFormula) {
                        Icon(
                            Icons.Default.MenuBook,
                            contentDescription = stringResource(R.string.editar_ver_formula),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.editar_subir), tint = if (!isFirst) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.editar_bajar), tint = if (!isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Close, stringResource(R.string.btn_eliminar), tint = MaterialTheme.colorScheme.error)
                }
            }
            if (asunto.tipo == TipoAsunto.OTROS) {
                OutlinedTextField(
                    value = asunto.columna2,
                    onValueChange = { onAsuntoChange(asunto.copy(columna2 = it, columna3 = "")) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.editar_texto_libre)) },
                    placeholder = { Text(stringResource(R.string.editar_otros_placeholder)) },
                    minLines = 3
                )
            } else {
                OutlinedTextField(
                    value = asunto.columna2, onValueChange = { onAsuntoChange(asunto.copy(columna2 = it)) },
                    modifier = Modifier.fillMaxWidth(), placeholder = { Text(stringResource(R.string.editar_nombre)) }, minLines = 1
                )
                OutlinedTextField(
                    value = asunto.columna3, onValueChange = { onAsuntoChange(asunto.copy(columna3 = it)) },
                    modifier = Modifier.fillMaxWidth(), placeholder = { Text(stringResource(R.string.editar_cargo)) }, minLines = 1
                )
            }
        }
    }
}

@Composable
fun TablaMensajes(
    mensajes: List<MensajeEvangelio>,
    onMensajesChange: (List<MensajeEvangelio>) -> Unit,
    nombresUsados: List<String>
) {
    val context = LocalContext.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.editar_mensajes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onMensajesChange(mensajes + MensajeEvangelio()) }) {
                Icon(Icons.Default.Add, stringResource(R.string.btn_agregar))
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
    val context = LocalContext.current
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
                        value = context.getString(mensaje.tipo.stringResId), onValueChange = {}, readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(), singleLine = true
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        TipoMensaje.values().forEach { tipo ->
                            DropdownMenuItem(
                                text = { Text(context.getString(tipo.stringResId)) },
                                onClick = { onMensajeChange(mensaje.copy(tipo = tipo)); expanded = false }
                            )
                        }
                    }
                }
                // Botones de reordenar
                IconButton(onClick = onMoveUp, enabled = !isFirst) {
                    Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.editar_subir), tint = if (!isFirst) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                }
                IconButton(onClick = onMoveDown, enabled = !isLast) {
                    Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.editar_bajar), tint = if (!isLast) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Close, stringResource(R.string.btn_eliminar), tint = MaterialTheme.colorScheme.error)
                }
            }

            if (mensaje.tipo == TipoMensaje.HIMNO_INTERMEDIO) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = himnoNumero,
                        onValueChange = { num ->
                            himnoNumero = num
                            val n = num.toIntOrNull()
                            val nombre = if (num.isBlank()) "" else if (n != null) getNombreHimno(n, context).ifEmpty { mensaje.himnoNombre } else mensaje.himnoNombre
                            onMensajeChange(mensaje.copy(himnoNumero = n ?: 0, himnoNombre = nombre))
                        },
                        label = { Text(stringResource(R.string.editar_himno_numero)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(80.dp), singleLine = true
                    )
                    OutlinedTextField(
                        value = mensaje.himnoNombre,
                        onValueChange = { onMensajeChange(mensaje.copy(himnoNombre = it)) },
                        label = { Text(stringResource(R.string.editar_himno_nombre)) },
                        modifier = Modifier.weight(1f), singleLine = true
                    )
                }
            } else {
                CampoConAutocompletado(
                    valor = mensaje.nombre,
                    onValorChange = { onMensajeChange(mensaje.copy(nombre = it)) },
                    label = if (mensaje.tipo == TipoMensaje.TESTIMONIO) stringResource(R.string.editar_nombre) else stringResource(R.string.editar_discursante),
                    sugerencias = nombresUsados
                )
            }
        }
    }
}

fun generarTextoAgenda(agenda: Agenda, dateFormat: SimpleDateFormat, context: android.content.Context): String {
    val sb = StringBuilder()
    sb.appendLine("📋 *${context.getString(R.string.editar_agenda_titulo_card).uppercase()}*")
    sb.appendLine("_D&C 46:2_")
    sb.appendLine()
    sb.appendLine("📅 ${context.getString(R.string.editar_fecha)}: ${dateFormat.format(agenda.fecha.toDate())}")
    if (agenda.asistencia > 0) sb.appendLine("👥 ${context.getString(R.string.pdf_asistencia)}: ${agenda.asistencia}")
    if (agenda.preside.isNotBlank()) sb.appendLine("👤 ${context.getString(R.string.editar_preside)}: ${agenda.preside}")
    if (agenda.dirige.isNotBlank()) sb.appendLine("🎙 ${context.getString(R.string.editar_dirige)}: ${agenda.dirige}")
    val reconocimientosStr = agenda.reconocimientos.split(",").map { it.trim() }.filter { it.isNotBlank() }
    val anunciosStr = agenda.anuncios.split(",").map { it.trim() }.filter { it.isNotBlank() }
    if (reconocimientosStr.isNotEmpty()) {
        sb.appendLine("⭐ ${context.getString(R.string.editar_reconocimientos)}:")
        reconocimientosStr.forEach { sb.appendLine("  • $it") }
    }
    if (anunciosStr.isNotEmpty()) {
        sb.appendLine("📣 ${context.getString(R.string.editar_anuncios)}:")
        anunciosStr.forEach { sb.appendLine("  • $it") }
    }
    sb.appendLine()
    if (agenda.primerHimnoNumero > 0) sb.appendLine("🎵 ${context.getString(R.string.editar_primer_himno)}: ${agenda.primerHimnoNumero} - ${agenda.primerHimnoNombre}")
    if (agenda.directorMusica.isNotBlank()) sb.appendLine("🎼 ${context.getString(R.string.editar_director_musica)}: ${agenda.directorMusica}")
    if (agenda.pianista.isNotBlank()) sb.appendLine("🎹 ${context.getString(R.string.editar_pianista)}: ${agenda.pianista}")
    if (agenda.primeraOracion.isNotBlank()) sb.appendLine("🙏 ${context.getString(R.string.editar_primera_oracion)}: ${agenda.primeraOracion}")

    if (agenda.reunionTestimonios) {
        sb.appendLine()
        sb.appendLine("🗣 ${context.getString(R.string.pdf_reunion_testimonios)}:")
        if (agenda.testimonios.isEmpty()) {
            sb.appendLine("  —")
        } else {
            agenda.testimonios.forEach { sb.appendLine("  • $it") }
        }
    }

    if (agenda.asuntosEstacaBarrio.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("📌 ${context.getString(R.string.editar_asuntos)}:")
        agenda.asuntosEstacaBarrio.forEach {
            if (it.tipo == TipoAsunto.OTROS) {
                sb.appendLine("  • ${context.getString(it.tipo.stringResId)}:")
                it.columna2.lines().forEach { linea -> sb.appendLine("    $linea") }
            } else {
                sb.appendLine("  • ${context.getString(it.tipo.stringResId)}: ${it.columna2} ${it.columna3}")
            }
        }
    }

    sb.appendLine()
    if (agenda.himnoSacramentalNumero > 0) sb.appendLine("🎵 ${context.getString(R.string.editar_himno_sacramental)}: ${agenda.himnoSacramentalNumero} - ${agenda.himnoSacramentalNombre}")

    if (agenda.mensajesEvangelio.isNotEmpty()) {
        sb.appendLine()
        sb.appendLine("📖 ${context.getString(R.string.editar_mensajes)}:")
        agenda.mensajesEvangelio.forEach {
            when (it.tipo) {
                TipoMensaje.HIMNO_INTERMEDIO -> sb.appendLine("  🎵 Himno Intermedio: ${it.himnoNumero} - ${it.himnoNombre}")
                TipoMensaje.TESTIMONIO -> sb.appendLine("  👁️‍🗨️ Testimonio: ${it.nombre}")
                else -> sb.appendLine("  📖 ${context.getString(it.tipo.stringResId)}: ${it.nombre}")
            }
        }
    }

    sb.appendLine()
    if (agenda.himnoFinalNumero > 0) sb.appendLine("🎵 ${context.getString(R.string.editar_himno_final)}: ${agenda.himnoFinalNumero} - ${agenda.himnoFinalNombre}")
    if (agenda.oracionFinal.isNotBlank()) sb.appendLine("🙏 ${context.getString(R.string.editar_oracion_final)}: ${agenda.oracionFinal}")

    return sb.toString()
}

fun generarFormulaLiturgica(tipo: TipoAsunto, asuntos: List<AsuntoEstacaBarrio>, context: android.content.Context): String {
    if (asuntos.isEmpty()) return ""

    return if (asuntos.size == 1) {
        val nombre = asuntos[0].columna2.ifBlank { "[Nombre]" }
        val cargo = asuntos[0].columna3.ifBlank { "[Cargo]" }
        when (tipo) {
            TipoAsunto.RELEVO -> context.getString(R.string.pdf_relevo_singular, nombre, cargo)
            TipoAsunto.SOSTENIMIENTO -> context.getString(R.string.pdf_sostenimiento_singular, nombre, cargo)
            TipoAsunto.OTROS -> asuntos[0].columna2
        }
    } else {
        val como = context.getString(R.string.pdf_como)
        val lista = asuntos.joinToString(", ") { asunto ->
            val nombre = asunto.columna2.ifBlank { "[Nombre]" }
            val cargo = asunto.columna3.ifBlank { "[Cargo]" }
            "$nombre $como $cargo"
        }
        when (tipo) {
            TipoAsunto.RELEVO -> context.getString(R.string.pdf_relevo_plural, lista)
            TipoAsunto.SOSTENIMIENTO -> context.getString(R.string.pdf_sostenimiento_plural, lista)
            TipoAsunto.OTROS -> asuntos.joinToString("\n") { it.columna2 }
        }
    }
}
@Composable
fun SolicitudRecomendacionesDialog(
    agenda: Agenda,
    dateFormat: java.text.SimpleDateFormat,
    context: android.content.Context,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var himnoApertura by remember { mutableStateOf(false) }
    var himnoSacramental by remember { mutableStateOf(false) }
    var himnoIntermedio by remember { mutableStateOf(false) }
    var himnoFinal by remember { mutableStateOf(false) }
    var oracionPrimera by remember { mutableStateOf(false) }
    var oracionFinal by remember { mutableStateOf(false) }
    var discursos by remember { mutableStateOf(false) }
    var cantidadDiscursos by remember { mutableStateOf(1) }

    val hayAlgoSeleccionado = himnoApertura || himnoSacramental || himnoIntermedio ||
            himnoFinal || oracionPrimera || oracionFinal || discursos

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.editar_pedir_sugerencias)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    context.getString(R.string.solicitud_selecciona, dateFormat.format(agenda.fecha.toDate())),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(stringResource(R.string.solicitud_himnos), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                listOf(
                    stringResource(R.string.solicitud_apertura) to himnoApertura,
                    stringResource(R.string.solicitud_sacramental) to himnoSacramental,
                    stringResource(R.string.solicitud_intermedio) to himnoIntermedio,
                    stringResource(R.string.solicitud_final) to himnoFinal
                ).forEachIndexed { i, (label, checked) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checked, onCheckedChange = { v ->
                            when (i) {
                                0 -> himnoApertura = v
                                1 -> himnoSacramental = v
                                2 -> himnoIntermedio = v
                                3 -> himnoFinal = v
                            }
                        })
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(stringResource(R.string.solicitud_oraciones), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                listOf(
                    stringResource(R.string.solicitud_primera_oracion) to oracionPrimera,
                    stringResource(R.string.solicitud_oracion_final) to oracionFinal
                ).forEachIndexed { i, (label, checked) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = checked, onCheckedChange = { v ->
                            when (i) {
                                0 -> oracionPrimera = v
                                1 -> oracionFinal = v
                            }
                        })
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(stringResource(R.string.solicitud_discursos), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = discursos, onCheckedChange = { discursos = it })
                    Text(stringResource(R.string.plan_discursos), style = MaterialTheme.typography.bodyMedium)
                }
                if (discursos) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(start = 48.dp)
                    ) {
                        IconButton(onClick = { if (cantidadDiscursos > 1) cantidadDiscursos-- }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Remove, stringResource(R.string.solicitud_menos))
                        }
                        Text("$cantidadDiscursos", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { if (cantidadDiscursos < 10) cantidadDiscursos++ }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Add, stringResource(R.string.solicitud_mas))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val fecha = dateFormat.format(agenda.fecha.toDate())
                    val sb = StringBuilder()
                    sb.appendLine(context.getString(R.string.solicitud_mensaje_intro, fecha))
                    sb.appendLine(context.getString(R.string.solicitud_mensaje_cuerpo))
                    sb.appendLine()
                    val himnos = mutableListOf<String>()
                    if (himnoApertura) himnos.add(context.getString(R.string.solicitud_apertura))
                    if (himnoSacramental) himnos.add(context.getString(R.string.solicitud_sacramental))
                    if (himnoIntermedio) himnos.add(context.getString(R.string.solicitud_intermedio))
                    if (himnoFinal) himnos.add(context.getString(R.string.solicitud_final))
                    if (himnos.isNotEmpty()) sb.appendLine("🎵 ${context.getString(R.string.solicitud_himnos_label)}: ${himnos.joinToString(", ")}")
                    val oraciones = mutableListOf<String>()
                    if (oracionPrimera) oraciones.add(context.getString(R.string.solicitud_primera_oracion))
                    if (oracionFinal) oraciones.add(context.getString(R.string.solicitud_oracion_final))
                    if (oraciones.isNotEmpty()) sb.appendLine("🙏 ${context.getString(R.string.solicitud_oraciones_label)}: ${oraciones.joinToString(", ")}")
                    if (discursos) {
                        val texto = if (cantidadDiscursos == 1) context.getString(R.string.plan_un_discurso) else context.getString(R.string.plan_n_discursos, cantidadDiscursos)
                        sb.appendLine("🎤 ${context.getString(R.string.plan_discursos)}: $texto")
                    }
                    sb.appendLine()
                    sb.append(context.getString(R.string.solicitud_mensaje_cierre))

                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
                        setPackage("com.whatsapp")
                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val intentGeneral = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, sb.toString())
                            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(android.content.Intent.createChooser(intentGeneral, context.getString(R.string.solicitud_enviar_titulo)))
                    }
                    onDismiss()
                },
                enabled = hayAlgoSeleccionado
            ) { Text(stringResource(R.string.btn_enviar)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.btn_cancelar)) } }
    )
}
