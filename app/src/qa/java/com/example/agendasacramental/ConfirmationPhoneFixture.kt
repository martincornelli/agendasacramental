package com.example.agendasacramental

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

/** QA APK only. Production screens and repositories, isolated emulator and demo project. */
class ConfirmationPhoneFixture : ComponentActivity() {
    private var qaResources: android.content.res.Resources? = null
    override fun getResources(): android.content.res.Resources = qaResources ?: super.getResources()

    override fun onCreate(savedInstanceState: Bundle?) {
        val language = intent.getStringExtra("language") ?: "es"
        val appearance = intent.getStringExtra("appearance") ?: "light"
        val configuration = android.content.res.Configuration(baseContext.resources.configuration).apply {
            setLocale(java.util.Locale.forLanguageTag(language))
            if (appearance.contains("large")) fontScale = 1.5f
        }
        // Configure the Activity itself: popup windows create their own themed contexts.
        qaResources = baseContext.createConfigurationContext(configuration).resources
        super.onCreate(savedInstanceState)
        check(BuildConfig.APPLICATION_ID.endsWith(".qa"))
        val old = FirebaseApp.getInstance()
        if (old.options.projectId != "demo-agenda-phone") {
            val options = FirebaseOptions.Builder(old.options).setProjectId("demo-agenda-phone").build()
            old.delete()
            FirebaseApp.initializeApp(this, options)
            FirebaseFirestore.getInstance().apply {
                firestoreSettings = FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build()
                useEmulator("127.0.0.1", 8191)
            }
        }
        val unit = when (intent.getStringExtra("scenario")) { "empty-unit" -> ""; "other-unit" -> "other-unit"; else -> "phone-qa" }
        requestedOrientation = if (appearance.contains("landscape")) android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContent {
            CompositionLocalProvider(
                LocalContext provides this,
                LocalConfiguration provides resources.configuration,
                androidx.activity.compose.LocalActivityResultRegistryOwner provides this,
                androidx.activity.compose.LocalOnBackPressedDispatcherOwner provides this
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(resources.displayMetrics.density, configuration.fontScale)
                ) {
                com.example.agendasacramental.ui.theme.AgendaSacramentalTheme(darkTheme = appearance.contains("dark")) {
                    Surface(Modifier.fillMaxSize()) {
                        var route by remember { mutableStateOf(intent.getStringExtra("screen") ?: "menu") }
                        when (route) {
                            "editor" -> EditarAgendaScreen(unit, "phone-agenda", "phone-qa@example.test", { route = "menu" })
                            "hymns" -> PlanificadorHimnosScreen(unit, "phone-qa@example.test", { route = "home" })
                            "planning" -> PlanificacionScreen("phone-qa", { route = "menu" })
                            "home" -> HomeScreen(
                                numeroUnidad = unit, tema = "light", onTemaChange = {},
                                idioma = language, onIdiomaChange = {}, onIrAgendas = { route = "agendas" },
                                onIrPlanificacion = { route = "planning" }, onIrPlanificadorHimnos = { route = "hymns" },
                                onIrNotificaciones = { route = "menu" }, onCambiarUnidad = { route = "menu" }, onLogout = { route = "menu" }
                            )
                            "agendas" -> ListaAgendasScreen(unit, "phone-qa@example.test", { route = "editor" }, { route = "editor" }, { route = "menu" }, { route = "home" })
                            else -> Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("QA · Firestore emulator · $language")
                                Button(onClick = { route = "editor" }) { Text("Editor") }
                                Button(onClick = { route = "planning" }) { Text("Planning") }
                            }
                        }
                    }
                }
                }
            }
        }
    }
}
