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
                        Icon(Icons.Default.ArrowBack, "Volver")
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
                CampoLectura("Preside", agenda.preside)
            }
            item {
                CampoLectura("Dirige", agenda.dirige)
            }

            // Reconocimientos
            item {
                val items = agenda.reconocimientos.split(",").map { it.trim() }.filter { it.isNotBlank() }
                SeccionLectura("Reconocimientos") {
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
                SeccionLectura("Anuncios") {
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
                CampoLectura("Himno de apertura", himno)
            }

            // Director / Pianista
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Box(modifier = Modifier.weight(1f)) { CampoLectura("Director/a", agenda.directorMusica.ifBlank { "—" }) }
                    Box(modifier = Modifier.weight(1f)) { CampoLectura("Pianista", agenda.pianista.ifBlank { "—" }) }
                }
            }

            // Primera oración
            item {
                CampoLectura("Primera oración", agenda.primeraOracion)
            }

            // Asuntos
            if (agenda.asuntosEstacaBarrio.isNotEmpty()) {
                item {
                    SeccionLectura("Asuntos") {
                        agenda.asuntosEstacaBarrio.forEach { asunto ->
                            Text(
                                "• ${asunto.tipo.label}: ${asunto.columna2} — ${asunto.columna3}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }

            // Himno sacramental
            item {
                val himno = if (agenda.himnoSacramentalNumero > 0)
                    "${agenda.himnoSacramentalNumero} - ${agenda.himnoSacramentalNombre}" else "—"
                CampoLectura("Himno Sacramental", himno)
            }

            // Mensajes del evangelio
            item {
                SeccionLectura("Mensajes del Evangelio") {
                    if (agenda.mensajesEvangelio.isEmpty()) {
                        Text("—", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        agenda.mensajesEvangelio.forEach { msg ->
                            val texto = when (msg.tipo) {
                                TipoMensaje.HIMNO_INTERMEDIO -> "🎵 Himno: ${msg.himnoNumero} - ${msg.himnoNombre}"
                                TipoMensaje.TESTIMONIO -> "Testimonio: ${msg.nombre}"
                                else -> "Discurso: ${msg.nombre}"
                            }
                            Text("• $texto", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            // Himno final
            item {
                val himno = if (agenda.himnoFinalNumero > 0)
                    "${agenda.himnoFinalNumero} - ${agenda.himnoFinalNombre}" else "—"
                CampoLectura("Himno Final", himno)
            }

            // Oración final
            item {
                CampoLectura("Oración Final", agenda.oracionFinal)
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
