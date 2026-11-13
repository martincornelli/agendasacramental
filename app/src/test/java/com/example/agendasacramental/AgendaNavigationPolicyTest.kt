package com.example.agendasacramental

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AgendaNavigationPolicyTest {
    @Test fun coldNotificationMustEstablishUnitBeforeEditing() {
        assertEquals(AgendaEntry.LOGIN, agendaEntry(false, ""))
        assertEquals(AgendaEntry.UNIT_SELECTION, agendaEntry(true, ""))
        assertEquals(AgendaEntry.EDITOR, agendaEntry(true, "123"))
    }
    @Test fun signOutRequiresLoginEvenWithAnOldUnit() {
        assertEquals(AgendaEntry.LOGIN, agendaEntry(false, "123"))
    }
    @Test fun saveCannotDetachOrMoveExistingAgenda() {
        assertThrows(IllegalArgumentException::class.java) { validateAgendaUnit("", "123") }
        assertThrows(IllegalArgumentException::class.java) { validateAgendaUnit("456", "123") }
        assertThrows(IllegalArgumentException::class.java) { validateAgendaUnit("123", "") }
        validateAgendaUnit("123", "123")
        validateAgendaUnit("123")
    }
}
