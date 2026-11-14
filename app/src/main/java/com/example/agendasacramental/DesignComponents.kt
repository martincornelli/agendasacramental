package com.example.agendasacramental

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

data class SecondaryAction(val label: String, val onClick: () -> Unit, val destructive: Boolean = false)

@Composable
fun SecondaryActionMenu(actions: List<SecondaryAction>, description: String = stringResource(R.string.design_more_options)) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.size(48.dp)) {
            Icon(Icons.Default.MoreVert, contentDescription = description)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label, color = if (action.destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface) },
                    onClick = { expanded = false; action.onClick() }
                )
            }
        }
    }
}

@Composable
fun DesignSectionHeading(title: String) {
    Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.small, modifier = Modifier.width(4.dp).height(24.dp)) {}
        Text(title, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun AgendaDateTile(date: java.util.Date) {
    val locale = androidx.compose.ui.platform.LocalContext.current.resources.configuration.locales[0]
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.widthIn(min = 56.dp).padding(10.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text(java.text.SimpleDateFormat("dd", locale).format(date), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(java.text.SimpleDateFormat("MMM", locale).format(date).uppercase(locale), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
