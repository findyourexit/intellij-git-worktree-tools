package dev.tomlarcher.gitarborist.carry

import com.intellij.configurationStore.saveSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getOpenedProjects
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import dev.tomlarcher.gitarborist.git.WorktreeGitService
import dev.tomlarcher.gitarborist.git.WorktreeInfo
import dev.tomlarcher.gitarborist.settings.GitArboristSettingsResolver
import dev.tomlarcher.gitarborist.util.PathUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.exists

/**
 * Project service that prepares a new worktree before its first open by copying curated setup files
 * from the configured source root, then refreshes the VFS so the opening project sees them. First
 * open is detected via the target `.idea/` directory; an explicit reapply path re-runs on demand.
 */
@Service(Service.Level.PROJECT)
class WorktreeCarryOverService(
    private val project: Project,
    private val cs: CoroutineScope,
) {
    /** Runs carry-over for a worktree's first open, skipping when the target already has `.idea/`. */
    suspend fun prepareFirstOpen(worktree: WorktreeInfo): CarryOverResult =
        withContext(Dispatchers.IO) {
            val effective = GitArboristSettingsResolver.effective(project)
            val settings = effective.carryOverSettings()
            val targetRoot = worktree.path
            if (settings.runCarryOverOnlyWhenIdeaMissing && targetRoot.resolve(".idea").exists()) {
                return@withContext CarryOverResult.skippedIdeaExists()
            }

            val sourceRoot = resolveSourceRoot(settings.carryOverSource, worktree)
            val result = prepareFirstOpen(sourceRoot, targetRoot, settings)
            refreshTarget(targetRoot)
            result
        }

    /** Re-runs carry-over on demand, bypassing the first-open guard so existing worktrees can refresh setup. */
    suspend fun reapplyCarryOver(worktree: WorktreeInfo): CarryOverResult =
        withContext(Dispatchers.IO) {
            val effective = GitArboristSettingsResolver.effective(project)
            val settings = effective.carryOverSettings().copy(runCarryOverOnlyWhenIdeaMissing = false)
            val sourceRoot = resolveSourceRoot(settings.carryOverSource, worktree)
            val result = prepareFirstOpen(sourceRoot, worktree.path, settings)
            refreshTarget(worktree.path)
            result
        }

    fun reapplyCarryOverAsync(
        worktree: WorktreeInfo,
        onComplete: (CarryOverResult) -> Unit,
    ) {
        cs.launch {
            val result = reapplyCarryOver(worktree)
            ApplicationManager.getApplication().invokeLater {
                onComplete(result)
            }
        }
    }

    suspend fun prepareFirstOpen(
        sourceRoot: Path,
        targetRoot: Path,
        settings: EffectiveCarryOverSettings,
    ): CarryOverResult {
        if (settings.runCarryOverOnlyWhenIdeaMissing && targetRoot.resolve(".idea").exists()) {
            return CarryOverResult.skippedIdeaExists()
        }
        flushSourceProjectSettings(sourceRoot)
        val ignoredPaths =
            if (settings.carryOverScope == CarryOverScope.AllIgnoredMinusDenylist) {
                project.service<WorktreeGitService>().listIgnoredPaths(sourceRoot)
            } else {
                emptyList()
            }
        val plan = CarryOverPlanner(settings).buildPlan(sourceRoot, targetRoot, ignoredPaths)
        return CarryOverExecutor(settings.allowHeavyManifestPaths).execute(plan)
    }

    /**
     * Flushes the open project that backs [sourceRoot] to disk before the copy reads its `.idea/`.
     * The platform debounces `PersistentStateComponent` writes and otherwise flushes only on frame
     * deactivation or project close, so settings edited just before a worktree is opened — notably
     * Settings | Tools entries from third-party plugins (Detekt, Develocity, KtLint) persisted to
     * the project's `.idea/` XML files — would be carried over stale or missing. No-op when the
     * source project is not open, leaving the on-disk `.idea/` authoritative.
     */
    private suspend fun flushSourceProjectSettings(sourceRoot: Path) {
        val owner = projectOwningSource(sourceRoot, getOpenedProjects().toList()) { it.basePath } ?: return
        saveSettings(owner, forceSavingAllSettings = true)
    }

    private fun resolveSourceRoot(
        source: CarryOverSource,
        worktree: WorktreeInfo,
    ): Path =
        when (source) {
            CarryOverSource.MainWorktree -> worktree.repositoryRoot
            CarryOverSource.CurrentProject -> project.basePath?.let(Path::of) ?: worktree.repositoryRoot
        }

    private fun refreshTarget(targetRoot: Path) {
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(targetRoot)
        if (virtualFile != null) {
            VfsUtil.markDirtyAndRefresh(true, true, true, virtualFile)
        }
    }
}

/**
 * The candidate whose base directory is exactly [sourceRoot] — the open project whose in-memory state
 * backs the `.idea/` about to be copied. Matches on the exact normalized path, never containment, so a
 * sibling worktree opened under the same repository is never mistaken for the source. Candidates with a
 * null base path are skipped.
 */
internal fun <T> projectOwningSource(
    sourceRoot: Path,
    candidates: List<T>,
    basePathOf: (T) -> String?,
): T? =
    candidates.firstOrNull { candidate ->
        basePathOf(candidate)?.let { PathUtil.samePath(Path.of(it), sourceRoot) } == true
    }
