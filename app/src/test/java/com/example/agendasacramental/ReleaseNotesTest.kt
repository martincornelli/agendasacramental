package com.example.agendasacramental

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseNotesTest {
    @Test fun freshInstallDoesNotShowUpdateNotes() {
        assertFalse(shouldShowReleaseNotes(null, 42, 100L, 100L))
    }
    @Test fun upgradeFromVersionWithoutTrackingShowsNotes() {
        assertTrue(shouldShowReleaseNotes(null, 42, 100L, 200L))
    }
    @Test fun acknowledgedVersionDoesNotRepeatAfterRestart() {
        assertFalse(shouldShowReleaseNotes(42, 42, 100L, 200L))
    }
    @Test fun laterVersionShowsNotesEvenWhenVersionsAreSkipped() {
        assertTrue(shouldShowReleaseNotes(42, 45, 100L, 300L))
    }
    @Test fun downgradeDoesNotShowUpdateNotes() {
        assertFalse(shouldShowReleaseNotes(45, 42, 100L, 300L))
    }
}
