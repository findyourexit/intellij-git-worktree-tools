package dev.tomlarcher.gitarborist.git

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class WorktreeCacheSnapshotTest {
    @Test
    fun emptySnapshotIsUnloadedAndErrorFree() {
        val snapshot = WorktreeCacheSnapshot.Empty

        assertEquals(emptyList(), snapshot.worktrees)
        assertNull(snapshot.error)
        assertFalse(snapshot.loaded)
    }
}
