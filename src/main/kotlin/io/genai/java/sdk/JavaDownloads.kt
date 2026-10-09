package io.genai.java.sdk

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.util.io.HttpRequests
import com.intellij.util.system.CpuArch

/**
 * JDK catalogue, read live from the Adoptium API so the version list stays current.
 *
 * Only LTS feature releases are offered. Someone reaching for this plugin wants to run a `.java`
 * file, not to track the six-month release train, and every extra entry in the dropdown is a
 * decision they didn't ask for.
 */
object JavaDownloads {

    private const val API = "https://api.adoptium.net/v3"

    /** Used when the API can't be reached. Known-good LTS at time of writing. */
    private val FALLBACK_LTS = listOf(21, 17)

    fun currentOs(): OsFamily = when {
        SystemInfo.isWindows -> OsFamily.WINDOWS
        SystemInfo.isMac -> OsFamily.MAC
        else -> OsFamily.LINUX
    }

    /** Adoptium's arch naming: x64 / aarch64, not amd64 / arm64. */
    private fun currentArch(): String = if (CpuArch.isArm64()) "aarch64" else "x64"

    private fun osTag(os: OsFamily): String = when (os) {
        OsFamily.MAC -> "mac"
        OsFamily.LINUX -> "linux"
        OsFamily.WINDOWS -> "windows"
    }

    fun fetchAvailableWithProgress(project: Project?): List<JavaRelease> =
        ProgressManager.getInstance().runProcessWithProgressSynchronously(
            ThrowableComputable { fetchAvailable() },
            "Fetching Available JDK Versions…",
            true,
            project,
        )

    /** Blocking fetch — must be called off the EDT. Returns newest-first. */
    fun fetchAvailable(): List<JavaRelease> {
        val lts = try {
            fetchLtsReleases().ifEmpty { FALLBACK_LTS }
        } catch (e: Exception) {
            FALLBACK_LTS
        }
        return lts.sortedDescending().mapNotNull { fetchRelease(it) }
    }

    private fun fetchLtsReleases(): List<Int> {
        val json = HttpRequests.request("$API/info/available_releases")
            .productNameAsUserAgent()
            .readString()
        val block = Regex(""""available_lts_releases"\s*:\s*\[([^]]*)]""").find(json)?.groupValues?.get(1)
            ?: return emptyList()
        // Java 8 predates single-file source launch (added in 11) and the module layout this
        // plugin assumes, so it is not offered.
        return Regex("""\d+""").findAll(block).map { it.value.toInt() }.filter { it >= 11 }.toList()
    }

    /** Resolves the newest build of one feature release for this machine, or null if none. */
    private fun fetchRelease(feature: Int): JavaRelease? = try {
        val os = currentOs()
        val arch = currentArch()
        val json = HttpRequests
            .request("$API/assets/latest/$feature/hotspot?os=${osTag(os)}&architecture=$arch&image_type=jdk")
            .productNameAsUserAgent()
            .readString()

        val link = Regex(""""link"\s*:\s*"([^"]+?\.(?:tar\.gz|zip))"""").find(json)?.groupValues?.get(1)
        val semver = Regex(""""semver"\s*:\s*"([^"]+)"""").find(json)?.groupValues?.get(1)
        val size = Regex(""""size"\s*:\s*(\d+)""").find(json)?.groupValues?.get(1)?.toLongOrNull()

        if (link == null) null
        else JavaRelease(
            feature = feature,
            version = semver ?: feature.toString(),
            os = os,
            arch = arch,
            url = link,
            kind = if (link.endsWith(".zip")) ArchiveKind.ZIP else ArchiveKind.TAR_GZ,
            sizeMb = ((size ?: 0L) / 1_048_576L).toInt(),
        )
    } catch (e: Exception) {
        null
    }
}
