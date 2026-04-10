package com.example.agendasacramental

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : FragmentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private var currentScreen = mutableStateOf<Screen>(Screen.Login)
    private var currentUnidad = mutableStateOf<String>("")
    private var currentAgendaId = mutableStateOf<String?>(null)

    // In-app update
    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
    private var installStateListener: InstallStateUpdatedListener? = null

    private val updateLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            // Usuario canceló o falló — no pasa nada, lo intentará la próxima vez
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        if (auth.currentUser != null) {
            currentScreen.value = Screen.SeleccionUnidad
        }

        verificarActualizacion()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val screen by currentScreen
                    val unidad by currentUnidad
                    val agendaId by currentAgendaId

                    BackHandler(enabled = screen != Screen.Login && screen != Screen.SeleccionUnidad) {
                        when (screen) {
                            Screen.Home -> {
                                currentUnidad.value = ""
                                currentScreen.value = Screen.SeleccionUnidad
                            }
                            Screen.ListaAgendas -> currentScreen.value = Screen.Home
                            Screen.EditarAgenda -> currentScreen.value = Screen.ListaAgendas
                            Screen.Planificacion -> currentScreen.value = Screen.Home
                            else -> {}
                        }
                    }

                    when (screen) {
                        Screen.Login -> LoginScreen(
                            onLoginClick = { signIn() }
                        )
                        Screen.SeleccionUnidad -> SeleccionUnidadScreen(
                            activity = this@MainActivity,
                            userEmail = auth.currentUser?.email ?: "",
                            onUnidadIngresada = { numeroUnidad ->
                                currentUnidad.value = numeroUnidad
                                currentScreen.value = Screen.Home
                            },
                            onLogout = { logout() }
                        )
                        Screen.Home -> HomeScreen(
                            numeroUnidad = unidad,
                            onIrAgendas = { currentScreen.value = Screen.ListaAgendas },
                            onIrPlanificacion = { currentScreen.value = Screen.Planificacion },
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
                            onLogout = { currentScreen.value = Screen.Home }
                        )
                        Screen.EditarAgenda -> EditarAgendaScreen(
                            numeroUnidad = unidad,
                            agendaId = agendaId,
                            userEmail = auth.currentUser?.email ?: "",
                            onBack = { currentScreen.value = Screen.ListaAgendas }
                        )
                        Screen.Planificacion -> PlanificacionScreen(
                            numeroUnidad = unidad,
                            onBack = { currentScreen.value = Screen.Home }
                        )
                    }
                }
            }
        }
    }

    private fun verificarActualizacion() {
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
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    updateLauncher,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
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
}