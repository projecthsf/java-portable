package io.genai.java.run

import com.intellij.execution.configurations.RunConfigurationOptions

/** Persisted state for a Java run configuration. */
class JavaRunConfigurationOptions : RunConfigurationOptions() {
    private val scriptPathProp = string("").provideDelegate(this, "scriptPath")
    private val sdkNameProp = string("").provideDelegate(this, "sdkName")
    private val rubyPathProp = string("").provideDelegate(this, "javaPath")

    var scriptPath: String?
        get() = scriptPathProp.getValue(this)
        set(value) = scriptPathProp.setValue(this, value)

    var sdkName: String?
        get() = sdkNameProp.getValue(this)
        set(value) = sdkNameProp.setValue(this, value)

    var javaPath: String?
        get() = rubyPathProp.getValue(this)
        set(value) = rubyPathProp.setValue(this, value)

    private val programArgsProp = string("").provideDelegate(this, "programArgs")

    /** Arguments passed to the program itself, after the file name. */
    var programArgs: String?
        get() = programArgsProp.getValue(this)
        set(value) = programArgsProp.setValue(this, value)

    /** User environment variables for the run (e.g. JAVA_LOG=debug). */
    var envs by map<String, String>()

    /** Whether to inherit the system/parent environment on top of [envs]. */
    var passParentEnvs by property(true)
}
