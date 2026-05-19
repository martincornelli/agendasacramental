package com.example.agendasacramental

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import java.text.Normalizer
import java.util.Calendar
import java.util.Date

data class Unidad(
    @DocumentId val id: String = "",
    val numeroUnidad: String = "",
    val passwordHash: String = "",
    val creadoPor: String = "",
    val creadoEn: Timestamp = Timestamp.now()
)

data class Agenda(
    @DocumentId val id: String = "",
    val numeroUnidad: String = "",
    val fecha: Timestamp = Timestamp.now(),
    val estado: EstadoAgenda = EstadoAgenda.BORRADOR,
    val asistencia: Int = 0,
    val preside: String = "",
    val dirige: String = "",
    val reconocimientos: String = "",
    val anuncios: String = "",
    val primerHimnoNumero: Int = 0,
    val primerHimnoNombre: String = "",
    val directorMusica: String = "",
    val pianista: String = "",
    val himnoSacramentalNumero: Int = 0,
    val himnoSacramentalNombre: String = "",
    val himnoFinalNumero: Int = 0,
    val himnoFinalNombre: String = "",
    val primeraOracion: String = "",
    val oracionFinal: String = "",
    val asuntosEstacaBarrio: List<AsuntoEstacaBarrio> = emptyList(),
    val mensajesEvangelio: List<MensajeEvangelio> = emptyList(),
    val reunionTestimonios: Boolean = false,
    val testimonios: List<String> = emptyList(),
    val creadoPor: String = "",
    val creadoEn: Timestamp = Timestamp.now(),
    val ultimaEdicionPor: String = "",
    val ultimaEdicionEn: Timestamp = Timestamp.now()
)

enum class EstadoAgenda(val label: String, val stringResId: Int) {
    BORRADOR("Borrador", R.string.estado_borrador),
    CONFIRMADA("Confirmada", R.string.estado_confirmada),
    REALIZADA("Realizada", R.string.estado_realizada)
}

data class AsuntoEstacaBarrio(
    val tipo: TipoAsunto = TipoAsunto.SOSTENIMIENTO,
    val columna2: String = "",
    val columna3: String = ""
)

enum class TipoAsunto(val label: String, val stringResId: Int) {
    RELEVO("Relevo", R.string.tipo_relevo),
    SOSTENIMIENTO("Sostenimiento", R.string.tipo_sostenimiento),
    OTROS("Otros", R.string.tipo_otros)
}

data class MensajeEvangelio(
    val tipo: TipoMensaje = TipoMensaje.DISCURSO,
    val nombre: String = "",
    val himnoNumero: Int = 0,
    val himnoNombre: String = ""
)

enum class TipoMensaje(val label: String, val stringResId: Int) {
    TESTIMONIO("Testimonio", R.string.lectura_testimonio),
    DISCURSO("Discurso", R.string.lectura_discurso),
    HIMNO_INTERMEDIO("Himno Intermedio", R.string.solicitud_intermedio)
}

data class Hermano(
    @DocumentId val id: String = "",
    val numeroUnidad: String = "",
    val nombre: String = "",
    val agregadoManualmente: Boolean = false,
    val inactivoDiscurso: Boolean = false,
    val inactivoOracion: Boolean = false,
    val ultimaVezDiscursoManual: Timestamp? = null,
    val ultimaVezOracionManual: Timestamp? = null,
    val creadoEn: Timestamp = Timestamp.now()
)

data class HermanoRanking(
    val hermano: Hermano,
    val ultimaVezDiscurso: Timestamp? = null,
    val ultimaVezOracion: Timestamp? = null,
    val vecesDiscurso90Dias: Int = 0,
    val vecesOracion90Dias: Int = 0
)

enum class ColorRanking(val label: String, val stringResId: Int) {
    VERDE("Sugerido", R.string.color_sugerido),
    AMARILLO("Posible", R.string.color_posible),
    ROJO("Reciente", R.string.color_reciente)
}

data class ConfiguracionPlanificacion(
    @DocumentId val id: String = "",
    val numeroUnidad: String = "",
    val diasVerdeDiscurso: Int = 90,
    val diasAmarilloDiscurso: Int = 30,
    val diasVerdeOracion: Int = 30,
    val diasAmarilloOracion: Int = 14
)

// Normalizar nombre: quita tildes y pone en minúsculas para comparar
fun normalizarNombre(nombre: String): String {
    val normalized = Normalizer.normalize(nombre.trim(), Normalizer.Form.NFD)
    return normalized.replace(Regex("[\\p{InCombiningDiacriticalMarks}]"), "").lowercase()
}

fun esPrimerDomingoDelMes(fecha: Date): Boolean {
    val calendar = Calendar.getInstance().apply { time = fecha }
    return calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY &&
            calendar.get(Calendar.DAY_OF_MONTH) in 1..7
}
