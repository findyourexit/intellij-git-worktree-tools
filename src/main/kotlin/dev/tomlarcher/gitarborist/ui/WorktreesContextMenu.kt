package dev.tomlarcher.gitarborist.ui

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.ui.PopupHandler
import com.intellij.ui.components.JBList
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

/**
 * Right-click context menu for the worktrees list. A right-click first selects the row under the
 * cursor so the chosen action targets it, then a [PopupHandler]-backed menu mirrors the
 * tool-window/menu operations. Operations are passed as callbacks so this stays UI-glue only.
 */
internal class WorktreesContextMenu(
    private val open: () -> Unit,
    private val lock: () -> Unit,
    private val unlock: () -> Unit,
    private val move: () -> Unit,
    private val reapplyCarryOver: () -> Unit,
    private val remove: () -> Unit,
    private val selectedIsLocked: () -> Boolean?,
) {
    fun installOn(list: JBList<WorktreeRow>) {
        list.addMouseListener(
            object : MouseAdapter() {
                override fun mousePressed(event: MouseEvent) = selectRowForPopup(list, event)

                override fun mouseReleased(event: MouseEvent) = selectRowForPopup(list, event)
            },
        )
        PopupHandler.installPopupMenu(list, buildGroup(), "WorktreesPanelContextMenu")
    }

    private fun buildGroup(): DefaultActionGroup =
        DefaultActionGroup().apply {
            add(menuAction("Open...", open))
            addSeparator()
            add(conditionalAction("Lock...", { selectedIsLocked() == false }, lock))
            add(conditionalAction("Unlock", { selectedIsLocked() == true }, unlock))
            add(menuAction("Move...", move))
            add(menuAction("Reapply Carry-over...", reapplyCarryOver))
            addSeparator()
            add(menuAction("Remove...", remove))
        }
}

private fun selectRowForPopup(
    list: JBList<WorktreeRow>,
    event: MouseEvent,
) {
    if (!event.isPopupTrigger) return
    val index = list.locationToIndex(event.point)
    if (index >= 0 && list.getCellBounds(index, index)?.contains(event.point) == true) {
        list.selectedIndex = index
    }
}

private fun menuAction(
    text: String,
    perform: () -> Unit,
): DumbAwareAction =
    object : DumbAwareAction(text) {
        override fun actionPerformed(e: AnActionEvent) = perform()

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
    }

private fun conditionalAction(
    text: String,
    visible: () -> Boolean,
    perform: () -> Unit,
): DumbAwareAction =
    object : DumbAwareAction(text) {
        override fun actionPerformed(e: AnActionEvent) = perform()

        override fun update(e: AnActionEvent) {
            e.presentation.isEnabledAndVisible = visible()
        }

        override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
    }
