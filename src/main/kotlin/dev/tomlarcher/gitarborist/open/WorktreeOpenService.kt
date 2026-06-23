package dev.tomlarcher.gitarborist.open

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import dev.tomlarcher.gitarborist.carry.WorktreeCarryOverService
import dev.tomlarcher.gitarborist.git.WorktreeInfo
import dev.tomlarcher.gitarborist.git.WorktreeOpenMode
import dev.tomlarcher.gitarborist.ui.CarryOverResultDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Project service that opens worktree projects. Focuses an already-open window instead of duplicating
 * it, runs carry-over before every first open, gates opening on unrecoverable copy failures, and
 * dispatches the chosen [WorktreeOpenMode]. [WorktreeOpenMode.AskEachTime] must be resolved by the
 * caller before reaching this service.
 */
@Service(Service.Level.PROJECT)
class WorktreeOpenService(
    private val project: Project,
    private val cs: CoroutineScope,
) {
    private val windowRegistry: WorktreeWindowRegistry
        get() = service()

    fun openWorktreeAsync(
        worktree: WorktreeInfo,
        mode: WorktreeOpenMode,
    ) {
        cs.launch { openWorktree(worktree, mode) }
    }

    suspend fun openWorktree(
        worktree: WorktreeInfo,
        mode: WorktreeOpenMode,
    ) {
        val existing = windowRegistry.findOpenProject(worktree.path)
        if (existing != null) {
            focus(existing)
            return
        }

        val carryOverResult = project.service<WorktreeCarryOverService>().prepareFirstOpen(worktree)
        if (!CarryOverResultDialog.confirmOpen(project, carryOverResult)) return

        when (mode) {
            WorktreeOpenMode.IdeDefault ->
                ProjectUtil.openOrImportAsync(
                    worktree.path,
                    OpenProjectTask.build(),
                )
            WorktreeOpenMode.NewWindow ->
                ProjectUtil.openOrImportAsync(
                    worktree.path,
                    OpenProjectTask.build().withForceOpenInNewFrame(true),
                )
            WorktreeOpenMode.AttachToCurrentFrame ->
                ProjectUtil.openOrImportAsync(
                    worktree.path,
                    OpenProjectTask.build().copy(forceReuseFrame = true),
                )
            WorktreeOpenMode.ReplaceCurrentProject ->
                ProjectUtil.openOrImportAsync(
                    worktree.path,
                    OpenProjectTask.build().withProjectToClose(project),
                )
            WorktreeOpenMode.AskEachTime -> error("Resolve AskEachTime before calling openWorktree")
        }
    }

    private suspend fun focus(project: Project) =
        withContext(Dispatchers.EDT) {
            WindowManager.getInstance().getFrame(project)?.toFront()
        }
}
