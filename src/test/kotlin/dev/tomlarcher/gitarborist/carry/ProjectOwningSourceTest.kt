package dev.tomlarcher.gitarborist.carry

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertSame

class ProjectOwningSourceTest {
    private data class FakeProject(
        val name: String,
        val basePath: String?,
    )

    @Test
    fun picksProjectWhoseBasePathEqualsSourceRootSkippingOtherWorktrees() {
        val main = FakeProject("main", "/repo")
        val worktree = FakeProject("worktree", "/repo/.worktrees/feature")

        val owner = projectOwningSource(Path.of("/repo"), listOf(worktree, main)) { it.basePath }

        assertSame(main, owner)
    }

    @Test
    fun picksNestedWorktreeNotItsParentRepository() {
        val main = FakeProject("main", "/repo")
        val worktree = FakeProject("worktree", "/repo/.worktrees/feature")

        val owner = projectOwningSource(Path.of("/repo/.worktrees/feature"), listOf(main, worktree)) { it.basePath }

        assertSame(worktree, owner)
    }

    @Test
    fun containmentDoesNotMatch() {
        val main = FakeProject("main", "/repo")

        // The source is inside /repo but is not itself an open project, so nothing should match.
        val owner = projectOwningSource(Path.of("/repo/module"), listOf(main)) { it.basePath }

        assertNull(owner)
    }

    @Test
    fun nullBasePathsAreSkipped() {
        val defaultProject = FakeProject("default", null)
        val main = FakeProject("main", "/repo")

        val owner = projectOwningSource(Path.of("/repo"), listOf(defaultProject, main)) { it.basePath }

        assertSame(main, owner)
    }

    @Test
    fun noOpenProjectMatchesReturnsNull() {
        val main = FakeProject("main", "/repo")

        val owner = projectOwningSource(Path.of("/elsewhere"), listOf(main)) { it.basePath }

        assertNull(owner)
    }
}
