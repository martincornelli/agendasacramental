package com.example.agendasacramental

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.Calendar
import java.util.Date

class AgendaRepository {
    private val db = FirebaseFirestore.getInstance()
    private val unidadesRef = db.collection("unidades")
    private val agendasRef = db.collection("agendas")
    private val hermanosRef = db.collection("hermanos")

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun existeUnidad(numeroUnidad: String): Boolean {
        val result = unidadesRef.document(numeroUnidad).get().await()
        return result.exists()
    }

    suspend fun crearUnidad(numeroUnidad: String, password: String, creadoPor: String): Result<Unit> {
        return try {
            val unidad = mapOf(
                "numeroUnidad" to numeroUnidad,
                "passwordHash" to hashPassword(password),
                "creadoPor" to creadoPor,
                "creadoEn" to Timestamp.now()
            )
            unidadesRef.document(numeroUnidad).set(unidad).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verificarPassword(numeroUnidad: String, password: String): Boolean {
        return try {
            val doc = unidadesRef.document(numeroUnidad).get().await()
            val storedHash = doc.getString("passwordHash") ?: return false
            storedHash == hashPassword(password)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAgendas(numeroUnidad: String): Result<List<Agenda>> {
        return try {
            val snapshot = agendasRef
                .whereEqualTo("numeroUnidad", numeroUnidad)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get().await()
            val agendas = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Agenda::class.java)?.copy(id = doc.id)
            }
            Result.success(agendas)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAgenda(agendaId: String): Result<Agenda> {
        return try {
            val doc = agendasRef.document(agendaId).get().await()
            val agenda = doc.toObject(Agenda::class.java)?.copy(id = doc.id)
                ?: return Result.failure(Exception("Agenda no encontrada"))
            Result.success(agenda)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun guardarProximaAgendaEnPrefs(numeroUnidad: String, context: android.content.Context) {
        try {
            val hoy = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.time
            val snapshot = agendasRef
                .whereEqualTo("numeroUnidad", numeroUnidad)
                .whereGreaterThanOrEqualTo("fecha", Timestamp(hoy))
                .orderBy("fecha", Query.Direction.ASCENDING)
                .limit(1)
                .get().await()
            val prefs = context.getSharedPreferences(NotifPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE)
            val agenda = snapshot.documents.firstOrNull()?.toObject(Agenda::class.java)
            if (agenda != null && agenda.estado != EstadoAgenda.REALIZADA) {
                android.util.Log.d("AgendaRepo", "guardarProximaAgenda: ${agenda.fecha.toDate()} estado=${agenda.estado}")
                prefs.edit()
                    .putString("notif_agenda_estado", agenda.estado.name)
                    .putString("notif_agenda_preside", agenda.preside)
                    .putLong("notif_agenda_fecha", agenda.fecha.toDate().time)
                    .putString("notif_agenda_id", agenda.id)
                    .apply()
            } else {
                prefs.edit()
                    .remove("notif_agenda_estado")
                    .remove("notif_agenda_preside")
                    .remove("notif_agenda_fecha")
                    .apply()
            }
        } catch (e: Exception) {
            android.util.Log.e("AgendaRepo", "guardarProximaAgenda error: ${e.message}")
        }
    }

    // Verificar si ya existe agenda con la misma fecha (mismo día) para la unidad
    private fun mismoDia(t1: Timestamp, t2: Timestamp): Boolean {
        val c1 = Calendar.getInstance().apply { time = t1.toDate() }
        val c2 = Calendar.getInstance().apply { time = t2.toDate() }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    suspend fun existeAgendaMismaFecha(numeroUnidad: String, fecha: Timestamp, excludeId: String = ""): Boolean {
        return try {
            val snapshot = agendasRef
                .whereEqualTo("numeroUnidad", numeroUnidad)
                .get().await()
            snapshot.documents.any { doc ->
                doc.id != excludeId &&
                        doc.getTimestamp("fecha")?.let { mismoDia(it, fecha) } == true
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun guardarAgenda(agenda: Agenda, userEmail: String): Result<String> {
        return try {
            // Validar fecha duplicada
            val duplicada = existeAgendaMismaFecha(agenda.numeroUnidad, agenda.fecha, agenda.id)
            if (duplicada) {
                return Result.failure(Exception("FECHA_DUPLICADA"))
            }

            val ahora = Timestamp.now()
            val data = agendaToMap(agenda).toMutableMap()
            data["ultimaEdicionPor"] = userEmail
            data["ultimaEdicionEn"] = ahora

            val id = if (agenda.id.isEmpty()) {
                data["creadoPor"] = userEmail
                data["creadoEn"] = ahora
                val ref = agendasRef.add(data).await()
                ref.id
            } else {
                agendasRef.document(agenda.id).set(data).await()
                agenda.id
            }
            sincronizarParticipantesAgenda(agenda.copy(id = id))
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarAgenda(agendaId: String): Result<Unit> {
        return try {
            agendasRef.document(agendaId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNombresUsados(numeroUnidad: String): List<String> {
        return try {
            val nombres = mutableSetOf<String>()

            // Del historial de agendas
            val snapshot = agendasRef
                .whereEqualTo("numeroUnidad", numeroUnidad)
                .get().await()
            snapshot.documents.forEach { doc ->
                doc.getString("preside")?.takeIf { it.isNotBlank() }?.let { nombres.add(it) }
                doc.getString("dirige")?.takeIf { it.isNotBlank() }?.let { nombres.add(it) }
                doc.getString("primeraOracion")?.takeIf { it.isNotBlank() }?.let { nombres.add(it) }
                doc.getString("oracionFinal")?.takeIf { it.isNotBlank() }?.let { nombres.add(it) }
                @Suppress("UNCHECKED_CAST")
                val mensajes = doc.get("mensajesEvangelio") as? List<Map<String, Any>> ?: emptyList()
                mensajes.forEach { m ->
                    (m["nombre"] as? String)?.takeIf { it.isNotBlank() }?.let { nombres.add(it) }
                }
                @Suppress("UNCHECKED_CAST")
                val testimonios = doc.get("testimonios") as? List<String> ?: emptyList()
                testimonios.forEach { it.takeIf { nombre -> nombre.isNotBlank() }?.let { nombre -> nombres.add(nombre) } }
            }

            // También incluir hermanos agregados manualmente al planificador
            val hermanosSnap = db.collection("hermanos")
                .whereEqualTo("numeroUnidad", numeroUnidad)
                .get().await()
            hermanosSnap.documents.forEach { doc ->
                if (doc.getBoolean("excluido") != true && doc.getBoolean("inactivo") != true) {
                    doc.getString("nombre")?.takeIf { it.isNotBlank() }?.let { nombres.add(it) }
                }
            }

            nombres.sorted()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun agendaToMap(agenda: Agenda): Map<String, Any> {
        return mapOf(
            "numeroUnidad" to agenda.numeroUnidad,
            "fecha" to agenda.fecha,
            "estado" to agenda.estado.name,
            "asistencia" to agenda.asistencia,
            "preside" to agenda.preside,
            "dirige" to agenda.dirige,
            "reconocimientos" to agenda.reconocimientos,
            "anuncios" to agenda.anuncios,
            "primerHimnoNumero" to agenda.primerHimnoNumero,
            "primerHimnoNombre" to agenda.primerHimnoNombre,
            "directorMusica" to agenda.directorMusica,
            "pianista" to agenda.pianista,
            "himnoSacramentalNumero" to agenda.himnoSacramentalNumero,
            "himnoSacramentalNombre" to agenda.himnoSacramentalNombre,
            "himnoFinalNumero" to agenda.himnoFinalNumero,
            "himnoFinalNombre" to agenda.himnoFinalNombre,
            "primeraOracion" to agenda.primeraOracion,
            "oracionFinal" to agenda.oracionFinal,
            "asuntosEstacaBarrio" to agenda.asuntosEstacaBarrio.map {
                mapOf("tipo" to it.tipo.name, "columna2" to it.columna2, "columna3" to it.columna3)
            },
            "mensajesEvangelio" to agenda.mensajesEvangelio.map {
                mapOf("tipo" to it.tipo.name, "nombre" to it.nombre,
                    "himnoNumero" to it.himnoNumero, "himnoNombre" to it.himnoNombre)
            },
            "reunionTestimonios" to agenda.reunionTestimonios,
            "testimonios" to agenda.testimonios.filter { it.isNotBlank() },
            "creadoPor" to agenda.creadoPor,
            "creadoEn" to agenda.creadoEn,
            "ultimaEdicionPor" to agenda.ultimaEdicionPor,
            "ultimaEdicionEn" to agenda.ultimaEdicionEn
        )
    }

    suspend fun excluirHermanoHistorial(numeroUnidad: String, nombre: String): Result<Unit> {
        return try {
            val data = mapOf(
                "numeroUnidad" to numeroUnidad,
                "nombre" to nombre,
                "agregadoManualmente" to false,
                "excluido" to true,
                "creadoEn" to Timestamp.now()
            )
            db.collection("hermanos").add(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarHermano(hermanoId: String): Result<Unit> {
        return try {
            db.collection("hermanos").document(hermanoId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cambiarPassword(numeroUnidad: String, newPassword: String): Result<Unit> {
        return try {
            unidadesRef.document(numeroUnidad)
                .update("passwordHash", hashPassword(newPassword))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHermanos(numeroUnidad: String): List<Hermano> {
        return try {
            val snapshot = db.collection("hermanos")
                .whereEqualTo("numeroUnidad", numeroUnidad)
                .orderBy("nombre")
                .get().await()

            val excluidos = snapshot.documents
                .filter { it.getBoolean("excluido") == true }
                .map { normalizarNombre(it.getString("nombre") ?: "") }
                .toSet()

            val manual = snapshot.documents
                .filter { it.getBoolean("excluido") != true }
                .mapNotNull { it.toObject(Hermano::class.java)?.copy(id = it.id) }

            // Extraer nombres del historial de agendas
            val agendasSnap = agendasRef
                .whereEqualTo("numeroUnidad", numeroUnidad)
                .get().await()
            val nombresHistorial = mutableSetOf<String>()
            agendasSnap.documents.forEach { doc ->
                doc.getString("primeraOracion")?.takeIf { it.isNotBlank() }?.let { nombresHistorial.add(it.trim()) }
                doc.getString("oracionFinal")?.takeIf { it.isNotBlank() }?.let { nombresHistorial.add(it.trim()) }
                @Suppress("UNCHECKED_CAST")
                val mensajes = doc.get("mensajesEvangelio") as? List<Map<String, Any>> ?: emptyList()
                mensajes.forEach { m ->
                    val tipo = m["tipo"] as? String
                    if (tipo != "HIMNO_INTERMEDIO") {
                        (m["nombre"] as? String)?.takeIf { it.isNotBlank() }?.let { nombresHistorial.add(it.trim()) }
                    }
                }
            }

            val nombresManual = manual.map { normalizarNombre(it.nombre) }.toSet()
            val deHistorial = nombresHistorial
                .filter { normalizarNombre(it) !in nombresManual && normalizarNombre(it) !in excluidos }
                .map { Hermano(numeroUnidad = numeroUnidad, nombre = it) }

            (manual + deHistorial).sortedBy { it.nombre.lowercase() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun agregarHermano(hermano: Hermano): Result<Unit> {
        return try {
            val data = mutableMapOf<String, Any?>(
                "numeroUnidad" to hermano.numeroUnidad,
                "nombre" to hermano.nombre,
                "agregadoManualmente" to hermano.agregadoManualmente,
                "excluido" to false,
                "inactivoDiscurso" to hermano.inactivoDiscurso,
                "inactivoOracion" to hermano.inactivoOracion,
                "ultimaVezDiscursoManual" to hermano.ultimaVezDiscursoManual,
                "ultimaVezOracionManual" to hermano.ultimaVezOracionManual,
                "creadoEn" to Timestamp.now()
            )
            db.collection("hermanos").add(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getConfiguracion(numeroUnidad: String): ConfiguracionPlanificacion? {
        return try {
            val snapshot = db.collection("configuracion")
                .whereEqualTo("numeroUnidad", numeroUnidad)
                .limit(1)
                .get().await()
            snapshot.documents.firstOrNull()?.toObject(ConfiguracionPlanificacion::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun guardarConfiguracion(config: ConfiguracionPlanificacion): Result<Unit> {
        return try {
            val data = mapOf(
                "numeroUnidad" to config.numeroUnidad,
                "diasVerdeDiscurso" to config.diasVerdeDiscurso,
                "diasAmarilloDiscurso" to config.diasAmarilloDiscurso,
                "diasVerdeOracion" to config.diasVerdeOracion,
                "diasAmarilloOracion" to config.diasAmarilloOracion
            )
            if (config.id.isBlank()) {
                db.collection("configuracion").add(data).await()
            } else {
                db.collection("configuracion").document(config.id).set(data).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun asignarHermanoAAgenda(agendaId: String, campo: String, nombre: String): Result<Unit> {
        return try {
            val doc = agendasRef.document(agendaId).get().await()
            val agenda = doc.toObject(Agenda::class.java)?.copy(id = doc.id)
                ?: return Result.failure(Exception("Agenda no encontrada"))
            when (normalizarNombre(campo)) {
                "primera oracion", "opening prayer" -> {
                    agendasRef.document(agendaId).update("primeraOracion", nombre).await()
                    actualizarFechaParticipacionManual(agenda.numeroUnidad, nombre, agenda.fecha, esDiscurso = false)
                }
                "oracion final", "closing prayer" -> {
                    agendasRef.document(agendaId).update("oracionFinal", nombre).await()
                    actualizarFechaParticipacionManual(agenda.numeroUnidad, nombre, agenda.fecha, esDiscurso = false)
                }
                "nuevo_discurso" -> {
                    val mensajes = agenda.mensajesEvangelio.toMutableList()
                    mensajes.add(MensajeEvangelio(tipo = TipoMensaje.DISCURSO, nombre = nombre))
                    val mensajesData = mensajes.map {
                        mapOf("tipo" to it.tipo.name, "nombre" to it.nombre,
                            "himnoNumero" to it.himnoNumero, "himnoNombre" to it.himnoNombre)
                    }
                    agendasRef.document(agendaId).update("mensajesEvangelio", mensajesData).await()
                    actualizarFechaParticipacionManual(agenda.numeroUnidad, nombre, agenda.fecha, esDiscurso = true)
                }
                else -> return Result.failure(Exception("Campo no reconocido"))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun sincronizarParticipantesAgenda(agenda: Agenda) {
        val discursos = agenda.mensajesEvangelio
            .filter { it.tipo != TipoMensaje.HIMNO_INTERMEDIO }
            .map { it.nombre.trim() }
            .filter { it.isNotBlank() }

        val oraciones = listOf(agenda.primeraOracion, agenda.oracionFinal)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        discursos.forEach { nombre ->
            actualizarFechaParticipacionManual(agenda.numeroUnidad, nombre, agenda.fecha, esDiscurso = true)
        }
        oraciones.forEach { nombre ->
            actualizarFechaParticipacionManual(agenda.numeroUnidad, nombre, agenda.fecha, esDiscurso = false)
        }
    }

    private suspend fun actualizarFechaParticipacionManual(
        numeroUnidad: String,
        nombre: String,
        fecha: Timestamp,
        esDiscurso: Boolean
    ) {
        val nombreLimpio = nombre.trim()
        if (nombreLimpio.isBlank()) return

        val snapshot = hermanosRef
            .whereEqualTo("numeroUnidad", numeroUnidad)
            .get()
            .await()

        val nombreNorm = normalizarNombre(nombreLimpio)
        val hermanoDoc = snapshot.documents.firstOrNull {
            it.getBoolean("excluido") != true &&
                    normalizarNombre(it.getString("nombre") ?: "") == nombreNorm
        }

        val campoFecha = if (esDiscurso) "ultimaVezDiscursoManual" else "ultimaVezOracionManual"
        val fechaActual = hermanoDoc?.getTimestamp(campoFecha)
        if (fechaActual != null && !fecha.toDate().after(fechaActual.toDate())) return

        if (hermanoDoc != null) {
            hermanoDoc.reference.update(campoFecha, fecha).await()
        } else {
            val data = mutableMapOf<String, Any?>(
                "numeroUnidad" to numeroUnidad,
                "nombre" to nombreLimpio,
                "agregadoManualmente" to false,
                "excluido" to false,
                "inactivoDiscurso" to false,
                "inactivoOracion" to false,
                "ultimaVezDiscursoManual" to if (esDiscurso) fecha else null,
                "ultimaVezOracionManual" to if (esDiscurso) null else fecha,
                "creadoEn" to Timestamp.now()
            )
            hermanosRef.add(data).await()
        }
    }

    suspend fun toggleInactivoHermano(hermanoId: String, campo: String, valor: Boolean): Result<Unit> {
        return try {
            db.collection("hermanos").document(hermanoId)
                .update(campo, valor).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Verificar si ya existe un hermano con nombre similar (ignorando tildes)
    suspend fun existeHermanoSimilar(numeroUnidad: String, nombre: String): String? {
        return try {
            val snapshot = db.collection("hermanos")
                .whereEqualTo("numeroUnidad", numeroUnidad)
                .whereEqualTo("excluido", false)
                .get().await()
            val nombreNorm = normalizarNombre(nombre)
            snapshot.documents.firstOrNull { doc ->
                val n = doc.getString("nombre") ?: ""
                normalizarNombre(n) == nombreNorm
            }?.getString("nombre")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun actualizarFechasManual(
        hermanoId: String,
        ultimaVezDiscurso: Timestamp?,
        ultimaVezOracion: Timestamp?
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any?>()
            updates["ultimaVezDiscursoManual"] = ultimaVezDiscurso
            updates["ultimaVezOracionManual"] = ultimaVezOracion
            db.collection("hermanos").document(hermanoId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sincronizarFechasParticipacionDesdeHistorial(numeroUnidad: String): Result<Int> {
        return try {
            val agendasSnap = agendasRef
                .whereEqualTo("numeroUnidad", numeroUnidad)
                .get()
                .await()

            data class Participacion(val nombre: String, val fecha: Timestamp)

            val ultimosDiscursos = mutableMapOf<String, Participacion>()
            val ultimasOraciones = mutableMapOf<String, Participacion>()

            fun registrar(
                destino: MutableMap<String, Participacion>,
                nombre: String?,
                fecha: Timestamp
            ) {
                val nombreLimpio = nombre?.trim().orEmpty()
                if (nombreLimpio.isBlank()) return
                val key = normalizarNombre(nombreLimpio)
                val actual = destino[key]?.fecha
                if (actual == null || fecha.toDate().after(actual.toDate())) {
                    destino[key] = Participacion(nombreLimpio, fecha)
                }
            }

            agendasSnap.documents.forEach { doc ->
                val fecha = doc.getTimestamp("fecha") ?: return@forEach
                registrar(ultimasOraciones, doc.getString("primeraOracion"), fecha)
                registrar(ultimasOraciones, doc.getString("oracionFinal"), fecha)

                @Suppress("UNCHECKED_CAST")
                val mensajes = doc.get("mensajesEvangelio") as? List<Map<String, Any>> ?: emptyList()
                mensajes.forEach { mensaje ->
                    if (mensaje["tipo"] as? String != TipoMensaje.HIMNO_INTERMEDIO.name) {
                        registrar(ultimosDiscursos, mensaje["nombre"] as? String, fecha)
                    }
                }
            }

            val hermanosSnap = hermanosRef
                .whereEqualTo("numeroUnidad", numeroUnidad)
                .get()
                .await()

            val excluidos = hermanosSnap.documents
                .filter { it.getBoolean("excluido") == true }
                .map { normalizarNombre(it.getString("nombre") ?: "") }
                .toSet()

            val hermanosPorNombre = hermanosSnap.documents
                .filter { it.getBoolean("excluido") != true }
                .filter { normalizarNombre(it.getString("nombre") ?: "").isNotBlank() }
                .groupBy { normalizarNombre(it.getString("nombre") ?: "") }

            var cambios = 0
            val nombres = (ultimosDiscursos.keys + ultimasOraciones.keys).filter { it !in excluidos }.toSet()
            nombres.forEach { key ->
                val ultimoDiscurso = ultimosDiscursos[key]
                val ultimaOracion = ultimasOraciones[key]
                val hermanos = hermanosPorNombre[key].orEmpty()

                if (hermanos.isEmpty()) {
                    val nombre = ultimoDiscurso?.nombre ?: ultimaOracion?.nombre ?: return@forEach
                    hermanosRef.add(
                        mapOf(
                            "numeroUnidad" to numeroUnidad,
                            "nombre" to nombre,
                            "agregadoManualmente" to false,
                            "excluido" to false,
                            "inactivoDiscurso" to false,
                            "inactivoOracion" to false,
                            "ultimaVezDiscursoManual" to ultimoDiscurso?.fecha,
                            "ultimaVezOracionManual" to ultimaOracion?.fecha,
                            "creadoEn" to Timestamp.now()
                        )
                    ).await()
                    cambios++
                } else {
                    hermanos.forEach { hermano ->
                        val updates = mutableMapOf<String, Any>()
                        val fechaDiscursoActual = hermano.getTimestamp("ultimaVezDiscursoManual")
                        val fechaOracionActual = hermano.getTimestamp("ultimaVezOracionManual")

                        if (ultimoDiscurso != null &&
                            (fechaDiscursoActual == null || ultimoDiscurso.fecha.toDate().after(fechaDiscursoActual.toDate()))
                        ) {
                            updates["ultimaVezDiscursoManual"] = ultimoDiscurso.fecha
                        }

                        if (ultimaOracion != null &&
                            (fechaOracionActual == null || ultimaOracion.fecha.toDate().after(fechaOracionActual.toDate()))
                        ) {
                            updates["ultimaVezOracionManual"] = ultimaOracion.fecha
                        }

                        if (updates.isNotEmpty()) {
                            hermano.reference.update(updates).await()
                            cambios++
                        }
                    }
                }
            }

            Result.success(cambios)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun editarNombreHermano(hermanoId: String, nuevoNombre: String): Result<Unit> {
        return try {
            db.collection("hermanos").document(hermanoId)
                .update("nombre", nuevoNombre).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Actualiza el nombre en todas las agendas BORRADOR de la unidad
    suspend fun renombrarEnAgendasFuturas(
        numeroUnidad: String,
        nombreAnterior: String,
        nuevoNombre: String
    ): Result<Int> {
        return try {
            val snapshot = agendasRef
                .whereEqualTo("numeroUnidad", numeroUnidad)
                .whereEqualTo("estado", EstadoAgenda.BORRADOR.name)
                .get().await()

            val anteriorNorm = normalizarNombre(nombreAnterior)
            var actualizadas = 0

            snapshot.documents.forEach { doc ->
                val updates = mutableMapOf<String, Any>()

                // Verificar y reemplazar en campos de oración
                if (normalizarNombre(doc.getString("primeraOracion") ?: "") == anteriorNorm)
                    updates["primeraOracion"] = nuevoNombre
                if (normalizarNombre(doc.getString("oracionFinal") ?: "") == anteriorNorm)
                    updates["oracionFinal"] = nuevoNombre
                if (normalizarNombre(doc.getString("preside") ?: "") == anteriorNorm)
                    updates["preside"] = nuevoNombre
                if (normalizarNombre(doc.getString("dirige") ?: "") == anteriorNorm)
                    updates["dirige"] = nuevoNombre

                // Verificar y reemplazar en mensajes del evangelio
                @Suppress("UNCHECKED_CAST")
                val mensajes = doc.get("mensajesEvangelio") as? List<Map<String, Any>> ?: emptyList()
                val mensajesActualizados = mensajes.map { m ->
                    val nombre = m["nombre"] as? String ?: ""
                    if (normalizarNombre(nombre) == anteriorNorm)
                        m.toMutableMap().also { it["nombre"] = nuevoNombre }
                    else m
                }
                if (mensajesActualizados != mensajes)
                    updates["mensajesEvangelio"] = mensajesActualizados

                if (updates.isNotEmpty()) {
                    doc.reference.update(updates).await()
                    actualizadas++
                }
            }
            Result.success(actualizadas)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Marca automáticamente como REALIZADA las agendas cuya fecha ya pasó
    suspend fun marcarAgendasPasadasComoRealizadas(numeroUnidad: String): Result<Unit> {
        return try {
            val hoy = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.time
            val snapshot = agendasRef
                .whereEqualTo("numeroUnidad", numeroUnidad)
                .whereIn("estado", listOf("BORRADOR", "CONFIRMADA"))
                .get().await()
            snapshot.documents.forEach { doc ->
                val fecha = doc.getTimestamp("fecha")?.toDate()
                if (fecha != null && fecha.before(hoy)) {
                    doc.reference.update("estado", EstadoAgenda.REALIZADA.name).await()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Crea agendas en blanco para los domingos hasta una fecha dada
    suspend fun crearAgendasDomingos(
        numeroUnidad: String,
        userEmail: String,
        hastaFecha: Date
    ): Result<Int> {
        return try {
            // Obtener agendas existentes para no duplicar
            val existentes = agendasRef
                .whereEqualTo("numeroUnidad", numeroUnidad)
                .get().await()
            val fechasExistentes = existentes.documents.mapNotNull {
                it.getTimestamp("fecha")?.toDate()?.let { d ->
                    val c = Calendar.getInstance().apply { time = d }
                    Triple(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
                }
            }.toSet()

            // Calcular próximos domingos desde hoy hasta hastaFecha (inclusive)
            val cal = Calendar.getInstance()
            // Avanzar al próximo domingo (si hoy es domingo, empezar desde hoy)
            while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                cal.add(Calendar.DAY_OF_MONTH, 1)
            }

            // Normalizar hastaFecha al final del día para que el domingo elegido sea inclusivo
            val hastaFechaFin = Calendar.getInstance().apply {
                time = hastaFecha
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.time

            var creadas = 0
            while (!cal.time.after(hastaFechaFin)) {
                val key = Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
                if (key !in fechasExistentes) {
                    val ahora = Timestamp.now()
                    val data = mapOf(
                        "numeroUnidad" to numeroUnidad,
                        "fecha" to Timestamp(cal.time),
                        "estado" to EstadoAgenda.BORRADOR.name,
                        "asistencia" to 0,
                        "preside" to "", "dirige" to "",
                        "reconocimientos" to "", "anuncios" to "",
                        "primerHimnoNumero" to 0, "primerHimnoNombre" to "",
                        "directorMusica" to "", "pianista" to "",
                        "himnoSacramentalNumero" to 0, "himnoSacramentalNombre" to "",
                        "himnoFinalNumero" to 0, "himnoFinalNombre" to "",
                        "primeraOracion" to "", "oracionFinal" to "",
                        "asuntosEstacaBarrio" to emptyList<Any>(),
                        "mensajesEvangelio" to emptyList<Any>(),
                        "reunionTestimonios" to esPrimerDomingoDelMes(cal.time),
                        "testimonios" to emptyList<String>(),
                        "creadoPor" to userEmail,
                        "creadoEn" to ahora,
                        "ultimaEdicionPor" to userEmail,
                        "ultimaEdicionEn" to ahora
                    )
                    agendasRef.add(data).await()
                    creadas++
                }
                cal.add(Calendar.WEEK_OF_YEAR, 1)
            }
            Result.success(creadas)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
