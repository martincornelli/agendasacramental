package com.example.agendasacramental

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource

@Composable
fun HymnTopicsDialog(numero: Int, nombre: String, temas: List<String>, guardando: Boolean,
                    error: String, onDismiss: () -> Unit, onSave: (List<String>?) -> Unit) {
    var texto by remember(numero) { mutableStateOf(temas.joinToString("\n")) }
    var invalido by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!guardando) onDismiss() },
        title = { Text("${stringResource(R.string.hymn_topics_edit)} · $numero $nombre") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.hymn_topics_scope))
                OutlinedTextField(value = texto, onValueChange = { texto = it; invalido = false },
                    enabled = !guardando, modifier = Modifier.fillMaxWidth(), minLines = 5,
                    label = { Text(stringResource(R.string.hymn_topics_label)) },
                    isError = invalido || error.isNotBlank(),
                    supportingText = { Text(if (error.isNotBlank()) error else stringResource(R.string.hymn_topics_limit)) })
                TextButton(enabled = !guardando, onClick = { onSave(null) }) {
                    Text(stringResource(R.string.hymn_topics_restore))
                }
                if (guardando) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(enabled = !guardando, onClick = {
                val parsed = runCatching { parsearTemasHimno(texto) }
                if (parsed.isSuccess) onSave(parsed.getOrThrow()) else invalido = true
            }) { Text(stringResource(R.string.btn_guardar)) }
        },
        dismissButton = { TextButton(enabled = !guardando, onClick = onDismiss) { Text(stringResource(R.string.btn_cancelar)) } }
    )
}
