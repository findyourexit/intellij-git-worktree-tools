package dev.tomlarcher.gitarborist.open

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import dev.tomlarcher.gitarborist.git.WorktreeCacheService

/** Warms the worktree cache on project open so menus and decorations have data without an EDT Git call. */
class WorktreeProjectActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.service<WorktreeCacheService>().refresh()
    }
}
