package com.example.agendasacramental

import com.google.firebase.Timestamp
import java.util.Calendar
import org.junit.Assert.*
import org.junit.Test

class FechasParticipacionTest {
    private fun date(day: Int, hour: Int = 12) = Timestamp(Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.NOVEMBER, day, hour, 0)
    }.time)

    @Test fun participationMatchesDayRegardlessOfStoredHour() {
        assertTrue(mismoDiaParticipacion(date(8, 9), date(8)))
        assertFalse(mismoDiaParticipacion(date(8), date(7)))
        assertFalse(mismoDiaParticipacion(null, null))
    }

    @Test fun removedOrReplacedTalkDoesNotLeaveAutomaticDateInRanking() {
        val brother = Hermano(nombre = "Ana", ultimaVezDiscursoManual = date(8),
            ultimaVezDiscursoAutoSync = date(8), ultimaVezDiscursoAntesAutoSync = date(1))
        val earlier = Agenda(fecha = date(4), mensajesEvangelio = listOf(MensajeEvangelio(tipo = TipoMensaje.DISCURSO, nombre = "Ana")))
        for (messages in listOf(emptyList(), listOf(MensajeEvangelio(tipo = TipoMensaje.HIMNO_INTERMEDIO, nombre = "Ana")), listOf(MensajeEvangelio(tipo = TipoMensaje.DISCURSO, nombre = "Juan")))) {
            val changed = Agenda(fecha = date(8), mensajesEvangelio = messages)
            assertEquals(date(4), calcularRankings(listOf(brother), listOf(earlier, changed), ConfiguracionPlanificacion()).single().ultimaVezDiscurso)
        }
        assertEquals(date(1), calcularRankings(listOf(brother), emptyList(), ConfiguracionPlanificacion()).single().ultimaVezDiscurso)
        assertNull(fechaManualParticipacion(date(8), date(8), null))
        assertEquals(date(8), fechaManualParticipacion(date(8), null, null))
    }
}
