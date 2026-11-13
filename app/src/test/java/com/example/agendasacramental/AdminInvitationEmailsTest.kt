package com.example.agendasacramental

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AdminInvitationEmailsTest {
    @Test fun multipleEmailsSupportSeparatorsAndRemoveDuplicates() {
        assertEquals(listOf("ana@example.com", "juan@example.org", "maria@example.net"),
            parseAdminInvitationEmails(" Ana@example.com;juan@example.org\nMARIA@example.net, ana@example.com"))
    }
    @Test fun invalidOrEmptyInputRejectsTheWholeBatch() {
        assertThrows(IllegalStateException::class.java) { parseAdminInvitationEmails("valid@example.com;invalid") }
        assertThrows(IllegalStateException::class.java) { parseAdminInvitationEmails(" ;,\n ") }
    }
}
