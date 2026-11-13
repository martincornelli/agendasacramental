package com.example.agendasacramental

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.Locale

data class UnitAdministration(val admins: Map<String, String> = emptyMap(), val invitations: List<String> = emptyList())

class UnitAdministrationRepository {
    private val db = FirebaseFirestore.getInstance()
    private fun identity(): Pair<String, String> {
        val user = FirebaseAuth.getInstance().currentUser ?: error("ADMIN_IDENTITY")
        check(user.isEmailVerified && user.providerData.any { it.providerId == "google.com" }) { "ADMIN_IDENTITY" }
        return user.uid to user.email.orEmpty().trim().lowercase(Locale.ROOT)
    }
    fun isAdmin(data: UnitAdministration) = data.admins.containsKey(identity().first)
    fun isInvited(data: UnitAdministration) = identity().second in data.invitations
    fun currentUid() = identity().first

    @Suppress("UNCHECKED_CAST")
    suspend fun load(unit: String): UnitAdministration {
        val (uid, email) = identity()
        val ref = db.collection("unidades").document(unit)
        return db.runTransaction { tx ->
            val snap = tx.get(ref)
            check(snap.exists()) { "ADMIN_MISSING" }
            if (!snap.contains("administradores") && snap.getString("creadoPor")?.lowercase(Locale.ROOT) == email) {
                tx.update(ref, mapOf("administradores" to mapOf(uid to email), "invitacionesAdmin" to emptyList<String>()))
                UnitAdministration(mapOf(uid to email))
            } else UnitAdministration(snap.get("administradores") as? Map<String, String> ?: emptyMap(), snap.get("invitacionesAdmin") as? List<String> ?: emptyList())
        }.await()
    }

    suspend fun create(unit: String, password: String) {
        val (uid, email) = identity()
        check(password.length >= 4) { "ADMIN_PASSWORD" }
        val ref = db.collection("unidades").document(unit)
        db.runTransaction { tx ->
            check(!tx.get(ref).exists()) { "ADMIN_EXISTS" }
            tx.set(ref, mapOf("numeroUnidad" to unit, "passwordHash" to hash(password),
                "creadoPor" to FirebaseAuth.getInstance().currentUser!!.email,
                "creadoEn" to FieldValue.serverTimestamp(), "administradores" to mapOf(uid to email),
                "invitacionesAdmin" to emptyList<String>()))
        }.await()
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun change(unit: String, action: String, value: String = "") {
        load(unit)
        val (uid, email) = identity()
        val ref = db.collection("unidades").document(unit)
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            val admins = (snap.get("administradores") as? Map<String, String> ?: emptyMap()).toMutableMap()
            val invitations = (snap.get("invitacionesAdmin") as? List<String> ?: emptyList()).toMutableList()
            if (action == "decline") {
                check(email in invitations) { "ADMIN_FORBIDDEN" }
                invitations.removeAll { it == email }
            } else if (action == "accept") {
                check(email in invitations) { "ADMIN_FORBIDDEN" }
                check(admins.size < 30 || uid in admins) { "ADMIN_LIMIT" }
                admins[uid] = email
                invitations.removeAll { it == email }
            } else {
                check(uid in admins) { "ADMIN_FORBIDDEN" }
                when (action) {
                    "invite" -> {
                        val targets = parseAdminInvitationEmails(value)
                        check(targets.none { it in admins.values }) { "ADMIN_ALREADY" }
                        targets.forEach { if (it !in invitations) invitations.add(it) }
                        check(invitations.size <= 30) { "ADMIN_LIMIT" }
                    }
                    "cancel" -> invitations.remove(value)
                    "remove" -> { admins.remove(value); check(admins.isNotEmpty()) { "ADMIN_LAST" } }
                    "password" -> {
                        check(value.length >= 4) { "ADMIN_PASSWORD" }
                        tx.update(ref, "passwordHash", hash(value))
                        return@runTransaction
                    }
                    else -> error("ADMIN_FORBIDDEN")
                }
            }
            tx.update(ref, mapOf("administradores" to admins, "invitacionesAdmin" to invitations))
        }.await()
    }
    private fun hash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
