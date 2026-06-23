package dev.gitworktreetools.git

/**
 * Decides whether a worktree is safe to delete from cheap, public Git facts. A clean working tree is
 * mandatory; beyond that any single cleanup signal is sufficient: the same commit as the target, the
 * branch HEAD being an ancestor of the target, an empty `target...branch` diff, or matching trees.
 */
internal object SafeDeleteEvaluator {
    fun isSafe(
        cleanWorkingTree: Boolean,
        sameCommit: Boolean,
        branchIsAncestor: Boolean,
        noAddedChanges: Boolean,
        treesMatch: Boolean,
    ): Boolean = cleanWorkingTree && (sameCommit || branchIsAncestor || noAddedChanges || treesMatch)
}
