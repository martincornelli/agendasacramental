package com.example.agendasacramental

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** QA-only UI fixture. No authentication, repositories or database writes. */
class HymnTopicsFixture : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        check(BuildConfig.APPLICATION_ID.endsWith(".qa"))
        val language = intent.getStringExtra("language") ?: "es"
        requestedOrientation = if (intent.getBooleanExtra("landscape", false)) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        val configuration = android.content.res.Configuration(resources.configuration).apply {
            setLocale(java.util.Locale.forLanguageTag(language))
            if (intent.getBooleanExtra("large", false)) fontScale = 1.5f
        }
        val localized = createConfigurationContext(configuration)
        setContent {
            CompositionLocalProvider(LocalContext provides localized, LocalConfiguration provides configuration) {
                MaterialTheme(colorScheme = if (intent.getBooleanExtra("dark", false)) darkColorScheme() else lightColorScheme()) {
                    var count by remember { mutableStateOf(0) }
                    var showEditor by remember { mutableStateOf(false) }
                    var topics by remember { mutableStateOf(temasOficialesHimno(1, language)) }
                    if (showEditor) HymnTopicsDialog(1, if (language == "en") "The Morning Breaks" else "Ya rompe el alba",
                        topics, false, "", { showEditor = false }, { topics = it ?: temasOficialesHimno(1, language); showEditor = false })
                    Surface(Modifier.fillMaxSize()) {
                        ModuleRefreshBox(Modifier.fillMaxSize().systemBarsPadding(), onRefresh = { delay(1500); count++ }) {
                            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                                item { Text("Refresh count: $count") }
                                item { Button(onClick = { showEditor = true }) { Text(if (language == "en") "Edit topics" else "Editar temas") } }
                                items(20) { Text("${it + 1} · ${topics.joinToString(", ")}", Modifier.padding(vertical = 12.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}
