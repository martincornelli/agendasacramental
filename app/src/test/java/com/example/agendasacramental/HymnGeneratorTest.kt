package com.example.agendasacramental

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class HymnGeneratorTest {

    private val catalogo = mapOf(
        1 to Himnos.lista.getValue(1),
        2 to Himnos.lista.getValue(2),
        42 to Himnos.lista.getValue(42),
        48 to Himnos.lista.getValue(48),
        103 to Himnos.lista.getValue(103),
        104 to Himnos.lista.getValue(104),
        1001 to Himnos.lista.getValue(1001),
        1007 to Himnos.lista.getValue(1007)
    )

    @Test
    fun `conserva las selecciones manuales y marca las nuevas como automaticas`() {
        val fecha = date(2026, Calendar.NOVEMBER, 15)
        val agenda = Agenda(
            fecha = Timestamp(fecha),
            primerHimnoNumero = 1,
            primerHimnoNombre = catalogo.getValue(1),
            primerHimnoOrigen = OrigenHimno.PLANIFICADOR_MANUAL
        )

        val propuesta = generar(listOf(agenda), listOf(fecha)).propuestas.single().propuesta

        assertEquals(1, propuesta.apertura.numero)
        assertEquals(OrigenHimno.PLANIFICADOR_MANUAL, propuesta.apertura.origen)
        assertEquals(OrigenHimno.AUTOMATICA, propuesta.sacramental.origen)
        assertEquals(OrigenHimno.AUTOMATICA, propuesta.final.origen)
    }

    @Test
    fun `no repite himnos dentro de una misma reunion`() {
        val fecha = date(2026, Calendar.NOVEMBER, 15)

        val plan = generar(emptyList(), listOf(fecha)).propuestas.single().propuesta

        assertTrue(numerosDuplicados(plan).isEmpty())
    }

    @Test
    fun `fuerza al menos un himno de cuatro digitos`() {
        val fecha = date(2026, Calendar.NOVEMBER, 15)

        val plan = generar(
            agendas = emptyList(),
            fechas = listOf(fecha),
            opciones = opciones().copy(forzarHimnoNuevo = true)
        ).propuestas.single().propuesta

        assertTrue(numeros(plan).any { it.toString().length == 4 })
    }

    @Test
    fun `nunca propone himnos excluidos`() {
        val fecha = date(2026, Calendar.NOVEMBER, 15)

        val plan = generar(
            agendas = emptyList(),
            fechas = listOf(fecha),
            opciones = opciones().copy(himnosExcluidos = setOf(1, 103, 1001))
        ).propuestas.single().propuesta

        assertTrue(numeros(plan).none { it in setOf(1, 103, 1001) })
    }

    @Test
    fun `respeta el intervalo minimo cuando hay alternativas`() {
        val fechaPrevia = date(2026, Calendar.NOVEMBER, 8)
        val fechaObjetivo = date(2026, Calendar.NOVEMBER, 15)
        val agendaPrevia = Agenda(
            fecha = Timestamp(fechaPrevia),
            primerHimnoNumero = 1,
            primerHimnoNombre = catalogo.getValue(1),
            primerHimnoOrigen = OrigenHimno.AUTOMATICA
        )

        val plan = generar(
            agendas = listOf(agendaPrevia),
            fechas = listOf(fechaObjetivo),
            opciones = opciones().copy(intervaloMinimoDias = 180)
        ).propuestas.single().propuesta

        assertFalse(1 in numeros(plan))
    }

    @Test
    fun `prioriza una categoria que coincide con el tema del discurso`() {
        val fecha = date(2026, Calendar.NOVEMBER, 15)
        val agenda = Agenda(
            fecha = Timestamp(fecha),
            mensajesEvangelio = listOf(
                MensajeEvangelio(tipo = TipoMensaje.DISCURSO, etiquetaTema = "Oración")
            )
        )
        val catalogoReducido = mapOf(
            42 to Himnos.lista.getValue(42),
            48 to Himnos.lista.getValue(48),
            103 to Himnos.lista.getValue(103)
        )

        val plan = generarPlanificacionHimnos(
            catalogo = catalogoReducido,
            agendas = listOf(agenda),
            fechas = listOf(fecha),
            opciones = opciones().copy(priorizarTemas = true)
        ).propuestas.single().propuesta

        assertTrue(48 in numeros(plan))
    }

    @Test
    fun `la misma semilla produce la misma propuesta`() {
        val fechas = listOf(
            date(2026, Calendar.NOVEMBER, 15),
            date(2026, Calendar.NOVEMBER, 22)
        )

        val primera = generar(emptyList(), fechas)
        val segunda = generar(emptyList(), fechas)

        assertEquals(
            primera.propuestas.map { it.propuesta },
            segunda.propuestas.map { it.propuesta }
        )
    }

    private fun generar(
        agendas: List<Agenda>,
        fechas: List<Date>,
        opciones: OpcionesGeneracionHimnos = opciones()
    ) = generarPlanificacionHimnos(catalogo, agendas, fechas, opciones)

    private fun opciones() = OpcionesGeneracionHimnos(
        intervaloMinimoDias = 0,
        soloFechasSinPlanificacion = false,
        reemplazarAutomaticas = true,
        priorizarTemas = true,
        semilla = 12345L
    )

    private fun numeros(plan: PlanificacionHimnos): Set<Int> = setOf(
        plan.apertura.numero,
        plan.sacramental.numero,
        plan.intermedio.numero,
        plan.final.numero
    ).filterTo(mutableSetOf()) { it > 0 }

    private fun date(year: Int, month: Int, day: Int): Date = Calendar.getInstance().apply {
        clear()
        set(year, month, day, 12, 0, 0)
    }.time
}
