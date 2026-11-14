package com.example.agendasacramental

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import android.content.Context
import android.content.Intent
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.auth.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private var currentScreen = mutableStateOf<Screen>(Screen.Login)
    private var currentUnidad = mutableStateOf<String>("")
    private var currentAgendaId = mutableStateOf<String?>(null)
    private var pendingNotificationAgendaId: String? = null
    private var currentModoLecturaAgenda = mutableStateOf<Agenda?>(null)

    // In-app update
    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
    private var installStateListener: InstallStateUpdatedListener? = null

    private val updatePreferences by lazy {
        getSharedPreferences("app_update_prefs", Context.MODE_PRIVATE)
    }

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            // Si el usuario cancela o el flujo falla, la misma versión no se vuelve a ofrecer
            // hasta que venza el período de espera.
        }
    }

    override fun attachBaseContext(newBase: Context) {
        // Aplicar idioma para API < 33
        super.attachBaseContext(LocaleHelper.aplicarIdioma(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Aplicar idioma al arrancar (API 33+)
        LocaleHelper.aplicarAlArrancar(this)

        if (auth.currentUser != null) {
            currentScreen.value = Screen.SeleccionUnidad
        }

        // Restaurar pantalla si venimos de un cambio de idioma
        val prefsRestore = getSharedPreferences("agenda_prefs", android.content.Context.MODE_PRIVATE)
        val pantallaGuardada = prefsRestore.getString("pantalla_actual", null)
        if (pantallaGuardada != null && auth.currentUser != null) {
            val unidadGuardada = prefsRestore.getString("unidad_recreate", "") ?: ""
            if (unidadGuardada.isNotBlank()) currentUnidad.value = unidadGuardada
            currentScreen.value = when (pantallaGuardada) {
                "Home" -> Screen.Home
                "Login" -> Screen.Login
                else -> Screen.SeleccionUnidad
            }
            prefsRestore.edit().remove("pantalla_actual").remove("unidad_recreate").apply()
        }

        pendingNotificationAgendaId = savedInstanceState?.getString("pending_notification_agenda")
        handleAgendaIntent(intent)

        verificarActualizacion()

        setContent {
            val context = LocalContext.current
            val prefs = remember { context.getSharedPreferences("agenda_prefs", Context.MODE_PRIVATE) }
            var tema by remember { mutableStateOf(prefs.getString("tema", "sistema") ?: "sistema") }
            var idioma by remember { mutableStateOf(LocaleHelper.getIdiomaGuardado(context)) }
            val sistemaOscuro = isSystemInDarkTheme()
            val usarOscuro = when (tema) {
                "oscuro" -> true
                "claro" -> false
                else -> sistemaOscuro
            }
            com.example.agendasacramental.ui.theme.AgendaSacramentalTheme(darkTheme = usarOscuro) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val screen by currentScreen
                    val unidad by currentUnidad
                    val agendaId by currentAgendaId
                    val modoLecturaAgenda by currentModoLecturaAgenda

                    ReleaseNotesDialog(visible = screen == Screen.Home)

                    BackHandler(enabled = screen != Screen.Login && screen != Screen.SeleccionUnidad) {
                        when (screen) {
                            Screen.Home -> {
                                currentUnidad.value = ""
                                currentScreen.value = Screen.SeleccionUnidad
                            }
                            Screen.ListaAgendas -> currentScreen.value = Screen.Home
                            Screen.EditarAgenda -> currentScreen.value = Screen.ListaAgendas
                            Screen.Planificacion -> currentScreen.value = Screen.Home
                            Screen.PlanificadorHimnos -> currentScreen.value = Screen.Home
                            Screen.ModoLectura -> currentScreen.value = Screen.ListaAgendas
                            Screen.Notificaciones -> currentScreen.value = Screen.Home
                            else -> {}
                        }
                    }

                    when (screen) {
                        Screen.Login -> LoginScreen(
                            onLoginClick = { signIn() },
                            idioma = idioma,
                            onIdiomaChange = { nuevoIdioma ->
                                idioma = nuevoIdioma
                                LocaleHelper.setIdioma(context, nuevoIdioma)
                                prefs.edit().putString("pantalla_actual", "Login").apply()
                                recreate()
                            }
                        )
                        Screen.SeleccionUnidad -> SeleccionUnidadScreen(
                            activity = this@MainActivity,
                            userEmail = auth.currentUser?.email ?: "",
                            onUnidadIngresada = { numeroUnidad ->
                                currentUnidad.value = numeroUnidad
                                currentScreen.value = Screen.Home
                                AgendaNotificationWorker.programar(this@MainActivity)
                                // Guardar próxima agenda en prefs para notificaciones
                                sincronizarDatosUnidad(numeroUnidad)
                                openPendingNotificationAgenda()
                            },
                            onLogout = { logout() }
                        )
                        Screen.Home -> HomeScreen(
                            numeroUnidad = unidad,
                            tema = tema,
                            onTemaChange = { nuevoTema ->
                                tema = nuevoTema
                                prefs.edit().putString("tema", nuevoTema).apply()
                            },
                            idioma = idioma,
                            onIdiomaChange = { nuevoIdioma ->
                                idioma = nuevoIdioma
                                LocaleHelper.setIdioma(context, nuevoIdioma)
                                prefs.edit()
                                    .putString("pantalla_actual", "Home")
                                    .putString("unidad_recreate", unidad)
                                    .apply()
                                recreate()
                            },
                            onIrAgendas = { currentScreen.value = Screen.ListaAgendas },
                            onIrPlanificacion = { currentScreen.value = Screen.Planificacion },
                            onIrPlanificadorHimnos = { currentScreen.value = Screen.PlanificadorHimnos },
                            onIrNotificaciones = { currentScreen.value = Screen.Notificaciones },
                            onCambiarUnidad = { currentUnidad.value = ""; currentScreen.value = Screen.SeleccionUnidad },
                            onLogout = { logout() }
                        )
                        Screen.ListaAgendas -> ListaAgendasScreen(
                            numeroUnidad = unidad,
                            userEmail = auth.currentUser?.email ?: "",
                            onNuevaAgenda = {
                                currentAgendaId.value = null
                                currentScreen.value = Screen.EditarAgenda
                            },
                            onEditarAgenda = { id ->
                                currentAgendaId.value = id
                                currentScreen.value = Screen.EditarAgenda
                            },
                            onModoLectura = { agenda ->
                                currentModoLecturaAgenda.value = agenda
                                currentScreen.value = Screen.ModoLectura
                            },
                            onLogout = { currentScreen.value = Screen.Home }
                        )
                        Screen.EditarAgenda -> key(unidad, agendaId) { EditarAgendaScreen(
                            numeroUnidad = unidad,
                            agendaId = agendaId,
                            userEmail = auth.currentUser?.email ?: "",
                            onBack = { currentScreen.value = Screen.ListaAgendas },
                            onModoLectura = { agenda ->
                                currentModoLecturaAgenda.value = agenda
                                currentScreen.value = Screen.ModoLectura
                            }
                        ) }
                        Screen.Planificacion -> PlanificacionScreen(
                            numeroUnidad = unidad,
                            onBack = { currentScreen.value = Screen.Home }
                        )
                        Screen.PlanificadorHimnos -> PlanificadorHimnosScreen(
                            numeroUnidad = unidad,
                            userEmail = auth.currentUser?.email ?: "",
                            onBack = { currentScreen.value = Screen.Home }
                        )
                        Screen.Notificaciones -> ConfiguracionNotificacionesScreen(
                            numeroUnidad = unidad,
                            onBack = { currentScreen.value = Screen.Home }
                        )
                        Screen.ModoLectura -> modoLecturaAgenda?.let { agenda ->
                            ModoLecturaScreen(
                                agenda = agenda,
                                onBack = { currentScreen.value = Screen.ListaAgendas }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAgendaIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("pending_notification_agenda", pendingNotificationAgendaId)
        super.onSaveInstanceState(outState)
    }

    private fun handleAgendaIntent(intent: Intent) {
        intent.getStringExtra("open_agenda_id")?.takeIf { it.isNotBlank() }?.let {
            pendingNotificationAgendaId = it
        }
        // Consume once: recreation or a subsequent launcher entry must not replay it.
        intent.removeExtra("open_agenda_id")
        openPendingNotificationAgenda()
    }

    private fun openPendingNotificationAgenda() {
        val target = pendingNotificationAgendaId ?: return
        when (agendaEntry(auth.currentUser != null, currentUnidad.value)) {
            AgendaEntry.LOGIN -> currentScreen.value = Screen.Login
            AgendaEntry.UNIT_SELECTION -> currentScreen.value = Screen.SeleccionUnidad
            AgendaEntry.EDITOR -> {
                currentAgendaId.value = target
                pendingNotificationAgendaId = null
                currentScreen.value = Screen.EditarAgenda
            }
        }
    }

    private fun sincronizarDatosUnidad(numeroUnidad: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val repository = AgendaRepository()
            repository.sincronizarFechasParticipacionDesdeHistorial(numeroUnidad)
            repository.guardarProximaAgendaEnPrefs(numeroUnidad, this@MainActivity)
        }
    }

    private fun verificarActualizacion() {
        if (!puedeUsarActualizacionesDePlay()) return

        val packageInfo = try {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        } catch (_: Exception) {
            return
        }
        val ahora = System.currentTimeMillis()
        val instalacionReciente = ahora - maxOf(
            packageInfo.firstInstallTime,
            packageInfo.lastUpdateTime
        ) < AppUpdatePromptPolicy.INSTALL_GRACE_PERIOD_MS
        if (instalacionReciente) return

        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // Listener para cuando la descarga termina en segundo plano
        installStateListener = InstallStateUpdatedListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                // La actualización está lista — completar instalación
                appUpdateManager.completeUpdate()
            }
        }
        installStateListener?.let { appUpdateManager.registerListener(it) }

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            val versionDisponible = appUpdateInfo.availableVersionCode()
            val debeOfrecer = AppUpdatePromptPolicy.shouldOfferUpdate(
                nowMillis = System.currentTimeMillis(),
                installedVersionCode = BuildConfig.VERSION_CODE,
                availableVersionCode = versionDisponible,
                firstInstallTimeMillis = packageInfo.firstInstallTime,
                lastUpdateTimeMillis = packageInfo.lastUpdateTime,
                lastPromptVersionCode = updatePreferences.getInt("last_prompt_version", -1),
                lastPromptTimeMillis = updatePreferences.getLong("last_prompt_time", 0L)
            )
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                debeOfrecer &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                updatePreferences.edit()
                    .putInt("last_prompt_version", versionDisponible)
                    .putLong("last_prompt_time", System.currentTimeMillis())
                    .apply()
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                )
            }
        }
    }

    private fun puedeUsarActualizacionesDePlay(): Boolean {
        if (BuildConfig.DEBUG) return false

        val installerPackage = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val sourceInfo = packageManager.getInstallSourceInfo(packageName)
                sourceInfo.installingPackageName
                    ?: sourceInfo.initiatingPackageName
                    ?: sourceInfo.originatingPackageName
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstallerPackageName(packageName)
            }
        } catch (_: Exception) {
            null
        }

        return installerPackage == "com.android.vending"
    }

    override fun onResume() {
        super.onResume()
        if (!puedeUsarActualizacionesDePlay()) return

        // Si el usuario vuelve a la app con una actualización ya descargada, completarla
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                appUpdateManager.completeUpdate()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        installStateListener?.let { appUpdateManager.unregisterListener(it) }
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener(this) { authTask ->
                        if (authTask.isSuccessful) {
                            currentScreen.value = Screen.SeleccionUnidad
                        }
                    }
            } catch (e: ApiException) {
                android.util.Log.e("AUTH", "ApiException: ${e.statusCode}")
            }
        }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private fun logout() {
        pendingNotificationAgendaId = null
        currentAgendaId.value = null
        auth.signOut()
        googleSignInClient.signOut()
        currentUnidad.value = ""
        currentScreen.value = Screen.Login
    }
}

sealed class Screen {
    object Login : Screen()
    object SeleccionUnidad : Screen()
    object Home : Screen()
    object ListaAgendas : Screen()
    object EditarAgenda : Screen()
    object Planificacion : Screen()
    object PlanificadorHimnos : Screen()
    object ModoLectura : Screen()
    object Notificaciones : Screen()
}
