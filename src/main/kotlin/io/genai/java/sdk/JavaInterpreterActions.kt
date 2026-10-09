package io.genai.java.sdk

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.file.Files

/**
 * Interactive install flows shared by the settings panel and the editor banner.
 * All entry points run on the EDT; [onComplete] fires on the EDT after registration.
 */
object JavaInterpreterActions {

    fun downloadInteractively(project: Project?, onComplete: (Sdk?) -> Unit) {
        val releases = JavaDownloads.fetchAvailableWithProgress(project)
        if (releases.isEmpty()) {
            Messages.showInfoMessage("No portable JDKs are listed for this OS.", "Download JDK")
            return
        }
        val dialog = JavaDownloadDialog(releases)
        if (!dialog.showAndGet()) return
        val release = dialog.selected ?: return
        val home = JavaSdkManager.plannedHome(release.version)

        ProgressManager.getInstance().run(object : Task.Modal(project, "Downloading Temurin ${release.version}", true) {
            override fun run(indicator: ProgressIndicator) {
                JavaSdkDownloadTask(release, home).doDownload(indicator)
            }

            override fun onSuccess() {
                val sdk = JavaSdkManager.registerFromHome(home.toString())
                if (sdk == null) {
                    Messages.showErrorDialog(
                        "Download finished but no cargo executable was found under\n$home",
                        "Download JDK",
                    )
                }
                onComplete(sdk)
            }

            override fun onThrowable(error: Throwable) {
                Messages.showErrorDialog(error.message ?: error.toString(), "Download JDK Failed")
            }
        })
    }

    fun addFromDisk(onComplete: (Sdk?) -> Unit) {
        val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            .withTitle("Select JDK Directory")
            .withDescription("Pick a JDK folder (contains cargo/bin/cargo, or bin/cargo).")
        val root = JavaSdkManager.downloadRoot()
        Files.createDirectories(root)
        val toSelect = LocalFileSystem.getInstance().findFileByNioFile(root)
        val chosen = FileChooser.chooseFile(descriptor, null, toSelect) ?: return
        val home = chosen.path
        if (JavaSdkType.findJavaExecutable(home) == null) {
            Messages.showErrorDialog("No cargo executable found under\n$home", "Add JDK")
            return
        }
        onComplete(JavaSdkManager.registerFromHome(home))
    }
}
