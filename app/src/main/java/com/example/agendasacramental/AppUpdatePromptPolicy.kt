package com.example.agendasacramental

object AppUpdatePromptPolicy {
    const val INSTALL_GRACE_PERIOD_MS = 24L * 60L * 60L * 1_000L
    const val REPEAT_PROMPT_COOLDOWN_MS = 7L * 24L * 60L * 60L * 1_000L

    fun shouldOfferUpdate(
        nowMillis: Long,
        installedVersionCode: Int,
        availableVersionCode: Int,
        firstInstallTimeMillis: Long,
        lastUpdateTimeMillis: Long,
        lastPromptVersionCode: Int,
        lastPromptTimeMillis: Long
    ): Boolean {
        if (availableVersionCode <= installedVersionCode) return false

        val installedOrUpdatedAt = maxOf(firstInstallTimeMillis, lastUpdateTimeMillis)
        if (nowMillis - installedOrUpdatedAt < INSTALL_GRACE_PERIOD_MS) return false

        val sameVersionPromptedRecently = lastPromptVersionCode == availableVersionCode &&
                nowMillis - lastPromptTimeMillis < REPEAT_PROMPT_COOLDOWN_MS
        return !sameVersionPromptedRecently
    }
}
