package dev.gitworktreetools.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import dev.gitworktreetools.carry.CarryOverScope
import dev.gitworktreetools.carry.CarryOverSource
import dev.gitworktreetools.carry.EffectiveCarryOverSettings
import dev.gitworktreetools.git.WorktreeOpenMode

/** Application-level persistent global defaults for Git Worktree Tools. */
@State(
    name = "GitWorktreeToolsSettings",
    storages = [Storage("git-worktree-tools.xml", roamingType = RoamingType.DISABLED)],
)
@Service(Service.Level.APP)
class GitWorktreeToolsSettings : PersistentStateComponent<GitWorktreeToolsSettings.State> {
    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    fun carryOverSettings(): EffectiveCarryOverSettings = state.toCarryOverSettings()

    data class State(
        var defaultWorktreeDirectory: String = ".worktrees",
        var defaultOpenMode: WorktreeOpenMode = WorktreeOpenMode.NewWindow,
        var carryOverScope: CarryOverScope = CarryOverScope.Curated,
        var carryOverSource: CarryOverSource = CarryOverSource.MainWorktree,
        var copyIdeaDirectory: Boolean = true,
        var manifestFileName: String = ".worktree-copy",
        var runCarryOverOnlyWhenIdeaMissing: Boolean = true,
        var allowHeavyManifestPaths: Boolean = false,
        var showRelativeLocations: Boolean = true,
    ) {
        fun toCarryOverSettings(): EffectiveCarryOverSettings =
            EffectiveCarryOverSettings(
                defaultWorktreeDirectory = defaultWorktreeDirectory,
                carryOverScope = carryOverScope,
                carryOverSource = carryOverSource,
                copyIdeaDirectory = copyIdeaDirectory,
                manifestFileName = manifestFileName,
                runCarryOverOnlyWhenIdeaMissing = runCarryOverOnlyWhenIdeaMissing,
                allowHeavyManifestPaths = allowHeavyManifestPaths,
            )
    }
}
