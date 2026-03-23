package com.example.agendasacramental

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

// --- Unidad (Barrio) ---
data class Unidad(
    @DocumentId val id: String = "",
    val numeroUnidad: String = "",
    val passwordHash: String = "",
    val creadoPor: String = "",
    val creadoEn: Timestamp = Timestamp.now()
)

// --- Agenda ---
data class Agenda(
    @DocumentId val id: String = "",
    val numeroUnidad: String = "",
    val fecha: Timestamp = Timestamp.now(),
    val estado: EstadoAgenda = EstadoAgenda.BORRADOR,

    // Campos fijos
    val preside: String = "",
    val dirige: String = "",
    val reconocimientos: String = "",
    val anuncios: String = "",

    // Himnos
    val primerHimnoNumero: Int = 0,
    val primerHimnoNombre: String = "",
    val himnoSacramentalNumero: Int = 0,
    val himnoSacramentalNombre: String = "",
    val himnoFinalNumero: Int = 0,
    val himnoFinalNombre: String = "",

    // Oraciones
    val primeraOracion: String = "",
    val oracionFinal: String = "",

    // Tablas
    val asuntosEstacaBarrio: List<AsuntoEstacaBarrio> = emptyList(),
    val mensajesEvangelio: List<MensajeEvangelio> = emptyList(),

    // Metadata
    val creadoPor: String = "",
    val creadoEn: Timestamp = Timestamp.now(),
    val ultimaEdicionPor: String = "",
    val ultimaEdicionEn: Timestamp = Timestamp.now()
)

enum class EstadoAgenda(val label: String) {
    BORRADOR("Borrador"),
    CONFIRMADA("Confirmada"),
    REALIZADA("Realizada")
}

data class AsuntoEstacaBarrio(
    val tipo: TipoAsunto = TipoAsunto.SOSTENIMIENTO,
    val columna2: String = "",
    val columna3: String = ""
)

enum class TipoAsunto(val label: String) {
    RELEVO("Relevo"),
    SOSTENIMIENTO("Sostenimiento")
}

data class MensajeEvangelio(
    val tipo: TipoMensaje = TipoMensaje.DISCURSO,
    val nombre: String = "",       // usado si tipo es TESTIMONIO o DISCURSO
    val himnoNumero: Int = 0,      // usado si tipo es HIMNO_INTERMEDIO
    val himnoNombre: String = ""   // usado si tipo es HIMNO_INTERMEDIO
)

enum class TipoMensaje(val label: String) {
    TESTIMONIO("Testimonio"),
    DISCURSO("Discurso"),
    HIMNO_INTERMEDIO("Himno Intermedio")
}
