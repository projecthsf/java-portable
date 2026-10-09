package io.genai.java.settings

import com.intellij.ide.actions.RevealFileAction
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.panel
import io.genai.java.lsp.CodeIntelligenceSetup
import io.genai.java.sdk.JavaInterpreterActions
import io.genai.java.sdk.JavaSdkManager
import java.io.File
import javax.swing.DefaultListModel
import javax.swing.JComponent

/**
 * Settings ▸ Languages & Frameworks ▸ Java Portable — download, add-from-disk, and remove
 * portable JDKs, plus the code-intelligence toggle. Changes apply immediately.
 */
class JavaConfigurable : Configurable {

    private val model = DefaultListModel<Sdk>()
    private val list = JBList(model)

    override fun getDisplayName(): String = "Java Portable"

    override fun createComponent(): JComponent {
        list.cellRenderer = object : ColoredListCellRenderer<Sdk>() {
            override fun customizeCellRenderer(
                list: javax.swing.JList<out Sdk>,
                sdk: Sdk,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean,
            ) {
                append("${sdk.name}   —   ${sdk.homePath ?: "?"}")
            }
        }
        reload()

        return panel {
            row {
                comment(
                    "Java Portable JDKs. Downloads are stored under " +
                        "<code>~/.java-portable</code> and shared with Java run configurations.",
                )
            }
            row {
                cell(JBScrollPane(list)).align(Align.FILL)
            }.resizableRow()
            row {
                button("Download JDK…") { downloadAction() }
                button("Add from Disk…") { addFromDiskAction() }
                button("Remove") { removeAction() }
                button("Clean Up") { cleanUpAction() }
                button("Open Folder") { openFolderAction() }
            }
            separator()
            row {
                checkBox("Code intelligence (completion, navigation, errors)")
                    .applyToComponent {
                        isSelected = JavaSettings.getInstance().codeIntelligenceEnabled
                        addActionListener {
                            JavaSettings.getInstance().codeIntelligenceEnabled = isSelected
                        }
                    }
                button(if (CodeIntelligenceSetup.isFullySetUp()) "Reinstall language server…" else "Enable Code Intelligence…") {
                    enableCodeIntelligenceAction()
                }
            }.rowComment(
                "Runs the official Java language server (Eclipse JDT LS) on the selected JDK — fully " +
                    "offline. Also installs the free <b>LSP4IJ</b> plugin (one click, may prompt a restart).",
            )
        }
    }

    private fun reload() {
        model.clear()
        JavaSdkManager.listSdks().forEach { model.addElement(it) }
    }

    private fun downloadAction() = JavaInterpreterActions.downloadInteractively(null) { reload() }

    private fun addFromDiskAction() = JavaInterpreterActions.addFromDisk { reload() }

    private fun removeAction() {
        val sdk = list.selectedValue ?: return
        val home = sdk.homePath
        val managed = home != null &&
            File(home).absolutePath.startsWith(JavaSdkManager.downloadRoot().toFile().absolutePath + File.separator)

        val deleteFiles: Boolean
        if (managed) {
            val answer = Messages.showYesNoCancelDialog(
                "Remove JDK \"${sdk.name}\"?\n\nIt was downloaded to $home.",
                "Remove JDK",
                "Remove and Delete Files",
                "Remove Only",
                "Cancel",
                Messages.getQuestionIcon(),
            )
            when (answer) {
                Messages.YES -> deleteFiles = true
                Messages.NO -> deleteFiles = false
                else -> return
            }
        } else {
            val ok = Messages.showYesNoDialog(
                "Remove JDK \"${sdk.name}\"?\n\n(Files on disk are left untouched.)",
                "Remove JDK",
                Messages.getQuestionIcon(),
            )
            if (ok != Messages.YES) return
            deleteFiles = false
        }

        JavaSdkManager.remove(sdk, deleteFiles)
        reload()
    }

    private fun cleanUpAction() {
        val result = JavaSdkManager.cleanUp()
        reload()
        Messages.showInfoMessage(
            "Removed ${result.removed} missing JDK(s); registered ${result.added} orphaned install(s).",
            "Clean Up JDKs",
        )
    }

    private fun enableCodeIntelligenceAction() {
        val project = ProjectManager.getInstance().openProjects.firstOrNull()
        if (project == null) {
            Messages.showInfoMessage(
                "Open a project first, then use the banner on a .rs file to enable code intelligence.",
                "Java Code Intelligence",
            )
            return
        }
        // Turning it on from here clears any earlier "Don't show again" on the editor banner,
        // so the two controls can't end up disagreeing.
        JavaSettings.getInstance().codeIntelligencePromptDismissed = false
        CodeIntelligenceSetup.enable(project, force = CodeIntelligenceSetup.isFullySetUp()) {}
    }

    private fun openFolderAction() {
        val sdk = list.selectedValue
        val dir = sdk?.homePath?.let { File(it) } ?: JavaSdkManager.downloadRoot().toFile()
        if (dir.exists()) RevealFileAction.openDirectory(dir)
    }

    override fun isModified(): Boolean = false

    override fun apply() {
        // Actions apply immediately; nothing to commit here.
    }
}
