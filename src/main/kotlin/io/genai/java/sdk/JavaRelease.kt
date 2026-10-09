package io.genai.java.sdk

enum class OsFamily { WINDOWS, MAC, LINUX }

enum class ArchiveKind { ZIP, TAR_GZ }

/**
 * A downloadable portable JDK build, from Eclipse Temurin via the Adoptium API.
 *
 * This is the easiest toolchain in the Portable family, for a reason worth stating: **a JDK
 * already contains its own JVM** (`lib/server/libjvm.*`), so one self-contained archive gives
 * both compiling and running with nothing else installed. There is no separate runtime to fetch,
 * no installer to drive, and — unlike Ruby — a usable archive format on every platform:
 * `.tar.gz` for macOS and Linux, `.zip` for Windows, both of which the platform's `Decompressor`
 * reads natively.
 *
 * It must be the full JDK rather than a JRE: `java Foo.java` single-file source launch compiles
 * in memory and therefore needs `jdk.compiler`.
 *
 * @param feature  the feature release, e.g. 21
 * @param version  the full version, e.g. "21.0.12+101"
 */
data class JavaRelease(
    val feature: Int,
    val version: String,
    val os: OsFamily,
    val arch: String,        // Adoptium naming: x64 / aarch64
    val url: String,
    val kind: ArchiveKind,
    val sizeMb: Int,
) {
    /** Directory name under ~/.java-portable. */
    val dirName: String get() = "jdk-$feature"

    val label: String get() =
        "Temurin $version  ·  ${os.name.lowercase()}/$arch  ·  ${sizeMb}MB"

    override fun toString(): String = label
}
