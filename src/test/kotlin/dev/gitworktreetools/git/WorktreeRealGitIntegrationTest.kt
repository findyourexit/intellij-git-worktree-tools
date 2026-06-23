package dev.gitworktreetools.git

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WorktreeRealGitIntegrationTest {
    private val parser = WorktreePorcelainParser()

    @Test
    fun realGitAddListLockUnlockRemoveRoundTrip() {
        val root = createTempDirectory("gwt-real-git")
        git(root, "init")
        git(root, "config", "user.email", "test@example.com")
        git(root, "config", "user.name", "Git Worktree Tools Test")
        root.resolve("README.md").writeText("baseline\n")
        git(root, "add", "README.md")
        git(root, "commit", "-m", "baseline")

        val worktree = root.resolveSibling("${root.fileName}-feature-demo").normalize()
        git(root, "worktree", "add", "-b", "feature/demo", worktree.toString(), "HEAD")

        val listed = parseList(root)
        assertEquals(2, listed.size)
        assertTrue(listed.any { it.isMain && it.path == root.toRealPath() })
        val linked = listed.firstOrNull { it.branch == "feature/demo" }
        assertNotNull(linked)
        assertTrue(linked.path.exists())
        assertFalse(linked.isMain)

        git(root, "worktree", "lock", "--reason", "waiting on review", worktree.toString())
        val locked = parseList(root).first { it.branch == "feature/demo" }
        assertTrue(locked.isLocked)
        assertEquals("waiting on review", locked.lockReason)

        git(root, "worktree", "unlock", worktree.toString())
        val unlocked = parseList(root).first { it.branch == "feature/demo" }
        assertFalse(unlocked.isLocked)

        git(root, "worktree", "remove", worktree.toString())
        val afterRemove = parseList(root)
        assertEquals(1, afterRemove.size)
        assertTrue(afterRemove.single().isMain)
        assertFalse(worktree.exists())
    }

    private fun parseList(root: Path): List<WorktreeInfo> {
        val realRoot = root.toRealPath()
        return parser.parse(
            repositoryRoot = realRoot,
            porcelain = git(root, "worktree", "list", "--porcelain"),
            currentRoot = realRoot,
        )
    }

    private fun git(
        cwd: Path,
        vararg args: String,
    ): String {
        cwd.createDirectories()
        val process =
            ProcessBuilder(listOf("git") + args)
                .directory(cwd.toFile())
                .redirectErrorStream(true)
                .start()
        val output = process.inputStream.bufferedReader().readText()
        val exit = process.waitFor()
        check(exit == 0) { "git ${args.joinToString(" ")} failed with $exit:\n$output" }
        return output
    }
}
