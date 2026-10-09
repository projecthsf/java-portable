package io.genai.java.notify

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import io.genai.java.lang.JavaFiles
import io.genai.java.sdk.JavaInterpreterActions
import io.genai.java.settings.JavaSettings
import io.genai.java.settings.JavaConfigurable
import java.util.function.Function
import javax.swing.JComponent

/**
 * On a .rs file, if no JDK is configured yet, show a banner offering to download or add
 * one. Disappears automatically once a JDK exists.
 */
class JavaSetupNotificationProvider : EditorNotificationProvider {

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile,
    ): Function<in FileEditor, out JComponent?>? {
        if (file.extension?.lowercase() !in JavaFiles.EXTENSIONS) return null
        if (JavaSettings.getInstance().defaultSdk() != null) return null

        return Function { fileEditor ->
            EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Info).apply {
                text("No JDK configured — download a portable one to run this file.")
                createActionLabel("Download JDK…") {
                    JavaInterpreterActions.downloadInteractively(project) { refresh(project) }
                }
                createActionLabel("Add from Disk…") {
                    JavaInterpreterActions.addFromDisk { refresh(project) }
                }
                createActionLabel("Settings…") {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, JavaConfigurable::class.java)
                }
            }
        }
    }

    private fun refresh(project: Project) {
        EditorNotifications.getInstance(project).updateAllNotifications()
    }
}
