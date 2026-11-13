package com.example.agendasacramental

import com.google.firebase.Timestamp
import java.util.Calendar

internal fun mismoDiaParticipacion(a: Timestamp?, b: Timestamp?): Boolean {
    if (a == null || b == null) return false
    val first = Calendar.getInstance().apply { time = a.toDate() }
    val second = Calendar.getInstance().apply { time = b.toDate() }
    return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
        first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
}

// Las fechas automáticas se calculan desde las agendas actuales, no desde la copia guardada.
internal fun fechaManualParticipacion(
    actual: Timestamp?, sincronizada: Timestamp?, anterior: Timestamp?
): Timestamp? = if (actual != null && actual == sincronizada) anterior else actual
