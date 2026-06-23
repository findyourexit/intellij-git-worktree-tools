package dev.tomlarcher.gitarborist.ui

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.openapi.components.service
import com.intellij.openapi.vfs.VirtualFile
import dev.tomlarcher.gitarborist.git.WorktreeCacheService
import dev.tomlarcher.gitarborist.git.WorktreeInfo
import dev.tomlarcher.gitarborist.util.PathUtil
import java.nio.file.Files
import java.nio.file.Path

/**
 * Marks worktree roots in the Project View with a `worktree` (and locked/prunable) label, reading the
 * cached snapshot and falling back to a cheap on-disk gitdir check only before the cache is populated.
 */
class WorktreeProjectViewDecorator : ProjectViewNodeDecorator {
    override fun decorate(
        node: ProjectViewNode<*>,
        data: PresentationData,
    ) {
        val file = node.virtualFile?.takeIf { it.isDirectory } ?: return
        val target = runCatching { PathUtil.normalize(Path.of(file.path)) }.getOrNull() ?: return

        val snapshot = node.project?.service<WorktreeCacheService>()?.current()
        val worktree = snapshot?.worktrees?.firstOrNull { !it.isMain && PathUtil.normalize(it.path) == target }
        val label =
            when {
                worktree != null -> markerLabel(worktree)
                // Until the cache is populated (cold start) fall back to a cheap on-disk gitdir check
                // so linked worktree roots are still marked on the first Project View paint.
                snapshot?.loaded != true && isWorktreeRoot(file) -> "worktree"
                else -> null
            }
        if (label != null) {
            data.presentableText = "${data.presentableText} [$label]"
        }
    }

    private fun markerLabel(worktree: WorktreeInfo): String =
        buildList {
            add("worktree")
            if (worktree.isLocked) add("locked")
            if (worktree.isPrunable) add("prunable")
        }.joinToString(", ")

    private fun isWorktreeRoot(file: VirtualFile): Boolean {
        val gitFile = Path.of(file.path).resolve(".git")
        if (!Files.isRegularFile(gitFile)) return false
        return runCatching { Files.readString(gitFile).startsWith("gitdir:") }.getOrDefault(false)
    }
}
