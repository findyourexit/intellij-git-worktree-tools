package dev.tomlarcher.gitarborist.git

import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RepositoryScopeTest {
    @Test
    fun explicitProjectVcsRootWinsOverNestedRepositories() {
        val roots =
            listOf(
                Path("/project"),
                Path("/project/build/spm4Kmp"),
                Path("/project/build/checkouts/package"),
            )

        val selected =
            RepositoryScope.chooseProjectRoots(
                projectBasePath = Path("/project"),
                projectBaseVcsRoot = Path("/project"),
                repositoryRoots = roots,
            )

        assertEquals(setOf(Path("/project").toAbsolutePath().normalize()), selected)
    }

    @Test
    fun containingRepositoryFallbackUsesClosestAncestor() {
        val roots =
            listOf(
                Path("/workspace"),
                Path("/workspace/project"),
                Path("/workspace/project/build/package"),
            )

        val selected =
            RepositoryScope.chooseProjectRoots(
                projectBasePath = Path("/workspace/project/app"),
                projectBaseVcsRoot = null,
                repositoryRoots = roots,
            )

        assertEquals(setOf(Path("/workspace/project").toAbsolutePath().normalize()), selected)
    }

    @Test
    fun noBasePathLeavesScopeOpenForFallbackBehavior() {
        val selected =
            RepositoryScope.chooseProjectRoots(
                projectBasePath = null,
                projectBaseVcsRoot = null,
                repositoryRoots = listOf(Path("/repo")),
            )

        assertTrue(selected.isEmpty())
    }
}
