package com.example.agendasacramental

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
import kotlinx.coroutines.launch

@Composable
fun SeleccionUnidadScreen(
    userEmail: String,
    onUnidadIngresada: (String) -> Unit,
    onLogout: () -> Unit
) {
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

    // Diálogo para confirmar creación de nueva unidad
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nueva unidad") },
            text = {
                Column {
                    Text("La unidad $numeroUnidad no existe. ¿Desea crearla?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Será el primero en configurar la contraseña para esta unidad.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    isNewUnit = true
                }) { Text("Sí, crear") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancelar") }
            }
        )
    }

    // Diálogo para cambiar contraseña
    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { showForgotDialog = false; newPassword = ""; confirmNewPassword = "" },
            title = { Text("Cambiar contraseña") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Unidad: $numeroUnidad\nSesión verificada: $userEmail",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("Nueva contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it },
                        label = { Text("Confirmar nueva contraseña") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPassword.length < 4) {
                            return@TextButton
                        }
                        if (newPassword != confirmNewPassword) {
                            return@TextButton
                        }
                        isChangingPassword = true
                        scope.launch {
                            val result = repository.cambiarPassword(numeroUnidad, newPassword)
                            isChangingPassword = false
                            showForgotDialog = false
                            newPassword = ""
                            confirmNewPassword = ""
                            if (result.isSuccess) {
                                successMessage = "Contraseña actualizada correctamente."
                                errorMessage = ""
                            } else {
                                errorMessage = "Error al cambiar la contraseña. Intente nuevamente."
                            }
                        }
                    },
                    enabled = newPassword.length >= 4 && newPassword == confirmNewPassword && !isChangingPassword
                ) {
                    if (isChangingPassword) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Cambiar")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showForgotDialog = false
                    newPassword = ""
                    confirmNewPassword = ""
                }) { Text("Cancelar") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Acceso a Unidad",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Sesión: $userEmail",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = numeroUnidad,
            onValueChange = {
                numeroUnidad = it
                isNewUnit = false
                errorMessage = ""
                successMessage = ""
            },
            label = { Text("Número de Unidad") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = ""; successMessage = "" },
            label = { Text(if (isNewUnit) "Crear contraseña" else "Contraseña") },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (passwordVisible) "Ocultar" else "Mostrar"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        if (isNewUnit) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; errorMessage = "" },
                label = { Text("Confirmar contraseña") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (successMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = successMessage,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (numeroUnidad.isBlank()) {
                    errorMessage = "Ingrese el número de unidad"
                    return@Button
                }
                if (password.isBlank()) {
                    errorMessage = "Ingrese la contraseña"
                    return@Button
                }
                if (isNewUnit && password != confirmPassword) {
                    errorMessage = "Las contraseñas no coinciden"
                    return@Button
                }
                if (isNewUnit && password.length < 4) {
                    errorMessage = "La contraseña debe tener al menos 4 caracteres"
                    return@Button
                }

                isLoading = true
                scope.launch {
                    if (isNewUnit) {
                        val result = repository.crearUnidad(numeroUnidad, password, userEmail)
                        isLoading = false
                        if (result.isSuccess) {
                            onUnidadIngresada(numeroUnidad)
                        } else {
                            errorMessage = "Error al crear la unidad. Intente nuevamente."
                        }
                    } else {
                        val existeResult = repository.existeUnidad(numeroUnidad)
                        if (existeResult.isFailure) {
                            isLoading = false
                            errorMessage = "Sin conexión. Verificá tu internet e intentá de nuevo."
                            return@launch
                        }
                        val existe = existeResult.getOrElse { false }
                        if (!existe) {
                            isLoading = false
                            showCreateDialog = true
                            return@launch
                        }
                        val passwordResult = repository.verificarPassword(numeroUnidad, password)
                        if (passwordResult.isFailure) {
                            isLoading = false
                            errorMessage = "Sin conexión. Verificá tu internet e intentá de nuevo."
                            return@launch
                        }
                        val passwordOk = passwordResult.getOrElse { false }
                        isLoading = false
                        if (passwordOk) {
                            onUnidadIngresada(numeroUnidad)
                        } else {
                            errorMessage = "Contraseña incorrecta"
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(if (isNewUnit) "Crear unidad" else "Ingresar")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botón olvidé mi contraseña
        TextButton(
            onClick = {
                if (numeroUnidad.isBlank()) {
                    errorMessage = "Ingrese el número de unidad primero"
                    return@TextButton
                }
                errorMessage = ""
                successMessage = ""
                scope.launch {
                    val existeResult = repository.existeUnidad(numeroUnidad)
                    if (existeResult.isFailure) {
                        errorMessage = "Sin conexión. Verificá tu internet e intentá de nuevo."
                        return@launch
                    }
                    if (existeResult.getOrElse { false }) {
                        showForgotDialog = true
                    } else {
                        errorMessage = "La unidad $numeroUnidad no existe."
                    }
                }
            }
        ) {
            Text("Olvidé mi contraseña")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onLogout) {
            Text("Cerrar sesión de Google")
        }
    }
}