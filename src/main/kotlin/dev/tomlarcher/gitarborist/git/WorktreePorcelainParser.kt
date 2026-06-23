package dev.tomlarcher.gitarborist.git

import dev.tomlarcher.gitarborist.util.PathUtil
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Parses `git worktree list --porcelain` output into [WorktreeInfo] rows. Implemented locally
 * because Git4Idea's own worktree parser is `internal` and not part of the public API.
 */
class WorktreePorcelainParser {
    fun parse(
        repositoryRoot: Path,
        porcelain: String,
        currentRoot: Path? = repositoryRoot,
    ): List<WorktreeInfo> {
        val blocks =
            porcelain
                .lineSequence()
                .fold(mutableListOf<MutableList<String>>(mutableListOf())) { acc, raw ->
                    val line = raw.trimEnd('\r')
                    if (line.isBlank()) {
                        if (acc.last().isNotEmpty()) acc.add(mutableListOf())
                    } else {
                        acc.last().add(line)
                    }
                    acc
                }.filter { it.isNotEmpty() }

        return blocks.mapNotNull { parseBlock(repositoryRoot, currentRoot, it) }
    }

    fun mainWorktreeRoot(porcelain: String): Path? =
        porcelain
            .lineSequence()
            .map { it.trimEnd('\r') }
            .firstOrNull { it.startsWith("worktree ") }
            ?.substringAfter(' ')
            ?.let(::Path)

    private fun parseBlock(
        repositoryRoot: Path,
        currentRoot: Path?,
        lines: List<String>,
    ): WorktreeInfo? {
        var path: Path? = null
        var head = ""
        var branch: String? = null
        var locked = false
        var lockReason: String? = null
        var prunable = false
        var prunableReason: String? = null
        var detached = false
        var bare = false

        for (line in lines) {
            val key = line.substringBefore(' ')
            val value = line.substringAfter(' ', missingDelimiterValue = "")
            when (key) {
                "worktree" -> path = Path(value)
                "HEAD" -> head = value
                "branch" -> branch = normalizeBranch(value)
                "detached" -> detached = true
                "bare" -> bare = true
                "locked" -> {
                    locked = true
                    lockReason = value.ifBlank { null }
                }
                "prunable" -> {
                    prunable = true
                    prunableReason = value.ifBlank { null }
                }
            }
        }

        val worktreePath = path ?: return null
        val normalizedRepo = PathUtil.normalize(repositoryRoot)
        val normalizedWorktree = PathUtil.normalize(worktreePath)
        val normalizedCurrent = currentRoot?.let(PathUtil::normalize)

        return WorktreeInfo(
            repositoryRoot = normalizedRepo,
            path = normalizedWorktree,
            branch = branch,
            commitHash = head,
            isMain = normalizedWorktree == normalizedRepo || bare,
            isCurrent = normalizedCurrent != null && normalizedWorktree == normalizedCurrent,
            isLocked = locked,
            lockReason = lockReason,
            isPrunable = prunable,
            prunableReason = prunableReason,
            isDetached = detached || branch == null,
        )
    }

    private fun normalizeBranch(ref: String): String =
        when {
            ref.startsWith("refs/heads/") -> ref.removePrefix("refs/heads/")
            ref.startsWith("refs/remotes/") -> ref.removePrefix("refs/remotes/")
            else -> ref
        }
}
