package dev.tomlarcher.gitarborist.git

import dev.tomlarcher.gitarborist.util.PathUtil
import java.nio.file.Path

/**
 * Selects the single owning Git root for a project so worktree listing ignores nested dependency or
 * build repositories. Prefers the project's base VCS root; otherwise the deepest repository that
 * still contains the project base path.
 */
internal object RepositoryScope {
    fun chooseProjectRoots(
        projectBasePath: Path?,
        projectBaseVcsRoot: Path?,
        repositoryRoots: Collection<Path>,
    ): Set<Path> {
        val normalizedRepositories = repositoryRoots.mapTo(linkedSetOf(), PathUtil::normalize)
        val normalizedVcsRoot = projectBaseVcsRoot?.let(PathUtil::normalize)
        if (normalizedVcsRoot != null && normalizedVcsRoot in normalizedRepositories) {
            return setOf(normalizedVcsRoot)
        }

        val normalizedBasePath = projectBasePath?.let(PathUtil::normalize) ?: return emptySet()
        return normalizedRepositories
            .filter { repositoryRoot -> PathUtil.isInside(repositoryRoot, normalizedBasePath) }
            .maxByOrNull { it.nameCount }
            ?.let { setOf(it) }
            .orEmpty()
    }
}
