package com.example.agendasacramental

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Refresh keeps the existing content and scroll position; the button also supports accessibility. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModuleRefreshBox(modifier: Modifier = Modifier, enabled: Boolean = true,
                     onRefresh: suspend () -> Unit, content: @Composable BoxScope.() -> Unit) {
    var refreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val latestRefresh by rememberUpdatedState(onRefresh)
    val refresh = {
        if (enabled && !refreshing) {
            refreshing = true
            scope.launch {
                try { latestRefresh() }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { Toast.makeText(context, R.string.module_refresh_error, Toast.LENGTH_LONG).show() }
                finally { refreshing = false }
            }
        }
    }
    Column(modifier) {
        TextButton(onClick = refresh, enabled = enabled && !refreshing, modifier = Modifier.align(Alignment.End)) {
            Icon(Icons.Default.Refresh, null)
            Text(stringResource(R.string.module_refresh))
        }
        PullToRefreshBox(isRefreshing = refreshing, onRefresh = refresh, modifier = Modifier.weight(1f).fillMaxWidth(), content = content)
    }
}
