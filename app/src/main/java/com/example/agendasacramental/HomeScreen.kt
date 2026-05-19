package com.example.agendasacramental

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    numeroUnidad: String,
    tema: String,
    onTemaChange: (String) -> Unit,
    idioma: String,
    onIdiomaChange: (String) -> Unit,
    onIrAgendas: () -> Unit,
    onIrPlanificacion: () -> Unit,
    onIrNotificaciones: () -> Unit,
    onCambiarUnidad: () -> Unit,
    onLogout: () -> Unit
) {
    var showTemaMenu by remember { mutableStateOf(false) }
    var showIdiomaDialog by remember { mutableStateOf(false) }

    if (showIdiomaDialog) {
        AlertDialog(
            onDismissRequest = { showIdiomaDialog = false },
            title = { Text(LocalContext.current.getString(R.string.ajustes_idioma)) },
            text = {
                Column {
                    LocaleHelper.IDIOMAS.forEach { (codigo, nombre) ->
                        Row(
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            modifier = androidx.compose.ui.Modifier.fillMaxWidth()
                                .clickable { onIdiomaChange(codigo); showIdiomaDialog = false }
                        ) {
                            RadioButton(
                                selected = idioma == codigo,
                                onClick = { onIdiomaChange(codigo); showIdiomaDialog = false }
                            )
                            Text(nombre, modifier = androidx.compose.ui.Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showIdiomaDialog = false }) { Text(LocalContext.current.getString(R.string.btn_cerrar)) }
            }
        )
    }
    val ctx = LocalContext.current
    android.util.Log.d("LOCALE_TEST", "Locale: ${ctx.resources.configuration.locales[0]}, lang: ${ctx.resources.configuration.locales[0].language}")
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(LocalContext.current.getString(R.string.agendas_titulo, numeroUnidad)) },
                navigationIcon = {
                    IconButton(onClick = onCambiarUnidad) {
                        Icon(Icons.Default.ArrowBack, LocalContext.current.getString(R.string.agendas_cambiar_unidad))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showTemaMenu = true }) {
                            Icon(Icons.Default.Settings, LocalContext.current.getString(R.string.ajustes))
                        }
                        // Diálogo de tema
                        var showTemaDialog by remember { mutableStateOf(false) }
                        if (showTemaDialog) {
                            AlertDialog(
                                onDismissRequest = { showTemaDialog = false },
                                title = { Text(LocalContext.current.getString(R.string.ajustes_tema)) },
                                text = {
                                    Column {
                                        listOf(
                                            "sistema" to LocalContext.current.getString(R.string.ajustes_tema_sistema),
                                            "claro" to LocalContext.current.getString(R.string.ajustes_tema_claro),
                                            "oscuro" to LocalContext.current.getString(R.string.ajustes_tema_oscuro)
                                        ).forEach { (valor, label) ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                RadioButton(
                                                    selected = tema == valor,
                                                    onClick = { onTemaChange(valor); showTemaDialog = false }
                                                )
                                                Text(label, modifier = Modifier.padding(start = 8.dp))
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    TextButton(onClick = { showTemaDialog = false }) { Text(LocalContext.current.getString(R.string.btn_cerrar)) }
                                }
                            )
                        }

                        DropdownMenu(
                            expanded = showTemaMenu,
                            onDismissRequest = { showTemaMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(LocalContext.current.getString(R.string.ajustes_tema)) },
                                leadingIcon = { Icon(Icons.Default.Palette, null) },
                                trailingIcon = {
                                    val temaLabel = when (tema) {
                                        "oscuro" -> LocalContext.current.getString(R.string.ajustes_tema_oscuro)
                                        "claro" -> LocalContext.current.getString(R.string.ajustes_tema_claro)
                                        else -> LocalContext.current.getString(R.string.ajustes_tema_sistema)
                                    }
                                    Text(temaLabel, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                onClick = { showTemaDialog = true; showTemaMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(LocalContext.current.getString(R.string.ajustes_notificaciones)) },
                                onClick = { onIrNotificaciones(); showTemaMenu = false },
                                leadingIcon = { Icon(Icons.Default.Notifications, null) }
                            )
                            DropdownMenuItem(
                                text = { Text(LocalContext.current.getString(R.string.ajustes_idioma)) },
                                leadingIcon = { Icon(Icons.Default.Language, null) },
                                trailingIcon = {
                                    val idiomaLabel = when (idioma) {
                                        "en" -> "English"
                                        else -> "Español"
                                    }
                                    Text(idiomaLabel, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                onClick = { showIdiomaDialog = true; showTemaMenu = false }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text(LocalContext.current.getString(R.string.ajustes_cerrar_sesion)) },
                                onClick = { onLogout(); showTemaMenu = false },
                                leadingIcon = { Icon(Icons.Default.ExitToApp, null) }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    LocalContext.current.getString(R.string.home_que_deseas),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Módulo Agendas
                Card(
                    onClick = onIrAgendas,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            LocalContext.current.getString(R.string.home_agendas),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            LocalContext.current.getString(R.string.home_agendas_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Módulo Planificación
                Card(
                    onClick = onIrPlanificacion,
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Groups,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            LocalContext.current.getString(R.string.home_planificacion),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            LocalContext.current.getString(R.string.home_planificacion_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Text(
                LocalContext.current.getString(R.string.version),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}