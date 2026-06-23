package dev.tomlarcher.gitarborist.carry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CarryOverResultDecisionTest {
    @Test
    fun cleanResultAllowsOpenWithoutPrompt() {
        val result =
            resultWith(
                CarryOverMessage(CarryOverMessageKind.Copied, ".idea/runConfigurations/App.xml", "Copied"),
            )

        assertEquals(CarryOverResultSeverity.Clean, result.severity)
        assertFalse(result.requiresOpenDecision)
        assertFalse(result.shouldReportBeforeOpen)
        assertFalse(result.hasBlockingError)
    }

    @Test
    fun rejectedSensitivePathsWarnButDoNotRequireDecision() {
        val result =
            resultWith(
                CarryOverMessage(CarryOverMessageKind.Rejected, ".env", "Sensitive denylist rejected manifest path"),
            )

        assertEquals(CarryOverResultSeverity.Warning, result.severity)
        assertFalse(result.requiresOpenDecision)
        assertTrue(result.shouldReportBeforeOpen)
        assertFalse(result.hasBlockingError)
    }

    @Test
    fun copyFailuresRequireOpenDecision() {
        val result =
            resultWith(
                CarryOverMessage(CarryOverMessageKind.Failed, ".idea/codeStyles/Project.xml", "Permission denied"),
            )

        assertEquals(CarryOverResultSeverity.Failure, result.severity)
        assertTrue(result.requiresOpenDecision)
        assertTrue(result.shouldReportBeforeOpen)
        assertTrue(result.hasBlockingError)
    }

    @Test
    fun existingIdeaSkipDoesNotPromptBeforeOpen() {
        val result = CarryOverResult.skippedIdeaExists()

        assertEquals(CarryOverResultSeverity.Skipped, result.severity)
        assertTrue(result.skippedBecauseIdeaExists)
        assertFalse(result.requiresOpenDecision)
        assertFalse(result.shouldReportBeforeOpen)
        assertFalse(result.hasBlockingError)
    }

    @Test
    fun skippedExistingFilesRemainNonBlocking() {
        val result =
            resultWith(
                CarryOverMessage(CarryOverMessageKind.Skipped, "justfile", "Skipped existing: justfile"),
            )

        assertEquals(CarryOverResultSeverity.Skipped, result.severity)
        assertFalse(result.requiresOpenDecision)
        assertFalse(result.shouldReportBeforeOpen)
        assertFalse(result.hasBlockingError)
    }

    private fun resultWith(vararg messages: CarryOverMessage): CarryOverResult =
        CarryOverResult(
            plan = null,
            messages = messages.toList(),
        )
}
