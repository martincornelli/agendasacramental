package com.example.agendasacramental

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LibraryMusic
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
    onIrPlanificadorHimnos: () -> Unit,
    onIrNotificaciones: () -> Unit,
    onCambiarUnidad: () -> Unit,
    onLogout: () -> Unit
) {
    var showTemaMenu by remember { mutableStateOf(false) }
    var showIdiomaDialog by remember { mutableStateOf(false) }
    var showAdminDialog by remember { mutableStateOf(false) }
    var canManage by remember(numeroUnidad) { mutableStateOf(false) }
    val adminRepo = remember { UnitAdministrationRepository() }
    DisposableEffect(numeroUnidad) {
        val listener = com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("unidades").document(numeroUnidad)
            .addSnapshotListener { snapshot, _ ->
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                canManage = (snapshot?.get("administradores") as? Map<*, *>)?.containsKey(uid) == true
            }
        onDispose { listener.remove() }
    }
    LaunchedEffect(numeroUnidad) {
        runCatching { adminRepo.load(numeroUnidad) }.onSuccess { data ->
            canManage = adminRepo.isAdmin(data)
            if (adminRepo.isInvited(data)) showAdminDialog = true
        }
    }
    if (showAdminDialog) UnitAdministrationDialog(numeroUnidad) { showAdminDialog = false }

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
                            if (canManage) DropdownMenuItem(
                                text = { Text(stringResource(R.string.admin_title)) },
                                onClick = { showAdminDialog = true; showTemaMenu = false },
                                leadingIcon = { Icon(Icons.Default.Groups, null) }
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
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 720.dp).fillMaxWidth()
                    .padding(bottom = 40.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Surface(color = androidx.compose.ui.graphics.Color(0xFF192F49), contentColor = androidx.compose.ui.graphics.Color.White, shape = MaterialTheme.shapes.large) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.design_workspace), style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color(0xFF95DADD))
                        Text(stringResource(R.string.design_home_title), style = MaterialTheme.typography.headlineMedium)
                        Text(stringResource(R.string.design_home_subtitle), style = MaterialTheme.typography.bodyMedium, color = androidx.compose.ui.graphics.Color(0xFFD4DEEA))
                    }
                }
                Text(stringResource(R.string.design_prepare), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))

                Spacer(modifier = Modifier.height(8.dp))

                HomeDestination(
                    stringResource(R.string.home_agendas), stringResource(R.string.home_agendas_desc),
                    Icons.Default.CalendarMonth, onIrAgendas, prominent = true
                )
                HomeDestination(
                    stringResource(R.string.home_planificacion), stringResource(R.string.home_planificacion_desc),
                    Icons.Default.Groups, onIrPlanificacion
                )
                HomeDestination(
                    stringResource(R.string.home_hymn_planner), stringResource(R.string.home_hymn_planner_desc),
                    Icons.Default.LibraryMusic, onIrPlanificadorHimnos
                )
            }

            Text(
                "v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun HomeDestination(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit, prominent: Boolean = false) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (prominent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, null, modifier = Modifier.padding(12.dp).size(24.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
