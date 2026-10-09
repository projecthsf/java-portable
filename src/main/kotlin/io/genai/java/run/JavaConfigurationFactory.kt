package io.genai.java.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.openapi.project.Project

class JavaConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {
    override fun getId(): String = "JavaPortableRun"

    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        JavaRunConfiguration(project, this, "Java")

    override fun getOptionsClass(): Class<out RunConfigurationOptions> =
        JavaRunConfigurationOptions::class.java
}
