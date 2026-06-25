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
import dev.tomlarcher.gitarborist.ui.CarryOverResultDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Project service that opens worktree projects. Focuses an already-open window instead of opening a
 * duplicate, runs carry-over before every first open, and gates opening on unrecoverable copy
 * failures.
 *
 * Opening delegates to the IDE's standard project-open flow ([OpenProjectTask.build]): the user gets
 * the platform's own prompt (this window / new window / cancel) and the IDE's "open project in a new
 * window vs. tab" preference is honored, rather than the plugin forcing a specific frame.
 */
@Service(Service.Level.PROJECT)
class WorktreeOpenService(
    private val project: Project,
    private val cs: CoroutineScope,
) {
    private val windowRegistry: WorktreeWindowRegistry
        get() = service()

    fun openWorktreeAsync(worktree: WorktreeInfo) {
        cs.launch { openWorktree(worktree) }
    }

    suspend fun openWorktree(worktree: WorktreeInfo) {
        val existing = windowRegistry.findOpenProject(worktree.path)
        if (existing != null) {
            focus(existing)
            return
        }

        val carryOverResult = project.service<WorktreeCarryOverService>().prepareFirstOpen(worktree)
        if (!CarryOverResultDialog.confirmOpen(project, carryOverResult)) return

        ProjectUtil.openOrImportAsync(worktree.path, OpenProjectTask.build())
    }

    private suspend fun focus(project: Project) =
        withContext(Dispatchers.EDT) {
            WindowManager.getInstance().getFrame(project)?.toFront()
        }
}
