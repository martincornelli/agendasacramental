package com.example.agendasacramental

import android.view.WindowManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModoLecturaScreen(
    agenda: Agenda,
    onBack: () -> Unit
) {
    val view = LocalView.current
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

    // Mantener pantalla encendida
    DisposableEffect(Unit) {
        val window = (context as? android.app.Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(dateFormat.format(agenda.fecha.toDate())) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.btn_volver))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            // Preside / Dirige
            item {
                CampoLectura(stringResource(R.string.lectura_preside), agenda.preside)
            }
            item {
                CampoLectura(stringResource(R.string.lectura_dirige), agenda.dirige)
            }

            // Reconocimientos
            item {
                val items = agenda.reconocimientos.split(",").map { it.trim() }.filter { it.isNotBlank() }
                SeccionLectura(stringResource(R.string.lectura_reconocimientos)) {
                    if (items.isEmpty()) {
                        Text("—", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        items.forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge) }
                    }
                }
            }

            // Anuncios
            item {
                val items = agenda.anuncios.split(",").map { it.trim() }.filter { it.isNotBlank() }
                SeccionLectura(stringResource(R.string.lectura_anuncios)) {
                    if (items.isEmpty()) {
                        Text("—", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        items.forEach { Text("• $it", style = MaterialTheme.typography.bodyLarge) }
                    }
                }
            }

            // Himno apertura
            item {
                val himno = if (agenda.primerHimnoNumero > 0)
                    "${agenda.primerHimnoNumero} - ${agenda.primerHimnoNombre}" else "—"
                CampoLectura(stringResource(R.string.lectura_himno_apertura), himno)
            }

            // Director / Pianista
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(modifier = Modifier.weight(1f)) { CampoLectura(stringResource(R.string.lectura_director), agenda.directorMusica.ifBlank { "—" }) }
                    Box(modifier = Modifier.weight(1f)) { CampoLectura(stringResource(R.string.lectura_pianista), agenda.pianista.ifBlank { "—" }) }
                }
            }

            // Primera oración
            item {
                CampoLectura(stringResource(R.string.lectura_primera_oracion), agenda.primeraOracion)
            }

            // Asuntos
            if (agenda.asuntosEstacaBarrio.isNotEmpty()) {
                item {
                    SeccionLectura(stringResource(R.string.lectura_asuntos)) {
                        agenda.asuntosEstacaBarrio.forEach { asunto ->
                            val texto = if (asunto.tipo == TipoAsunto.OTROS) {
                                "• ${context.getString(asunto.tipo.stringResId)}:\n${asunto.columna2}"
                            } else {
                                "• ${context.getString(asunto.tipo.stringResId)}: ${asunto.columna2} — ${asunto.columna3}"
                            }
                            Text(texto, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            // Himno sacramental
            item {
                val himno = if (agenda.himnoSacramentalNumero > 0)
                    "${agenda.himnoSacramentalNumero} - ${agenda.himnoSacramentalNombre}" else "—"
                CampoLectura(stringResource(R.string.lectura_himno_sacramental), himno)
            }

            // Mensajes del evangelio
            item {
                SeccionLectura(stringResource(R.string.lectura_mensajes)) {
                    if (agenda.mensajesEvangelio.isEmpty()) {
                        Text("—", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        agenda.mensajesEvangelio.forEach { msg ->
                            val texto = when (msg.tipo) {
                                TipoMensaje.HIMNO_INTERMEDIO -> "🎵 Himno: ${msg.himnoNumero} - ${msg.himnoNombre}"
                                TipoMensaje.TESTIMONIO -> stringResource(R.string.lectura_testimonio) + ": ${msg.nombre}"
                                else -> stringResource(R.string.lectura_discurso) + ": ${msg.nombre}"
                            }
                            Text("• $texto", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            if (agenda.reunionTestimonios) {
                item {
                    SeccionLectura(stringResource(R.string.pdf_reunion_testimonios)) {
                        if (agenda.testimonios.isEmpty()) {
                            Text("—", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            agenda.testimonios.forEach { nombre ->
                                Text("• $nombre", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }

            // Himno final
            item {
                val himno = if (agenda.himnoFinalNumero > 0)
                    "${agenda.himnoFinalNumero} - ${agenda.himnoFinalNombre}" else "—"
                CampoLectura(stringResource(R.string.lectura_himno_final), himno)
            }

            // Oración final
            item {
                CampoLectura(stringResource(R.string.lectura_oracion_final), agenda.oracionFinal)
            }
        }
    }
}

@Composable
private fun CampoLectura(label: String, valor: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Text(
            valor.ifBlank { "—" },
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 18.sp
        )
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun SeccionLectura(titulo: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            titulo,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            content()
        }
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
