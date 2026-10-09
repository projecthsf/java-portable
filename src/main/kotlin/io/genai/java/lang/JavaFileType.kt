package io.genai.java.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * `.java` file type. Referenced by plugin.xml via fieldName="INSTANCE" — a Kotlin `object`
 * already exposes a static `INSTANCE` field, so no extra declaration needed.
 */
object JavaFileType : LanguageFileType(JavaLanguage) {
    private val ICON: Icon = IconLoader.getIcon("/icons/java.svg", JavaFileType::class.java.classLoader)

    override fun getName(): String = "Java File (Portable)"
    override fun getDescription(): String = "Java source file"
    override fun getDefaultExtension(): String = "java"
    override fun getIcon(): Icon = ICON
}
