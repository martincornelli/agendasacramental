package com.example.agendasacramental

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdatePromptPolicyTest {

    private val now = 10_000_000_000L
    private val oldInstall = now - AppUpdatePromptPolicy.INSTALL_GRACE_PERIOD_MS - 1L

    @Test
    fun `no ofrece actualizacion durante las primeras 24 horas`() {
        assertFalse(
            shouldOffer(
                firstInstall = now - 1_000L,
                lastUpdate = now - 1_000L
            )
        )
    }

    @Test
    fun `no ofrece actualizacion durante las 24 horas posteriores a una actualizacion`() {
        assertFalse(
            shouldOffer(
                firstInstall = oldInstall,
                lastUpdate = now - 1_000L
            )
        )
    }

    @Test
    fun `no ofrece una version igual o inferior`() {
        assertFalse(shouldOffer(availableVersion = 38))
        assertFalse(shouldOffer(availableVersion = 37))
    }

    @Test
    fun `ofrece una version superior luego del periodo de gracia`() {
        assertTrue(shouldOffer())
    }

    @Test
    fun `no repite el aviso de la misma version durante siete dias`() {
        assertFalse(
            shouldOffer(
                lastPromptVersion = 39,
                lastPromptTime = now - AppUpdatePromptPolicy.REPEAT_PROMPT_COOLDOWN_MS + 1L
            )
        )
    }

    @Test
    fun `vuelve a ofrecer la misma version luego de siete dias`() {
        assertTrue(
            shouldOffer(
                lastPromptVersion = 39,
                lastPromptTime = now - AppUpdatePromptPolicy.REPEAT_PROMPT_COOLDOWN_MS
            )
        )
    }

    @Test
    fun `permite avisar una version nueva aunque otra se haya mostrado recientemente`() {
        assertTrue(
            shouldOffer(
                availableVersion = 40,
                lastPromptVersion = 39,
                lastPromptTime = now - 1_000L
            )
        )
    }

    private fun shouldOffer(
        availableVersion: Int = 39,
        firstInstall: Long = oldInstall,
        lastUpdate: Long = oldInstall,
        lastPromptVersion: Int = -1,
        lastPromptTime: Long = 0L
    ) = AppUpdatePromptPolicy.shouldOfferUpdate(
        nowMillis = now,
        installedVersionCode = 38,
        availableVersionCode = availableVersion,
        firstInstallTimeMillis = firstInstall,
        lastUpdateTimeMillis = lastUpdate,
        lastPromptVersionCode = lastPromptVersion,
        lastPromptTimeMillis = lastPromptTime
    )
}
