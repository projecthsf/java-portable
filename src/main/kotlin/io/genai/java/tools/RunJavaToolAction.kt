package io.genai.java.tools

import com.intellij.execution.RunContentExecutor
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import io.genai.java.lsp.JdtlsManager
import io.genai.java.sdk.JavaSdkType
import kotlin.io.path.Path

/**
 * Runs a JDK tool (javac / jshell / javap / jar) with the portable JDK in a Run console, using the
 * project directory as the working dir. No system Java needed.
 *
 * Simpler than the sibling plugins' equivalents: JDK tools are native launchers, not shebang
 * scripts, so they are executed directly rather than passed to an JDK.
 */
class RunJavaToolAction : AnAction() {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val base = project.basePath ?: run {
            Messages.showErrorDialog(project, "No project directory.", "Run JDK Tool")
            return
        }

        val home = JdtlsManager.defaultHome()
        if (home == null) {
            Messages.showErrorDialog(
                project,
                "No JDK configured. Set one up in Settings ▸ Java Portable.",
                "Run JDK Tool",
            )
            return
        }

        val dialog = RunJavaToolDialog(project)
        if (!dialog.showAndGet()) return
        val args = dialog.commandArgs
        if (args.isEmpty()) return

        // "java -version" arrives as a single combo entry; split it back into tool + flag.
        val parts = args.first().split(" ")
        val toolName = parts.first()
        val rest = parts.drop(1) + args.drop(1)

        val tool = JavaSdkType.findToolExecutable(home, toolName) ?: run {
            Messages.showErrorDialog(project, "No $toolName found in this JDK.", "Run JDK Tool")
            return
        }

        val cmd = GeneralCommandLine()
            .withExePath(tool.absolutePath)
            .withParameters(rest)
            .withWorkDirectory(base)
            .withEnvironment(JavaSdkType.environment(home))
        val handler = OSProcessHandler(cmd)
        // javac and jar write .class/.jar files via an external process; refresh the project dir
        // when it finishes so they show up without a manual "Reload from Disk".
        handler.addProcessListener(object : ProcessListener {
            override fun processTerminated(event: ProcessEvent) {
                LocalFileSystem.getInstance().findFileByNioFile(Path(base))?.let {
                    VfsUtil.markDirtyAndRefresh(true, true, true, it)
                }
            }
        })
        RunContentExecutor(project, handler)
            .withTitle(args.joinToString(" "))
            .withActivateToolWindow(true)
            .run()
    }
}
