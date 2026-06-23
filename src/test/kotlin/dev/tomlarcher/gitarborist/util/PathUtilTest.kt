package dev.tomlarcher.gitarborist.util

import java.nio.file.Path
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
