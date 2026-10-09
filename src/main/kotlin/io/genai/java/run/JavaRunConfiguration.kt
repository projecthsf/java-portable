package io.genai.java.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import io.genai.java.sdk.JavaSdkType
import io.genai.java.settings.JavaSettings
import java.io.File

class JavaRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String?,
) : RunConfigurationBase<JavaRunConfigurationOptions>(project, factory, name) {

    public override fun getOptions(): JavaRunConfigurationOptions =
        super.getOptions() as JavaRunConfigurationOptions

    var scriptPath: String?
        get() = options.scriptPath
        set(value) { options.scriptPath = value }

    var sdkName: String?
        get() = options.sdkName
        set(value) { options.sdkName = value }

    var javaPath: String?
        get() = options.javaPath
        set(value) { options.javaPath = value }

    var programArgs: String?
        get() = options.programArgs
        set(value) { options.programArgs = value }

    var envs: MutableMap<String, String>
        get() = options.envs
        set(value) { options.envs = value }

    var passParentEnvs: Boolean
        get() = options.passParentEnvs
        set(value) { options.passParentEnvs = value }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        JavaSettingsEditor(project)

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return object : CommandLineState(environment) {
            @Throws(ExecutionException::class)
            override fun startProcess(): ProcessHandler {
                val script = scriptPath?.takeIf { it.isNotBlank() }
                    ?: throw ExecutionException("No Java file specified")
                val java = resolveJava()
                    ?: throw ExecutionException(
                        "No JDK configured — pick a Java SDK or set a java executable path",
                    )

                val file = File(script)

                // Single-file source launch (`java Foo.java`), JEP 330, Java 11+. The launcher
                // compiles in memory and runs the first top-level class with a main method, so
                // there is no javac step, no output directory and no project model to set up —
                // which is exactly the "I just want to run this one file" case this plugin is for.
                // It is also why the SDK must be a full JDK and not a JRE: this needs jdk.compiler.
                val cmd = GeneralCommandLine()
                cmd.exePath = java.absolutePath
                cmd.addParameter(file.absolutePath)
                programArgs?.takeIf { it.isNotBlank() }
                    ?.let { cmd.addParameters(it.trim().split(Regex("\\s+"))) }
                cmd.setWorkDirectory(file.parentFile ?: File("."))

                resolveHome()?.let { cmd.withEnvironment(JavaSdkType.environment(it)) }
                cmd.withEnvironment(options.envs)
                cmd.withParentEnvironmentType(
                    if (options.passParentEnvs) GeneralCommandLine.ParentEnvironmentType.CONSOLE
                    else GeneralCommandLine.ParentEnvironmentType.NONE,
                )

                val handler = OSProcessHandler(cmd)
                ProcessTerminatedListener.attach(handler)
                return handler
            }
        }
    }

    /** An explicit java path wins; then the SDK pinned on this config; otherwise the default. */
    private fun resolveJava(): File? {
        javaPath?.takeIf { it.isNotBlank() }?.let { return File(it).takeIf { f -> f.isFile } }

        val name = sdkName?.takeIf { it.isNotBlank() }
        val pinned = name?.let { ProjectJdkTable.getInstance().findJdk(it) }
        pinned?.homePath?.let { JavaSdkType.findJavaExecutable(it) }?.let { return it }

        val default = JavaSettings.getInstance().defaultSdk()
        return default?.homePath?.let { JavaSdkType.findJavaExecutable(it) }
    }

    /** The SDK home backing this run, for JAVA_HOME. Null for an explicit java-path override. */
    private fun resolveHome(): String? {
        if (!javaPath.isNullOrBlank()) return null

        val name = sdkName?.takeIf { it.isNotBlank() }
        val pinned = name?.let { ProjectJdkTable.getInstance().findJdk(it) }
        pinned?.homePath?.takeIf { JavaSdkType.findJavaExecutable(it) != null }?.let { return it }

        return JavaSettings.getInstance().defaultSdk()?.homePath
    }
}
