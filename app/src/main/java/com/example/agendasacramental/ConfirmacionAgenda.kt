package com.example.agendasacramental

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

data class AceptacionAsignacion(val firma: String = "", val registradoPor: String = "", val registradoEn: Timestamp? = null)
data class ComprobacionAsignacion(val id: String, val label: Int, val persona: String, val detalle: String, val completa: Boolean, val firma: String)

val organizaciones = linkedMapOf(
    "ELDERS_QUORUM" to R.string.org_elders, "RELIEF_SOCIETY" to R.string.org_relief,
    "YOUNG_MEN" to R.string.org_young_men, "YOUNG_WOMEN" to R.string.org_young_women,
    "PRIMARY" to R.string.org_primary, "BISHOPRIC" to R.string.org_bishopric
)

fun comprobacionesAgenda(agenda: Agenda): List<ComprobacionAsignacion> {
    val dia = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(agenda.fecha.toDate())
    val modo = if (agenda.presentacionPrimaria) "PRIMARY" else if (agenda.reunionTestimonios) "TESTIMONY" else "NORMAL"
    val rows = mutableListOf<ComprobacionAsignacion>()
    fun add(id: String, label: Int, persona: String, contenido: List<String> = emptyList(), completa: Boolean = true, detalle: String = "") {
        val firma = (listOf(dia, modo, id, persona) + contenido).joinToString("") { "${it.trim().length}:${it.trim()}" }
        rows += ComprobacionAsignacion(id, label, persona.trim(), detalle, persona.isNotBlank() && completa, firma)
    }
    add("preside", R.string.check_presides, agenda.preside)
    add("dirige", R.string.check_conducts, agenda.dirige)
    add("primeraOracion", R.string.check_opening_prayer, agenda.primeraOracion)
    add("oracionFinal", R.string.check_closing_prayer, agenda.oracionFinal)
    fun himno(id: String, label: Int, numero: Int, nombre: String) {
        val detalle = formatearHimno(numero, nombre)
        add("${id}_pianista", label, agenda.pianista, listOf(numero.toString(), nombre), numero > 0 || nombre.isNotBlank(), "pianista|$detalle")
        add("${id}_directorMusica", label, agenda.directorMusica, listOf(numero.toString(), nombre), numero > 0 || nombre.isNotBlank(), "directorMusica|$detalle")
    }
    himno("primerHimno", R.string.check_opening_hymn, agenda.primerHimnoNumero, agenda.primerHimnoNombre)
    himno("himnoSacramental", R.string.check_sacrament_hymn, agenda.himnoSacramentalNumero, agenda.himnoSacramentalNombre)
    himno("himnoFinal", R.string.check_closing_hymn, agenda.himnoFinalNumero, agenda.himnoFinalNombre)
    if (agenda.presentacionPrimaria) add("responsablePrimaria", R.string.check_primary_leader, agenda.responsablePrimaria)
    else if (!agenda.reunionTestimonios) {
        if (agenda.mensajesEvangelio.none { it.tipo != TipoMensaje.HIMNO_INTERMEDIO }) add("missingTalk", R.string.check_talk, "", completa = false)
        agenda.mensajesEvangelio.forEachIndexed { i, m ->
            val id = m.id.ifBlank { "legacy-$i" }
            if (m.tipo == TipoMensaje.HIMNO_INTERMEDIO) himno("mensaje_$id", R.string.check_intermediate_hymn, m.himnoNumero, m.himnoNombre)
            else add("mensaje_$id", if (m.tipo == TipoMensaje.TESTIMONIO) R.string.check_testimony else R.string.check_talk, m.nombre, listOf(m.tipo.name, m.tema), detalle = m.tema)
        }
    }
    return rows
}

fun cambiarEstadoAgenda(agenda: Agenda, estado: EstadoAgenda, usuario: String, ahora: Timestamp): Agenda {
    val actual = reconciliarConfirmacion(agenda.copy(confirmacionVersion = 1))
    return when (estado) {
        EstadoAgenda.REALIZADA -> actual.copy(estado = estado)
        EstadoAgenda.BORRADOR -> reconciliarConfirmacion(actual.copy(estado = estado))
        EstadoAgenda.CONFIRMADA -> {
            val checks = comprobacionesAgenda(actual)
            require(checks.isNotEmpty() && checks.all { it.completa }) { "ASSIGNMENTS_INCOMPLETE" }
            actual.copy(estado = estado, aceptaciones = checks.associate { row ->
                row.id to (actual.aceptaciones[row.id] ?: AceptacionAsignacion(row.firma, usuario, ahora))
            })
        }
    }
}

fun reconciliarConfirmacion(agenda: Agenda): Agenda {
    val next = agenda.copy(mensajesEvangelio = agenda.mensajesEvangelio.mapIndexed { i, m -> m.copy(id = m.id.ifBlank { "legacy-$i" }) })
    if (next.confirmacionVersion == 0) return next
    val checks = comprobacionesAgenda(next)
    val aceptaciones = next.aceptaciones.filter { (id, a) -> checks.any { it.id == id && it.completa && it.firma == a.firma } }
    val estado = if (next.estado == EstadoAgenda.REALIZADA) next.estado else if (checks.isNotEmpty() && checks.all { it.id in aceptaciones }) EstadoAgenda.CONFIRMADA else EstadoAgenda.BORRADOR
    return next.copy(estado = estado, aceptaciones = aceptaciones)
}
