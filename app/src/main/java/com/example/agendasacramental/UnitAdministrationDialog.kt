package com.example.agendasacramental

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

fun adminError(context: Context, error: Throwable): String {
    val code = generateSequence(error) { it.cause }.mapNotNull { it.message }.joinToString(" ")
    val resource = when {
        "ADMIN_LAST" in code -> R.string.admin_last
        "ADMIN_EMAIL" in code -> R.string.admin_invalid_email
        "ADMIN_ALREADY" in code -> R.string.admin_already
        "ADMIN_LIMIT" in code -> R.string.admin_limit
        "ADMIN_IDENTITY" in code -> R.string.admin_identity
        "ADMIN_MISSING" in code -> R.string.admin_missing
        "ADMIN_FORBIDDEN" in code -> R.string.admin_only
        "ADMIN_EXISTS" in code -> R.string.admin_exists
        "ADMIN_PASSWORD" in code -> R.string.admin_password_mismatch
        else -> R.string.admin_error
    }
    return context.getString(resource)
}

@Composable
fun UnitAdministrationDialog(unit: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { UnitAdministrationRepository() }
    val confirmationFocus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<UnitAdministration?>(null) }
    var busy by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var notice by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var remove by remember { mutableStateOf<String?>(null) }
    var review by remember { mutableStateOf(false) }
    fun change(action: String, value: String = "") {
        busy = true; error = ""; notice = ""
        scope.launch {
            runCatching { repo.change(unit, action, value) }
                .onSuccess {
                    email = ""; password = ""; confirmation = ""
                    if (action == "password" || action == "decline") {
                        Toast.makeText(context, context.getString(if (action == "password") R.string.admin_password_success else R.string.admin_declined), Toast.LENGTH_LONG).show()
                        onDismiss()
                    } else {
                        notice = context.getString(R.string.admin_saved)
                        runCatching { repo.load(unit) }.onSuccess { data = it }.onFailure { error = adminError(context, it) }
                    }
                }
                .onFailure { error = adminError(context, it) }
            busy = false
        }
    }
    LaunchedEffect(unit) {
        runCatching { repo.load(unit) }.onSuccess { data = it }.onFailure { error = adminError(context, it) }
        busy = false
    }
    if (review) UnitReviewDialog(unit, false, onCompleted = { review = false; onDismiss() }) { review = false }
    if (remove != null) AlertDialog(onDismissRequest = { remove = null },
        title = { Text(stringResource(R.string.admin_remove_confirm)) },
        text = { Text(data?.admins?.get(remove).orEmpty()) },
        confirmButton = { TextButton(onClick = { val uid = remove!!; remove = null; change("remove", uid) }) { Text(stringResource(R.string.admin_confirm)) } },
        dismissButton = { TextButton(onClick = { remove = null }) { Text(stringResource(R.string.admin_cancel)) } })
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(stringResource(R.string.admin_title)) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(unit)
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
                if (notice.isNotBlank()) Text(notice)
                data?.let { current ->
                    when {
                        repo.isAdmin(current) -> {
                            current.admins.forEach { (uid, address) ->
                                Text(address)
                                TextButton(enabled = !busy && current.admins.size > 1, onClick = { remove = uid }) {
                                    Text(stringResource(if (uid == repo.currentUid()) R.string.admin_leave else R.string.admin_remove))
                                }
                            }
                            if (current.admins.size == 1) Text(stringResource(R.string.admin_last), style = MaterialTheme.typography.bodySmall)
                            OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text(stringResource(R.string.admin_emails)) }, supportingText = { Text(stringResource(R.string.admin_emails_hint)) }, minLines = 2, maxLines = 4)
                            Button(enabled = !busy && email.isNotBlank(), onClick = { change("invite", email) }) { Text(stringResource(R.string.admin_invite_many)) }
                            Text(stringResource(R.string.admin_invite_help), style = MaterialTheme.typography.bodySmall)
                            if (current.invitations.isNotEmpty()) Text(stringResource(R.string.admin_pending))
                            current.invitations.forEach { address ->
                                Text(address)
                                TextButton(enabled = !busy, onClick = { change("cancel", address) }) { Text(stringResource(R.string.admin_cancel_invite)) }
                            }
                            Divider()
                            OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text(stringResource(R.string.admin_password)) }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { confirmationFocus.requestFocus() }))
                            OutlinedTextField(value = confirmation, onValueChange = { confirmation = it }, label = { Text(stringResource(R.string.admin_password_confirm)) }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.focusRequester(confirmationFocus),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); keyboard?.hide() }))
                            Button(enabled = !busy && password.length >= 4 && password == confirmation, onClick = { change("password", password) }) { Text(stringResource(R.string.admin_password_change)) }
                        }
                        repo.isInvited(current) -> {
                            Text(stringResource(R.string.admin_invitation))
                            Button(enabled = !busy, onClick = { change("accept") }) { Text(stringResource(R.string.admin_accept)) }
                            TextButton(enabled = !busy, onClick = { change("decline") }) { Text(stringResource(R.string.admin_decline)) }
                        }
                        else -> {
                            Text(stringResource(R.string.admin_only))
                            TextButton(onClick = { review = true }) { Text(stringResource(R.string.admin_review)) }
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text(stringResource(R.string.admin_close)) } })
}

@Composable
fun UnitReviewDialog(unit: String, alreadyExists: Boolean = true, onCompleted: (() -> Unit)? = null, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val recipient = "martin.cornelli.lds@gmail.com"
    val subject = context.getString(R.string.admin_review_subject, unit)
    val body = context.getString(R.string.admin_review_body, unit, FirebaseAuth.getInstance().currentUser?.email.orEmpty())
    var compose by remember { mutableStateOf(!alreadyExists) }
    var failed by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(if (compose) R.string.admin_review else R.string.admin_exists)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(if (compose) R.string.admin_review_help else R.string.admin_exists_help))
                if (compose) {
                    Text(recipient)
                    Text(subject)
                    Text(body)
                    Button(onClick = {
                        val uri = Uri.parse("mailto:$recipient?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}")
                        runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, uri)) }
                            .onSuccess {
                                Toast.makeText(context, context.getString(R.string.admin_mail_ready), Toast.LENGTH_LONG).show()
                                (onCompleted ?: onDismiss)()
                            }.onFailure { failed = true }
                    }) { Text(stringResource(R.string.admin_mail)) }
                    if (failed) Text(stringResource(R.string.admin_review_help))
                    TextButton(onClick = {
                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText(subject, "$recipient\n$subject\n\n$body"))
                    }) { Text(stringResource(R.string.admin_copy)) }
                } else Button(onClick = { compose = true }) { Text(stringResource(R.string.admin_review)) }
            }
        }, confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.admin_back)) } })
}
