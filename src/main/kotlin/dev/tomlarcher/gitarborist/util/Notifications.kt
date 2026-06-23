package dev.tomlarcher.gitarborist.util

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

/** Thin wrapper over the plugin's balloon notification group for info and error messages. */
object Notifications {
    private const val GROUP_ID = "Git Arborist"

    fun info(
        project: Project,
        title: String,
        content: String,
    ) {
        notify(project, title, content, NotificationType.INFORMATION)
    }

    fun error(
        project: Project,
        title: String,
        content: String,
    ) {
        notify(project, title, content, NotificationType.ERROR)
    }

    private fun notify(
        project: Project,
        title: String,
        content: String,
        type: NotificationType,
    ) {
        NotificationGroupManager
            .getInstance()
            .getNotificationGroup(GROUP_ID)
            .createNotification(title, content, type)
            .notify(project)
    }
}
