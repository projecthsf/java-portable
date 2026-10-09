package io.genai.java.run

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import io.genai.java.sdk.JavaSdkType
import javax.swing.JComponent

class JavaSettingsEditor(project: Project) : SettingsEditor<JavaRunConfiguration>() {

    private val sdkCombo = ComboBox<String>()
    private val javaField = TextFieldWithBrowseButton()
    private val scriptField = TextFieldWithBrowseButton()
    private val envComponent = EnvironmentVariablesComponent()

    init {
        sdkCombo.addItem(NONE)
        ProjectJdkTable.getInstance().getSdksOfType(JavaSdkType.getInstance()).forEach {
            sdkCombo.addItem(it.name)
        }
        scriptField.addBrowseFolderListener(
            TextBrowseFolderListener(
                FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor().withTitle("Select Java File"),
                project,
            ),
        )
        javaField.addBrowseFolderListener(
            TextBrowseFolderListener(
                FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor().withTitle("Select java Executable"),
                project,
            ),
        )
    }

    override fun createEditor(): JComponent = panel {
        row("Java SDK:") {
            cell(sdkCombo).align(AlignX.FILL)
        }
        row("Or java executable:") {
            cell(javaField).align(AlignX.FILL)
        }.rowComment("Overrides the SDK above when set.")
        row("Java file:") {
            cell(scriptField).align(AlignX.FILL)
        }
        row {
            cell(envComponent).align(AlignX.FILL)
        }
    }

    override fun resetEditorFrom(s: JavaRunConfiguration) {
        sdkCombo.selectedItem = s.sdkName?.takeIf { it.isNotBlank() } ?: NONE
        javaField.text = s.javaPath.orEmpty()
        scriptField.text = s.scriptPath.orEmpty()
        envComponent.envData = EnvironmentVariablesData.create(s.envs, s.passParentEnvs)
    }

    override fun applyEditorTo(s: JavaRunConfiguration) {
        val selected = sdkCombo.selectedItem as? String
        s.sdkName = if (selected == null || selected == NONE) "" else selected
        s.javaPath = javaField.text.trim()
        s.scriptPath = scriptField.text.trim()
        val data = envComponent.envData
        s.envs = HashMap(data.envs)
        s.passParentEnvs = data.isPassParentEnvs
    }

    companion object {
        private const val NONE = "<none>"
    }
}
