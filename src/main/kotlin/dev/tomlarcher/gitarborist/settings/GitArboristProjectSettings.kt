package dev.tomlarcher.gitarborist.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import dev.tomlarcher.gitarborist.carry.CarryOverScope
import dev.tomlarcher.gitarborist.carry.CarryOverSource
import dev.tomlarcher.gitarborist.git.WorktreeOpenMode

/** Project-level settings stored in the workspace file; applied only when [State.useProjectSettings] is set. */
@State(
    name = "GitArboristProjectSettings",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)],
)
@Service(Service.Level.PROJECT)
class GitArboristProjectSettings : PersistentStateComponent<GitArboristProjectSettings.State> {
    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    data class State(
        var useProjectSettings: Boolean = false,
        var defaultWorktreeDirectory: String = ".worktrees",
        var defaultOpenMode: WorktreeOpenMode = WorktreeOpenMode.NewWindow,
        var carryOverScope: CarryOverScope = CarryOverScope.Curated,
        var carryOverSource: CarryOverSource = CarryOverSource.MainWorktree,
        var copyIdeaDirectory: Boolean = true,
        var manifestFileName: String = ".worktree-copy",
        var runCarryOverOnlyWhenIdeaMissing: Boolean = true,
        var allowHeavyManifestPaths: Boolean = false,
        var showRelativeLocations: Boolean = true,
    )
}
