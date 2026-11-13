package com.example.agendasacramental

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import com.google.firebase.Timestamp
import java.util.GregorianCalendar

class BulkPrintFixture : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val agendas = listOf(8, 15, 1).map { day ->
            Agenda(id = day.toString(), fecha = Timestamp(GregorianCalendar(2026, 10, day, 12, 0).time),
                preside = "Presidente de prueba", dirige = "Consejero de prueba",
                anuncios = "Actividad de servicio el sábado de mañana.",
                reunionTestimonios = day == 1, testimonios = listOf("Ana de prueba", "Luis de prueba"))
        }
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface { ImprimirAgendasScreen(agendas) { finish() } }
            }
        }
    }
}
