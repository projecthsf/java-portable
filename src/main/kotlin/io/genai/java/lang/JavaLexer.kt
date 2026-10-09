package io.genai.java.lang

import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

/**
 * A small hand-written lexer for basic Java highlighting. Not a full Java grammar — semantic
 * understanding comes from jdtls via LSP; this is the local highlighting/PSI skeleton the platform
 * expects, and what you see before code intelligence is enabled.
 *
 * Java is much tamer than Ruby here, but three cases still need care:
 *
 *  - **Text blocks.** `"""…"""` spans lines and may contain bare `"` characters, so it has to be
 *    recognised before the ordinary string rule or it terminates after two quotes and the rest of
 *    the file lexes as code.
 *  - **Javadoc vs block comment.** A slash-star-star opener is Javadoc, but the empty comment
 *    slash-star-star-slash is not — it is a closed block comment, and treating it as Javadoc
 *    swallows the rest of the file. (Spelled out in words here on purpose: Kotlin block comments
 *    NEST, so writing the sequences literally in this KDoc opens a comment that never closes.)
 *  - **Char literals vs generics.** `'a'` is a char; the `'` never appears unpaired in valid Java,
 *    so unlike Ruby's lifetimes no lookahead is needed — but a stray quote must not run away, so
 *    the scan stops at a newline.
 */
class JavaLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var endOffset = 0
    private var tokenStart = 0
    private var tokenEnd = 0
    private var tokenType: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        this.endOffset = endOffset
        this.tokenStart = startOffset
        locateToken()
    }

    override fun getState(): Int = 0
    override fun getTokenType(): IElementType? = tokenType
    override fun getTokenStart(): Int = tokenStart
    override fun getTokenEnd(): Int = tokenEnd
    override fun getBufferSequence(): CharSequence = buffer
    override fun getBufferEnd(): Int = endOffset

    override fun advance() {
        tokenStart = tokenEnd
        locateToken()
    }

    private fun locateToken() {
        if (tokenStart >= endOffset) {
            tokenType = null
            tokenEnd = tokenStart
            return
        }
        val c = buffer[tokenStart]
        when {
            c.isWhitespace() -> whitespace()
            c == '/' && peek(tokenStart + 1) == '/' -> lineComment()
            c == '/' && peek(tokenStart + 1) == '*' -> blockComment()
            c == '"' && peek(tokenStart + 1) == '"' && peek(tokenStart + 2) == '"' -> textBlock()
            c == '"' -> quoted('"')
            c == '\'' -> quoted('\'')
            c == '@' && (peek(tokenStart + 1).isLetter() || peek(tokenStart + 1) == '_') -> annotation()
            c.isDigit() -> number()
            c == '_' || c == '$' || c.isLetter() -> word()
            else -> {
                tokenType = JavaTokenTypes.OPERATOR
                tokenEnd = tokenStart + 1
            }
        }
    }

    private fun whitespace() {
        var i = tokenStart
        while (i < endOffset && buffer[i].isWhitespace()) i++
        tokenType = TokenType.WHITE_SPACE
        tokenEnd = i
    }

    private fun lineComment() {
        var i = tokenStart
        while (i < endOffset && buffer[i] != '\n') i++
        tokenType = JavaTokenTypes.LINE_COMMENT
        tokenEnd = i
    }

    /** Block comment; a `**` opener means Javadoc, except for the empty comment. */
    private fun blockComment() {
        val isDoc = peek(tokenStart + 2) == '*' && peek(tokenStart + 3) != '/'
        var i = tokenStart + 2
        while (i < endOffset) {
            if (buffer[i] == '*' && peek(i + 1) == '/') { i += 2; break }
            i++
        }
        tokenType = if (isDoc) JavaTokenTypes.DOC_COMMENT else JavaTokenTypes.BLOCK_COMMENT
        tokenEnd = i.coerceAtMost(endOffset)
    }

    /** Text block `"""…"""` (Java 15+). Ends only at three unescaped quotes. */
    private fun textBlock() {
        var i = tokenStart + 3
        while (i < endOffset) {
            if (buffer[i] == '\\') { i += 2; continue }
            if (buffer[i] == '"' && peek(i + 1) == '"' && peek(i + 2) == '"') { i += 3; break }
            i++
        }
        tokenType = JavaTokenTypes.STRING
        tokenEnd = i.coerceAtMost(endOffset)
    }

    /** String or char literal; stops at a newline so an unterminated quote can't eat the file. */
    private fun quoted(quote: Char) {
        var i = tokenStart + 1
        while (i < endOffset) {
            val ch = buffer[i]
            if (ch == '\\') { i += 2; continue }
            if (ch == '\n') break
            if (ch == quote) { i++; break }
            i++
        }
        tokenType = JavaTokenTypes.STRING
        tokenEnd = i.coerceAtMost(endOffset)
    }

    /** `@Override`, `@java.lang.Deprecated` — the `@` plus the qualified name. */
    private fun annotation() {
        var i = tokenStart + 1
        while (i < endOffset && (buffer[i] == '_' || buffer[i] == '.' || buffer[i].isLetterOrDigit())) i++
        tokenType = JavaTokenTypes.ANNOTATION
        tokenEnd = i
    }

    /** Decimal/hex/binary/octal with `_` separators, floats, and type suffixes (`1_000L`, `0xFFp0`). */
    private fun number() {
        var i = tokenStart
        if (buffer[i] == '0' && peek(i + 1).lowercaseChar() in "xb") {
            i += 2
            while (i < endOffset && (buffer[i].isLetterOrDigit() || buffer[i] == '_')) i++
        } else {
            while (i < endOffset) {
                val ch = buffer[i]
                when {
                    ch.isDigit() || ch == '_' -> i++
                    ch == '.' && peek(i + 1).isDigit() -> i++
                    ch == 'e' || ch == 'E' -> {
                        i++
                        if (peek(i) == '+' || peek(i) == '-') i++
                    }
                    ch == 'f' || ch == 'F' || ch == 'd' || ch == 'D' || ch == 'l' || ch == 'L' -> {
                        i++
                        break
                    }
                    else -> break
                }
            }
        }
        tokenType = JavaTokenTypes.NUMBER
        tokenEnd = i.coerceAtMost(endOffset)
    }

    private fun word() {
        var i = tokenStart
        while (i < endOffset && (buffer[i] == '_' || buffer[i] == '$' || buffer[i].isLetterOrDigit())) i++
        val text = buffer.subSequence(tokenStart, i).toString()
        tokenType = if (text in JavaTokenTypes.KEYWORDS) JavaTokenTypes.KEYWORD
        else JavaTokenTypes.IDENTIFIER
        tokenEnd = i
    }

    private fun peek(index: Int): Char = if (index in 0 until endOffset) buffer[index] else EOF

    private companion object {
        /** Sentinel for "past the end". Must NOT be a space: callers test isWhitespace(). */
        const val EOF = Character.MIN_VALUE
    }
}
