package dev.tomlarcher.gitarborist.carry

import dev.tomlarcher.gitarborist.util.PathUtil
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

/**
 * Copies the entries of a [CarryOverPlan] into the target worktree. Never overwrites existing target
 * files, preserves symlinks as symlinks, refuses to follow a symlink outside the source root or write
 * outside the target root, and continues past per-file failures so a partial copy is still reported.
 */
class CarryOverExecutor(
    private val allowHeavyManifestPaths: Boolean,
) {
    fun execute(plan: CarryOverPlan): CarryOverResult {
        val messages = mutableListOf<CarryOverMessage>()
        messages += plan.rejected
        for (entry in plan.entries) {
            copyEntry(plan.sourceRoot, plan.targetRoot, entry, messages)
        }
        return CarryOverResult(plan = plan, messages = messages)
    }

    private fun copyEntry(
        sourceRoot: Path,
        targetRoot: Path,
        entry: CarryOverEntry,
        messages: MutableList<CarryOverMessage>,
    ) {
        val source = sourceRoot.resolve(entry.relativePath).normalize()
        if (!PathUtil.isInside(sourceRoot, source)) {
            messages += rejected(entry.relativePath, "Source path escapes source root")
            return
        }
        if (!source.exists(LinkOption.NOFOLLOW_LINKS)) {
            messages += skipped(entry.relativePath, "Source path does not exist")
            return
        }
        if (!Denylist.shouldCopy(entry.relativePath, allowHeavyManifestPaths)) {
            messages += rejected(entry.relativePath, "Denylist rejected path")
            return
        }

        if (source.isDirectory(LinkOption.NOFOLLOW_LINKS)) {
            copyDirectory(sourceRoot, targetRoot, source, messages)
        } else {
            copyPath(sourceRoot, targetRoot, source, messages)
        }
    }

    private fun copyDirectory(
        sourceRoot: Path,
        targetRoot: Path,
        start: Path,
        messages: MutableList<CarryOverMessage>,
    ) {
        Files.walkFileTree(
            start,
            setOf(),
            Int.MAX_VALUE,
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(
                    dir: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    val relative = sourceRoot.relativize(dir)
                    if (!Denylist.shouldCopy(relative, allowHeavyManifestPaths)) {
                        messages += rejected(relative, "Denylist rejected directory")
                        return FileVisitResult.SKIP_SUBTREE
                    }
                    val target = targetRoot.resolve(relative).normalize()
                    if (!PathUtil.isInside(targetRoot, target)) {
                        messages += rejected(relative, "Target path escapes target root")
                        return FileVisitResult.SKIP_SUBTREE
                    }
                    if (!target.exists()) target.createDirectories()
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(
                    file: Path,
                    attrs: BasicFileAttributes,
                ): FileVisitResult {
                    copyPath(sourceRoot, targetRoot, file, messages)
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(
                    file: Path,
                    exc: IOException,
                ): FileVisitResult {
                    messages += failed(sourceRoot.relativize(file), exc.message ?: exc.javaClass.simpleName)
                    return FileVisitResult.CONTINUE
                }
            },
        )
    }

    private fun copyPath(
        sourceRoot: Path,
        targetRoot: Path,
        source: Path,
        messages: MutableList<CarryOverMessage>,
    ) {
        val relative = sourceRoot.relativize(source)
        val target = targetRoot.resolve(relative).normalize()
        if (!PathUtil.isInside(targetRoot, target)) {
            messages += rejected(relative, "Target path escapes target root")
            return
        }
        if (!Denylist.shouldCopy(relative, allowHeavyManifestPaths)) {
            messages += rejected(relative, "Denylist rejected path")
            return
        }
        if (target.exists(LinkOption.NOFOLLOW_LINKS)) {
            messages += skipped(relative, "Skipped existing: ${display(relative)}")
            return
        }

        try {
            target.parent?.createDirectories()
            if (Files.isSymbolicLink(source)) {
                copySymlink(sourceRoot, source, target, relative, messages)
            } else {
                Files.copy(source, target, StandardCopyOption.COPY_ATTRIBUTES)
                messages += copied(relative, "Copied ${display(relative)}")
            }
        } catch (t: Throwable) {
            messages += failed(relative, t.message ?: t.javaClass.simpleName)
        }
    }

    private fun copySymlink(
        sourceRoot: Path,
        source: Path,
        target: Path,
        relative: Path,
        messages: MutableList<CarryOverMessage>,
    ) {
        val linkTarget = Files.readSymbolicLink(source)
        val resolved = if (linkTarget.isAbsolute) linkTarget.normalize() else source.parent.resolve(linkTarget).normalize()
        if (!PathUtil.isInside(sourceRoot, resolved)) {
            messages += skipped(relative, "Skipped symlink escaping source root: ${display(relative)}")
            return
        }
        Files.createSymbolicLink(target, linkTarget)
        messages += copied(relative, "Copied symlink ${display(relative)}")
    }

    private fun display(path: Path): String = path.normalize().joinToString("/")

    private fun copied(
        path: Path,
        message: String,
    ): CarryOverMessage = CarryOverMessage(CarryOverMessageKind.Copied, display(path), message)

    private fun skipped(
        path: Path,
        message: String,
    ): CarryOverMessage = CarryOverMessage(CarryOverMessageKind.Skipped, display(path), message)

    private fun failed(
        path: Path,
        message: String,
    ): CarryOverMessage = CarryOverMessage(CarryOverMessageKind.Failed, display(path), message)

    private fun rejected(
        path: Path,
        message: String,
    ): CarryOverMessage = CarryOverMessage(CarryOverMessageKind.Rejected, display(path), message)
}
