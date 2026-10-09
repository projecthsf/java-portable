package io.genai.java.sdk

import com.intellij.openapi.projectRoots.AdditionalDataConfigurable
import com.intellij.openapi.projectRoots.SdkAdditionalData
import com.intellij.openapi.projectRoots.SdkModel
import com.intellij.openapi.projectRoots.SdkModificator
import com.intellij.openapi.projectRoots.SdkType
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.util.SystemInfo
import org.jdom.Element
import java.io.File
import javax.swing.Icon

/**
 * The "Java Portable" SDK type. Downloading/switching JDKs is handled by our own UI
 * (Settings ▸ Java Portable), so we keep this type out of the platform's SDK combos:
 * `allowCreationByUser() = false` removes both the "Add" and "Download" actions there.
 *
 * Named distinctly from the platform's own JavaSdk on purpose — this plugin never loads in an IDE
 * that has real Java support (see `<incompatible-with>` in plugin.xml), so the two never coexist.
 */
class JavaSdkType : SdkType("Java Portable") {

    override fun suggestHomePath(): String? = null

    override fun isValidSdkHome(path: String): Boolean = findJavaExecutable(path) != null

    override fun getVersionString(sdkHome: String): String? {
        val java = findJavaExecutable(sdkHome) ?: return null
        return try {
            val process = ProcessBuilder(java.absolutePath, "-version")
                .redirectErrorStream(true)   // java -version writes to stderr
                .start()
            val out = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            // openjdk version "21.0.12" 2024-07-16  ->  21.0.12
            Regex("""version\s+"([^"]+)"""").find(out)?.groupValues?.get(1)
        } catch (e: Exception) {
            null
        }
    }

    override fun suggestSdkName(currentSdkName: String?, sdkHome: String): String {
        val version = getVersionString(sdkHome)
        return if (version != null) "Temurin $version" else "JDK"
    }

    override fun createAdditionalDataConfigurable(
        sdkModel: SdkModel,
        sdkModificator: SdkModificator,
    ): AdditionalDataConfigurable? = null

    override fun saveAdditionalData(additionalData: SdkAdditionalData, additional: Element) {}

    override fun getPresentableName(): String = "Java Portable"

    override fun getIcon(): Icon = ICON

    override fun allowCreationByUser(): Boolean = false

    companion object {
        private val ICON: Icon = IconLoader.getIcon("/icons/java.svg", JavaSdkType::class.java.classLoader)

        fun getInstance(): JavaSdkType = SdkType.findInstance(JavaSdkType::class.java)

        private fun exe(name: String): String = if (SystemInfo.isWindows) "$name.exe" else name

        fun findJavaExecutable(home: String?): File? = findToolExecutable(home, "java")

        /**
         * Locate a JDK tool (java, javac, jshell, jar, javap) within an SDK home.
         *
         * Three layouts have to work:
         *   <home>/bin/java                              a home pointed straight at a JDK
         *   <home>/jdk-21.0.12+7/bin/java                Linux/Windows archive, one level of nesting
         *   <home>/jdk-21.0.12+7/Contents/Home/bin/java  macOS archive — the JDK is an app bundle
         *
         * The macOS case is the one that bites: the top-level folder looks like a JDK but holds
         * only `Contents`, so a search that stops at one level finds nothing.
         */
        fun findToolExecutable(home: String?, tool: String): File? {
            if (home.isNullOrBlank()) return null
            val root = File(home)
            if (!root.exists()) return null
            val fileName = exe(tool)

            fun at(dir: File): File? = File(File(dir, "bin"), fileName).takeIf { it.isFile }

            at(root)?.let { return it }
            root.listFiles()?.filter { it.isDirectory }?.forEach { lvl1 ->
                at(lvl1)?.let { return it }
                at(File(lvl1, "Contents/Home"))?.let { return it }
            }
            return null
        }

        /** JAVA_HOME for an SDK home: the directory containing `bin/java`. */
        fun javaHome(home: String?): File? = findJavaExecutable(home)?.parentFile?.parentFile

        /**
         * Environment for running java/javac against the portable JDK. JAVA_HOME plus its `bin`
         * on PATH is enough — the JDK needs nothing else, which is the whole reason this plugin's
         * SDK layer is so much smaller than Ruby's or Rust's.
         */
        fun environment(home: String?): Map<String, String> {
            val javaHome = javaHome(home) ?: return emptyMap()
            val existingPath = System.getenv("PATH").orEmpty()
            val path = listOfNotNull(
                File(javaHome, "bin").absolutePath,
                existingPath.ifBlank { null },
            ).joinToString(File.pathSeparator)
            return mapOf("JAVA_HOME" to javaHome.absolutePath, "PATH" to path)
        }
    }
}
