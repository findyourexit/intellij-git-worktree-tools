package dev.tomlarcher.gitarborist.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WorktreesRepositoryTitleTest {
    @Test
    fun remoteDisplayNameHandlesHttpsSshAndScpForms() {
        assertEquals("findyourexit/spm4Kmp", remoteDisplayName("https://github.com/findyourexit/spm4Kmp.git"))
        assertEquals("findyourexit/spm4Kmp", remoteDisplayName("git@github.com:findyourexit/spm4Kmp.git"))
        assertEquals("findyourexit/spm4Kmp", remoteDisplayName("ssh://git@github.com/findyourexit/spm4Kmp"))
    }

    @Test
    fun remoteDisplayNameNullWhenNoOwnerSegment() {
        assertNull(remoteDisplayName("https://example.com/repo"))
    }

    @Test
    fun parseGitConfigPrefersOriginOverOtherRemotes() {
        val config =
            """
            [core]
            repositoryformatversion = 0
            [remote "upstream"]
            url = git@github.com:upstream/spm4Kmp.git
            [remote "origin"]
            url = git@github.com:findyourexit/spm4Kmp.git
            fetch = +refs/heads/*:refs/remotes/origin/*
            [branch "main"]
            remote = origin
            """.trimIndent()

        assertEquals("git@github.com:findyourexit/spm4Kmp.git", parseGitConfigRemoteUrl(config))
    }

    @Test
    fun parseGitConfigFallsBackToFirstRemoteAndNullWhenNone() {
        val onlyFork =
            """
            [remote "fork"]
            url = https://example.com/a/b.git
            """.trimIndent()

        assertEquals("https://example.com/a/b.git", parseGitConfigRemoteUrl(onlyFork))
        assertNull(parseGitConfigRemoteUrl("[core]\n\tbare = false\n"))
    }
}
