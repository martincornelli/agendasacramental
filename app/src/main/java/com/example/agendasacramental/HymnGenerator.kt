package com.example.agendasacramental

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.abs
import kotlin.random.Random

enum class PosicionHimno { APERTURA, SACRAMENTAL, INTERMEDIO, FINAL }

enum class CodigoAdvertenciaGeneracion {
    INTERVALO_MINIMO_RELAJADO,
    SIN_CANDIDATO,
    SIN_HIMNO_NUEVO
}

data class AdvertenciaGeneracionHimnos(
    val codigo: CodigoAdvertenciaGeneracion,
    val posicion: PosicionHimno? = null
)

data class OpcionesGeneracionHimnos(
    val intervaloMinimoDias: Int = 180,
    val soloFechasSinPlanificacion: Boolean = true,
    val reemplazarAutomaticas: Boolean = true,
    val priorizarTemas: Boolean = true,
    val forzarHimnoNuevo: Boolean = false,
    val incluirIntermedio: Boolean = false,
    val himnosExcluidos: Set<Int> = emptySet(),
    val semilla: Long = 0L
)

data class PropuestaGeneracionHimnos(
    val fecha: Date,
    val original: PlanificacionHimnos,
    val propuesta: PlanificacionHimnos,
    val temas: List<String>,
    val seleccionesManualesPreservadas: Int,
    val posicionesModificadas: Int,
    val advertencias: List<AdvertenciaGeneracionHimnos>
)

data class ResultadoGeneracionHimnos(
    val propuestas: List<PropuestaGeneracionHimnos>,
    val fechasEvaluadas: Int,
    val fechasSinCambios: Int
)

fun generarPlanificacionHimnos(
    catalogo: Map<Int, String>,
    agendas: List<Agenda>,
    fechas: List<Date>,
    opciones: OpcionesGeneracionHimnos
): ResultadoGeneracionHimnos {
    val random = Random(opciones.semilla)
    val agendasPorFecha = agendas.associateBy { claveFecha(it.fecha.toDate()) }
    val fechasOrdenadas = fechas.distinctBy(::claveFecha).sorted()
    val fechasObjetivo = fechasOrdenadas.filter { fecha ->
        val plan = planificacionDesdeAgenda(agendasPorFecha[claveFecha(fecha)])
        !opciones.soloFechasSinPlanificacion || !plan.tieneSelecciones()
    }
    val clavesObjetivo = fechasObjetivo.mapTo(mutableSetOf(), ::claveFecha)

    val usosReales = mutableMapOf<Int, MutableList<Date>>()
    val fechasOcupadas = mutableMapOf<Int, MutableList<Date>>()

    agendas.forEach { agenda ->
        val plan = planificacionDesdeAgenda(agenda)
        val selecciones = PosicionHimno.entries.map { it to plan.seleccion(it) }
        if (agenda.estado == EstadoAgenda.REALIZADA) {
            selecciones.map { it.second }.filter { it.numero > 0 }.forEach {
                usosReales.getOrPut(it.numero) { mutableListOf() } += agenda.fecha.toDate()
            }
        }

        val esObjetivo = claveFecha(agenda.fecha.toDate()) in clavesObjetivo
        selecciones.forEach { (_, seleccion) ->
            val conservar = !esObjetivo || agenda.estado == EstadoAgenda.REALIZADA ||
                    seleccion.esManual() || !opciones.reemplazarAutomaticas
            if (conservar && seleccion.numero > 0) {
                fechasOcupadas.getOrPut(seleccion.numero) { mutableListOf() } += agenda.fecha.toDate()
            }
        }
    }

    val propuestas = mutableListOf<PropuestaGeneracionHimnos>()
    var fechasSinCambios = 0

    fechasObjetivo.forEach { fecha ->
        val agenda = agendasPorFecha[claveFecha(fecha)]
        val original = planificacionDesdeAgenda(agenda)
        val temas = temasDeAgenda(agenda)
        val advertencias = mutableListOf<AdvertenciaGeneracionHimnos>()
        var propuesta = original

        val posiciones = buildList {
            add(PosicionHimno.APERTURA)
            add(PosicionHimno.SACRAMENTAL)
            if (opciones.incluirIntermedio) add(PosicionHimno.INTERMEDIO)
            add(PosicionHimno.FINAL)
        }

        posiciones.forEach { posicion ->
            val actual = propuesta.seleccion(posicion)
            val conservar = actual.numero > 0 &&
                    (actual.esManual() || !opciones.reemplazarAutomaticas)
            if (!conservar) {
                val usadosEnReunion = propuesta.numerosSeleccionados() - actual.numero
                val eleccion = elegirHimno(
                    catalogo = catalogo,
                    posicion = posicion,
                    fecha = fecha,
                    temas = temas,
                    numerosYaElegidos = usadosEnReunion,
                    fechasOcupadas = fechasOcupadas,
                    usosReales = usosReales,
                    opciones = opciones,
                    soloHimnarioNuevo = false,
                    random = random
                )
                advertencias += eleccion.advertencias
                eleccion.seleccion?.let { propuesta = propuesta.conSeleccion(posicion, it) }
            }
        }

        if (opciones.forzarHimnoNuevo && propuesta.numerosSeleccionados().none { it.toString().length == 4 }) {
            val reemplazables = listOf(
                PosicionHimno.APERTURA,
                PosicionHimno.FINAL,
                PosicionHimno.INTERMEDIO,
                PosicionHimno.SACRAMENTAL
            ).filter { posicion ->
                posicion != PosicionHimno.INTERMEDIO || opciones.incluirIntermedio ||
                        original.intermedio.numero > 0
            }.filter { posicion -> !original.seleccion(posicion).esManual() }

            var incorporado = false
            for (posicion in reemplazables) {
                val actual = propuesta.seleccion(posicion)
                val eleccion = elegirHimno(
                    catalogo = catalogo,
                    posicion = posicion,
                    fecha = fecha,
                    temas = temas,
                    numerosYaElegidos = propuesta.numerosSeleccionados() - actual.numero,
                    fechasOcupadas = fechasOcupadas,
                    usosReales = usosReales,
                    opciones = opciones,
                    soloHimnarioNuevo = true,
                    random = random
                )
                if (eleccion.seleccion != null) {
                    propuesta = propuesta.conSeleccion(posicion, eleccion.seleccion)
                    advertencias += eleccion.advertencias
                    incorporado = true
                    break
                }
            }
            if (!incorporado) {
                advertencias += AdvertenciaGeneracionHimnos(CodigoAdvertenciaGeneracion.SIN_HIMNO_NUEVO)
            }
        }

        PosicionHimno.entries.map { propuesta.seleccion(it) }
            .filter { it.numero > 0 }
            .forEach { fechasOcupadas.getOrPut(it.numero) { mutableListOf() } += fecha }

        val posicionesModificadas = PosicionHimno.entries.count {
            propuesta.seleccion(it) != original.seleccion(it)
        }
        val manualesPreservadas = PosicionHimno.entries.count {
            original.seleccion(it).esManual() && propuesta.seleccion(it) == original.seleccion(it)
        }

        if (posicionesModificadas > 0) {
            propuestas += PropuestaGeneracionHimnos(
                fecha = fecha,
                original = original,
                propuesta = propuesta,
                temas = temas,
                seleccionesManualesPreservadas = manualesPreservadas,
                posicionesModificadas = posicionesModificadas,
                advertencias = advertencias.distinct()
            )
        } else {
            fechasSinCambios++
        }
    }

    return ResultadoGeneracionHimnos(
        propuestas = propuestas,
        fechasEvaluadas = fechasObjetivo.size,
        fechasSinCambios = fechasSinCambios
    )
}

private data class EleccionHimno(
    val seleccion: SeleccionHimno?,
    val advertencias: List<AdvertenciaGeneracionHimnos>
)

private fun elegirHimno(
    catalogo: Map<Int, String>,
    posicion: PosicionHimno,
    fecha: Date,
    temas: List<String>,
    numerosYaElegidos: Set<Int>,
    fechasOcupadas: Map<Int, List<Date>>,
    usosReales: Map<Int, List<Date>>,
    opciones: OpcionesGeneracionHimnos,
    soloHimnarioNuevo: Boolean,
    random: Random
): EleccionHimno {
    val candidatos = catalogo.entries.asSequence()
        .filter { it.key !in opciones.himnosExcluidos }
        .filter { it.key !in numerosYaElegidos }
        .filter { !soloHimnarioNuevo || it.key.toString().length == 4 }
        .filter {
            val metadata = metadataHimno(it.key, it.value)
            if (posicion == PosicionHimno.SACRAMENTAL) metadata.sacramental else !metadata.sacramental
        }
        .sortedBy { it.key }
        .toList()

    if (candidatos.isEmpty()) {
        return EleccionHimno(
            null,
            listOf(AdvertenciaGeneracionHimnos(CodigoAdvertenciaGeneracion.SIN_CANDIDATO, posicion))
        )
    }

    val intervalo = opciones.intervaloMinimoDias.coerceAtLeast(0)
    val candidatosConIntervalo = candidatos.filter { candidato ->
        fechasOcupadas[candidato.key].orEmpty().all { otraFecha ->
            diferenciaDias(fecha, otraFecha) >= intervalo
        }
    }
    val relajaIntervalo = candidatosConIntervalo.isEmpty()
    val disponibles = if (relajaIntervalo) candidatos else candidatosConIntervalo

    val elegido = disponibles.maxByOrNull { (numero, nombre) ->
        val usos = usosReales[numero].orEmpty()
        val ultimaAnterior = usos.filter { !it.after(fecha) }.maxOrNull()
        val puntosHistorial = if (usos.isEmpty()) {
            1_000.0
        } else {
            (ultimaAnterior?.let { diferenciaDias(fecha, it).coerceAtMost(730) } ?: 365).toDouble() -
                    usos.size * 35.0
        }
        val distanciaPlanificada = fechasOcupadas[numero].orEmpty()
            .minOfOrNull { diferenciaDias(fecha, it) }
            ?.coerceAtMost(365)
            ?.times(0.5) ?: 180.0
        val puntosTema = if (opciones.priorizarTemas) {
            coincidenciasTematicas(metadataHimno(numero, nombre), temas) * 350.0
        } else 0.0
        val bonoNuevo = if (numero.toString().length == 4) 12.0 else 0.0
        puntosHistorial + distanciaPlanificada + puntosTema + bonoNuevo + random.nextDouble(0.0, 10.0)
    }

    return EleccionHimno(
        seleccion = elegido?.let {
            SeleccionHimno(it.key, it.value, OrigenHimno.AUTOMATICA)
        },
        advertencias = if (relajaIntervalo) {
            listOf(AdvertenciaGeneracionHimnos(CodigoAdvertenciaGeneracion.INTERVALO_MINIMO_RELAJADO, posicion))
        } else emptyList()
    )
}

private fun PlanificacionHimnos.tieneSelecciones(): Boolean =
    PosicionHimno.entries.any { seleccion(it).numero > 0 || seleccion(it).nombre.isNotBlank() }

private fun PlanificacionHimnos.numerosSeleccionados(): Set<Int> =
    PosicionHimno.entries.map { seleccion(it).numero }.filterTo(mutableSetOf()) { it > 0 }

private fun PlanificacionHimnos.seleccion(posicion: PosicionHimno): SeleccionHimno = when (posicion) {
    PosicionHimno.APERTURA -> apertura
    PosicionHimno.SACRAMENTAL -> sacramental
    PosicionHimno.INTERMEDIO -> intermedio
    PosicionHimno.FINAL -> final
}

private fun PlanificacionHimnos.conSeleccion(
    posicion: PosicionHimno,
    seleccion: SeleccionHimno
): PlanificacionHimnos = when (posicion) {
    PosicionHimno.APERTURA -> copy(apertura = seleccion)
    PosicionHimno.SACRAMENTAL -> copy(sacramental = seleccion)
    PosicionHimno.INTERMEDIO -> copy(intermedio = seleccion)
    PosicionHimno.FINAL -> copy(final = seleccion)
}

private fun SeleccionHimno.esManual(): Boolean =
    (numero > 0 || nombre.isNotBlank()) && origen != OrigenHimno.AUTOMATICA

private fun diferenciaDias(una: Date, otra: Date): Int =
    abs(diaUtc(una) - diaUtc(otra)).toInt()

private fun diaUtc(fecha: Date): Long {
    val local = Calendar.getInstance().apply { time = fecha }
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(
            local.get(Calendar.YEAR),
            local.get(Calendar.MONTH),
            local.get(Calendar.DAY_OF_MONTH),
            0,
            0,
            0
        )
    }
    return utc.timeInMillis / 86_400_000L
}
