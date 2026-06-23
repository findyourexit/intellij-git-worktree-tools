package dev.tomlarcher.gitarborist.carry

import dev.tomlarcher.gitarborist.util.PathUtil
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * Builds a [CarryOverPlan] from effective settings: the source `.idea/` directory, the manifest file,
 * and (for the all-ignored scope) git-ignored paths. Validates every candidate for source-root
 * containment and the sensitive/heavy denylists, recording rejections instead of copying them.
 */
class CarryOverPlanner(
    private val settings: EffectiveCarryOverSettings,
) {
    fun buildPlan(
        sourceRoot: Path,
        targetRoot: Path,
        ignoredPaths: List<Path> = emptyList(),
    ): CarryOverPlan {
        val normalizedSource = PathUtil.normalize(sourceRoot)
        val normalizedTarget = PathUtil.normalize(targetRoot)
        val entries = mutableListOf<CarryOverEntry>()
        val rejected = mutableListOf<CarryOverMessage>()

        if (settings.copyIdeaDirectory && settings.carryOverScope != CarryOverScope.ManifestOnly) {
            val idea = normalizedSource.resolve(".idea")
            if (idea.exists() && idea.isDirectory()) {
                entries += CarryOverEntry(normalizedSource.relativize(idea), CarryOverReason.IdeaDirectory)
            }
        }

        val manifest = normalizedSource.resolve(settings.manifestFileName)
        if (manifest.exists()) {
            readManifest(normalizedSource, manifest, entries, rejected)
        }

        if (settings.carryOverScope == CarryOverScope.AllIgnoredMinusDenylist) {
            addIgnoredEntries(normalizedSource, ignoredPaths, entries, rejected)
        }

        return CarryOverPlan(
            sourceRoot = normalizedSource,
            targetRoot = normalizedTarget,
            entries = entries.distinctBy { it.relativePath.normalize().toString() },
            rejected = rejected,
        )
    }

    private fun readManifest(
        sourceRoot: Path,
        manifest: Path,
        entries: MutableList<CarryOverEntry>,
        rejected: MutableList<CarryOverMessage>,
    ) {
        Files.readAllLines(manifest, StandardCharsets.UTF_8).forEachIndexed { index, raw ->
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
            val relative = Path.of(line)
            val display = line
            when {
                relative.isAbsolute -> rejected += rejected(display, "Absolute manifest path at line ${index + 1}")
                !PathUtil.isInside(sourceRoot, sourceRoot.resolve(relative).normalize()) -> {
                    rejected += rejected(display, "Manifest path escapes source root at line ${index + 1}")
                }
                Denylist.isSensitive(relative) -> rejected += rejected(display, "Sensitive denylist rejected manifest path")
                Denylist.isHeavy(relative) && !settings.allowHeavyManifestPaths -> {
                    rejected += rejected(display, "Heavy-path denylist rejected manifest path")
                }
                else -> entries += CarryOverEntry(relative.normalize(), CarryOverReason.Manifest)
            }
        }
    }

    private fun addIgnoredEntries(
        sourceRoot: Path,
        ignoredPaths: List<Path>,
        entries: MutableList<CarryOverEntry>,
        rejected: MutableList<CarryOverMessage>,
    ) {
        for (raw in ignoredPaths) {
            if (raw.isAbsolute) continue
            val relative = raw.normalize()
            val display = relative.joinToString("/")
            when {
                display.isEmpty() -> Unit
                !PathUtil.isInside(sourceRoot, sourceRoot.resolve(relative).normalize()) ->
                    rejected += rejected(display, "Ignored path escapes source root")
                Denylist.isSensitive(relative) -> rejected += rejected(display, "Sensitive denylist rejected ignored path")
                Denylist.isHeavy(relative) && !settings.allowHeavyManifestPaths ->
                    rejected += rejected(display, "Heavy-path denylist rejected ignored path")
                else -> entries += CarryOverEntry(relative, CarryOverReason.AllIgnored)
            }
        }
    }

    private fun rejected(
        path: String,
        message: String,
    ): CarryOverMessage =
        CarryOverMessage(
            kind = CarryOverMessageKind.Rejected,
            relativePath = path,
            message = message,
        )
}

/**
 * Path policy for carry-over. Sensitive paths (secrets, tokens, `.env`, `.idea/httpRequests`) are
 * never copied; heavy build and dependency directories are skipped unless the user opts in.
 */
object Denylist {
    private val sensitiveContains = listOf("secret", "secrets", "credential", "credentials", "token", "tokens", "private")
    private val heavyRoots =
        setOf(
            "node_modules",
            ".gradle",
            "build",
            "dist",
            "out",
            "target",
            ".venv",
            "venv",
            "__pycache__",
            ".next",
            ".turbo",
            ".cache",
        )

    fun isSensitive(relativePath: Path): Boolean {
        val normalized = normalize(relativePath)
        val fileName = normalized.substringAfterLast('/')
        val lower = normalized.lowercase()
        return normalized == ".env" ||
            fileName.startsWith(".env.") ||
            fileName.endsWith(".local") ||
            fileName == "settings.local.json" ||
            sensitiveContains.any { lower.contains(it) } ||
            normalized == ".idea/httpRequests" ||
            normalized.startsWith(".idea/httpRequests/")
    }

    fun isHeavy(relativePath: Path): Boolean = relativePath.normalize().any { segment -> segment.toString() in heavyRoots }

    fun shouldCopy(
        relativePath: Path,
        allowHeavy: Boolean,
    ): Boolean = !isSensitive(relativePath) && (allowHeavy || !isHeavy(relativePath))

    private fun normalize(path: Path): String = path.normalize().joinToString("/")
}
