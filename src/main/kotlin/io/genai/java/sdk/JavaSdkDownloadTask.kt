package io.genai.java.sdk

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.roots.ui.configuration.projectRoot.SdkDownloadTask
import com.intellij.util.io.Decompressor
import com.intellij.util.io.HttpRequests
import java.nio.file.Files
import java.nio.file.Path

/**
 * Downloads a Temurin JDK into [homeDir] and extracts it.
 *
 * The simplest SDK task in the family: a plain archive in a format the platform already reads —
 * `.tar.gz` on macOS/Linux, `.zip` on Windows — and no post-install step, because the JDK carries
 * its own JVM and resolves `java.home` from the launcher's own location at runtime.
 *
 * The archive expands into a versioned folder, and on macOS that folder is an app bundle with the
 * real JDK under `Contents/Home`. [JavaSdkType.findJavaExecutable] handles both shapes.
 */
class JavaSdkDownloadTask(
    private val release: JavaRelease,
    private val homeDir: Path,
) : SdkDownloadTask {

    override fun getSuggestedSdkName(): String = "Temurin ${release.version}"
    override fun getPlannedHomeDir(): String = homeDir.toString()
    override fun getPlannedVersion(): String = release.version

    override fun doDownload(indicator: ProgressIndicator) {
        indicator.isIndeterminate = false
        indicator.text = "Downloading Temurin ${release.version} (${release.sizeMb}MB)…"
        Files.createDirectories(homeDir)

        val suffix = if (release.kind == ArchiveKind.ZIP) ".zip" else ".tar.gz"
        val tmp = Files.createTempFile("temurin-", suffix)
        try {
            HttpRequests.request(release.url).saveToFile(tmp.toFile(), indicator)
            indicator.isIndeterminate = true
            indicator.text = "Extracting Temurin ${release.version}…"
            when (release.kind) {
                ArchiveKind.ZIP -> Decompressor.Zip(tmp).extract(homeDir)
                ArchiveKind.TAR_GZ -> Decompressor.Tar(tmp).extract(homeDir)
            }
        } finally {
            Files.deleteIfExists(tmp)
        }

        val java = JavaSdkType.findJavaExecutable(homeDir.toString())
            ?: throw RuntimeException("Download finished but no java binary was found under $homeDir.")
        // A .zip carries no POSIX modes, so Windows-built archives extracted anywhere lose the
        // executable bit. Restore it on the launchers we actually invoke.
        java.setExecutable(true)
        JavaSdkType.findToolExecutable(homeDir.toString(), "javac")?.setExecutable(true)
    }
}
