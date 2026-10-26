package com.example.agendasacramental

// Commas are punctuation, never separators. Legacy strings remain intact.
fun separarAnuncios(texto: String): List<String> = texto.replace("\r\n", "\n")
    .replace('\r', '\n').split('\n').map { it.trim() }.filter { it.isNotEmpty() }
