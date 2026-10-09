package io.genai.java.lang

import com.intellij.lang.Language

/**
 * The Java language for this plugin.
 *
 * The ID is deliberately **"JavaPortable"**, NOT "JAVA": IntelliJ requires language IDs to be
 * globally unique, and the platform's own Java plugin registers "JAVA". A distinct ID means that
 * even if the two were ever loaded together nothing clashes — though `<incompatible-with>` in
 * plugin.xml keeps us out of any IDE that has real Java support. Display name is still "Java".
 */
object JavaLanguage : Language("JavaPortable") {
    override fun getDisplayName(): String = "Java"
}
