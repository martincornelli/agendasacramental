package com.example.agendasacramental

import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ConfirmacionAgendaTest {
    private fun date(day: Int) = Timestamp(Calendar.getInstance().apply { set(2026, 10, day, 12, 0, 0); set(Calendar.MILLISECOND, 0) }.time)
    private fun agenda() = Agenda(fecha = date(15), confirmacionVersion = 1, preside = "A", dirige = "B", primeraOracion = "C", oracionFinal = "D", pianista = "E", directorMusica = "F", primerHimnoNumero = 1, himnoSacramentalNumero = 2, himnoFinalNumero = 3, mensajesEvangelio = listOf(MensajeEvangelio(id = "one", nombre = "G", tema = "Fe"), MensajeEvangelio(id = "two", nombre = "H", tema = "Amor")))
    private fun acceptAll(a: Agenda) = reconciliarConfirmacion(a.copy(aceptaciones = comprobacionesAgenda(a).filter { it.completa }.associate { it.id to AceptacionAsignacion(it.firma, "qa") }))
    @Test fun manualStatusPreservesAcceptances() {
        val confirmed = cambiarEstadoAgenda(agenda(), EstadoAgenda.CONFIRMADA, "editor", date(15))
        assertEquals(EstadoAgenda.CONFIRMADA, confirmed.estado)
        assertEquals(12, confirmed.aceptaciones.size)
        val completed = cambiarEstadoAgenda(confirmed, EstadoAgenda.REALIZADA, "editor", date(15))
        assertEquals(EstadoAgenda.REALIZADA, completed.estado)
        assertEquals(confirmed.aceptaciones, completed.aceptaciones)
        val draft = cambiarEstadoAgenda(completed, EstadoAgenda.BORRADOR, "editor", date(15))
        assertEquals(EstadoAgenda.CONFIRMADA, draft.estado)
        assertEquals(confirmed.aceptaciones, draft.aceptaciones)
        val partial = confirmed.copy(aceptaciones = confirmed.aceptaciones - "primeraOracion")
        val partialDraft = cambiarEstadoAgenda(partial, EstadoAgenda.BORRADOR, "editor", date(15))
        assertEquals(EstadoAgenda.BORRADOR, partialDraft.estado)
        assertEquals(partial.aceptaciones, partialDraft.aceptaciones)
        assertThrows(IllegalArgumentException::class.java) { cambiarEstadoAgenda(agenda().copy(pianista = ""), EstadoAgenda.CONFIRMADA, "editor", date(15)) }
    }
    @Test fun emptyAndPartialNeverConfirm() {
        assertEquals(EstadoAgenda.BORRADOR, acceptAll(Agenda(confirmacionVersion = 1)).estado)
        val a = acceptAll(agenda())
        assertEquals(EstadoAgenda.CONFIRMADA, a.estado)
        assertEquals(EstadoAgenda.BORRADOR, reconciliarConfirmacion(a.copy(aceptaciones = a.aceptaciones - "primeraOracion")).estado)
    }
    @Test fun reorderPreservesButChangesInvalidateOnlyAffectedChecks() {
        val a = acceptAll(agenda())
        assertEquals(EstadoAgenda.CONFIRMADA, reconciliarConfirmacion(a.copy(mensajesEvangelio = a.mensajesEvangelio.reversed())).estado)
        val b = reconciliarConfirmacion(a.copy(primeraOracion = "New"))
        assertEquals(a.aceptaciones.size - 1, b.aceptaciones.size)
        assertFalse(reconciliarConfirmacion(b.copy(primeraOracion = "C")).aceptaciones.containsKey("primeraOracion"))
        assertEquals(a.aceptaciones.size - 2, reconciliarConfirmacion(a.copy(primerHimnoNumero = 99)).aceptaciones.size)
        assertEquals(a.aceptaciones.size - 3, reconciliarConfirmacion(a.copy(pianista = "New")).aceptaciones.size)
        assertEquals(0, reconciliarConfirmacion(a.copy(fecha = date(22))).aceptaciones.size)
        val topic = reconciliarConfirmacion(a.copy(mensajesEvangelio = a.mensajesEvangelio.map { if (it.id == "one") it.copy(tema = "New") else it }))
        assertFalse(topic.aceptaciones.containsKey("mensaje_one")); assertTrue(topic.aceptaciones.containsKey("mensaje_two"))
    }
    @Test fun meetingTypesAndLegacy() {
        assertEquals(EstadoAgenda.CONFIRMADA, acceptAll(agenda().copy(reunionTestimonios = true, mensajesEvangelio = emptyList())).estado)
        assertEquals(EstadoAgenda.BORRADOR, acceptAll(agenda().copy(presentacionPrimaria = true)).estado)
        assertEquals(EstadoAgenda.CONFIRMADA, acceptAll(agenda().copy(presentacionPrimaria = true, responsablePrimaria = "Leader")).estado)
        assertEquals(EstadoAgenda.CONFIRMADA, reconciliarConfirmacion(Agenda(estado = EstadoAgenda.CONFIRMADA)).estado)
        assertEquals(EstadoAgenda.REALIZADA, reconciliarConfirmacion(agenda().copy(estado = EstadoAgenda.REALIZADA)).estado)
    }
    @Test fun wireSignatureMatchesWebIncludingUnicode() {
        assertEquals("10:2026-11-156:NORMAL7:preside7:José 😀", comprobacionesAgenda(agenda().copy(preside = "José 😀")).first().firma)
        assertEquals(6, organizaciones.size)
    }
}
