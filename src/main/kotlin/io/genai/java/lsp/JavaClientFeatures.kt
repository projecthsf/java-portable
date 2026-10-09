package io.genai.java.lsp

import com.intellij.openapi.vfs.VirtualFile
import com.redhat.devtools.lsp4ij.client.features.LSPClientFeatures
import io.genai.java.settings.JavaSettings

/**
 * Gates when the Java language server is active. LSP4IJ calls [isEnabled] before starting the
 * server for a file, so this enforces the "Code intelligence" toggle and the prerequisites
 * (a JDK is configured, jdtls is downloaded). Returning false keeps the server dormant — which
 * matters more here than in the sibling plugins, because jdtls is a heavyweight process.
 */
class JavaClientFeatures : LSPClientFeatures() {

    override fun isEnabled(file: VirtualFile): Boolean {
        val settings = JavaSettings.getInstance()
        if (!settings.codeIntelligenceEnabled) return false
        return JdtlsManager.defaultHome() != null && JdtlsManager.isInstalled()
    }
}
