# Java Portable

Open and run `.java` files **without installing another IDE**.

## Why this one is different from the rest of the family

Every other Portable plugin fills a gap in **IntelliJ IDEA Community**. This one inverts that:
IDEA has excellent Java support, and this plugin deliberately refuses to load there. The gap is
everywhere else — **PhpStorm, WebStorm, PyCharm, GoLand, RubyMine, CLion and Rider ship no Java
support at all**. Today, a PHP developer who wants to read or run one `.java` file has to download
IntelliJ IDEA for it.

That is the same insight that explains Python Portable's numbers: PyCharm Community is free, and
Python Portable still does ~315 downloads/day. The barrier was never licence cost — it is that
nobody wants to install and configure a second IDE for a file they will touch for ten minutes.

`.java` is also close to unclaimed in those IDEs. Of the four plugins in the Marketplace file-type
index that register it, three (JetBrains' own Java, Java for JetBrains Client, Kotlin/Native
Platform Dependencies) are IDEA-only; the fourth is a test-writing tool, not language support.

## Features
- **Portable JDK** — download an Eclipse Temurin LTS build from inside the IDE; stored under
  `~/.java-portable`, self-contained, no `JAVA_HOME` to set and no conflict with a system JDK.
- **Run a `.java` file** — ▶ gutter marker next to `main`. Uses **single-file source launch**
  (`java Hello.java`, JEP 330): no compile step, no output folder, no project model.
- **Syntax highlighting** — lexer-based, covering text blocks, Javadoc vs block comments,
  annotations, char literals, underscore numerics and the contextual keywords (`var`, `record`,
  `sealed`, `yield`).
- **Code intelligence (optional)** — completion, go-to-definition, hover docs and diagnostics via
  **Eclipse JDT LS**, the server VS Code's Java extension uses, bridged with **LSP4IJ**.
- **Run JDK tools** — `javac`, `jshell`, `javap`, `jar` in a console.

## Why the toolchain layer is the smallest in the family

**A JDK already contains its own JVM** (`lib/server/libjvm.*`), so one self-contained archive gives
both compiling and running — there is no separate runtime to fetch, no installer to drive, and no
relocation problem. Compare:

| Plugin | Toolchain source | Complication |
|---|---|---|
| Ruby | Homebrew portable-ruby | the obvious source (`ruby-builder`) is not redistributable at all |
| Rust | `rustup-init` | component trees need an installer; Windows is an `.msi` |
| **Java** | **Temurin archive** | **none — extract and run** |

Temurin also ships `.tar.gz` for macOS/Linux and `.zip` for Windows, both of which the platform's
`Decompressor` reads natively. **Windows is supported here**, unlike Ruby.

Two layout details that would otherwise bite:

- On macOS the archive is an **app bundle**: the JDK is at `<dir>/Contents/Home/bin/java`, not
  `<dir>/bin/java`. A search that stops one level down finds nothing. `JavaSdkType.findToolExecutable`
  handles both shapes.
- It must be a **full JDK, not a JRE** — single-file source launch compiles in memory and needs
  `jdk.compiler`.

## Testing the lexer without an IDE

```bash
./gradlew lexDump -PlexFile=examples/Hello.java
```

Dumps the token stream and asserts the cases that silently corrupt a whole file when wrong — a
text block ending early at an inner `"`, or `/**` mistaken for an empty block comment. `Hello.java`
is valid Java and verified to run.

A Kotlin gotcha worth remembering if you edit the lexer: **Kotlin block comments nest**, so writing
a literal comment-opener inside a KDoc opens a comment that never closes. The KDoc there spells the
sequences out in words for that reason. Likewise, write the end-of-input sentinel as the named
`EOF` constant — a NUL escape lands in the file as a raw NUL byte and makes git treat the source as
binary.

## Build
```
JAVA_HOME=<a JBR 17> ./gradlew buildPlugin      # -> build/distributions/java-portable-*.zip
JAVA_HOME=<a JBR 17> ./gradlew runIde           # sandbox IDE for testing
JAVA_HOME=<a JBR 17> ./gradlew verifyPlugin     # Plugin Verifier, against PhpStorm
```

`verifyPlugin` targets **PhpStorm, not IntelliJ IDEA** — this plugin declares
`<incompatible-with>com.intellij.java</incompatible-with>`, so IDEA is the one IDE it will never
run in. Verifying there would check a configuration that cannot occur and miss the ones that can.

Note also that `plugin.xml` deliberately does **not** depend on `com.intellij.modules.java`. Doing
so would make the plugin uninstallable in exactly the IDEs it exists for.

## Releasing
Publishing is tag-triggered; `.github/workflows/publish.yml` refuses to run when the tag disagrees
with `version` in `build.gradle.kts`. **The first release must be uploaded by hand** at
<https://plugins.jetbrains.com/plugin/add> — the Marketplace API rejects a plugin it has never seen.
CI takes over from the second release on.
