package com.example.agendasacramental

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Date

class HymnPlannerDomainTest {

    @Test
    fun `genera solamente domingos dentro del anio`() {
        val domingos = domingosDelAnio(2026)

        assertEquals(52, domingos.size)
        assertTrue(domingos.all { fecha ->
            Calendar.getInstance().apply { time = fecha }.let {
                it.get(Calendar.YEAR) == 2026 && it.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
            }
        })
        assertEquals("2026-01-04", claveFecha(domingos.first()))
        assertEquals("2026-12-27", claveFecha(domingos.last()))
    }

    @Test
    fun `detecta himnos repetidos dentro de la reunion`() {
        val plan = PlanificacionHimnos(
            apertura = SeleccionHimno(10, "Himno 10"),
            sacramental = SeleccionHimno(20, "Himno 20"),
            intermedio = SeleccionHimno(10, "Himno 10"),
            final = SeleccionHimno(30, "Himno 30")
        )

        assertEquals(setOf(10), numerosDuplicados(plan))
    }

    @Test
    fun `solo ofrece la posicion sacramental para himnos aptos`() {
        val posicionesNoSacramentales = posicionesAsignables(metadataHimno(10, Himnos.lista.getValue(10)))
        val posicionesSacramentales = posicionesAsignables(metadataHimno(101, Himnos.lista.getValue(101)))

        assertFalse(PosicionHimno.SACRAMENTAL in posicionesNoSacramentales)
        assertTrue(PosicionHimno.SACRAMENTAL in posicionesSacramentales)
    }

    @Test
    fun `el himno 101 espanol es sacramental pero el 101 ingles no`() {
        val espanol = metadataHimno(101, Himnos.lista.getValue(101))
        val ingles = metadataHimno(101, HimnosEn.himnos.getValue(101))

        assertTrue(espanol.sacramental)
        assertTrue(CategoriaHimno.SACRAMENTO in espanol.categorias)
        assertFalse(ingles.sacramental)
    }

    @Test
    fun `asigna un himno manual a una posicion sin alterar las demas`() {
        val original = PlanificacionHimnos(
            apertura = SeleccionHimno(10, "Apertura"),
            final = SeleccionHimno(20, "Final")
        )
        val manual = SeleccionHimno(101, "Dios, escúchanos orar", OrigenHimno.PLANIFICADOR_MANUAL)

        val actualizado = original.conSeleccionEn(PosicionHimno.SACRAMENTAL, manual)

        assertEquals(original.apertura, actualizado.apertura)
        assertEquals(manual, actualizado.seleccionEn(PosicionHimno.SACRAMENTAL))
        assertEquals(original.final, actualizado.final)
    }

    @Test
    fun `historial separa usos realizados de planificaciones futuras`() {
        val realizada = Agenda(
            fecha = timestamp(2026, Calendar.MARCH, 1),
            estado = EstadoAgenda.REALIZADA,
            primerHimnoNumero = 10,
            primerHimnoNombre = "Himno 10"
        )
        val futura = Agenda(
            fecha = timestamp(2026, Calendar.NOVEMBER, 1),
            estado = EstadoAgenda.BORRADOR,
            primerHimnoNumero = 10,
            primerHimnoNombre = "Himno 10",
            himnoFinalNumero = 20,
            himnoFinalNombre = "Himno 20"
        )

        val resumen = calcularHistorialHimnos(
            catalogo = mapOf(10 to "Himno 10", 20 to "Himno 20"),
            agendas = listOf(realizada, futura),
            anio = 2026,
            ahora = date(2026, Calendar.JUNE, 1)
        ).associateBy { it.numero }

        assertEquals(1, resumen.getValue(10).usosEnAnio)
        assertEquals(1, resumen.getValue(10).usosTotales)
        assertEquals(claveFecha(futura.fecha.toDate()), claveFecha(resumen.getValue(10).proximaPlanificacion!!.toDate()))
        assertEquals(0, resumen.getValue(20).usosTotales)
        assertNull(resumen.getValue(20).ultimaUtilizacion)
        assertTrue(resumen.getValue(20).proximaPlanificacion != null)
    }

    private fun timestamp(year: Int, month: Int, day: Int) = Timestamp(date(year, month, day))

    private fun date(year: Int, month: Int, day: Int): Date = Calendar.getInstance().apply {
        clear()
        set(year, month, day, 12, 0, 0)
    }.time
}
