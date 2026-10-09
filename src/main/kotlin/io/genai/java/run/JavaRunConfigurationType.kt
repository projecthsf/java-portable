package io.genai.java.run

import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.NotNullLazyValue

class JavaRunConfigurationType : ConfigurationTypeBase(
    "JavaPortableRunConfiguration",
    // "(Portable)" so it is unmistakable in a listing; the real Java run types only exist in IDEA,
    // where this plugin never loads.
    "Java File (Portable)",
    "Run a Java file with a portable JDK",
    NotNullLazyValue.createValue { AllIcons.Actions.Execute },
) {
    init {
        addFactory(JavaConfigurationFactory(this))
    }
}
