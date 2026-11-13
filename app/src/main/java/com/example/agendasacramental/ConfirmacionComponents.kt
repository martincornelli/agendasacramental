package com.example.agendasacramental

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrganizacionSelector(valor: String, onChange: (String) -> Unit, incluirTodas: Boolean = false, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    val opciones = linkedMapOf<String, Int>().apply {
        if (incluirTodas) put("ALL", R.string.org_all)
        put("", R.string.org_unspecified)
        putAll(organizaciones)
    }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(value = stringResource(opciones[valor] ?: R.string.org_unspecified), onValueChange = {}, readOnly = true,
            label = { Text(stringResource(R.string.org_label)) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            opciones.forEach { (id, label) -> DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { onChange(id); expanded = false }) }
        }
    }
}

data class AcceptanceUi(val agenda: Agenda, val onToggle: (ComprobacionAsignacion, Boolean) -> Unit)
val LocalAcceptanceUi = staticCompositionLocalOf<AcceptanceUi?> { null }

@Composable
fun AceptacionCheck(id: String, musician: Boolean = false) {
    val ui = LocalAcceptanceUi.current ?: return
    val row = comprobacionesAgenda(ui.agenda).find { it.id == id } ?: return
    val checked = ui.agenda.aceptaciones.containsKey(id)
    val status = stringResource(if (!row.completa) R.string.check_incomplete else if (checked) R.string.check_accepted else R.string.check_pending)
    val role = if (id.endsWith("_pianista")) stringResource(R.string.check_pianist) else stringResource(R.string.check_music_director)
    val label = if (musician) role else status
    val description = listOf(stringResource(row.label), row.persona, if (musician) role else "", status).filter { it.isNotBlank() }.joinToString(" · ")
    Row(
        Modifier.heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .toggleable(value = checked, enabled = row.completa && ui.agenda.estado != EstadoAgenda.REALIZADA,
                role = Role.Checkbox, onValueChange = { ui.onToggle(row, it) }),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null,
            enabled = row.completa && ui.agenda.estado != EstadoAgenda.REALIZADA,
            modifier = Modifier.padding(12.dp).clearAndSetSemantics {})
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clearAndSetSemantics {})
    }
}

@Composable
fun AceptacionCampo(id: String, content: @Composable () -> Unit) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 300.dp && fontScale < 1.3f) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { content() }
                Box(Modifier.widthIn(max = 128.dp)) { AceptacionCheck(id) }
            }
        } else {
            Column { content(); AceptacionCheck(id) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AceptacionesHimno(id: String) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        AceptacionCheck("${id}_pianista", musician = true)
        AceptacionCheck("${id}_directorMusica", musician = true)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConfirmacionResumen(agenda: Agenda, legacy: Boolean, canUndo: Boolean, onUndo: () -> Unit, onEstadoChange: (EstadoAgenda) -> Unit) {
    val checks = comprobacionesAgenda(agenda)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        EstadoSelector(agenda.estado, onEstadoChange)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.Center) {
            Text(stringResource(R.string.check_progress, agenda.aceptaciones.size, checks.size),
                modifier = Modifier.padding(vertical = 12.dp), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { onEstadoChange(EstadoAgenda.CONFIRMADA) },
                enabled = agenda.estado != EstadoAgenda.REALIZADA && agenda.aceptaciones.size < checks.size) {
                Text(stringResource(R.string.check_accept_all))
            }
            if (canUndo) TextButton(onClick = onUndo) { Text(stringResource(R.string.check_undo)) }
        }
        Text(stringResource(R.string.check_save_hint), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (legacy) Text(stringResource(R.string.check_legacy), style = MaterialTheme.typography.bodySmall)
    }
}
