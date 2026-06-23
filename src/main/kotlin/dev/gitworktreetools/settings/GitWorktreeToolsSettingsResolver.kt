package dev.gitworktreetools.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import dev.gitworktreetools.carry.CarryOverScope
import dev.gitworktreetools.carry.CarryOverSource
import dev.gitworktreetools.carry.EffectiveCarryOverSettings
import dev.gitworktreetools.git.WorktreeOpenMode

/** Fully resolved settings after applying the global/project override cascade. */
data class EffectiveGitWorktreeToolsSettings(
    val defaultWorktreeDirectory: String,
    val defaultOpenMode: WorktreeOpenMode,
    val carryOverScope: CarryOverScope,
    val carryOverSource: CarryOverSource,
    val copyIdeaDirectory: Boolean,
    val manifestFileName: String,
    val runCarryOverOnlyWhenIdeaMissing: Boolean,
    val allowHeavyManifestPaths: Boolean,
    val showRelativeLocations: Boolean,
) {
    fun carryOverSettings(): EffectiveCarryOverSettings =
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

/** Resolves effective settings: project values win when the project opts into overrides, otherwise global. */
object GitWorktreeToolsSettingsResolver {
    fun effective(project: Project): EffectiveGitWorktreeToolsSettings {
        val global = service<GitWorktreeToolsSettings>().state
        val projectState = project.service<GitWorktreeToolsProjectSettings>().state
        return if (projectState.useProjectSettings) {
            EffectiveGitWorktreeToolsSettings(
                defaultWorktreeDirectory = projectState.defaultWorktreeDirectory,
                defaultOpenMode = projectState.defaultOpenMode,
                carryOverScope = projectState.carryOverScope,
                carryOverSource = projectState.carryOverSource,
                copyIdeaDirectory = projectState.copyIdeaDirectory,
                manifestFileName = projectState.manifestFileName,
                runCarryOverOnlyWhenIdeaMissing = projectState.runCarryOverOnlyWhenIdeaMissing,
                allowHeavyManifestPaths = projectState.allowHeavyManifestPaths,
                showRelativeLocations = projectState.showRelativeLocations,
            )
        } else {
            EffectiveGitWorktreeToolsSettings(
                defaultWorktreeDirectory = global.defaultWorktreeDirectory,
                defaultOpenMode = global.defaultOpenMode,
                carryOverScope = global.carryOverScope,
                carryOverSource = global.carryOverSource,
                copyIdeaDirectory = global.copyIdeaDirectory,
                manifestFileName = global.manifestFileName,
                runCarryOverOnlyWhenIdeaMissing = global.runCarryOverOnlyWhenIdeaMissing,
                allowHeavyManifestPaths = global.allowHeavyManifestPaths,
                showRelativeLocations = global.showRelativeLocations,
            )
        }
    }
}
