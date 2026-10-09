package io.genai.java.lang

import com.intellij.psi.tree.IElementType

object JavaTokenTypes {
    @JvmField val KEYWORD = IElementType("JAVA_KEYWORD", JavaLanguage)
    @JvmField val IDENTIFIER = IElementType("JAVA_IDENTIFIER", JavaLanguage)
    @JvmField val STRING = IElementType("JAVA_STRING", JavaLanguage)
    @JvmField val NUMBER = IElementType("JAVA_NUMBER", JavaLanguage)
    @JvmField val LINE_COMMENT = IElementType("JAVA_LINE_COMMENT", JavaLanguage)
    @JvmField val BLOCK_COMMENT = IElementType("JAVA_BLOCK_COMMENT", JavaLanguage)
    @JvmField val DOC_COMMENT = IElementType("JAVA_DOC_COMMENT", JavaLanguage)
    @JvmField val OPERATOR = IElementType("JAVA_OPERATOR", JavaLanguage)

    /** `@Override`, `@SuppressWarnings(...)` — the annotation name including the `@`. */
    @JvmField val ANNOTATION = IElementType("JAVA_ANNOTATION", JavaLanguage)

    /** Reserved words, through Java 21. `record`, `sealed`, `var`, `yield` are contextual but
     *  read as keywords everywhere they appear in practice. */
    val KEYWORDS: Set<String> = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
        "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
        "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
        "interface", "long", "native", "new", "package", "private", "protected", "public",
        "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
        "throw", "throws", "transient", "try", "void", "volatile", "while",
        // literals
        "true", "false", "null",
        // contextual / modern
        "var", "record", "sealed", "permits", "non-sealed", "yield",
    )
}
