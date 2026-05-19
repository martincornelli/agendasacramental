package com.example.agendasacramental

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    idioma: String,
    onIdiomaChange: (String) -> Unit
) {
    var showIdiomaMenu by remember { mutableStateOf(false) }
    val idiomaLabel = when (idioma) {
        "en" -> "English"
        else -> "Español"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Selector de idioma arriba a la derecha
        Box(modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 16.dp)) {
            TextButton(
                onClick = { showIdiomaMenu = true },
                modifier = Modifier.defaultMinSize(minHeight = 48.dp)
            ) {
                Text(idiomaLabel, style = MaterialTheme.typography.bodyMedium)
            }
            DropdownMenu(
                expanded = showIdiomaMenu,
                onDismissRequest = { showIdiomaMenu = false }
            ) {
                LocaleHelper.IDIOMAS.forEach { (codigo, nombre) ->
                    DropdownMenuItem(
                        text = { Text(nombre) },
                        onClick = { onIdiomaChange(codigo); showIdiomaMenu = false },
                        trailingIcon = {
                            if (idioma == codigo) Text("✓", color = MaterialTheme.colorScheme.primary)
                        }
                    )
                }
            }
        }

        // Contenido principal centrado
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.login_titulo),
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.app_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth()
            )
            {
                Text(stringResource(R.string.login_iniciar_google))
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = LocalContext.current.getString(R.string.disclaimer),
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}