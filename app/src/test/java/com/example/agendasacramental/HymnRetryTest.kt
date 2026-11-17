package com.example.agendasacramental

import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class HymnRetryTest {
    private val fecha = Date(1808049600000L)
    @Test fun `retries differ without changing manual seasonal or sacrament requirements`() {
        for (catalogo in listOf(Himnos.lista, HimnosEn.himnos)) {
            for (modo in listOf("NONE", "ONE_CHRISTMAS", "CHRISTMAS_ONLY", "EASTER_ONLY")) {
                val categoria = if (modo == "EASTER_ONLY") CategoriaHimno.PASCUA else CategoriaHimno.NAVIDAD
                val manual = catalogo.entries.first { (n,nombre) -> metadataHimno(n,nombre).let { !it.sacramental && categoria in it.categorias } }
                val agenda = Agenda(fecha=Timestamp(fecha),primerHimnoNumero=manual.key,primerHimnoNombre=manual.value,primerHimnoOrigen=OrigenHimno.PLANIFICADOR_MANUAL)
                val opciones = OpcionesGeneracionHimnos(modoTemporada=modo,soloFechasSinPlanificacion=false,forzarHimnoNuevo=true,incluirIntermedio=true,himnosExcluidos=setOf(2))
                var resultado = generarPlanificacionHimnos(catalogo,listOf(agenda),listOf(fecha),opciones)
                repeat(4) { intento ->
                    val anterior = resultado.propuestas.single().propuesta
                    resultado = generarPlanificacionHimnos(catalogo,listOf(agenda),listOf(fecha),opciones.copy(semilla=intento.toLong()+1,combinacionAnterior=mapOf(claveFecha(fecha) to anterior)))
                    val siguiente = resultado.propuestas.single().propuesta
                    assertNotEquals(anterior,siguiente)
                    assertEquals(anterior.apertura,siguiente.apertura)
                    assertTrue(metadataHimno(siguiente.sacramental.numero,siguiente.sacramental.nombre).sacramental)
                    val otros = listOf(siguiente.apertura,siguiente.intermedio,siguiente.final)
                    val numeros = (otros + siguiente.sacramental).map { it.numero }
                    assertEquals(4,numeros.distinct().size)
                    assertTrue(numeros.any { it >= 1000 })
                    assertFalse(2 in numeros)
                    if (modo.endsWith("_ONLY")) assertTrue(otros.all { categoria in metadataHimno(it.numero,it.nombre).categorias })
                }
                assertEquals(manual.key,agenda.primerHimnoNumero)
            }
        }
    }
    @Test fun `no alternative retains the only valid suggestions`() {
        val catalogo = Himnos.lista.filterKeys { it in setOf(1,2,101) }
        val agenda = Agenda(fecha=Timestamp(fecha),primerHimnoNumero=1,primerHimnoNombre=catalogo.getValue(1),primerHimnoOrigen=OrigenHimno.PLANIFICADOR_MANUAL)
        val opciones = OpcionesGeneracionHimnos(soloFechasSinPlanificacion=false)
        val primero = generarPlanificacionHimnos(catalogo,listOf(agenda),listOf(fecha),opciones)
        val otro = generarPlanificacionHimnos(catalogo,listOf(agenda),listOf(fecha),opciones.copy(semilla=99,combinacionAnterior=mapOf(claveFecha(fecha) to primero.propuestas.single().propuesta)))
        assertEquals(primero,otro)
    }
    @Test fun `retry respects interval even when only the previous choices qualify`() {
        val catalogo = Himnos.lista.filterKeys { it in setOf(1,2,3,101,102) }
        val agendas = listOf(
            Agenda(fecha=Timestamp(fecha),primerHimnoNumero=1,primerHimnoNombre=catalogo.getValue(1),primerHimnoOrigen=OrigenHimno.PLANIFICADOR_MANUAL),
            Agenda(fecha=Timestamp(Date(fecha.time-7*86400000L)),estado=EstadoAgenda.REALIZADA,primerHimnoNumero=3,primerHimnoNombre=catalogo.getValue(3),himnoSacramentalNumero=102,himnoSacramentalNombre=catalogo.getValue(102))
        )
        val opciones = OpcionesGeneracionHimnos(soloFechasSinPlanificacion=false,intervaloMinimoDias=180)
        val primero = generarPlanificacionHimnos(catalogo,agendas,listOf(fecha),opciones)
        val otro = generarPlanificacionHimnos(catalogo,agendas,listOf(fecha),opciones.copy(semilla=99,combinacionAnterior=mapOf(claveFecha(fecha) to primero.propuestas.single().propuesta)))
        assertEquals(primero,otro)
        assertTrue(otro.propuestas.single().advertencias.none { it.codigo == CodigoAdvertenciaGeneracion.INTERVALO_MINIMO_RELAJADO })
    }
}
