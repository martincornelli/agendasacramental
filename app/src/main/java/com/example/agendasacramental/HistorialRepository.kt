package com.example.agendasacramental

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class HistorialRepository {
    private val db = FirebaseFirestore.getInstance()
    suspend fun registros(unidad: String): List<Map<String, Any>> {
        val snapshot = db.collection("historialParticipaciones").document(unidad).get().await()
        @Suppress("UNCHECKED_CAST")
        return snapshot.get("records") as? List<Map<String, Any>> ?: emptyList()
    }

    suspend fun payload(unidad: String): JSONObject {
        val snapshot = db.collection("historialParticipaciones").document(unidad).get(Source.SERVER).await()
        @Suppress("UNCHECKED_CAST")
        val imported = snapshot.get("records") as? List<Map<String, Any>> ?: emptyList()
        val all = JSONArray()
        imported.forEach { all.put(JSONObject(it).put("source", "import")) }
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = fmt.format(Date())
        val agendas = db.collection("agendas").whereEqualTo("numeroUnidad", unidad).get(Source.SERVER).await()
        fun add(id: String, date: String, name: String?, type: String, topic: String = "", source: String) {
            if (!name.isNullOrBlank()) all.put(JSONObject().put("id", id).put("date", date).put("name", name.trim())
                .put("type", type).put("topic", topic).put("source", source))
        }
        for (doc in agendas.documents) {
            val a = doc.toObject(Agenda::class.java) ?: continue
            val date = fmt.format(a.fecha.toDate())
            if (date > today) continue
            add("$unidad:agenda:${doc.id}:opening", date, a.primeraOracion, "Primera oración", source = "agenda")
            add("$unidad:agenda:${doc.id}:closing", date, a.oracionFinal, "Última oración", source = "agenda")
            a.mensajesEvangelio.forEachIndexed { i, m -> if (m.tipo == TipoMensaje.DISCURSO)
                add("$unidad:agenda:${doc.id}:talk:$i", date, m.nombre, "Discurso", m.tema, "agenda") }
        }
        val brothers = db.collection("hermanos").whereEqualTo("numeroUnidad", unidad).get(Source.SERVER).await()
        for (doc in brothers.documents) for ((field, type) in listOf("ultimaVezDiscursoManual" to "Discurso", "ultimaVezOracionManual" to "Oración")) {
            val date = doc.getTimestamp(field) ?: continue
            add("$unidad:manual:${doc.id}:$field", fmt.format(date.toDate()), doc.getString("nombre"), type, source = "manual")
        }
        return JSONObject().put("unit", unidad).put("revision", snapshot.getLong("revision") ?: 0)
            .put("existing", all)
    }

    suspend fun guardar(unidad: String, revision: Long, changes: JSONArray) {
        require(changes.length() in 1..5000)
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val updates = (0 until changes.length()).map { i ->
            val r = changes.getJSONObject(i)
            val map = listOf("id", "date", "name", "type", "topic").associateWith { r.getString(it) }
            require(map.getValue("id").startsWith("$unidad:import:"))
            require(map.getValue("name").length in 1..200 && map.getValue("topic").length <= 2000)
            require(map.getValue("type") in listOf("Discurso", "Primera oración", "Última oración", "Oración"))
            val date = map.getValue("date")
            require(Regex("\\d{4}-\\d{2}-\\d{2}").matches(date) && fmt.format(fmt.parse(date)!!) == date)
            map
        }
        require(updates.map { it["id"] }.toSet().size == updates.size)
        val email = FirebaseAuth.getInstance().currentUser?.email ?: error("Sign in again / Volvé a iniciar sesión")
        val ref = db.collection("historialParticipaciones").document(unidad)
        db.runTransaction { tx ->
            val current = tx.get(ref)
            check((current.getLong("revision") ?: 0) == revision) { "History changed. Reopen the preview. / El historial cambió. Volvé a abrir la vista previa." }
            @Suppress("UNCHECKED_CAST")
            val records = (current.get("records") as? List<Map<String, Any>> ?: emptyList()).associateBy { it["id"] }.toMutableMap()
            updates.forEach { records[it.getValue("id")] = it }
            require(records.size <= 5000)
            require(JSONArray(records.values.toList()).toString().toByteArray(Charsets.UTF_8).size <= 700000)
            tx.set(ref, mapOf("numeroUnidad" to unidad, "revision" to revision + 1, "records" to records.values.toList(),
                "actualizadoEn" to FieldValue.serverTimestamp(), "actualizadoPor" to email))
        }.await()
    }
}

// Transient projection for existing ranking/topic calculations; never stored as agendas.
fun historialParaPlanificacion(unit: String, records: List<Map<String, Any>>): List<Agenda> {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
    return records.mapNotNull { r -> runCatching {
        val date = Timestamp(fmt.parse(r["date"] as String)!!)
        val name = r["name"] as String
        val type = r["type"] as String
        Agenda(id = "import:${r["id"]}", numeroUnidad = unit, fecha = date, estado = EstadoAgenda.REALIZADA,
            primeraOracion = if (type == "Primera oración" || type == "Oración") name else "",
            oracionFinal = if (type == "Última oración") name else "",
            mensajesEvangelio = if (type == "Discurso") listOf(MensajeEvangelio(tipo = TipoMensaje.DISCURSO, nombre = name, tema = r["topic"] as? String ?: "")) else emptyList())
    }.getOrNull() }
}
