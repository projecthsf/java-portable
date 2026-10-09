package io.genai.java.run

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import io.genai.java.lang.JavaTokenTypes

/**
 * Puts a green ▶ Run marker next to `main`, so a Java file launches with one click.
 *
 * Our PSI is flat (one leaf per lexer token), so we anchor on the `main` identifier that directly
 * follows the `void` keyword — `public static void main(String[] args)`. That gives exactly one
 * marker per entry point, and matches what single-file source launch actually runs: the first
 * top-level class with a main method.
 */
class JavaRunLineMarkerContributor : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        if (element.firstChild != null) return null // leaves only
        if (element.node?.elementType != JavaTokenTypes.IDENTIFIER) return null
        if (element.text != "main") return null
        if (PsiTreeUtil.prevVisibleLeaf(element)?.text != "void") return null

        val actions = ExecutorAction.getActions(0)
        if (actions.isEmpty()) return null
        return Info(AllIcons.RunConfigurations.TestState.Run, actions) { "Run Java file" }
    }
}
