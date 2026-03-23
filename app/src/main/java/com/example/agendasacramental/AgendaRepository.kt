package com.example.agendasacramental

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

class AgendaRepository {
    private val db = FirebaseFirestore.getInstance()
    private val unidadesRef = db.collection("unidades")
    private val agendasRef = db.collection("agendas")

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun existeUnidad(numeroUnidad: String): Result<Boolean> {
        return try {
            val result = unidadesRef.document(numeroUnidad).get().await()
            Result.success(result.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
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

    suspend fun verificarPassword(numeroUnidad: String, password: String): Result<Boolean> {
        return try {
            val doc = unidadesRef.document(numeroUnidad).get().await()
            val storedHash = doc.getString("passwordHash") ?: return Result.success(false)
            Result.success(storedHash == hashPassword(password))
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

    suspend fun getAgendas(numeroUnidad: String): Result<List<Agenda>> {
        return try {
            val snapshot = agendasRef
                .whereEqualTo("numeroUnidad", numeroUnidad)
                .orderBy("fecha", Query.Direction.DESCENDING)
                .get()
                .await()
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

    suspend fun guardarAgenda(agenda: Agenda, userEmail: String): Result<String> {
        return try {
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
            val snapshot = agendasRef
                .whereEqualTo("numeroUnidad", numeroUnidad)
                .get()
                .await()
            val nombres = mutableSetOf<String>()
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
            "preside" to agenda.preside,
            "dirige" to agenda.dirige,
            "reconocimientos" to agenda.reconocimientos,
            "anuncios" to agenda.anuncios,
            "primerHimnoNumero" to agenda.primerHimnoNumero,
            "primerHimnoNombre" to agenda.primerHimnoNombre,
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
                mapOf(
                    "tipo" to it.tipo.name,
                    "nombre" to it.nombre,
                    "himnoNumero" to it.himnoNumero,
                    "himnoNombre" to it.himnoNombre
                )
            },
            "creadoPor" to agenda.creadoPor,
            "creadoEn" to agenda.creadoEn,
            "ultimaEdicionPor" to agenda.ultimaEdicionPor,
            "ultimaEdicionEn" to agenda.ultimaEdicionEn
        )
    }
}