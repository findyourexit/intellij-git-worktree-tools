package dev.tomlarcher.gitarborist.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import dev.tomlarcher.gitarborist.git.RemovalPreflight
import dev.tomlarcher.gitarborist.git.WorktreeInfo
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.Action
import javax.swing.BoxLayout
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea

/**
 * Remove-worktree confirmation. Surfaces the removal preflight warnings (dirty, unpushed, locked) and
 * offers force removal plus optional deletion of the backing local branch.
 */
class RemoveWorktreeDialog(
    project: Project,
    private val worktree: WorktreeInfo,
    private val preflight: RemovalPreflight,
) : DialogWrapper(project) {
    private val branchName = worktree.branch?.takeIf { !worktree.isMain && it.isNotBlank() }
    private val forceBox = JCheckBox("Force remove dirty, locked, or untracked/ignored files", preflight.dirty || preflight.locked)
    private val deleteBranchBox = JCheckBox(branchName?.let { "Also delete local branch '$it'" } ?: "Also delete local branch", false)

    init {
        title = "Remove Worktree"
        okAction.putValue(Action.NAME, "Remove")
        init()
    }

    val force: Boolean
        get() = forceBox.isSelected

    val deleteBranch: Boolean
        get() = branchName != null && deleteBranchBox.isSelected

    override fun createCenterPanel(): JComponent =
        JPanel(BorderLayout(8, 8)).apply {
            add(JLabel("Remove this worktree?"), BorderLayout.NORTH)
            add(
                JTextArea(message()).apply {
                    isEditable = false
                    isOpaque = false
                    lineWrap = true
                    wrapStyleWord = true
                },
                BorderLayout.CENTER,
            )
            add(options(), BorderLayout.SOUTH)
            preferredSize = Dimension(640, 210)
        }

    private fun options(): JComponent =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(forceBox)
            if (branchName != null) add(deleteBranchBox)
        }

    private fun message(): String =
        buildString {
            append(worktree.path)
            append("\n\n")
            if (preflight.dirty) append("Dirty files detected.\n")
            if (preflight.unpushed) append("Unpushed commits detected.\n")
            if (preflight.locked) append("Worktree is locked.\n")
            append("Git may also require force when ignored or generated files remain in the worktree directory.")
        }
}
