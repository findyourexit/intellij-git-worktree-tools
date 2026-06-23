package dev.tomlarcher.gitarborist.git

import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorktreePorcelainParserTest {
    private val parser = WorktreePorcelainParser()

    @Test
    fun parsesMainLinkedLockedAndPrunableWorktrees() {
        val output =
            """
            worktree /repo
            HEAD 1111111111111111111111111111111111111111
            branch refs/heads/main

            worktree /repo/.worktrees/feature-demo
            HEAD 2222222222222222222222222222222222222222
            branch refs/heads/feature/demo
            locked waiting on review

            worktree /repo/.worktrees/detached
            HEAD 3333333333333333333333333333333333333333
            detached
            prunable gitdir file points to non-existent location
            """.trimIndent()

        val parsed = parser.parse(Path("/repo"), output, Path("/repo/.worktrees/feature-demo"))

        assertEquals(3, parsed.size)
        assertTrue(parsed[0].isMain)
        assertEquals("main", parsed[0].branch)
        assertTrue(parsed[1].isCurrent)
        assertEquals("feature/demo", parsed[1].branch)
        assertTrue(parsed[1].isLocked)
        assertEquals("waiting on review", parsed[1].lockReason)
        assertTrue(parsed[2].isDetached)
        assertTrue(parsed[2].isPrunable)
        assertEquals("gitdir file points to non-existent location", parsed[2].prunableReason)
    }

    @Test
    fun stripsRemotePrefixAndKeepsDetachedBranchNull() {
        val output =
            """
            worktree /repo
            HEAD aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
            branch refs/remotes/origin/main

            worktree /repo/.worktrees/tag
            HEAD bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
            detached
            """.trimIndent()

        val parsed = parser.parse(Path("/repo"), output)

        assertEquals("origin/main", parsed[0].branch)
        assertFalse(parsed[0].isDetached)
        assertEquals(null, parsed[1].branch)
        assertTrue(parsed[1].isDetached)
    }

    @Test
    fun extractsCanonicalMainWorktreeRootFromPorcelain() {
        val output =
            """
            worktree /repo-main
            HEAD aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
            branch refs/heads/main

            worktree /repo-main/.worktrees/feature
            HEAD bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
            branch refs/heads/feature
            """.trimIndent()

        assertEquals(Path("/repo-main"), parser.mainWorktreeRoot(output))
    }

    @Test
    fun canonicalRootKeepsMainClassificationWhenCurrentRootIsLinkedWorktree() {
        val output =
            """
            worktree /repo-main
            HEAD aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
            branch refs/heads/main

            worktree /repo-main/.worktrees/feature
            HEAD bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb
            branch refs/heads/feature
            """.trimIndent()

        val parsed = parser.parse(Path("/repo-main"), output, Path("/repo-main/.worktrees/feature"))

        assertTrue(parsed[0].isMain)
        assertFalse(parsed[0].isCurrent)
        assertFalse(parsed[1].isMain)
        assertTrue(parsed[1].isCurrent)
    }
}
