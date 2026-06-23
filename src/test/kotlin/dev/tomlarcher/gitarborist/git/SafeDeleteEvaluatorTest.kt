package dev.tomlarcher.gitarborist.git

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SafeDeleteEvaluatorTest {
    @Test
    fun dirtyBranchesAreNeverSafeToDelete() {
        assertFalse(
            SafeDeleteEvaluator.isSafe(
                cleanWorkingTree = false,
                sameCommit = true,
                branchIsAncestor = true,
                noAddedChanges = true,
                treesMatch = true,
            ),
        )
    }

    @Test
    fun sameCommitIsSafeWhenClean() {
        assertTrue(
            SafeDeleteEvaluator.isSafe(
                cleanWorkingTree = true,
                sameCommit = true,
                branchIsAncestor = false,
                noAddedChanges = false,
                treesMatch = false,
            ),
        )
    }

    @Test
    fun ancestorIsSafeWhenClean() {
        assertTrue(
            SafeDeleteEvaluator.isSafe(
                cleanWorkingTree = true,
                sameCommit = false,
                branchIsAncestor = true,
                noAddedChanges = false,
                treesMatch = false,
            ),
        )
    }

    @Test
    fun emptyThreeDotDiffIsSafeWhenClean() {
        assertTrue(
            SafeDeleteEvaluator.isSafe(
                cleanWorkingTree = true,
                sameCommit = false,
                branchIsAncestor = false,
                noAddedChanges = true,
                treesMatch = false,
            ),
        )
    }

    @Test
    fun matchingTreesAreSafeWhenClean() {
        assertTrue(
            SafeDeleteEvaluator.isSafe(
                cleanWorkingTree = true,
                sameCommit = false,
                branchIsAncestor = false,
                noAddedChanges = false,
                treesMatch = true,
            ),
        )
    }

    @Test
    fun unmergedContentIsNotSafe() {
        assertFalse(
            SafeDeleteEvaluator.isSafe(
                cleanWorkingTree = true,
                sameCommit = false,
                branchIsAncestor = false,
                noAddedChanges = false,
                treesMatch = false,
            ),
        )
    }
}
