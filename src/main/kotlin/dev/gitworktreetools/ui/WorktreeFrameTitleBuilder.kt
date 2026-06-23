package dev.gitworktreetools.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.impl.FrameTitleBuilder
import java.nio.file.Files
import java.nio.file.Path

/** Appends the worktree directory name to the IDE frame title when the open project is a linked worktree. */
class WorktreeFrameTitleBuilder : FrameTitleBuilder() {
    override fun getProjectTitle(project: Project): String {
        val base = project.name
        val label = project.basePath?.let { worktreeLabel(Path.of(it)) }
        return if (label == null) base else "$base [$label]"
    }

    override fun getFileTitle(
        project: Project,
        file: VirtualFile,
    ): String = file.presentableUrl

    private fun worktreeLabel(root: Path): String? {
        val gitFile = root.resolve(".git")
        val content =
            if (Files.isRegularFile(gitFile)) {
                runCatching { Files.readString(gitFile) }.getOrNull()
            } else {
                null
            }
        return root.fileName?.toString()?.takeIf { content?.startsWith("gitdir:") == true }
    }
}
