package dev.tomlarcher.gitarborist.settings

import dev.tomlarcher.gitarborist.carry.CarryOverScope
import dev.tomlarcher.gitarborist.carry.CarryOverSource
import dev.tomlarcher.gitarborist.git.WorktreeOpenMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitArboristSettingsTest {
    @Test
    fun globalDefaultsAreStable() {
        val state = GitArboristSettings().state

        assertEquals(".worktrees", state.defaultWorktreeDirectory)
        assertEquals(WorktreeOpenMode.NewWindow, state.defaultOpenMode)
        assertEquals(CarryOverScope.Curated, state.carryOverScope)
        assertEquals(CarryOverSource.MainWorktree, state.carryOverSource)
        assertTrue(state.copyIdeaDirectory)
        assertEquals(".worktree-copy", state.manifestFileName)
        assertTrue(state.runCarryOverOnlyWhenIdeaMissing)
        assertFalse(state.allowHeavyManifestPaths)
        assertTrue(state.showRelativeLocations)
    }

    @Test
    fun projectSettingsDefaultToDisabledOverride() {
        val state = GitArboristProjectSettings().state

        assertFalse(state.useProjectSettings)
        assertEquals(".worktrees", state.defaultWorktreeDirectory)
        assertEquals(WorktreeOpenMode.NewWindow, state.defaultOpenMode)
        assertEquals(CarryOverScope.Curated, state.carryOverScope)
        assertEquals(CarryOverSource.MainWorktree, state.carryOverSource)
    }

    @Test
    fun loadStateReplacesPersistentState() {
        val settings = GitArboristSettings()
        settings.loadState(
            GitArboristSettings.State(
                defaultWorktreeDirectory = "../worktrees",
                defaultOpenMode = WorktreeOpenMode.AskEachTime,
                carryOverScope = CarryOverScope.ManifestOnly,
                carryOverSource = CarryOverSource.CurrentProject,
                copyIdeaDirectory = false,
            ),
        )

        assertEquals("../worktrees", settings.state.defaultWorktreeDirectory)
        assertEquals(WorktreeOpenMode.AskEachTime, settings.state.defaultOpenMode)
        assertEquals(CarryOverScope.ManifestOnly, settings.state.carryOverScope)
        assertEquals(CarryOverSource.CurrentProject, settings.state.carryOverSource)
        assertFalse(settings.state.copyIdeaDirectory)
    }
}
