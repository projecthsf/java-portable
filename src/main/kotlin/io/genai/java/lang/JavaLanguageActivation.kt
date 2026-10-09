package io.genai.java.lang

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.ExtensionFileNameMatcher
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Binds our lightweight Java file type to `.java` files — but ONLY in an IDE that has no official
 * Java support. That is the whole point of this plugin: PhpStorm, WebStorm, PyCharm, GoLand,
 * RubyMine, CLion and Rider ship no Java support at all, so opening a `.java` file there means
 * installing IntelliJ IDEA just to read one file.
 *
 * In IDEA (and Android Studio) the official Java plugin owns `.java`, and `<incompatible-with>`
 * in plugin.xml keeps this plugin from loading there at all. This runtime binding is a belt-and-
 * braces fallback; re-associating an extension we already own is a no-op.
 */
class JavaLanguageActivation : ProjectActivity {

    override suspend fun execute(project: Project) {
        if (!activated.compareAndSet(false, true)) return
        if (javaExtensionAlreadyOwned()) return

        val fileTypeManager = FileTypeManager.getInstance()
        ApplicationManager.getApplication().invokeLater {
            ApplicationManager.getApplication().runWriteAction {
                for (ext in JavaFiles.EXTENSIONS) {
                    fileTypeManager.associate(JavaFileType, ExtensionFileNameMatcher(ext))
                }
            }
        }
    }

    /**
     * Is `.java` already claimed by some other file type? On IDEA / Android Studio the official
     * Java plugin registers it at load — before this runs — so we defer to whoever owns it.
     * `UnknownFileType` means nobody owns it (Community); our own [JavaFileType] means we bound it
     * in a prior session (re-associating is harmless).
     */
    private fun javaExtensionAlreadyOwned(): Boolean {
        val existing = FileTypeManager.getInstance().getFileTypeByExtension("java")
        return existing != UnknownFileType.INSTANCE && existing != JavaFileType
    }

    companion object {
        private val activated = AtomicBoolean(false)
    }
}
