package com.example.agendasacramental

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch

@Composable
fun SeleccionUnidadScreen(
    activity: FragmentActivity,
    userEmail: String,
    onUnidadIngresada: (String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { AgendaRepository() }
    val scope = rememberCoroutineScope()

    var numeroUnidad by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showForgotDialog by remember { mutableStateOf(false) }
    var isNewUnit by remember { mutableStateOf(false) }
    var isChangingPassword by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var recordarDispositivo by remember { mutableStateOf(false) }
    var usandoContrasena by remember { mutableStateOf(false) }

    val prefs = remember { activity.getSharedPreferences("agenda_prefs", Context.MODE_PRIVATE) }
    val unidadGuardada = remember { prefs.getString("unidad_guardada", "") ?: "" }
    val passwordGuardado = remember { prefs.getString("password_guardado", "") ?: "" }
    val biometriaDisponible = remember { isBiometriaDisponible(activity) }
    val tieneSesionGuardada = unidadGuardada.isNotBlank() && passwordGuardado.isNotBlank() && biometriaDisponible
    val tieneUnidadGuardada = unidadGuardada.isNotBlank()

    // Pre-cargar número de unidad
    LaunchedEffect(Unit) {
        if (unidadGuardada.isNotBlank()) numeroUnidad = unidadGuardada
    }

    // Lanzar biometría automáticamente si hay sesión guardada
    LaunchedEffect(tieneSesionGuardada) {
        if (tieneSesionGuardada && !usandoContrasena) {
            lanzarBiometria(
                activity = activity,
                onSuccess = { onUnidadIngresada(unidadGuardada) },
                onUsarContrasena = { usandoContrasena = true },
                onError = { msg -> errorMessage = msg }
            )
        }
    }

    // Diálogo crear nueva unidad
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text(stringResource(R.string.unidad_nueva)) },
            text = {
                Column {
                    Text(stringResource(R.string.unidad_confirmacion_crear, numeroUnidad))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.unidad_primera_contrasena),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCreateDialog = false; isNewUnit = true }) { Text("Sí, crear") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text(stringResource(R.string.btn_cancelar)) }
            }
        )
    }

    // Diálogo cambiar contraseña
    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false; newPassword = ""; confirmNewPassword = "" },
            title = { Text(stringResource(R.string.contrasena_cambiar)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Unidad: $numeroUnidad\nSesión verificada: $userEmail",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newPassword, onValueChange = { newPassword = it },
                        label = { Text(stringResource(R.string.contrasena_nueva)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    OutlinedTextField(
                        value = confirmNewPassword, onValueChange = { confirmNewPassword = it },
                        label = { Text(stringResource(R.string.contrasena_confirmar_nueva)) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isChangingPassword = true
                        scope.launch {
                            val result = repository.cambiarPassword(numeroUnidad, newPassword)
                            isChangingPassword = false
                            showForgotDialog = false
                            newPassword = ""; confirmNewPassword = ""
                            if (result.isSuccess) {
                                successMessage = context.getString(R.string.contrasena_actualizada)
                                errorMessage = ""
                                prefs.edit().remove("unidad_guardada").remove("password_guardado").apply()
                                usandoContrasena = true
                            } else {
                                errorMessage = context.getString(R.string.contrasena_error)
                            }
                        }
                    },
                    enabled = newPassword.length >= 4 && newPassword == confirmNewPassword && !isChangingPassword
                ) {
                    if (isChangingPassword) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    else Text("Cambiar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false; newPassword = ""; confirmNewPassword = "" }) { Text(stringResource(R.string.btn_cancelar)) }
            }
        )
    }

    // Pantalla principal
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = stringResource(R.string.unidad_acceso), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Sesión: $userEmail", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = numeroUnidad,
            onValueChange = { numeroUnidad = it; isNewUnit = false; errorMessage = ""; successMessage = "" },
            label = { Text(stringResource(R.string.unidad_numero)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = ""; successMessage = "" },
            label = { Text(if (isNewUnit) stringResource(R.string.contrasena_crear) else stringResource(R.string.contrasena)) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) stringResource(R.string.contrasena_ocultar) else stringResource(R.string.contrasena_mostrar)
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )

        if (isNewUnit) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMessage = "" },
                label = { Text(stringResource(R.string.contrasena_confirmar)) },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
        }

        // Checkbox recordar — siempre visible si no es unidad nueva
        if (!isNewUnit) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Checkbox(checked = recordarDispositivo, onCheckedChange = { recordarDispositivo = it })
                Text(
                    if (biometriaDisponible) stringResource(R.string.biometria_recordar) else stringResource(R.string.recordar_unidad),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (successMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = successMessage, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (numeroUnidad.isBlank()) { errorMessage = context.getString(R.string.unidad_ingrese_numero); return@Button }
                if (password.isBlank()) { errorMessage = context.getString(R.string.contrasena_ingrese); return@Button }
                if (isNewUnit && password != confirmPassword) { errorMessage = context.getString(R.string.contrasena_no_coinciden); return@Button }
                if (isNewUnit && password.length < 4) { errorMessage = context.getString(R.string.contrasena_minimo); return@Button }

                isLoading = true
                scope.launch {
                    if (isNewUnit) {
                        val result = repository.crearUnidad(numeroUnidad, password, userEmail)
                        isLoading = false
                        if (result.isSuccess) {
                            if (recordarDispositivo) { if (biometriaDisponible) guardarCredenciales(prefs, numeroUnidad, password) else prefs.edit().putString("unidad_guardada", numeroUnidad).remove("password_guardado").apply() }
                            onUnidadIngresada(numeroUnidad)
                        } else {
                            errorMessage = context.getString(R.string.unidad_error_crear)
                        }
                    } else {
                        val existe = repository.existeUnidad(numeroUnidad)
                        if (!existe) { isLoading = false; showCreateDialog = true; return@launch }
                        val passwordOk = repository.verificarPassword(numeroUnidad, password)
                        isLoading = false
                        if (passwordOk) {
                            if (recordarDispositivo) { if (biometriaDisponible) guardarCredenciales(prefs, numeroUnidad, password) else prefs.edit().putString("unidad_guardada", numeroUnidad).remove("password_guardado").apply() }
                            onUnidadIngresada(numeroUnidad)
                        } else {
                            errorMessage = context.getString(R.string.contrasena_incorrecta)
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            else Text(if (isNewUnit) stringResource(R.string.unidad_crear) else stringResource(R.string.btn_ingresar))
        }

        // Botón huella si hay sesión guardada y el usuario eligió usar contraseña
        if (tieneSesionGuardada && usandoContrasena) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = {
                usandoContrasena = false
                lanzarBiometria(
                    activity = activity,
                    onSuccess = { onUnidadIngresada(unidadGuardada) },
                    onUsarContrasena = { usandoContrasena = true },
                    onError = { msg -> errorMessage = msg }
                )
            }) { Text(stringResource(R.string.biometria_usar)) }
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = {
            if (numeroUnidad.isBlank()) { errorMessage = context.getString(R.string.unidad_ingrese_numero_primero); return@TextButton }
            errorMessage = ""; successMessage = ""
            scope.launch {
                val existe = repository.existeUnidad(numeroUnidad)
                if (existe) showForgotDialog = true
                else errorMessage = "La unidad $numeroUnidad no existe."
            }
        }) { Text(stringResource(R.string.contrasena_olvide)) }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onLogout) { Text(stringResource(R.string.cerrar_sesion_google)) }
    }
}

// --- Helpers ---

fun isBiometriaDisponible(context: Context): Boolean {
    val bm = BiometricManager.from(context)
    return bm.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
    ) == BiometricManager.BIOMETRIC_SUCCESS
}

fun guardarCredenciales(prefs: android.content.SharedPreferences, numeroUnidad: String, password: String) {
    prefs.edit().putString("unidad_guardada", numeroUnidad).putString("password_guardado", password).apply()
}

fun lanzarBiometria(
    activity: FragmentActivity,
    onSuccess: () -> Unit,
    onUsarContrasena: () -> Unit,
    onError: (String) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onSuccess()
        }
        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            when (errorCode) {
                BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                BiometricPrompt.ERROR_USER_CANCELED -> onUsarContrasena()
                else -> onError("Error: $errString")
            }
        }
        override fun onAuthenticationFailed() {
            onError(activity.getString(R.string.biometria_no_reconocida))
        }
    })

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Agenda Sacramental")
        .setSubtitle(activity.getString(R.string.biometria_autenticar))
        .setNegativeButtonText(activity.getString(R.string.contrasena_usar))
        .setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        .build()

    prompt.authenticate(promptInfo)
}