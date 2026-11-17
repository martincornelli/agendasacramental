package com.example.agendasacramental

import com.google.firebase.Timestamp
import org.junit.Assert.*
import org.junit.Test
import java.util.Date

class HymnSeasonalTest {
    private val fecha = Date(1808049600000L)
    @Test fun `seasonal choices keep sacrament and new collection in both languages`() {
        for (catalogo in listOf(Himnos.lista, HimnosEn.himnos)) {
            for (modo in listOf("ONE_CHRISTMAS", "CHRISTMAS_ONLY", "EASTER_ONLY")) {
                for (nuevo in listOf(false, true)) for (semilla in listOf(0L, 1L, 7L)) {
                    val plan = generarPlanificacionHimnos(catalogo, emptyList(), listOf(fecha),
                        OpcionesGeneracionHimnos(modoTemporada = modo, forzarHimnoNuevo = nuevo, incluirIntermedio = true, semilla = semilla)).propuestas.single().propuesta
                    fun meta(s: SeleccionHimno) = metadataHimno(s.numero, s.nombre)
                    assertTrue(meta(plan.sacramental).sacramental)
                    val otros = listOf(plan.apertura, plan.intermedio, plan.final)
                    assertTrue(otros.none { meta(it).sacramental })
                    val categoria = if (modo == "EASTER_ONLY") CategoriaHimno.PASCUA else CategoriaHimno.NAVIDAD
                    val cuantos = otros.count { categoria in meta(it).categorias }
                    assertTrue(if (modo == "ONE_CHRISTMAS") cuantos >= 1 else cuantos == 3)
                    val todos = otros + plan.sacramental
                    assertEquals(4, todos.map { it.numero }.distinct().size)
                    if (nuevo) assertTrue(todos.any { meta(it).himnarioNuevo })
                }
            }
        }
    }
    @Test fun `excluded seasonal hymns do not fall back to ordinary hymns`() {
        val excluidos = Himnos.lista.filter { (n, nombre) -> CategoriaHimno.NAVIDAD in metadataHimno(n,nombre).categorias }.keys
        val result = generarPlanificacionHimnos(Himnos.lista, emptyList(), listOf(fecha),
            OpcionesGeneracionHimnos(modoTemporada = "CHRISTMAS_ONLY", himnosExcluidos = excluidos)).propuestas.single()
        assertEquals(0,result.propuesta.apertura.numero)
        assertTrue(metadataHimno(result.propuesta.sacramental.numero,result.propuesta.sacramental.nombre).sacramental)
        assertTrue(result.advertencias.any { it.codigo == CodigoAdvertenciaGeneracion.TEMPORADA_NO_CUMPLIDA })
    }
    @Test fun `manual choices survive seasonal generation and report conflict`() {
        val agenda = Agenda(fecha=Timestamp(fecha), primerHimnoNumero=1, primerHimnoNombre=Himnos.lista.getValue(1), primerHimnoOrigen=OrigenHimno.PLANIFICADOR_MANUAL)
        val result = generarPlanificacionHimnos(Himnos.lista,listOf(agenda),listOf(fecha),
            OpcionesGeneracionHimnos(modoTemporada="CHRISTMAS_ONLY",soloFechasSinPlanificacion=false)).propuestas.single()
        assertEquals(1,result.propuesta.apertura.numero)
        assertTrue(result.advertencias.any { it.codigo == CodigoAdvertenciaGeneracion.TEMPORADA_NO_CUMPLIDA })
    }
}
