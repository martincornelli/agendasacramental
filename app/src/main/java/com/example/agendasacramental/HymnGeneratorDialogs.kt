package com.example.agendasacramental

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigurarGeneracionHimnosDialog(
    fechas: List<FechaPlanificador>,
    configuracion: ConfiguracionPlanificacion,
    onDismiss: () -> Unit,
    onPreview: (List<Date>, ConfiguracionPlanificacion) -> Unit
) {
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    val hoy = remember { inicioDelDia(Date()) }
    val disponibles = remember(fechas, hoy) {
        fechas.map { it.fecha }.filter { !it.before(hoy) }.sorted()
    }
    val formatoFecha = remember(locale) {
        SimpleDateFormat(context.getString(R.string.hymn_planner_date_pattern), locale)
    }
    var inicio by remember(disponibles) { mutableStateOf(disponibles.firstOrNull()) }
    var fin by remember(disponibles) { mutableStateOf(disponibles.lastOrNull()) }
    var intervalo by remember(configuracion.intervaloMinimoHimnosDias) {
        mutableStateOf(configuracion.intervaloMinimoHimnosDias.toString())
    }
    var soloVacias by remember(configuracion.soloFechasSinHimnos) {
        mutableStateOf(configuracion.soloFechasSinHimnos)
    }
    var reemplazarAutomaticas by remember(configuracion.reemplazarHimnosAutomaticos) {
        mutableStateOf(configuracion.reemplazarHimnosAutomaticos)
    }
    var forzarNuevo by remember(configuracion.forzarHimnoNuevo) {
        mutableStateOf(configuracion.forzarHimnoNuevo)
    }
    var priorizarTemas by remember(configuracion.priorizarTemasHimnos) {
        mutableStateOf(configuracion.priorizarTemasHimnos)
    }
    var incluirIntermedio by remember(configuracion.generarHimnoIntermedio) {
        mutableStateOf(configuracion.generarHimnoIntermedio)
    }
    val intervaloNumero = intervalo.toIntOrNull()
    val rangoValido = inicio != null && fin != null && !inicio!!.after(fin) &&
            intervaloNumero != null && intervaloNumero in 0..3650

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    stringResource(R.string.hymn_generate_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.hymn_generate_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                if (disponibles.isEmpty()) {
                    Text(
                        stringResource(R.string.hymn_generate_no_future_dates),
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 540.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            SelectorFechaGeneracion(
                                label = stringResource(R.string.hymn_generate_start),
                                fechas = disponibles,
                                seleccion = inicio,
                                formato = formatoFecha,
                                onSelect = {
                                    inicio = it
                                    if (fin?.before(it) == true) fin = it
                                }
                            )
                        }
                        item {
                            SelectorFechaGeneracion(
                                label = stringResource(R.string.hymn_generate_end),
                                fechas = disponibles.filter { inicio == null || !it.before(inicio) },
                                seleccion = fin,
                                formato = formatoFecha,
                                onSelect = { fin = it }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = intervalo,
                                onValueChange = { value ->
                                    if (value.all(Char::isDigit) && value.length <= 4) intervalo = value
                                },
                                label = { Text(stringResource(R.string.hymn_generate_interval)) },
                                supportingText = { Text(stringResource(R.string.hymn_generate_interval_help)) },
                                isError = intervaloNumero == null || intervaloNumero !in 0..3650,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            OpcionGeneracion(
                                texto = stringResource(R.string.hymn_generate_only_empty),
                                marcada = soloVacias,
                                onChange = { soloVacias = it }
                            )
                        }
                        item {
                            OpcionGeneracion(
                                texto = stringResource(R.string.hymn_generate_replace_automatic),
                                marcada = reemplazarAutomaticas,
                                onChange = { reemplazarAutomaticas = it }
                            )
                        }
                        item {
                            OpcionGeneracion(
                                texto = stringResource(R.string.hymn_generate_force_new),
                                marcada = forzarNuevo,
                                onChange = { forzarNuevo = it }
                            )
                        }
                        item {
                            OpcionGeneracion(
                                texto = stringResource(R.string.hymn_generate_prioritize_topics),
                                marcada = priorizarTemas,
                                onChange = { priorizarTemas = it }
                            )
                        }
                        item {
                            OpcionGeneracion(
                                texto = stringResource(R.string.hymn_generate_intermediate),
                                marcada = incluirIntermedio,
                                onChange = { incluirIntermedio = it }
                            )
                        }
                        item {
                            Text(
                                stringResource(R.string.hymn_generate_manual_note),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.btn_cancelar))
                    }
                    TextButton(
                        enabled = rangoValido,
                        onClick = {
                            val fechasElegidas = disponibles.filter {
                                !it.before(inicio) && !it.after(fin)
                            }
                            onPreview(
                                fechasElegidas,
                                configuracion.copy(
                                    intervaloMinimoHimnosDias = intervaloNumero!!,
                                    forzarHimnoNuevo = forzarNuevo,
                                    priorizarTemasHimnos = priorizarTemas,
                                    generarHimnoIntermedio = incluirIntermedio,
                                    soloFechasSinHimnos = soloVacias,
                                    reemplazarHimnosAutomaticos = reemplazarAutomaticas
                                )
                            )
                        }
                    ) {
                        Text(stringResource(R.string.hymn_generate_preview_action))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorFechaGeneracion(
    label: String,
    fechas: List<Date>,
    seleccion: Date?,
    formato: SimpleDateFormat,
    onSelect: (Date) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = seleccion?.let(formato::format).orEmpty(),
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            singleLine = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            fechas.forEach { fecha ->
                DropdownMenuItem(
                    text = { Text(formato.format(fecha)) },
                    onClick = {
                        onSelect(fecha)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun OpcionGeneracion(
    texto: String,
    marcada: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = marcada, onCheckedChange = onChange)
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun GenerandoPrevisualizacionHimnosDialog() {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()
                Text(
                    stringResource(R.string.hymn_generating_preview),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

@Composable
fun PrevisualizacionGeneracionHimnosDialog(
    resultado: ResultadoGeneracionHimnos,
    guardando: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    val locale = context.resources.configuration.locales[0]
    val formato = remember(locale) {
        SimpleDateFormat(context.getString(R.string.hymn_planner_date_pattern), locale)
    }

    Dialog(
        onDismissRequest = { if (!guardando) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f).padding(horizontal = 16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                Text(
                    stringResource(R.string.hymn_preview_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(
                        R.string.hymn_preview_summary,
                        resultado.fechasEvaluadas,
                        resultado.propuestas.size,
                        resultado.fechasSinCambios
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                if (resultado.propuestas.isEmpty()) {
                    Text(
                        stringResource(R.string.hymn_preview_empty),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(resultado.propuestas, key = { claveFecha(it.fecha) }) { propuesta ->
                            PropuestaHimnosCard(propuesta, formato)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (guardando) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 12.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    TextButton(enabled = !guardando, onClick = onDismiss) {
                        Text(stringResource(R.string.btn_cancelar))
                    }
                    TextButton(
                        enabled = !guardando && resultado.propuestas.isNotEmpty(),
                        onClick = onConfirm
                    ) {
                        Text(stringResource(R.string.hymn_preview_confirm))
                    }
                }
            }
        }
    }
}

@Composable
private fun PropuestaHimnosCard(
    propuesta: PropuestaGeneracionHimnos,
    formato: SimpleDateFormat
) {
    val locale = LocalContext.current.resources.configuration.locales[0]
    val himnos = listOf(
        PosicionHimno.APERTURA to propuesta.propuesta.apertura,
        PosicionHimno.SACRAMENTAL to propuesta.propuesta.sacramental,
        PosicionHimno.INTERMEDIO to propuesta.propuesta.intermedio,
        PosicionHimno.FINAL to propuesta.propuesta.final
    ).filter { (_, seleccion) -> seleccion.numero > 0 || seleccion.nombre.isNotBlank() }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                formato.format(propuesta.fecha).replaceFirstChar { it.titlecase(locale) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            himnos.forEach { (posicion, seleccion) ->
                Row(Modifier.fillMaxWidth()) {
                    Text(
                        "${etiquetaPosicion(posicion)}:",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        formatearHimno(seleccion.numero, seleccion.nombre),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (propuesta.temas.isNotEmpty()) {
                Text(
                    stringResource(R.string.hymn_preview_topics, propuesta.temas.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (propuesta.seleccionesManualesPreservadas > 0) {
                Text(
                    pluralStringResource(
                        R.plurals.hymn_preview_manual_preserved,
                        propuesta.seleccionesManualesPreservadas,
                        propuesta.seleccionesManualesPreservadas
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            propuesta.advertencias.forEach { advertencia ->
                Text(
                    textoAdvertencia(advertencia),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun etiquetaPosicion(posicion: PosicionHimno): String = stringResource(
    when (posicion) {
        PosicionHimno.APERTURA -> R.string.hymn_position_opening
        PosicionHimno.SACRAMENTAL -> R.string.hymn_position_sacrament
        PosicionHimno.INTERMEDIO -> R.string.hymn_position_intermediate
        PosicionHimno.FINAL -> R.string.hymn_position_closing
    }
)

@Composable
private fun textoAdvertencia(advertencia: AdvertenciaGeneracionHimnos): String = when (advertencia.codigo) {
    CodigoAdvertenciaGeneracion.INTERVALO_MINIMO_RELAJADO -> stringResource(
        R.string.hymn_preview_interval_warning,
        advertencia.posicion?.let { etiquetaPosicion(it) }.orEmpty()
    )
    CodigoAdvertenciaGeneracion.SIN_CANDIDATO -> stringResource(
        R.string.hymn_preview_no_candidate_warning,
        advertencia.posicion?.let { etiquetaPosicion(it) }.orEmpty()
    )
    CodigoAdvertenciaGeneracion.SIN_HIMNO_NUEVO ->
        stringResource(R.string.hymn_preview_no_new_warning)
}

private fun inicioDelDia(fecha: Date): Date = Calendar.getInstance().apply {
    time = fecha
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.time
