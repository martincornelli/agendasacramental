package com.example.agendasacramental

enum class AgendaEntry { LOGIN, UNIT_SELECTION, EDITOR }

/** A notification identifies a record; it does not establish access to its unit. */
fun agendaEntry(signedIn: Boolean, unit: String): AgendaEntry = when {
    !signedIn -> AgendaEntry.LOGIN
    unit.isBlank() -> AgendaEntry.UNIT_SELECTION
    else -> AgendaEntry.EDITOR
}

fun validateAgendaUnit(unit: String, existingUnit: String? = null) {
    require(unit.isNotBlank()) { "AGENDA_UNIT_REQUIRED" }
    require(existingUnit == null || unit == existingUnit) { "AGENDA_UNIT_MISMATCH" }
}
