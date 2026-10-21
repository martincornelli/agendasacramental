package com.example.agendasacramental

import com.google.firebase.Timestamp
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ResumenUsoHimno(
    val numero: Int,
    val nombre: String,
    val usosEnAnio: Int = 0,
    val usosTotales: Int = 0,
    val ultimaUtilizacion: Timestamp? = null,
    val proximaPlanificacion: Timestamp? = null
)

fun domingosDelAnio(anio: Int): List<Date> {
    val calendar = Calendar.getInstance().apply {
        clear()
        set(anio, Calendar.JANUARY, 1, 12, 0, 0)
        while (get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            add(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val domingos = mutableListOf<Date>()
    while (calendar.get(Calendar.YEAR) == anio) {
        domingos += calendar.time
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
    }
    return domingos
}

fun claveFecha(fecha: Date): String {
    val calendar = Calendar.getInstance().apply { time = fecha }
    return String.format(Locale.ROOT, "%04d-%02d-%02d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )
}

fun planificacionDesdeAgenda(agenda: Agenda?): PlanificacionHimnos {
    if (agenda == null) return PlanificacionHimnos()
    val intermedio = agenda.mensajesEvangelio.firstOrNull { it.tipo == TipoMensaje.HIMNO_INTERMEDIO }
    return PlanificacionHimnos(
        apertura = SeleccionHimno(
            agenda.primerHimnoNumero,
            agenda.primerHimnoNombre,
            agenda.primerHimnoOrigen.ifBlank {
                if (agenda.primerHimnoNumero > 0 || agenda.primerHimnoNombre.isNotBlank()) OrigenHimno.AGENDA_MANUAL else ""
            }
        ),
        sacramental = SeleccionHimno(
            agenda.himnoSacramentalNumero,
            agenda.himnoSacramentalNombre,
            agenda.himnoSacramentalOrigen.ifBlank {
                if (agenda.himnoSacramentalNumero > 0 || agenda.himnoSacramentalNombre.isNotBlank()) OrigenHimno.AGENDA_MANUAL else ""
            }
        ),
        intermedio = SeleccionHimno(
            intermedio?.himnoNumero ?: 0,
            intermedio?.himnoNombre.orEmpty(),
            agenda.himnoIntermedioOrigen.ifBlank {
                if (intermedio != null) OrigenHimno.AGENDA_MANUAL else ""
            }
        ),
        final = SeleccionHimno(
            agenda.himnoFinalNumero,
            agenda.himnoFinalNombre,
            agenda.himnoFinalOrigen.ifBlank {
                if (agenda.himnoFinalNumero > 0 || agenda.himnoFinalNombre.isNotBlank()) OrigenHimno.AGENDA_MANUAL else ""
            }
        )
    )
}

fun numerosDuplicados(planificacion: PlanificacionHimnos): Set<Int> =
    listOf(
        planificacion.apertura.numero,
        planificacion.sacramental.numero,
        planificacion.intermedio.numero,
        planificacion.final.numero
    )
        .filter { it > 0 }
        .groupingBy { it }
        .eachCount()
        .filterValues { it > 1 }
        .keys

val posicionesHimnoEnOrden = listOf(
    PosicionHimno.APERTURA,
    PosicionHimno.SACRAMENTAL,
    PosicionHimno.INTERMEDIO,
    PosicionHimno.FINAL
)

fun posicionesAsignables(metadata: MetadataHimno): List<PosicionHimno> =
    posicionesHimnoEnOrden.filter { it != PosicionHimno.SACRAMENTAL || metadata.sacramental }

fun PlanificacionHimnos.seleccionEn(posicion: PosicionHimno): SeleccionHimno = when (posicion) {
    PosicionHimno.APERTURA -> apertura
    PosicionHimno.SACRAMENTAL -> sacramental
    PosicionHimno.INTERMEDIO -> intermedio
    PosicionHimno.FINAL -> final
}

fun PlanificacionHimnos.conSeleccionEn(
    posicion: PosicionHimno,
    seleccion: SeleccionHimno
): PlanificacionHimnos = when (posicion) {
    PosicionHimno.APERTURA -> copy(apertura = seleccion)
    PosicionHimno.SACRAMENTAL -> copy(sacramental = seleccion)
    PosicionHimno.INTERMEDIO -> copy(intermedio = seleccion)
    PosicionHimno.FINAL -> copy(final = seleccion)
}

fun calcularHistorialHimnos(
    catalogo: Map<Int, String>,
    agendas: List<Agenda>,
    anio: Int,
    ahora: Date = Date()
): List<ResumenUsoHimno> {
    data class Acumulador(
        var nombre: String,
        var usosEnAnio: Int = 0,
        var usosTotales: Int = 0,
        var ultima: Timestamp? = null,
        var proxima: Timestamp? = null
    )

    val acumuladores = catalogo.mapValues { Acumulador(it.value) }.toMutableMap()
    agendas.forEach { agenda ->
        val calendar = Calendar.getInstance().apply { time = agenda.fecha.toDate() }
        val esUsoReal = agenda.estado == EstadoAgenda.REALIZADA
        val inicioDeHoy = Calendar.getInstance().apply {
            time = ahora
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        val esPlanFuturo = !esUsoReal && !agenda.fecha.toDate().before(inicioDeHoy)
        val selecciones = planificacionDesdeAgenda(agenda).let {
            listOf(it.apertura, it.sacramental, it.intermedio, it.final)
        }
        selecciones.filter { it.numero > 0 }.forEach { seleccion ->
            val acumulador = acumuladores.getOrPut(seleccion.numero) {
                Acumulador(seleccion.nombre.ifBlank { seleccion.numero.toString() })
            }
            if (acumulador.nombre.isBlank() && seleccion.nombre.isNotBlank()) acumulador.nombre = seleccion.nombre
            if (esUsoReal) {
                acumulador.usosTotales++
                if (calendar.get(Calendar.YEAR) == anio) acumulador.usosEnAnio++
                if (acumulador.ultima == null || agenda.fecha.seconds > acumulador.ultima!!.seconds) {
                    acumulador.ultima = agenda.fecha
                }
            } else if (esPlanFuturo &&
                (acumulador.proxima == null || agenda.fecha.seconds < acumulador.proxima!!.seconds)
            ) {
                acumulador.proxima = agenda.fecha
            }
        }
    }

    return acumuladores.map { (numero, acumulador) ->
        ResumenUsoHimno(
            numero = numero,
            nombre = acumulador.nombre,
            usosEnAnio = acumulador.usosEnAnio,
            usosTotales = acumulador.usosTotales,
            ultimaUtilizacion = acumulador.ultima,
            proximaPlanificacion = acumulador.proxima
        )
    }.sortedBy { it.numero }
}
