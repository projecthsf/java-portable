package io.genai.java.lsp

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages

/**
 * One-click setup for Java code intelligence. Two prerequisites the user would otherwise have to
 * piece together:
 *   1. Eclipse JDT LS — downloaded and unpacked into ~/.java-portable/jdtls.
 *   2. LSP4IJ — a JetBrains Marketplace plugin. We open the Plugins screen so the user installs
 *      it with one click, then restarts.
 *
 * Programmatic plugin install is intentionally avoided: the APIs for it are in the frontend-split
 * `app-client` module or marked `@ApiStatus.Internal` — both fail the JetBrains Plugin Verifier.
 * Opening the Plugins screen via its public configurable id is the supported, verifier-clean path.
 */
object CodeIntelligenceSetup {

    const val LSP4IJ_ID = "com.redhat.devtools.lsp4ij"
    private const val PLUGINS_CONFIGURABLE_ID = "preferences.pluginManager"

    fun isLsp4ijInstalled(): Boolean =
        PluginManager.isPluginInstalled(PluginId.getId(LSP4IJ_ID))

    /** Both prerequisites present — code intelligence can actually run. */
    fun isFullySetUp(): Boolean = isLsp4ijInstalled() && JdtlsManager.isInstalled()

    fun enable(project: Project, onChanged: () -> Unit) = enable(project, force = false, onChanged)

    /**
     * @param force re-download jdtls even when it is already present. This is what the settings
     *   panel's "Reinstall" button needs; without it the call would early-return and the button
     *   would silently do nothing.
     */
    fun enable(project: Project, force: Boolean, onChanged: () -> Unit) {
        if (force || !JdtlsManager.isInstalled()) {
            if (JdtlsManager.defaultHome() == null) {
                Messages.showErrorDialog(
                    project,
                    "No JDK configured yet. Download one in Settings \u25B8 Java Portable first — " +
                        "the language server runs on it.",
                    "Java Code Intelligence",
                )
                return
            }
            var failure: Exception? = null
            ProgressManager.getInstance().run(
                object : Task.Modal(project, if (force) "Reinstalling language server\u2026" else "Installing language server\u2026", true) {
                    override fun run(indicator: ProgressIndicator) {
                        indicator.isIndeterminate = false
                        try {
                            JdtlsManager.install(indicator)
                        } catch (e: Exception) {
                            failure = e
                        }
                    }
                },
            )
            failure?.let {
                Messages.showErrorDialog(
                    project,
                    "Failed to install the language server: ${it.message}",
                    "Java Code Intelligence",
                )
                return
            }
            onChanged()
        }

        if (!isLsp4ijInstalled()) {
            Messages.showInfoMessage(
                project,
                "The language server is ready. One step left: in the Plugins window that opens, go to " +
                    "<b>Marketplace</b>, search <b>LSP4IJ</b>, click <b>Install</b>, then restart the IDE.",
                "Enable Java Code Intelligence",
            )
            ShowSettingsUtil.getInstance().showSettingsDialog(project, PLUGINS_CONFIGURABLE_ID)
        } else {
            onChanged()
        }
    }
}
