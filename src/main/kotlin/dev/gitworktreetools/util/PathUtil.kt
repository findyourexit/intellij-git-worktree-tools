package dev.gitworktreetools.util

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.name

/** Path normalization, containment checks, ref-name sanitization, and worktree-location helpers. */
object PathUtil {
    private val reserved = Regex("[\\\\/:*?\"<>|]+")
    private val whitespace = Regex("\\s+")
    private val repeatedDash = Regex("-+")

    fun normalize(path: Path): Path = path.toAbsolutePath().normalize()

    fun samePath(
        first: Path,
        second: Path,
    ): Boolean = normalize(first) == normalize(second)

    fun isInside(
        root: Path,
        candidate: Path,
    ): Boolean {
        val normalizedRoot = normalize(root)
        val normalizedCandidate = normalize(candidate)
        return normalizedCandidate == normalizedRoot || normalizedCandidate.startsWith(normalizedRoot)
    }

    fun sanitizeRefName(
        ref: String,
        fallback: String,
    ): String {
        val sanitized =
            ref
                .replace(reserved, "-")
                .replace(whitespace, "-")
                .replace(repeatedDash, "-")
                .trim('-')
        return sanitized.ifBlank { fallback }
    }

    fun lowerKebabRefName(
        ref: String,
        fallback: String,
    ): String = sanitizeRefName(ref, fallback).lowercase()

    /**
     * Default create-dialog target for a new worktree. When [worktreeDirectory] is set, the worktree
     * lands in `<worktreeDirectory>/<kebab-branch>` (relative entries resolve against [repositoryRoot]);
     * otherwise it falls back to the sibling layout `<repo-parent>/<repo-name>.<kebab-branch>`.
     */
    fun defaultWorktreeTarget(
        repositoryRoot: Path,
        worktreeDirectory: String,
        branchName: String,
    ): Path {
        val suffix = lowerKebabRefName(branchName, "worktree")
        val directory = worktreeDirectory.trim()
        if (directory.isNotEmpty()) {
            val configured = Path.of(directory)
            val base = if (configured.isAbsolute) configured else repositoryRoot.resolve(configured)
            return normalize(base).resolve(suffix)
        }
        val repoName = repositoryRoot.fileName?.toString() ?: "worktree"
        return repositoryRoot.parent?.resolve("$repoName.$suffix") ?: repositoryRoot.resolveSibling("$repoName.$suffix")
    }

    fun shortestUnique(paths: Collection<Path>): Map<Path, String> {
        if (paths.isEmpty()) return emptyMap()
        val normalized = paths.map(::normalize)
        return normalized.associateWith { path ->
            val names = generateSequence(path) { it.parent }.map { it.name }.toList()
            for (depth in 1..names.size) {
                val candidate = names.take(depth).asReversed().joinToString(path.fileSystem.separator)
                val duplicates =
                    normalized.count { other ->
                        val otherNames = generateSequence(other) { it.parent }.map { it.name }.take(depth).toList()
                        otherNames == names.take(depth)
                    }
                if (duplicates == 1) return@associateWith candidate
            }
            path.toString()
        }
    }

    fun existingParent(path: Path): Path? {
        var current = path
        while (!current.exists()) {
            current = current.parent ?: return null
        }
        return current
    }
}
