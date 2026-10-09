package io.genai.java.sdk

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/** Lets the user pick which JDK to download. */
class JavaDownloadDialog(releases: List<JavaRelease>) : DialogWrapper(true) {
    private val combo = ComboBox(releases.toTypedArray())

    var selected: JavaRelease? = null
        private set

    init {
        title = "Download JDK"
        init()
    }

    override fun createCenterPanel(): JComponent = panel {
        row("Interpreter:") {
            cell(combo).align(AlignX.FILL)
        }
        row {
            comment("Downloaded to ~/.java-portable and registered as a Java SDK.")
        }
    }

    override fun doOKAction() {
        selected = combo.selectedItem as? JavaRelease
        super.doOKAction()
    }
}
