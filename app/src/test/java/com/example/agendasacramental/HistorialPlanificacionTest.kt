package com.example.agendasacramental

import org.junit.Test
import org.junit.Assert.*

class HistorialPlanificacionTest {
    @Test fun importedTalksAndPrayersFeedPlannerWithoutBecomingEditableAgendas() {
        val records = listOf(
            mapOf("id" to "123:import:1", "date" to "2026-01-04", "name" to "Ana", "type" to "Discurso", "topic" to "Fe"),
            mapOf("id" to "123:import:2", "date" to "2026-01-11", "name" to "Ana", "type" to "Oración"),
            mapOf("id" to "123:import:3", "date" to "2026-01-18", "name" to "José", "type" to "Última oración")
        )
        val agendas = historialParaPlanificacion("123", records)
        assertEquals(3, agendas.size)
        assertTrue(agendas.all { it.estado == EstadoAgenda.REALIZADA && it.id.startsWith("import:") })
        val ranking = calcularRankings(listOf(Hermano(nombre = "Ana")), agendas, ConfiguracionPlanificacion()).single()
        assertEquals("Fe", ranking.ultimoTemaDiscurso)
        assertEquals(agendas[0].fecha, ranking.ultimaVezDiscurso)
        assertEquals(agendas[1].fecha, ranking.ultimaVezOracion)
        assertEquals("José", agendas[2].oracionFinal)
    }
}
