package com.example.agendasacramental

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

internal fun shouldShowReleaseNotes(
    lastSeenVersion: Int?,
    currentVersion: Int,
    firstInstallTime: Long,
    lastUpdateTime: Long
): Boolean = if (lastSeenVersion == null) {
    lastUpdateTime > firstInstallTime
} else {
    lastSeenVersion < currentVersion
}

@Composable
fun ReleaseNotesDialog(visible: Boolean) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("release_notes", Context.MODE_PRIVATE) }
    var pending by remember {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val lastSeen = if (preferences.contains("last_seen_version")) {
            preferences.getInt("last_seen_version", 0)
        } else null
        mutableStateOf(shouldShowReleaseNotes(
            lastSeen, BuildConfig.VERSION_CODE, info.firstInstallTime, info.lastUpdateTime
        ))
    }
    // Fresh installs establish a baseline without showing an update announcement.
    LaunchedEffect(Unit) {
        if (!pending && !preferences.contains("last_seen_version")) {
            preferences.edit().putInt("last_seen_version", BuildConfig.VERSION_CODE).apply()
        }
    }
    val dismiss = {
        preferences.edit().putInt("last_seen_version", BuildConfig.VERSION_CODE).apply()
        pending = false
    }
    if (visible && pending) {
        AlertDialog(
            onDismissRequest = dismiss,
            title = { Text(stringResource(R.string.release_notes_title, BuildConfig.VERSION_NAME)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(stringResource(R.string.release_notes_topics_210))
                    Text(stringResource(R.string.release_notes_unit_topics_210))
                    Text(stringResource(R.string.release_notes_seasonal_210))
                    Text(stringResource(R.string.release_notes_library_210))
                    Text(stringResource(R.string.release_notes_refresh_210))
                }
            },
            confirmButton = {
                TextButton(onClick = dismiss) { Text(stringResource(R.string.release_notes_continue)) }
            }
        )
    }
}
