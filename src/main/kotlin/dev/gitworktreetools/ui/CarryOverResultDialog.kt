package dev.gitworktreetools.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import dev.gitworktreetools.carry.CarryOverMessage
import dev.gitworktreetools.carry.CarryOverMessageKind
import dev.gitworktreetools.carry.CarryOverResult
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * Reports carry-over outcome with copied/skipped/rejected/failed counts and the first failures or
 * rejections. When copies failed, the user must choose Open Anyway or Cancel Open before the worktree
 * opens; otherwise it is purely informational.
 */
class CarryOverResultDialog(
    project: Project,
    private val result: CarryOverResult,
    private val requiresOpenDecision: Boolean,
    titleText: String,
) : DialogWrapper(project) {
    init {
        title = titleText
        if (requiresOpenDecision) {
            okAction.putValue(Action.NAME, "Open Anyway")
            cancelAction.putValue(Action.NAME, "Cancel Open")
        } else {
            okAction.putValue(Action.NAME, "Continue")
        }
        init()
    }

    override fun createCenterPanel(): JComponent =
        JPanel(BorderLayout(8, 8)).apply {
            add(JLabel(summary()), BorderLayout.NORTH)
            add(JBScrollPane(JBTextArea(details()).apply { isEditable = false }), BorderLayout.CENTER)
            preferredSize = Dimension(720, 320)
        }

    override fun createActions(): Array<Action> =
        if (requiresOpenDecision) {
            arrayOf(okAction, cancelAction)
        } else {
            arrayOf(okAction)
        }

    private fun summary(): String =
        "Copied: ${result.copiedCount}, skipped: ${result.skippedCount}, rejected: ${result.rejectedCount}, failed: ${result.failedCount}"

    private fun details(): String {
        val important =
            result.messages.filter {
                it.kind == CarryOverMessageKind.Failed || it.kind == CarryOverMessageKind.Rejected
            }
        if (important.isEmpty()) {
            return "No failures or rejected paths."
        }
        return important.take(MAX_DETAILS).joinToString("\n") { it.format() } +
            if (important.size > MAX_DETAILS) "\n… ${important.size - MAX_DETAILS} more" else ""
    }

    private fun CarryOverMessage.format(): String = "${kind.name}: $relativePath — $message"

    companion object {
        private const val MAX_DETAILS = 30

        fun confirmOpen(
            project: Project,
            result: CarryOverResult,
        ): Boolean {
            if (!result.shouldReportBeforeOpen) return true
            return showOnEdt {
                CarryOverResultDialog(
                    project = project,
                    result = result,
                    requiresOpenDecision = result.requiresOpenDecision,
                    titleText = if (result.requiresOpenDecision) "Carry-over Incomplete" else "Carry-over Warnings",
                ).showAndGet()
            }
        }

        fun showResult(
            project: Project,
            result: CarryOverResult,
            title: String = "Carry-over Result",
        ) {
            showOnEdt {
                CarryOverResultDialog(
                    project = project,
                    result = result,
                    requiresOpenDecision = false,
                    titleText = title,
                ).showAndGet()
            }
        }

        private fun showOnEdt(action: () -> Boolean): Boolean {
            val application = ApplicationManager.getApplication()
            if (application.isDispatchThread) return action()
            val result = AtomicBoolean(false)
            application.invokeAndWait {
                result.set(action())
            }
            return result.get()
        }
    }
}
