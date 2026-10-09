package io.genai.java.notify

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import com.intellij.ui.EditorNotifications
import io.genai.java.lang.JavaFileType
import io.genai.java.lang.JavaFiles
import io.genai.java.lsp.CodeIntelligenceSetup
import io.genai.java.settings.JavaConfigurable
import io.genai.java.settings.JavaSettings
import io.genai.java.sdk.JavaSdkType
import java.util.function.Function
import javax.swing.JComponent

/**
 * On a `.rs` file where our language layer is active (IDEs without native Java support), offer a
 * single click to turn on code intelligence (download the language server + install LSP4IJ). Disappears once both are
 * present. On RubyMine / Ultimate the native Java support handles this, so we stay quiet.
 */
class JavaCodeIntelligenceNotificationProvider : EditorNotificationProvider {

    override fun collectNotificationData(
        project: Project,
        file: VirtualFile,
    ): Function<in FileEditor, out JComponent?>? {
        if (file.extension?.lowercase() !in JavaFiles.EXTENSIONS) return null
        if (FileTypeManager.getInstance().getFileTypeByExtension("rs") != JavaFileType) return null

        val settings = JavaSettings.getInstance()
        if (!settings.codeIntelligenceEnabled) return null
        if (settings.codeIntelligencePromptDismissed) return null
        val hasToolchain = settings.defaultSdk()?.homePath
            ?.let { JavaSdkType.findJavaExecutable(it) } != null
        if (!hasToolchain) return null

        if (CodeIntelligenceSetup.isFullySetUp()) return null

        return Function { fileEditor ->
            EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Info).apply {
                text("Turn on Java code intelligence — completion, go-to-definition and error highlighting.")
                createActionLabel("Enable code intelligence") {
                    CodeIntelligenceSetup.enable(project) {
                        EditorNotifications.getInstance(project).updateAllNotifications()
                    }
                }
                createActionLabel("Settings…") {
                    ShowSettingsUtil.getInstance().showSettingsDialog(project, JavaConfigurable::class.java)
                }
                // For people who only ever want to run .rs files. Hides the prompt for good;
                // Settings ▸ Java Portable still has the button to turn code intelligence on later.
                createActionLabel("Don't show again") {
                    JavaSettings.getInstance().codeIntelligencePromptDismissed = true
                    EditorNotifications.getInstance(project).updateAllNotifications()
                }
            }
        }
    }
}
