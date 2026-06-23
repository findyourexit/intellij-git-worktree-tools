package dev.gitworktreetools.open

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import dev.gitworktreetools.util.PathUtil
import java.nio.file.Path

/**
 * Application service that maps a normalized worktree path to its open [Project], cross-checking the
 * live open-projects list so focus and open decisions never target a stale window.
 */
@Service(Service.Level.APP)
class WorktreeWindowRegistry {
    fun findOpenProject(path: Path): Project? {
        val target = PathUtil.normalize(path)
        return ProjectManager.getInstance().openProjects.firstOrNull { project ->
            val basePath = project.basePath?.let(Path::of)
            basePath != null && PathUtil.normalize(basePath) == target
        }
    }
}
