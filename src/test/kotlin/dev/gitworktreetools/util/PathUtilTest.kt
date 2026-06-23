package dev.gitworktreetools.util

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PathUtilTest {
    @Test
    fun sanitizeRefNameProducesSafeDirectoryName() {
        assertEquals("feature-demo-work", PathUtil.sanitizeRefName("feature/demo work", "fallback"))
        assertEquals("bug-fix", PathUtil.sanitizeRefName("///bug///fix///", "fallback"))
        assertEquals("fallback", PathUtil.sanitizeRefName("///   ///", "fallback"))
        assertEquals("release-1.0", PathUtil.sanitizeRefName("release:1.0", "fallback"))
    }

    @Test
    fun lowerKebabRefNameLowercasesBranchPathSegments() {
        assertEquals("findyourexit-feature-name", PathUtil.lowerKebabRefName("findyourexit/Feature Name", "fallback"))
    }

    @Test
    fun isInsideAcceptsRootAndChildrenOnly() {
        val root = createTempDirectory("gwt-root")
        val child = root.resolve("nested/file.txt")
        val sibling = root.resolveSibling(root.fileName.toString() + "-sibling")

        assertTrue(PathUtil.isInside(root, root))
        assertTrue(PathUtil.isInside(root, child))
        assertFalse(PathUtil.isInside(root, sibling))
        assertFalse(PathUtil.isInside(root, root.resolve("..").resolve(sibling.fileName).normalize()))
    }

    @Test
    fun shortestUniqueUsesShortestDistinguishingSuffix() {
        val root = createTempDirectory("gwt-paths")
        val first = root.resolve("alpha/app")
        val second = root.resolve("beta/app")
        val third = root.resolve("gamma/tool")

        val labels = PathUtil.shortestUnique(listOf(first, second, third))

        assertEquals("alpha${Path.of("x").fileSystem.separator}app", labels[PathUtil.normalize(first)])
        assertEquals("beta${Path.of("x").fileSystem.separator}app", labels[PathUtil.normalize(second)])
        assertEquals("tool", labels[PathUtil.normalize(third)])
    }

    @Test
    fun existingParentWalksUpToExistingDirectory() {
        val root = createTempDirectory("gwt-parent")
        val existing = root.resolve("existing").createDirectories()
        val missing = existing.resolve("a/b/c.txt")

        assertEquals(existing, PathUtil.existingParent(missing))
    }

    @Test
    fun defaultWorktreeTargetUsesConfiguredDirectory() {
        assertEquals(
            Path.of("/workspace/repo/.worktrees/feature-demo"),
            PathUtil.defaultWorktreeTarget(Path.of("/workspace/repo"), ".worktrees", "feature/Demo"),
        )
    }

    @Test
    fun defaultWorktreeTargetAcceptsAbsoluteDirectory() {
        assertEquals(
            Path.of("/tmp/wt/feature-demo"),
            PathUtil.defaultWorktreeTarget(Path.of("/workspace/repo"), "/tmp/wt", "feature/Demo"),
        )
    }

    @Test
    fun defaultWorktreeTargetFallsBackToSiblingWhenDirectoryBlank() {
        assertEquals(
            Path.of("/workspace/repo.feature-demo"),
            PathUtil.defaultWorktreeTarget(Path.of("/workspace/repo"), "  ", "feature/Demo"),
        )
    }
}
