package dev.tomlarcher.gitarborist.settings

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import dev.tomlarcher.gitarborist.carry.CarryOverScope
import dev.tomlarcher.gitarborist.carry.CarryOverSource
import dev.tomlarcher.gitarborist.carry.EffectiveCarryOverSettings

/** Fully resolved settings after applying the global/project override cascade. */
data class EffectiveGitArboristSettings(
    val defaultWorktreeDirectory: String,
    val openAfterCreate: Boolean,
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
object GitArboristSettingsResolver {
    fun effective(project: Project): EffectiveGitArboristSettings {
        val global = service<GitArboristSettings>().state
        val projectState = project.service<GitArboristProjectSettings>().state
        return if (projectState.useProjectSettings) {
            EffectiveGitArboristSettings(
                defaultWorktreeDirectory = projectState.defaultWorktreeDirectory,
                openAfterCreate = projectState.openAfterCreate,
                carryOverScope = projectState.carryOverScope,
                carryOverSource = projectState.carryOverSource,
                copyIdeaDirectory = projectState.copyIdeaDirectory,
                manifestFileName = projectState.manifestFileName,
                runCarryOverOnlyWhenIdeaMissing = projectState.runCarryOverOnlyWhenIdeaMissing,
                allowHeavyManifestPaths = projectState.allowHeavyManifestPaths,
                showRelativeLocations = projectState.showRelativeLocations,
            )
        } else {
            EffectiveGitArboristSettings(
                defaultWorktreeDirectory = global.defaultWorktreeDirectory,
                openAfterCreate = global.openAfterCreate,
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
