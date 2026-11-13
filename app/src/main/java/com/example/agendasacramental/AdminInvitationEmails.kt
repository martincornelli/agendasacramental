package com.example.agendasacramental

import java.util.Locale

internal fun parseAdminInvitationEmails(value: String): List<String> {
    val emails = value.split(Regex("[,;\\n\\r]+")).map { it.trim().lowercase(Locale.ROOT) }
        .filter { it.isNotEmpty() }.distinct()
    check(emails.isNotEmpty() && emails.all { Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(it) }) { "ADMIN_EMAIL" }
    return emails
}
