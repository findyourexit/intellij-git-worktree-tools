package dev.tomlarcher.gitarborist.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorktreesPanelFilterTest {
    @Test
    fun filterMatchesAnyVisibleColumnCaseInsensitively() {
        val row = listOf("/repo", "feature-demo", "feature/demo", "/repo/.worktrees/feature-demo", "ready")

        assertTrue(rowMatchesFilter(row, listOf("FEATURE")))
        assertTrue(rowMatchesFilter(row, listOf("worktrees")))
        assertTrue(rowMatchesFilter(row, listOf("ready")))
    }

    @Test
    fun filterRequiresAllTokensToMatchSomewhereInRow() {
        val row = listOf("/repo", "feature-demo", "feature/demo", "/repo/.worktrees/feature-demo", "ready")

        assertTrue(rowMatchesFilter(row, listOf("feature", "ready")))
        assertFalse(rowMatchesFilter(row, listOf("feature", "dirty")))
    }
}
