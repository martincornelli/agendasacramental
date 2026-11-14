package com.example.agendasacramental

import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

class HistoryOrganizationsTest {
    @Test fun organizationsUpdateAtomicallyWithoutDuplicatingHistory() = runBlocking {
        // Opt in explicitly; this test can only address the local demo emulator.
        assumeTrue(InstrumentationRegistry.getArguments().getString("historyEmulator") == "true")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val app = FirebaseApp.initializeApp(context, FirebaseOptions.Builder()
            .setApplicationId("1:123:android:historytest").setProjectId("demo-agenda-history-test")
            .setApiKey("test-key").build(), "history-${System.nanoTime()}")
        val db = FirebaseFirestore.getInstance(app)
        db.useEmulator("127.0.0.1", 8191)
        val repo = HistorialRepository(db) { "qa@example.test" }
        val unit = System.currentTimeMillis().toString()
        val member = db.collection("hermanos").document()
        member.set(mapOf("numeroUnidad" to unit, "nombre" to "Ana", "organizacion" to "RELIEF_SOCIETY",
            "ultimaVezDiscursoManual" to Timestamp(1767484800, 0))).await()
        val history = db.collection("historialParticipaciones").document(unit)
        fun row(id: String, name: String = "Ana", organization: String = "PRIMARY") = JSONObject()
            .put("id", "$unit:import:$id").put("date", "2026-01-04").put("name", name)
            .put("type", "Discurso").put("topic", "Fe").put("organization", organization)
        try {
            repo.guardar(unit, 0, JSONArray().put(row("one")))
            assertEquals("PRIMARY", member.get().await().getString("organizacion"))
            assertEquals(1, (history.get().await().get("records") as List<*>).size)
            // A legacy spreadsheet with a blank organization preserves the profile and history value.
            repo.guardar(unit, 1, JSONArray().put(row("one", organization = "").put("topic", "Esperanza")))
            assertEquals("PRIMARY", member.get().await().getString("organizacion"))
            assertEquals("PRIMARY", (repo.registros(unit).single()["organization"]))
            // New participants get a profile, making the organization available to planning filters.
            repo.guardar(unit, 2, JSONArray().put(row("two", "Eva", "YOUNG_WOMEN")))
            val profiles = db.collection("hermanos").whereEqualTo("numeroUnidad", unit).get().await()
            assertEquals("YOUNG_WOMEN", profiles.documents.single { it.getString("nombre") == "Eva" }.getString("organizacion"))
            // Organization-only corrections to a manual participation never create a history duplicate.
            val all = repo.payload(unit).getJSONArray("existing")
            val manual = (0 until all.length()).map { all.getJSONObject(it) }.single { it.getString("source") == "manual" }
            manual.put("organization", "BISHOPRIC").put("organizationOnly", true)
            repo.guardar(unit, 3, JSONArray().put(manual))
            assertEquals("BISHOPRIC", member.get().await().getString("organizacion"))
            assertEquals(2, repo.registros(unit).size)
            val conflict = runCatching { repo.guardar(unit, 4, JSONArray().put(row("three")).put(row("four", organization = "ELDERS_QUORUM"))) }
            assertTrue(conflict.isFailure)
            assertEquals(4L, history.get().await().getLong("revision"))
            assertEquals("BISHOPRIC", member.get().await().getString("organizacion"))
            db.collection("hermanos").add(mapOf("nombre" to "Ana", "numeroUnidad" to unit)).await()
            assertTrue(runCatching { repo.guardar(unit, 4, JSONArray().put(row("one"))) }.isFailure)
            assertEquals(4L, history.get().await().getLong("revision"))
        } finally {
            db.terminate().await()
            app.delete()
        }
    }
}
