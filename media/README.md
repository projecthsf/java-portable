# Marketplace media

`marketplace/*.png` are the screenshots for the JetBrains Marketplace listing, at the same
2400×1520 the other Portable plugins use.

## Regenerating

```bash
cd media/src && python3 build.py
```

One 1200×760 HTML page per screen, rendered by headless Chrome at
`--force-device-scale-factor=2`. `shell.css` holds the shared IDE chrome and the Darcula palette.

Chrome on macOS **writes the PNG correctly but exits non-zero** on harmless
`CVDisplayLinkCreateWithCGDisplay` errors, so the script checks the output file rather than the
exit code. Don't "fix" that with `check=True`.

## The project in the screenshots is a PHP project, on purpose

The tree shows `OrderController.php`, `Invoice.php`, `composer.json` — and one `Hello.java`. That
is the entire pitch of this plugin in one image: you are in PhpStorm, there is a Java file, and
you do not want to install IntelliJ IDEA to read it. A generic Java project would show the
feature and hide the reason.

The settings tree includes a `PHP` node for the same reason — these screens are PhpStorm, not IDEA.

## The palette is read, not guessed

Every colour in `shell.css` is the Darcula scheme's own value, extracted from the IDE
distribution:

```bash
unzip -p <ide>/lib/app-client.jar DefaultColorSchemesManager.xml > schemes.xml
# then read the <scheme name="Darcula"> block; FONT_TYPE 2 means italic
```

| Key | Darcula | Used for |
|---|---|---|
| `DEFAULT_KEYWORD` | `#CC7832` | keywords, incl. `var` / `record` / `int` |
| `DEFAULT_STRING` | `#6A8759` | strings, text blocks, char literals |
| `DEFAULT_NUMBER` | `#6897BB` | numbers |
| `DEFAULT_LINE_COMMENT` | `#808080` | `//` comments |
| `DEFAULT_DOC_COMMENT` | `#629755` *italic* | Javadoc |
| `DEFAULT_METADATA` | `#BBB529` | annotations |
| `DEFAULT_IDENTIFIER` | `#A9B7C6` | everything else |

**`String` is an identifier, not a keyword.** It is a class name, and `JavaTokenTypes.KEYWORDS`
does not contain it — `./gradlew lexDump` confirms `JAVA_IDENTIFIER String`. An earlier draft of
these renders coloured it orange, which overstated what the plugin does.

## Keeping the listing honest

Every string in these renders is copied from the plugin's own source. **If you change UI copy,
change it here and re-render.** The run-marker position matters too: `JavaRunLineMarkerContributor`
anchors on the `main` identifier after `void`, so the ▶ belongs on the `main` line — not line 1
(that is the Ruby plugin's convention, where any file is runnable).

## What is deliberately missing

No completion / go-to-definition / hover / diagnostics screens. Those are Eclipse JDT LS features
and the server has **never been started** — only the download URL was checked. Add those screens
once jdtls has actually been seen running, not before.
