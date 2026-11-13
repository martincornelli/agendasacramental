package com.example.agendasacramental

import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.*
import android.print.pdf.PrintedPdfDocument
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import java.text.DateFormat
import java.util.*

private val Agenda.conTestimonios get() = reunionTestimonios && !presentacionPrimaria

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImprimirAgendasScreen(agendas: List<Agenda>, volver: () -> Unit) {
    val context = LocalContext.current
    val ordenadas = remember(agendas) { agendas.sortedBy { it.fecha } }
    var ids by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    var elegirRango by rememberSaveable { mutableStateOf(false) }
    var elegirTanda by rememberSaveable { mutableStateOf(false) }
    val seleccion = ordenadas.filter { it.id in ids }
    val (testimonios, regulares) = seleccion.partition { it.conTestimonios }
    val mixta = testimonios.isNotEmpty() && regulares.isNotEmpty()
    val formato = remember { DateFormat.getDateInstance(DateFormat.LONG) }
    BackHandler { if (elegirTanda) elegirTanda = false else volver() }

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { if (elegirTanda) elegirTanda = false else volver() }) { Text(stringResource(R.string.btn_volver)) }
        Text(stringResource(R.string.imprimir_agendas), style = MaterialTheme.typography.headlineSmall)
        if (elegirTanda) {
            listOf(regulares to R.string.print_regular, testimonios to R.string.print_testimony, seleccion to R.string.print_all).forEach { (tanda, label) ->
                Button(onClick = { imprimirTanda(context, tanda) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(label))
                }
                Text(stringResource(when (label) {
                    R.string.print_regular -> R.string.print_simple
                    R.string.print_testimony -> R.string.print_duplex
                    else -> R.string.print_separate
                }))
            }
        } else {
            OutlinedButton(onClick = { elegirRango = true }) { Text(stringResource(R.string.print_range)) }
            LazyColumn(Modifier.weight(1f)) {
                items(ordenadas.asReversed(), key = { it.id }) { agenda ->
                    val marcada = agenda.id in ids
                    Row(Modifier.fillMaxWidth().toggleable(marcada, role = Role.Checkbox) {
                        ids = ArrayList(if (marcada) ids - agenda.id else ids + agenda.id)
                    }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(marcada, onCheckedChange = null)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(formato.format(agenda.fecha.toDate()))
                            Text(stringResource(if (agenda.conTestimonios) R.string.print_testimony else R.string.print_regular), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            if (!mixta && seleccion.isNotEmpty()) Text(stringResource(if (testimonios.isEmpty()) R.string.print_simple else R.string.print_duplex))
            Button(enabled = seleccion.isNotEmpty(), onClick = {
                if (mixta) elegirTanda = true else imprimirTanda(context, seleccion)
            }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (mixta) R.string.print_continue else R.string.imprimir_agendas))
            }
        }
    }
    if (elegirRango) {
        val rango = rememberDateRangePickerState()
        DatePickerDialog(onDismissRequest = { elegirRango = false }, confirmButton = {
            TextButton(enabled = rango.selectedEndDateMillis != null, onClick = {
                val desde = requireNotNull(rango.selectedStartDateMillis)
                val hasta = requireNotNull(rango.selectedEndDateMillis)
                val local = Calendar.getInstance()
                val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                ids = ArrayList(ordenadas.filter {
                    local.time = it.fecha.toDate()
                    utc.clear()
                    utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
                    utc.timeInMillis in desde..hasta
                }.map { it.id })
                elegirRango = false
            }) { Text(stringResource(R.string.print_select)) }
        }, dismissButton = {
            TextButton(onClick = { elegirRango = false }) { Text(stringResource(R.string.btn_cancelar)) }
        }) { DateRangePicker(rango, modifier = Modifier.weight(1f, fill = false), title = null, headline = null) }
    }
}

private fun imprimirTanda(context: Context, agendas: List<Agenda>) {
    val nombre = context.getString(R.string.imprimir_agendas)
    val paginas = agendas.flatMap { agenda ->
        if (agenda.conTestimonios) listOf(agenda to false, agenda to true) else listOf(agenda to false)
    }
    val opciones = PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4)
        .setDuplexMode(if (agendas.all { it.conTestimonios }) PrintAttributes.DUPLEX_MODE_LONG_EDGE else PrintAttributes.DUPLEX_MODE_NONE)
        .build()
    val adapter = object : PrintDocumentAdapter() {
        lateinit var atributos: PrintAttributes
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        override fun onLayout(old: PrintAttributes?, new: PrintAttributes, signal: CancellationSignal, callback: LayoutResultCallback, extras: Bundle?) {
            atributos = new
            if (signal.isCanceled) callback.onLayoutCancelled() else callback.onLayoutFinished(
                PrintDocumentInfo.Builder(nombre).setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(paginas.size).build(), old != new)
        }

        override fun onWrite(ranges: Array<out PageRange>, destination: ParcelFileDescriptor, signal: CancellationSignal, callback: WriteResultCallback) {
            val layout = atributos
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val pdf = PrintedPdfDocument(context, layout)
                        try {
                            paginas.forEachIndexed { i, (agenda, testimonios) ->
                                signal.throwIfCanceled()
                                if (ranges.any { i in it.start..it.end }) {
                                    val page = pdf.startPage(i)
                                    val bounds = pdf.pageContentRect
                                    val scale = minOf(bounds.width() / 595f, bounds.height() / 842f)
                                    page.canvas.translate((bounds.width() - 595f * scale) / 2f, 0f)
                                    page.canvas.scale(scale, scale)
                                    try {
                                        GeneradorPDF.dibujarPaginaImpresion(page.canvas, agenda, testimonios, context)
                                    } finally {
                                        pdf.finishPage(page)
                                    }
                                }
                            }
                            signal.throwIfCanceled()
                            ParcelFileDescriptor.AutoCloseOutputStream(destination).use { pdf.writeTo(it) }
                        } finally {
                            pdf.close()
                        }
                    }
                    if (signal.isCanceled) callback.onWriteCancelled() else callback.onWriteFinished(ranges)
                } catch (error: Exception) {
                    if (signal.isCanceled) callback.onWriteCancelled() else callback.onWriteFailed(error.localizedMessage)
                }
            }
        }
        override fun onFinish() { scope.cancel() }
    }
    context.getSystemService(PrintManager::class.java).print(nombre, adapter, opciones)
}
