package dev.gitworktreetools.git

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorktreeGitServiceHelpersTest {
    @Test
    fun localBranchesWithSlashesAreDeletable() {
        assertTrue(isDeletableLocalBranch("feature/demo"))
        assertTrue(isDeletableLocalBranch("findyourexit/feature-name"))
        assertTrue(isDeletableLocalBranch("main"))
    }

    @Test
    fun blankBranchNamesAreNotDeletable() {
        assertFalse(isDeletableLocalBranch(""))
        assertFalse(isDeletableLocalBranch("   "))
    }
}
