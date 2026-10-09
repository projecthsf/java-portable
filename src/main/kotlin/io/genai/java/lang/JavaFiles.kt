package io.genai.java.lang

/** Single source of truth for what counts as a Java file, by extension. */
object JavaFiles {
    /** Only `.java`. That is the extension the Marketplace recommendation indexes, and the only
     *  one this plugin has any business claiming. */
    val EXTENSIONS: Set<String> = setOf("java")
}
