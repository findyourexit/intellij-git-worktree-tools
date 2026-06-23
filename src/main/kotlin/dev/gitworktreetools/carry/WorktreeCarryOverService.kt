package dev.gitworktreetools.carry

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import dev.gitworktreetools.git.WorktreeGitService
import dev.gitworktreetools.git.WorktreeInfo
import dev.gitworktreetools.settings.GitWorktreeToolsSettingsResolver
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
            val effective = GitWorktreeToolsSettingsResolver.effective(project)
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
            val effective = GitWorktreeToolsSettingsResolver.effective(project)
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

    fun prepareFirstOpen(
        sourceRoot: Path,
        targetRoot: Path,
        settings: EffectiveCarryOverSettings,
    ): CarryOverResult {
        if (settings.runCarryOverOnlyWhenIdeaMissing && targetRoot.resolve(".idea").exists()) {
            return CarryOverResult.skippedIdeaExists()
        }
        val ignoredPaths =
            if (settings.carryOverScope == CarryOverScope.AllIgnoredMinusDenylist) {
                project.service<WorktreeGitService>().listIgnoredPaths(sourceRoot)
            } else {
                emptyList()
            }
        val plan = CarryOverPlanner(settings).buildPlan(sourceRoot, targetRoot, ignoredPaths)
        return CarryOverExecutor(settings.allowHeavyManifestPaths).execute(plan)
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
