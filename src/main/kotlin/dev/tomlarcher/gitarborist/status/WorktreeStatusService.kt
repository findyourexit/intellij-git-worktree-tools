package dev.tomlarcher.gitarborist.status

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import dev.tomlarcher.gitarborist.git.WorktreeGitService
import dev.tomlarcher.gitarborist.git.WorktreeInfo
import dev.tomlarcher.gitarborist.git.WorktreeStatus
import dev.tomlarcher.gitarborist.util.PathUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path

/**
 * Project service that loads status for every worktree concurrently off the UI thread and posts the
 * result back on the EDT, cancelling any previous batch so stale results never reach the view.
 */
@Service(Service.Level.PROJECT)
class WorktreeStatusService(
    private val project: Project,
    private val cs: CoroutineScope,
) {
    @Volatile
    private var inFlight: Job? = null

    /**
     * Loads status for every worktree concurrently off the UI thread, cancelling any prior in-flight
     * batch so obsolete results never reach the UI. [onResult] is invoked on the EDT with statuses
     * keyed by normalized worktree path; a cancelled batch never invokes it.
     */
    fun reload(
        worktrees: List<WorktreeInfo>,
        defaultBranch: String?,
        onResult: (Map<Path, WorktreeStatus>) -> Unit,
    ) {
        inFlight?.cancel()
        inFlight =
            cs.launch(Dispatchers.IO) {
                val git = project.service<WorktreeGitService>()
                val pairs =
                    worktrees
                        .map { worktree ->
                            async {
                                val normalized = PathUtil.normalize(worktree.path)
                                normalized to runCatching { git.status(normalized, defaultBranch, worktree.branch) }.getOrNull()
                            }
                        }.awaitAll()
                ensureActive()
                val statuses = pairs.mapNotNull { (path, status) -> status?.let { path to it } }.toMap()
                withContext(Dispatchers.EDT) { onResult(statuses) }
            }
    }
}
