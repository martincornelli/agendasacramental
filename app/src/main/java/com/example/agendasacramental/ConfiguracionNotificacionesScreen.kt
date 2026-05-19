package com.example.agendasacramental

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import java.util.Calendar

// Constantes compartidas con el Worker
object NotifPrefs {
    const val PREFS_NAME = "agenda_notif_prefs"
    const val KEY_HABILITADA = "habilitada"
    const val KEY_DIAS_ANTES = "dias_antes"
    const val KEY_HORA = "hora"
    const val KEY_MINUTO = "minuto"
    const val KEY_UNIDAD = "unidad"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionNotificacionesScreen(
    numeroUnidad: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(NotifPrefs.PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Estado — por defecto OFF
    var habilitada by remember { mutableStateOf(prefs.getBoolean(NotifPrefs.KEY_HABILITADA, false)) }
    var diasAntes by remember { mutableStateOf(prefs.getInt(NotifPrefs.KEY_DIAS_ANTES, 2)) }
    var hora by remember { mutableStateOf(prefs.getInt(NotifPrefs.KEY_HORA, 9)) }
    var minuto by remember { mutableStateOf(prefs.getInt(NotifPrefs.KEY_MINUTO, 0)) }
    var showDiasMenu by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showPermisoDenegadoDialog by remember { mutableStateOf(false) }

    // Guardar unidad siempre
    LaunchedEffect(Unit) {
        prefs.edit().putString(NotifPrefs.KEY_UNIDAD, numeroUnidad).apply()
    }

    fun guardar(hab: Boolean = habilitada, dias: Int = diasAntes, h: Int = hora, m: Int = minuto) {
        prefs.edit()
            .putBoolean(NotifPrefs.KEY_HABILITADA, hab)
            .putInt(NotifPrefs.KEY_DIAS_ANTES, dias)
            .putInt(NotifPrefs.KEY_HORA, h)
            .putInt(NotifPrefs.KEY_MINUTO, m)
            .putString(NotifPrefs.KEY_UNIDAD, numeroUnidad)
            .apply()
        AgendaNotificationWorker.programar(context)
    }

    fun tienePermiso(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as android.app.NotificationManager
            nm.areNotificationsEnabled()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            habilitada = true
            guardar(hab = true)
        } else {
            showPermisoDenegadoDialog = true
        }
    }

    // Diálogo de permiso denegado
    if (showPermisoDenegadoDialog) {
        AlertDialog(
            onDismissRequest = { showPermisoDenegadoDialog = false },
            title = { Text(stringResource(R.string.notif_bloqueadas_titulo)) },
            text = {
                Text(stringResource(R.string.notif_bloqueadas_desc))
            },
            confirmButton = {
                TextButton(onClick = {
                    showPermisoDenegadoDialog = false
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }) { Text(stringResource(R.string.btn_ir_ajustes)) }
            },
            dismissButton = {
                TextButton(onClick = { showPermisoDenegadoDialog = false }) { Text(stringResource(R.string.btn_cancelar)) }
            }
        )
    }

    // TimePicker
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = hora,
            initialMinute = minuto,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.notif_hora_aviso)) },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    hora = timePickerState.hour
                    minuto = timePickerState.minute
                    guardar(h = timePickerState.hour, m = timePickerState.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.btn_aceptar)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.btn_cancelar)) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notif_titulo)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.btn_volver))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Switch activar/desactivar
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (habilitada) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = if (habilitada) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.notif_recordatorio), style = MaterialTheme.typography.titleMedium)
                        Text(
                            stringResource(R.string.notif_avisa),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = habilitada,
                        onCheckedChange = { quiereActivar ->
                            if (quiereActivar) {
                                if (tienePermiso()) {
                                    habilitada = true
                                    guardar(hab = true)
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    showPermisoDenegadoDialog = true
                                }
                            } else {
                                habilitada = false
                                guardar(hab = false)
                            }
                        }
                    )
                }
            }

            if (habilitada) {
                // Selector de día
                val diasOpciones = listOf(
                    1 to stringResource(R.string.notif_sabado),
                    2 to stringResource(R.string.notif_viernes),
                    3 to stringResource(R.string.notif_jueves),
                    4 to stringResource(R.string.notif_miercoles),
                    5 to stringResource(R.string.notif_martes),
                    6 to stringResource(R.string.notif_lunes),
                    7 to stringResource(R.string.notif_domingo)
                )
                val diaLabel = diasOpciones.firstOrNull { it.first == diasAntes }?.second ?: "Viernes"

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.notif_cuando_avisar), style = MaterialTheme.typography.titleMedium)
                        Box {
                            OutlinedButton(
                                onClick = { showDiasMenu = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(diaLabel, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ArrowBack, null,
                                    modifier = Modifier.padding(start = 8.dp))
                            }
                            DropdownMenu(
                                expanded = showDiasMenu,
                                onDismissRequest = { showDiasMenu = false }
                            ) {
                                diasOpciones.forEach { (dias, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            diasAntes = dias
                                            guardar(dias = dias)
                                            showDiasMenu = false
                                        },
                                        trailingIcon = {
                                            if (diasAntes == dias) Text("✓", color = MaterialTheme.colorScheme.primary)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Selector de hora
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.notif_hora_aviso), style = MaterialTheme.typography.titleMedium)
                            Text(
                                "%02d:%02d".format(hora, minuto),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        OutlinedButton(onClick = { showTimePicker = true }) {
                            Text(stringResource(R.string.notif_cambiar_hora))
                        }
                    }
                }

                // Resumen
                val diaResumen = diasOpciones.firstOrNull { it.first == diasAntes }
                    ?.second?.substringBefore(" (") ?: "Viernes"
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.notif_resumen, diaResumen, "%02d:%02d".format(hora, minuto)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

// Convierte diasAntes a día Calendar
fun diasAntesADiaCalendar(diasAntes: Int): Int = when (diasAntes) {
    1 -> Calendar.SATURDAY
    2 -> Calendar.FRIDAY
    3 -> Calendar.THURSDAY
    4 -> Calendar.WEDNESDAY
    5 -> Calendar.TUESDAY
    6 -> Calendar.MONDAY
    7 -> Calendar.SUNDAY
    else -> Calendar.FRIDAY
}