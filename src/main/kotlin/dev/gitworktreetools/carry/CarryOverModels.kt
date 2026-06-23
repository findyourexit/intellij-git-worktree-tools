package dev.gitworktreetools.carry

import java.nio.file.Path

/** Breadth of files copied into a new worktree before its first open. */
enum class CarryOverScope {
    Curated,
    AllIgnoredMinusDenylist,
    ManifestOnly,
}

/** Which root supplies the files that carry over into a new worktree. */
enum class CarryOverSource { MainWorktree, CurrentProject }

/** The entries to copy from [sourceRoot] to [targetRoot], plus paths rejected during planning. */
data class CarryOverPlan(
    val sourceRoot: Path,
    val targetRoot: Path,
    val entries: List<CarryOverEntry>,
    val rejected: List<CarryOverMessage> = emptyList(),
)

/** A single source-relative path to copy, tagged with why it was included. */
data class CarryOverEntry(
    val relativePath: Path,
    val reason: CarryOverReason,
)

/** Why a [CarryOverEntry] was added to the plan. */
enum class CarryOverReason { IdeaDirectory, Manifest, AllIgnored }

/** Classification of a single [CarryOverMessage]. */
enum class CarryOverMessageKind { Copied, Skipped, Failed, Rejected }

/** Overall carry-over severity derived from the message kinds. */
enum class CarryOverResultSeverity { Clean, Skipped, Warning, Failure }

/** One line of carry-over outcome: what happened to [relativePath] and why. */
data class CarryOverMessage(
    val kind: CarryOverMessageKind,
    val relativePath: String,
    val message: String,
)

/** Aggregated carry-over outcome with per-kind counts and open-gating decisions. */
data class CarryOverResult(
    val plan: CarryOverPlan?,
    val messages: List<CarryOverMessage>,
    val skippedBecauseIdeaExists: Boolean = false,
    val userChoseOpenAnyway: Boolean = true,
) {
    val copiedCount: Int = messages.count { it.kind == CarryOverMessageKind.Copied }
    val skippedCount: Int = messages.count { it.kind == CarryOverMessageKind.Skipped }
    val failedCount: Int = messages.count { it.kind == CarryOverMessageKind.Failed }
    val rejectedCount: Int = messages.count { it.kind == CarryOverMessageKind.Rejected }
    val severity: CarryOverResultSeverity =
        when {
            failedCount > 0 -> CarryOverResultSeverity.Failure
            rejectedCount > 0 -> CarryOverResultSeverity.Warning
            copiedCount == 0 && skippedCount > 0 -> CarryOverResultSeverity.Skipped
            else -> CarryOverResultSeverity.Clean
        }
    val requiresOpenDecision: Boolean = failedCount > 0
    val shouldReportBeforeOpen: Boolean = failedCount > 0 || rejectedCount > 0
    val hasBlockingError: Boolean = requiresOpenDecision

    companion object {
        fun skippedIdeaExists(): CarryOverResult =
            CarryOverResult(
                plan = null,
                messages =
                    listOf(
                        CarryOverMessage(
                            kind = CarryOverMessageKind.Skipped,
                            relativePath = ".idea",
                            message = "Carry-over skipped: target already has .idea/",
                        ),
                    ),
                skippedBecauseIdeaExists = true,
            )
    }
}

/** Resolved carry-over configuration handed to the planner and executor. */
data class EffectiveCarryOverSettings(
    val defaultWorktreeDirectory: String = ".worktrees",
    val carryOverScope: CarryOverScope = CarryOverScope.Curated,
    val carryOverSource: CarryOverSource = CarryOverSource.MainWorktree,
    val copyIdeaDirectory: Boolean = true,
    val manifestFileName: String = ".worktree-copy",
    val runCarryOverOnlyWhenIdeaMissing: Boolean = true,
    val allowHeavyManifestPaths: Boolean = false,
)
