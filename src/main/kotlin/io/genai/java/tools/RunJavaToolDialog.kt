package io.genai.java.tools

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * Prompts for a JDK tool to run. The useful ones for someone without a build system: compile a
 * file, inspect a class, open a REPL, or check what the JDK actually is.
 */
class RunJavaToolDialog(project: Project) : DialogWrapper(project) {

    private val commandCombo = ComboBox(COMMANDS)
    private val argField = JBTextField()
    private lateinit var argRow: Row

    init {
        title = "Run JDK Tool"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = panel {
            row("Tool:") { cell(commandCombo).align(AlignX.FILL) }
            row("Argument:") {
                cell(argField).align(AlignX.FILL)
                    .comment("a file or class name, e.g. Hello.java")
            }.also { argRow = it }
        }
        commandCombo.addActionListener { syncArgRow() }
        syncArgRow()
        return panel
    }

    override fun getPreferredFocusedComponent(): JComponent = commandCombo

    private fun syncArgRow() = argRow.visible(needsArg())

    private fun needsArg(): Boolean = (commandCombo.selectedItem as? String) in setOf("javac", "javap", "jar")

    /** `[tool, args…]`; [RunJavaToolAction] resolves the tool against the portable JDK. */
    val commandArgs: List<String>
        get() {
            val tool = commandCombo.selectedItem as? String ?: return emptyList()
            val arg = argField.text.trim()
            return if (needsArg() && arg.isNotEmpty()) listOf(tool) + arg.split(Regex("\\s+")) else listOf(tool)
        }

    companion object {
        private val COMMANDS = arrayOf("javac", "jshell", "javap", "jar", "java -version")
    }
}
