package io.genai.java.lsp

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.io.Decompressor
import com.intellij.util.io.HttpRequests
import com.intellij.util.system.CpuArch
import io.genai.java.sdk.JavaSdkManager
import io.genai.java.sdk.JavaSdkType
import java.io.File
import java.nio.file.Files

/**
 * Locates (and, on request, downloads) **Eclipse JDT Language Server** — the server VS Code's Java
 * extension runs, and the code intelligence behind this plugin.
 *
 * Unlike the sibling plugins, the server is not a package manager away: there is no `gem install`
 * or `rustup component add` for jdtls. It ships as a ~50 MB Eclipse runtime that we unpack into
 * `~/.java-portable/jdtls` and launch on the portable JDK, which is convenient — the JDK the user
 * already downloaded to *run* their file is exactly what jdtls needs to run itself.
 *
 * It is shared across all JDKs rather than installed per-SDK: jdtls is a Java program, so one copy
 * serves every JDK, and at 50 MB it isn't worth duplicating.
 */
object JdtlsManager {

    private const val DOWNLOAD = "https://download.eclipse.org/jdtls/snapshots/jdt-language-server-latest.tar.gz"

    fun jdtlsRoot(): File = JavaSdkManager.downloadRoot().resolve("jdtls").toFile()

    /**
     * The Equinox launcher jar, whose name carries a build-specific version
     * (`org.eclipse.equinox.launcher_1.6.900.v20240613-2009.jar`), so it has to be found by glob
     * rather than hard-coded.
     */
    fun launcherJar(): File? =
        File(jdtlsRoot(), "plugins").listFiles()
            ?.firstOrNull { it.name.startsWith("org.eclipse.equinox.launcher_") && it.extension == "jar" }

    /**
     * jdtls ships one Equinox configuration per platform, and separate ones for ARM. Picking the
     * wrong directory makes the server start and then fail to resolve its own bundles.
     */
    fun configDir(): File? {
        val names = when {
            SystemInfo.isWindows -> listOf("config_win")
            SystemInfo.isMac -> if (CpuArch.isArm64()) listOf("config_mac_arm", "config_mac") else listOf("config_mac")
            else -> if (CpuArch.isArm64()) listOf("config_linux_arm", "config_linux") else listOf("config_linux")
        }
        return names.map { File(jdtlsRoot(), it) }.firstOrNull { it.isDirectory }
    }

    /** Per-project workspace jdtls keeps its index in; it creates the directory itself. */
    fun workspaceFor(projectPath: String): File {
        val slug = projectPath.replace(Regex("[^A-Za-z0-9]"), "_").takeLast(80)
        return File(JavaSdkManager.downloadRoot().resolve("jdtls-workspaces").toFile(), slug)
    }

    fun isInstalled(): Boolean = launcherJar() != null && configDir() != null

    /** Download and unpack jdtls. Blocking — call off the EDT. */
    fun install(indicator: ProgressIndicator) {
        val root = jdtlsRoot()
        indicator.text = "Downloading Eclipse JDT Language Server…"
        Files.createDirectories(root.toPath())

        val tmp = Files.createTempFile("jdtls-", ".tar.gz")
        try {
            HttpRequests.request(DOWNLOAD).saveToFile(tmp.toFile(), indicator)
            indicator.isIndeterminate = true
            indicator.text = "Extracting language server…"
            Decompressor.Tar(tmp).extract(root.toPath())
        } finally {
            Files.deleteIfExists(tmp)
        }

        if (!isInstalled()) {
            throw RuntimeException(
                "Download finished but the language server looks incomplete under $root " +
                    "(launcher=${launcherJar()}, config=${configDir()}).",
            )
        }
    }

    /** Home directory of the current default JDK, or null if none configured. */
    fun defaultHome(): String? =
        io.genai.java.settings.JavaSettings.getInstance().defaultSdk()?.homePath
            ?.takeIf { JavaSdkType.findJavaExecutable(it) != null }

    /** The current default JDK's java binary, or null if none configured. */
    fun defaultJava(): File? = defaultHome()?.let { JavaSdkType.findJavaExecutable(it) }
}
