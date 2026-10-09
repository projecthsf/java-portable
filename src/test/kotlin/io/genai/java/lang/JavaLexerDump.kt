package io.genai.java.lang

import java.io.File

/**
 * Dumps the token stream [JavaLexer] produces for a file, so the Java-specific hard cases can be
 * checked without launching an IDE:
 *
 *     ./gradlew lexDump -PlexFile=examples/Hello.java
 *
 * Prints one line per non-blank token, then asserts the cases that silently corrupt the rest of a
 * file when the lexer gets them wrong — a text block that terminates early, or a Javadoc opener
 * mistaken for an empty block comment, both swallow everything after them.
 */
object JavaLexerDump {

    @JvmStatic
    fun main(args: Array<String>) {
        val path = args.firstOrNull() ?: "examples/Hello.java"
        val text = File(path).readText()
        val tokens = lex(text)

        tokens.forEach { (type, raw) ->
            println("%-22s %s".format(type, raw.replace("\n", "\\n").take(70)))
        }

        println("\n--- checks ---")
        var failures = 0
        fun check(name: String, ok: Boolean) {
            if (!ok) failures++
            println("${if (ok) "PASS" else "FAIL"}  $name")
        }
        fun has(type: String, pred: (String) -> Boolean) =
            tokens.any { it.first == type && pred(it.second) }

        check(
            "text block is ONE string token",
            has("JAVA_STRING") { it.startsWith("\"\"\"") && it.contains("without escaping") },
        )
        check("quotes inside the text block did not end it", !has("JAVA_STRING") { it == "\"quotes\"" })
        check("javadoc distinguished from block comment", has("JAVA_DOC_COMMENT") { it.contains("Javadoc highlights") })
        check("ordinary block comment is BLOCK_COMMENT", has("JAVA_BLOCK_COMMENT") { it.contains("ordinary block comment") })
        check("line comment is LINE_COMMENT", has("JAVA_LINE_COMMENT") { it.contains("standalone .java file") })
        check("annotation includes the @", has("JAVA_ANNOTATION") { it == "@FunctionalInterface" })
        check("char literal is a STRING", has("JAVA_STRING") { it == "'J'" })
        check("underscore numeric literal", has("JAVA_NUMBER") { it == "1_000_000" })
        check("contextual keywords: var", has("JAVA_KEYWORD") { it == "var" })
        check("contextual keywords: record", has("JAVA_KEYWORD") { it == "record" })
        check("class name is an IDENTIFIER", has("JAVA_IDENTIFIER") { it == "Hello" })
        check("whole file consumed", tokens.isNotEmpty() && lexedLength(text) == text.length)

        println(if (failures == 0) "\nall checks passed" else "\n$failures check(s) FAILED")
    }

    private fun lex(text: String): List<Pair<String, String>> {
        val lexer = JavaLexer()
        lexer.start(text, 0, text.length, 0)
        val out = mutableListOf<Pair<String, String>>()
        while (lexer.tokenType != null) {
            val raw = text.substring(lexer.tokenStart, lexer.tokenEnd)
            if (raw.isNotBlank()) out += lexer.tokenType.toString() to raw
            lexer.advance()
        }
        return out
    }

    /** Where the lexer stopped — anything short of the file length means a token ran off a cliff. */
    private fun lexedLength(text: String): Int {
        val lexer = JavaLexer()
        lexer.start(text, 0, text.length, 0)
        var end = 0
        while (lexer.tokenType != null) {
            end = lexer.tokenEnd
            lexer.advance()
        }
        return end
    }
}
