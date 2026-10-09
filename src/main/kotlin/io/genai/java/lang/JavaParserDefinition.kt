package io.genai.java.lang

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.lang.ASTNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.lexer.Lexer
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.TokenSet

/**
 * A minimal parser definition. It builds a FLAT PSI tree (one leaf per lexer token) so the file
 * has real word-level structure — what the editor needs for navigation/selection (e.g. Cmd-hover
 * underlines the token under the cursor, not the whole file). Semantics come from Eclipse JDT LS via LSP.
 */
class JavaParserDefinition : ParserDefinition {

    override fun createLexer(project: Project?): Lexer = JavaLexer()

    override fun createParser(project: Project?): PsiParser = JavaParser()

    override fun getFileNodeType(): IFileElementType = FILE

    override fun getWhitespaceTokens(): TokenSet = WHITESPACE

    override fun getCommentTokens(): TokenSet = COMMENTS

    override fun getStringLiteralElements(): TokenSet = STRINGS

    override fun createElement(node: ASTNode): PsiElement = ASTWrapperPsiElement(node)

    override fun createFile(viewProvider: FileViewProvider): PsiFile = JavaFile(viewProvider)

    private class JavaParser : PsiParser {
        override fun parse(root: IElementType, builder: PsiBuilder): ASTNode {
            val rootMarker = builder.mark()
            while (!builder.eof()) builder.advanceLexer()
            rootMarker.done(root)
            return builder.treeBuilt
        }
    }

    private class JavaFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, JavaLanguage) {
        override fun getFileType(): FileType = JavaFileType
        override fun toString(): String = "Java File"
    }

    companion object {
        private val FILE = IFileElementType(JavaLanguage)
        private val WHITESPACE = TokenSet.create(TokenType.WHITE_SPACE)
        private val COMMENTS = TokenSet.create(JavaTokenTypes.LINE_COMMENT, JavaTokenTypes.BLOCK_COMMENT)
        private val STRINGS = TokenSet.create(JavaTokenTypes.STRING)
    }
}
