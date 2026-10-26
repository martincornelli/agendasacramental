package com.example.agendasacramental

import org.junit.Assert.assertEquals
import org.junit.Test

class AnunciosTest {
    @Test fun commasRemainInsideAnnouncements() {
        assertEquals(listOf("El sábado, a las 18:00, habrá una actividad", "Traer abrigo, agua y comida"),
            separarAnuncios("El sábado, a las 18:00, habrá una actividad\r\n\r\nTraer abrigo, agua y comida"))
    }
    @Test fun roundTripPreservesSpanishAndEnglishPunctuation() {
        val items = listOf("Sábado, 18:00: reunión", "Saturday, 6 pm: meeting")
        assertEquals(items, separarAnuncios(items.joinToString("\n")))
        assertEquals(listOf("Older announcement, with a comma"), separarAnuncios("Older announcement, with a comma"))
    }
}
