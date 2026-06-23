package dev.gitworktreetools.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextField
import dev.gitworktreetools.git.AddWorktreeRequest
import dev.gitworktreetools.git.WorktreeInfo
import dev.gitworktreetools.git.WorktreeOpenMode
import dev.gitworktreetools.util.PathUtil
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.nio.file.Path
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.event.DocumentEvent

/** Modal list picker used by menu actions to choose a worktree to operate on. */
class WorktreePickerDialog(
    project: Project,
    private val worktrees: List<WorktreeInfo>,
    titleText: String,
) : DialogWrapper(project) {
    private val list = JBList(worktrees.map(::label))

    init {
        title = titleText
        init()
    }

    val selectedWorktree: WorktreeInfo?
        get() = worktrees.getOrNull(list.selectedIndex)

    override fun createCenterPanel(): JComponent =
        JScrollPane(list).apply {
            preferredSize = Dimension(720, 280)
        }

    companion object {
        private fun label(info: WorktreeInfo): String =
            buildString {
                append(info.branch ?: info.commitHash.take(12))
                append(" — ")
                append(info.path)
                if (info.isMain) append(" [main]")
                if (info.isLocked) append(" [locked]")
            }
    }
}

/**
 * Create-worktree dialog. Collects the starting point, optional new branch, target path, detach
 * toggle, and after-create open mode, defaulting the target from the branch name until the user edits
 * the target manually.
 */
class CreateWorktreeDialog(
    project: Project,
    repositoryRoot: Path,
    defaultOpenMode: WorktreeOpenMode,
    private val worktreeDirectory: String,
) : DialogWrapper(project) {
    private val repositoryRootPath = PathUtil.normalize(repositoryRoot)
    private val sourceRefField = JBTextField("HEAD")
    private val branchField = JBTextField("")
    private val targetPathField = JBTextField(defaultTargetFor("worktree").toString())
    private val detachBox = JCheckBox("Create detached worktree", false)
    private val afterCreateBox = JComboBox(AfterCreateMode.entries.toTypedArray())
    private var updatingTarget = false
    private var targetEditedByUser = false

    init {
        title = "Create Worktree"
        branchField.emptyText.text = "e.g. findyourexit/feature-name"
        sourceRefField.emptyText.text = "HEAD, branch, tag, or commit SHA"
        afterCreateBox.selectedItem = AfterCreateMode.from(defaultOpenMode)
        branchField.document.addDocumentListener(
            object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    if (!targetEditedByUser) {
                        updateTargetFromBranch()
                    }
                }
            },
        )
        targetPathField.document.addDocumentListener(
            object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    if (!updatingTarget) targetEditedByUser = true
                }
            },
        )
        init()
    }

    val selectedOpenMode: WorktreeOpenMode?
        get() = (afterCreateBox.selectedItem as AfterCreateMode).openMode

    fun request(): AddWorktreeRequest =
        AddWorktreeRequest(
            repositoryRoot = repositoryRootPath,
            targetPath = Path.of(targetPathField.text.trim()),
            sourceRef = sourceRefField.text.trim().ifBlank { "HEAD" },
            branchName = branchField.text.trim().ifBlank { null },
            createBranch = branchField.text.isNotBlank() && !detachBox.isSelected,
            detach = detachBox.isSelected,
        )

    override fun createCenterPanel(): JComponent =
        JPanel(GridBagLayout()).apply {
            addRow("Starting point:", sourceRefField, row = 0)
            addRow("New branch:", branchField, row = 1)
            addRow("Target path:", targetPathField, row = 2)
            addRow("Checkout mode:", detachBox, row = 3)
            addRow("After create:", afterCreateBox, row = 4)
            preferredSize = Dimension(760, 190)
        }

    private fun JPanel.addRow(
        label: String,
        field: JComponent,
        row: Int,
    ) {
        add(
            JLabel(label),
            GridBagConstraints().apply {
                gridx = 0
                gridy = row
                anchor = GridBagConstraints.LINE_END
                insets = Insets(4, 0, 4, 12)
            },
        )
        add(
            field,
            GridBagConstraints().apply {
                gridx = 1
                gridy = row
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                anchor = GridBagConstraints.LINE_START
                insets = Insets(4, 0, 4, 0)
            },
        )
    }

    private fun updateTargetFromBranch() {
        updatingTarget = true
        targetPathField.text = defaultTargetFor(branchField.text.ifBlank { "worktree" }).toString()
        updatingTarget = false
    }

    private fun defaultTargetFor(branchName: String): Path = PathUtil.defaultWorktreeTarget(repositoryRootPath, worktreeDirectory, branchName)
}

/** What to do with a worktree immediately after it is created. */
enum class AfterCreateMode(
    private val label: String,
    val openMode: WorktreeOpenMode?,
) {
    DoNotOpen("Do not open", null),
    OpenWithIde("Open...", WorktreeOpenMode.IdeDefault),
    NewWindow("Open in new window", WorktreeOpenMode.NewWindow),
    OpenAsTab("Open as tab", WorktreeOpenMode.AttachToCurrentFrame),
    ReplaceCurrent("Replace current project", WorktreeOpenMode.ReplaceCurrentProject),
    ;

    override fun toString(): String = label

    companion object {
        fun from(mode: WorktreeOpenMode): AfterCreateMode =
            when (mode) {
                WorktreeOpenMode.IdeDefault -> OpenWithIde
                WorktreeOpenMode.NewWindow -> NewWindow
                WorktreeOpenMode.AttachToCurrentFrame -> OpenAsTab
                WorktreeOpenMode.ReplaceCurrentProject -> ReplaceCurrent
                WorktreeOpenMode.AskEachTime -> NewWindow
            }
    }
}
