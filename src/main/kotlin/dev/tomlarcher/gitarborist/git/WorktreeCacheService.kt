package dev.tomlarcher.gitarborist.git

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Project service holding the most recent worktree-list snapshot so menus and Project View context
 * actions can render without running Git on the EDT. The snapshot is only ever refreshed from a
 * background thread.
 */
@Service(Service.Level.PROJECT)
class WorktreeCacheService(
    private val project: Project,
    private val cs: CoroutineScope,
) {
    @Volatile
    private var snapshot: WorktreeCacheSnapshot = WorktreeCacheSnapshot.Empty

    fun current(): WorktreeCacheSnapshot = snapshot

    fun refreshAsync(onComplete: (WorktreeCacheSnapshot) -> Unit = {}) {
        cs.launch {
            val refreshed = refresh()
            ApplicationManager.getApplication().invokeLater {
                onComplete(refreshed)
            }
        }
    }

    suspend fun refresh(): WorktreeCacheSnapshot =
        withContext(Dispatchers.IO) {
            refreshBlocking()
        }

    fun refreshBlocking(): WorktreeCacheSnapshot =
        runCatching { project.service<WorktreeGitService>().listWorktrees() }
            .fold(
                onSuccess = { worktrees ->
                    WorktreeCacheSnapshot(worktrees = worktrees, error = null, loaded = true)
                },
                onFailure = { error ->
                    WorktreeCacheSnapshot(
                        worktrees = emptyList(),
                        error = error.message ?: error.javaClass.simpleName,
                        loaded = true,
                    )
                },
            ).also { snapshot = it }
}

/** Immutable worktree-list snapshot; [loaded] separates "not yet refreshed" from a genuinely empty list. */
data class WorktreeCacheSnapshot(
    val worktrees: List<WorktreeInfo>,
    val error: String?,
    val loaded: Boolean,
) {
    companion object {
        val Empty = WorktreeCacheSnapshot(worktrees = emptyList(), error = null, loaded = false)
    }
}
