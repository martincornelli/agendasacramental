package com.example.agendasacramental

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

private val spanishTopicLabels = mapOf(
    "Easter" to "Pascua", "God's love" to "Amor de Dios", "New Testament" to "Nuevo Testamento",
    "blessings" to "Bendiciones", "commandments" to "Mandamientos", "family of God" to "Familia de Dios",
    "peace" to "Paz", "prayer" to "Oración", "repentance" to "Arrepentimiento"
)

fun idiomaHimno(numero: Int, nombre: String): String =
    if (HimnosEn.himnos[numero]?.let { normalizarNombre(it) == normalizarNombre(nombre) } == true) "en" else "es"

fun claveTemasHimno(numero: Int, idioma: String) = "${if (idioma.startsWith("en")) "en" else "es"}-$numero"

fun temasOficialesHimno(numero: Int, idioma: String): List<String> =
    if (idioma.startsWith("en")) OfficialHymnTopics.en[numero].orEmpty()
    else OfficialHymnTopics.es[numero].orEmpty().map { spanishTopicLabels[it] ?: it }

fun temasEfectivosHimno(numero: Int, idioma: String, cambios: Map<String, List<String>> = emptyMap()): List<String> =
    cambios[claveTemasHimno(numero, idioma)] ?: temasOficialesHimno(numero, idioma)

fun parsearTemasHimno(texto: String): List<String> = texto.lines().map { it.trim() }.filter { it.isNotEmpty() }.distinct().also {
    require(it.size <= 30 && it.all { tema -> tema.length <= 80 })
}

class UnitHymnTopicsRepository {
    fun collection(unidad: String) = FirebaseFirestore.getInstance()
        .collection("temasHimnosUnidad").document(unidad).collection("himnos")

    suspend fun save(unidad: String, numero: Int, idioma: String, temas: List<String>?) {
        require(unidad.isNotBlank() && numero > 0 && idioma in listOf("es", "en"))
        val ref = collection(unidad).document(claveTemasHimno(numero, idioma))
        if (temas == null) ref.delete().await()
        else {
            require(temas.size <= 30 && temas.all { it.isNotBlank() && it.length <= 80 })
            ref.set(mapOf("numeroUnidad" to unidad, "numero" to numero, "idioma" to idioma,
                "temas" to temas, "actualizadoEn" to FieldValue.serverTimestamp())).await()
        }
    }
}
