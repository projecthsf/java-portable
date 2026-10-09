package io.genai.java.lsp

import com.intellij.openapi.project.Project
import com.redhat.devtools.lsp4ij.server.ProcessStreamConnectionProvider
import io.genai.java.sdk.JavaSdkType

/**
 * Launches Eclipse JDT LS over stdio on the portable JDK.
 *
 * The command line is the one the jdtls project documents: run the Equinox launcher jar with a
 * platform-specific `-configuration` directory and a per-project `-data` workspace. The
 * `--add-opens` flags are required on Java 17+ — jdtls reflects into `java.base` internals, and
 * without them it starts and then dies on the first request.
 *
 * [JavaClientFeatures.isEnabled] gates this, so if no JDK or no server is present we never get here.
 */
class JdtlsConnectionProvider(project: Project) : ProcessStreamConnectionProvider() {
    init {
        val home = JdtlsManager.defaultHome()
        val java = JdtlsManager.defaultJava()
        val launcher = JdtlsManager.launcherJar()
        val config = JdtlsManager.configDir()
        val base = project.basePath

        if (home != null && java != null && launcher != null && config != null && base != null) {
            val workspace = JdtlsManager.workspaceFor(base)
            setCommands(
                listOf(
                    java.absolutePath,
                    "-Declipse.application=org.eclipse.jdt.ls.core.id1",
                    "-Dosgi.bundles.defaultStartLevel=4",
                    "-Declipse.product=org.eclipse.jdt.ls.core.product",
                    "-Dlog.level=ALL",
                    "-Xmx1G",
                    "--add-modules=ALL-SYSTEM",
                    "--add-opens", "java.base/java.util=ALL-UNNAMED",
                    "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                    "-jar", launcher.absolutePath,
                    "-configuration", config.absolutePath,
                    "-data", workspace.absolutePath,
                ),
            )
            setWorkingDirectory(base)
            setIncludeSystemEnvironmentVariables(true)
            setUserEnvironmentVariables(JavaSdkType.environment(home))
        }
    }
}
