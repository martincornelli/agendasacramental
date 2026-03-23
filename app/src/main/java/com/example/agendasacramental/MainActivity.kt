package com.example.agendasacramental

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    // Estado de navegación
    private var currentScreen = mutableStateOf<Screen>(Screen.Login)
    private var currentUnidad = mutableStateOf<String>("")
    private var currentAgendaId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = Firebase.auth

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Si ya está logueado con Google, saltar a pantalla de unidad
        if (auth.currentUser != null) {
            currentScreen.value = Screen.SeleccionUnidad
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val screen by currentScreen
                    val unidad by currentUnidad
                    val agendaId by currentAgendaId

                    when (screen) {
                        Screen.Login -> LoginScreen(
                            onLoginClick = { signIn() }
                        )
                        Screen.SeleccionUnidad -> SeleccionUnidadScreen(
                            userEmail = auth.currentUser?.email ?: "",
                            onUnidadIngresada = { numeroUnidad ->
                                currentUnidad.value = numeroUnidad
                                currentScreen.value = Screen.ListaAgendas
                            },
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
                            onLogout = { logout() }
                        )
                        Screen.EditarAgenda -> EditarAgendaScreen(
                            numeroUnidad = unidad,
                            agendaId = agendaId,
                            userEmail = auth.currentUser?.email ?: "",
                            onBack = { currentScreen.value = Screen.ListaAgendas }
                        )
                    }
                }
            }
        }
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
    object ListaAgendas : Screen()
    object EditarAgenda : Screen()
}
