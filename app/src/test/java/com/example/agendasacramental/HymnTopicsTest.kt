package com.example.agendasacramental

import org.junit.Assert.*
import org.junit.Test

class HymnTopicsTest {
    @Test fun `every shipped hymn has official metadata`() {
        assertTrue(OfficialHymnTopics.es.keys.containsAll(Himnos.lista.keys))
        assertTrue(OfficialHymnTopics.en.keys.containsAll(HimnosEn.himnos.keys))
        assertEquals(299, OfficialHymnTopics.es.size)
        assertEquals(431, OfficialHymnTopics.en.size)
        assertTrue(temasOficialesHimno(1, "en").contains("gathering of Israel"))
    }

    @Test fun `overrides are explicit scoped inputs and do not alter defaults or sacrament suitability`() {
        val cambios = mapOf("es-1" to listOf("Tema local"), "es-101" to emptyList())
        assertEquals(listOf("Tema local"), temasEfectivosHimno(1, "es", cambios))
        assertEquals(temasOficialesHimno(1, "en"), temasEfectivosHimno(1, "en", cambios))
        assertEquals(temasOficialesHimno(1, "es"), temasEfectivosHimno(1, "es"))
        assertTrue(metadataHimno(101, Himnos.lista.getValue(101), cambios).sacramental)
        assertTrue(metadataHimno(101, Himnos.lista.getValue(101), cambios).temas.isEmpty())
        assertEquals(1, coincidenciasTematicas(metadataHimno(1, Himnos.lista.getValue(1), cambios), listOf("Tema local")))
    }

    @Test fun `missing published topics remain empty and validation rejects oversized edits`() {
        OfficialHymnTopics.es.filterValues { it.isEmpty() }.keys.forEach {
            assertTrue(temasEfectivosHimno(it, "es").isEmpty())
        }
        assertEquals(listOf("Fe", "Esperanza"), parsearTemasHimno(" Fe \n\nFe\nEsperanza "))
        assertTrue(runCatching { parsearTemasHimno("a".repeat(81)) }.isFailure)
        assertTrue(runCatching { parsearTemasHimno((1..31).joinToString("\n")) }.isFailure)
    }
}
