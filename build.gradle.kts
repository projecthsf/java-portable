import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.18.0"
}

group = "io.genai"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Compile against 2024.2 (242): LSP4IJ requires build 242+, and the optional
        // code-intelligence module compiles against its API. Core sinceBuild stays 233 —
        // the LSP layer is an OPTIONAL dependency, active only where LSP4IJ can install.
        intellijIdeaCommunity("2024.2")

        // Optional Go code-intelligence layer (completion / navigation / errors) via
        // LSP4IJ + gopls. Compile-time dep here; runtime it's optional (see plugin.xml).
        plugin("com.redhat.devtools.lsp4ij", "0.20.1")
    }

    // TEST ONLY. gradle.properties sets kotlin.stdlib.default.dependency=false because the IDE
    // ships its own stdlib and bundling a second copy breaks plugin loading. The lexDump harness
    // runs outside the IDE, so it needs the stdlib on its own classpath — scoped to the test
    // source set so nothing extra lands in the plugin zip.
    testImplementation(kotlin("stdlib"))
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

kotlin {
    jvmToolchain(17)
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "233"
            untilBuild = provider { null }
        }
    }

    // `./gradlew publishPlugin` reads the JetBrains Marketplace token from the PUBLISH_TOKEN
    // env var (set as a GitHub Actions secret). No signing configured, so uploads are unsigned.
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    // `./gradlew verifyPlugin` runs the JetBrains Plugin Verifier (same tool Marketplace uses).
    // This is a publish gate in CI (see .github/workflows/publish.yml).
    pluginVerification {
        failureLevel.set(listOf(
            FailureLevel.COMPATIBILITY_PROBLEMS,
            FailureLevel.INTERNAL_API_USAGES,
            FailureLevel.MISSING_DEPENDENCIES,
            FailureLevel.INVALID_PLUGIN,
        ))
        // Verify against PhpStorm, NOT IntelliJ IDEA. This plugin declares
        // <incompatible-with>com.intellij.java</incompatible-with>, so IDEA is the one IDE it
        // will never run in — verifying there would check a configuration that cannot occur and
        // miss the ones that can. PhpStorm is representative of the actual targets.
        ides {
            latest {
                types.set(listOf(IntelliJPlatformType.PhpStorm))
            }
        }
    }
}

// Indexing settings via a headless IDE is slow and clashes with a running runIde sandbox;
// not needed for a dev build.
tasks.named("buildSearchableOptions") { enabled = false }

// Dumps the JavaLexer token stream for a file and asserts the Ruby-specific hard cases
// (regex vs division, heredocs, %-literals). Lets the lexer be checked without an IDE:
//     ./gradlew lexDump -PlexFile=examples/Hello.java
tasks.register<JavaExec>("lexDump") {
    group = "verification"
    description = "Dump and check the JavaLexer token stream for a .java file"
    mainClass.set("io.genai.java.lang.JavaLexerDump")
    // The IntelliJ Platform Gradle Plugin puts the platform jars on main's COMPILE classpath,
    // not on test runtime, so both are needed to run the lexer outside an IDE.
    classpath = sourceSets["test"].runtimeClasspath +
        sourceSets["main"].compileClasspath +
        sourceSets["main"].output
    args = listOf(project.findProperty("lexFile")?.toString() ?: "examples/Hello.java")
}

// Sandbox IDE for manual testing.
//
// MUST be PhpStorm, not IntelliJ IDEA. This plugin declares
// <incompatible-with>com.intellij.java</incompatible-with>, so the default `runIde` task — which
// launches IDEA — would start a sandbox in which the plugin is silently not loaded at all, and
// look exactly like a broken plugin. PhpStorm is a representative target: no Java support,
// which is the whole reason this plugin exists.
//
//     ./gradlew runPhpStorm
val runPhpStorm by intellijPlatformTesting.runIde.registering {
    type = IntelliJPlatformType.PhpStorm
    version = "2024.2.4"
}
