package io.genai.java.lang

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors as Colors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.colors.TextAttributesKey.createTextAttributesKey as key
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType

class JavaSyntaxHighlighter : SyntaxHighlighterBase() {

    override fun getHighlightingLexer(): Lexer = JavaLexer()

    override fun getTokenHighlights(tokenType: IElementType?): Array<TextAttributesKey> =
        when (tokenType) {
            JavaTokenTypes.KEYWORD -> pack(KEYWORD)
            JavaTokenTypes.STRING -> pack(STRING)
            JavaTokenTypes.NUMBER -> pack(NUMBER)
            JavaTokenTypes.LINE_COMMENT -> pack(LINE_COMMENT)
            JavaTokenTypes.BLOCK_COMMENT -> pack(BLOCK_COMMENT)
            JavaTokenTypes.DOC_COMMENT -> pack(DOC_COMMENT)
            JavaTokenTypes.IDENTIFIER -> pack(IDENTIFIER)
            JavaTokenTypes.OPERATOR -> pack(OPERATOR)
            JavaTokenTypes.ANNOTATION -> pack(ANNOTATION)
            else -> EMPTY
        }

    companion object {
        // Darcula values behind these keys were read from DefaultColorSchemesManager.xml in the
        // IDE distribution; METADATA (#BBB529) is what annotations get. Keys Darcula does not
        // override render as plain identifier colour, so none are used here.
        val KEYWORD: TextAttributesKey = key("JAVA_PORTABLE_KEYWORD", Colors.KEYWORD)
        val STRING: TextAttributesKey = key("JAVA_PORTABLE_STRING", Colors.STRING)
        val NUMBER: TextAttributesKey = key("JAVA_PORTABLE_NUMBER", Colors.NUMBER)
        val LINE_COMMENT: TextAttributesKey = key("JAVA_PORTABLE_LINE_COMMENT", Colors.LINE_COMMENT)
        val BLOCK_COMMENT: TextAttributesKey = key("JAVA_PORTABLE_BLOCK_COMMENT", Colors.BLOCK_COMMENT)
        val DOC_COMMENT: TextAttributesKey = key("JAVA_PORTABLE_DOC_COMMENT", Colors.DOC_COMMENT)
        val IDENTIFIER: TextAttributesKey = key("JAVA_PORTABLE_IDENTIFIER", Colors.IDENTIFIER)
        val OPERATOR: TextAttributesKey = key("JAVA_PORTABLE_OPERATOR", Colors.OPERATION_SIGN)
        val ANNOTATION: TextAttributesKey = key("JAVA_PORTABLE_ANNOTATION", Colors.METADATA)
        private val EMPTY = emptyArray<TextAttributesKey>()
    }
}
