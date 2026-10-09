#!/usr/bin/env python3
"""
Generates the JetBrains Marketplace screenshots for Java Portable.

Renders each screen as a 1200x760 HTML page, then screenshots it with headless
Chrome at device-scale 2 to produce the 2400x1520 PNGs the other Portable
plugins use (see python-portable/media/marketplace).

    python3 build.py

Every string shown here is copied from the plugin's own source — banner text,
button labels, dialog titles, the toolchain list. If you change UI copy, change
it here too or the listing drifts from the product.
"""

import html
import pathlib
import shutil
import subprocess
import sys

SRC = pathlib.Path(__file__).resolve().parent
OUT = SRC.parent / "marketplace"
WORK = SRC / ".render"
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

# --- the sample file shown in the editor -------------------------------------
# Valid Rust, chosen to exercise exactly the constructs the lexer handles
# specially: doc comments (//! and ///), attributes, lifetimes vs char
# literals, raw strings, macros, and suffixed hex literals.
CODE = [
    ('<span class="c">// A .java file inside a PHP project — no second IDE needed.</span>', ""),
    ("", ""),
    ('<span class="k">import</span> java.util.List;', ""),
    ("", ""),
    ('<span class="d">/** Javadoc highlights differently from an ordinary block comment. */</span>', ""),
    ('<span class="k">public class</span> Hello {', ""),
    ("", ""),
    ('    <span class="k">record</span> Greeting(String name, <span class="k">int</span> times) {', ""),
    ('        String render() {', ""),
    ('            <span class="k">return</span> <span class="s">"Hello, %s!"</span>.formatted(name);', ""),
    ("        }", ""),
    ("    }", ""),
    ("", ""),
    ('    <span class="an">@FunctionalInterface</span>', ""),
    ('    <span class="k">interface</span> Shout { String apply(String s); }', ""),
    ("", ""),
    ('    <span class="k">static final</span> String BANNER = <span class="s">"""</span>', ""),
    ('<span class="s">            A text block spans lines and may contain "quotes"</span>', ""),
    ('<span class="s">            without escaping any of them.</span>', ""),
    ('<span class="s">            """</span>;', ""),
    ("", ""),
    ('    <span class="k">public static</span> <span class="k">void</span> main(String[] args) {', "run"),
    ('        <span class="k">var</span> greeting = <span class="k">new</span> Greeting(<span class="s">"Java"</span>, <span class="n">2</span>);', ""),
    ('        Shout shout = s -&gt; s.toUpperCase();', ""),
    ("", ""),
    ('        <span class="k">for</span> (<span class="k">int</span> i = <span class="n">1</span>; i &lt;= greeting.times(); i++) {', ""),
    ('            System.out.println(i + <span class="s">". "</span> + greeting.render());', ""),
    ("        }", ""),
    ('        System.out.printf(<span class="s">"avg=%d hex=%X big=%d chr=%c%n"</span>, <span class="n">10</span> / <span class="n">4</span>, <span class="n">255</span>, <span class="n">1_000_000</span>, <span class="s">\'J\'</span>);', ""),
    ("    }", ""),
    ("}", ""),
]

TREE = """
      <div class="tree">
        <div class="hdr">PROJECT</div>
        <div class="row"><span class="ic ic-folder">▾</span>shop-api</div>
        <div class="row nest"><span class="ic ic-folder">▾</span>src</div>
        <div class="row" style="padding-left:54px"><span class="ic ic-php">php</span>OrderController.php</div>
        <div class="row" style="padding-left:54px"><span class="ic ic-php">php</span>Invoice.php</div>
        <div class="row nest"><span class="ic ic-folder">▾</span>legacy</div>
        <div class="row sel" style="padding-left:54px"><span class="ic ic-rs">J</span>Hello.java</div>
        <div class="row nest"><span class="ic ic-file">☰</span>composer.json</div>
      </div>
"""


def editor(rows=CODE, banner="", console="", extra=""):
    gutter, code = [], []
    for i, (line, mark) in enumerate(rows, 1):
        gutter.append(f'<span class="run">▶</span>' if mark == "run" else str(i))
        code.append(line if line else "&nbsp;")
    return f"""
      <div class="main">
        <div class="tabs"><div class="tab"><span class="ic ic-rs">J</span>Hello.java</div></div>
        {banner}
        <div class="editor">
          <div class="gutter">{"<br>".join(gutter)}</div>
          <div class="code">{"<br>".join(code)}</div>
          {extra}
        </div>
        {console}
      </div>
"""


def page(title, inner, footer=None):
    """footer=None gives the editor status bar; pass HTML for a dialog button row."""
    bottom = footer if footer is not None else (
        '<div class="status"><span>Temurin 21.0.12</span><span>UTF-8</span></div>'
    )
    return f"""<!doctype html>
<html><head><meta charset="utf-8"><link rel="stylesheet" href="shell.css"></head>
<body><div class="window">
  <div class="titlebar">
    <div class="lights"><i class="light red"></i><i class="light amber"></i><i class="light green"></i></div>
    <div class="title">{html.escape(title)}</div>
  </div>
  <div class="body">{inner}</div>
  {bottom}
</div></body></html>
"""


SETTINGS_FOOTER = """
  <div class="sfooter">
    <div class="btn">Cancel</div><div class="btn">Apply</div><div class="btn pri">OK</div>
  </div>
"""


BANNER_SETUP = """
        <div class="banner">
          <span class="info">i</span>
          <span>No JDK configured — download a portable one to run this file.</span>
          <span class="spacer"></span>
          <a>Download JDK…</a><a>Add from Disk…</a><a>Settings…</a>
        </div>
"""

BANNER_CI = """
        <div class="banner">
          <span class="info">i</span>
          <span>Turn on Java code intelligence — completion, go-to-definition and error highlighting.</span>
          <span class="spacer"></span>
          <a>Enable code intelligence</a><a>Settings…</a><a>Don't show again</a>
        </div>
"""

CONSOLE = """
        <div class="console">
          <div class="ch"><span style="color:#3f9c35">▶</span><span>Run:</span><span style="color:#d6d6d6">Hello.java</span></div>
          <div class="cb">1. Hello, Java!
2. Hello, Java!
PORTABLE
NO INSTALL
avg=2 hex=FF big=1000000 chr=J
A text block spans lines and may contain "quotes"
without escaping any of them.

<span class="dim">Process finished with exit code 0</span></div>
        </div>
"""

DIALOG_DOWNLOAD = """
          <div class="scrim"></div>
          <div class="dialog" style="width:470px">
            <div class="dh">Download JDK</div>
            <div class="dc">
              <div class="row2">
                <label>JDK:</label>
                <div class="field combo"><span>Temurin 21.0.12  ·  mac/aarch64  ·  190MB</span><span class="caret">▾</span></div>
              </div>
              <div class="hint" style="margin-left:98px">Downloaded to ~/.java-portable and registered as a Java SDK.</div>
            </div>
            <div class="df"><div class="btn">Cancel</div><div class="btn pri">OK</div></div>
          </div>
"""

DIALOG_CARGO = """
          <div class="scrim"></div>
          <div class="dialog" style="width:430px">
            <div class="dh">Run JDK Tool</div>
            <div class="dc">
              <div class="row2">
                <label>Command:</label>
                <div class="field combo" style="flex:0 0 190px"><span>jshell</span><span class="caret">▾</span></div>
              </div>
            </div>
            <div class="df"><div class="btn">Cancel</div><div class="btn pri">OK</div></div>
          </div>
"""

SETTINGS_TREE = """
      <div class="stree">
        <div class="trow"><span class="tw">▸</span>Appearance &amp; Behavior</div>
        <div class="trow"><span class="tw"></span>Keymap</div>
        <div class="trow"><span class="tw">▸</span>Editor</div>
        <div class="trow"><span class="tw"></span>Plugins</div>
        <div class="trow"><span class="tw">▾</span>Languages &amp; Frameworks</div>
        <div class="trow sel" style="padding-left:52px"><span class="ic ic-rs" style="margin-right:8px">R</span>Java Portable</div>
        <div class="trow" style="padding-left:52px">Markdown</div>
        <div class="trow"><span class="tw">▸</span>PHP</div>
        <div class="trow"><span class="tw">▸</span>Build, Execution, Deployment</div>
      </div>
"""

SETTINGS = """
      <div class="settings">
        <div class="sh">Java Portable</div>
        <div class="sb">
          <div class="sdesc">Java Portable JDKs. Downloads are stored under <span style="color:#cc7832">~/.java-portable</span> and shared with Java run configurations.</div>
          <div class="list">
            <div class="li sel">Temurin 21.0.12   —   ~/.java-portable/jdk-21</div>
            <div class="li">Temurin 17.0.12   —   ~/.java-portable/jdk-17</div>
          </div>
          <div class="btnrow">
            <div class="btn">Download JDK…</div><div class="btn">Add from Disk…</div>
            <div class="btn">Remove</div><div class="btn">Clean Up</div><div class="btn">Open Folder</div>
          </div>
          <div class="sep"></div>
          <div class="check"><span class="box">✓</span>Code intelligence (completion, navigation, errors)
            <span style="margin-left:14px"><span class="btn">Reinstall language server…</span></span>
          </div>
          <div class="sdesc" style="margin-top:6px">Runs the official Java language server (Eclipse JDT LS) on the selected JDK — fully offline. Also installs the free <b style="color:#bbbbbb">LSP4IJ</b> plugin (one click, may prompt a restart).</div>
        </div>
      </div>
"""

SCREENS = [
    ("01-syntax-highlighting", "Hello.java — shop-api", TREE + editor(), None),
    ("02-run-a-file", "Hello.java — shop-api", TREE + editor(console=CONSOLE), None),
    ("03-download-jdk", "Hello.java — shop-api", TREE + editor(extra=DIALOG_DOWNLOAD), None),
    ("04-setup-banner", "Hello.java — shop-api", TREE + editor(banner=BANNER_SETUP), None),
    ("05-code-intelligence", "Hello.java — shop-api", TREE + editor(banner=BANNER_CI), None),
    ("06-settings", "Settings", SETTINGS_TREE + SETTINGS, SETTINGS_FOOTER),
    ("07-jdk-tools", "Hello.java — shop-api", TREE + editor(extra=DIALOG_CARGO), None),
]


def main():
    if not pathlib.Path(CHROME).exists():
        sys.exit(f"Chrome not found at {CHROME}")
    WORK.mkdir(exist_ok=True)
    shutil.copy(SRC / "shell.css", WORK / "shell.css")
    OUT.mkdir(parents=True, exist_ok=True)

    for name, title, inner, footer in SCREENS:
        (WORK / f"{name}.html").write_text(page(title, inner, footer))

    for name, *_ in SCREENS:
        png = OUT / f"{name}.png"
        png.unlink(missing_ok=True)
        # Not check=True: headless Chrome on macOS writes the PNG correctly but still
        # exits non-zero on harmless CVDisplayLinkCreateWithCGDisplay errors. The
        # output file is the real success signal.
        subprocess.run(
            [CHROME, "--headless", "--disable-gpu", "--hide-scrollbars",
             "--force-device-scale-factor=2", "--window-size=1200,760",
             f"--screenshot={png}", str(WORK / f"{name}.html")],
            capture_output=True,
        )
        if not png.exists() or png.stat().st_size < 10_000:
            sys.exit(f"render failed: {name}")
        print(f"  rendered {name}.png")

    shutil.rmtree(WORK, ignore_errors=True)
    print(f"\n{len(SCREENS)} screenshots -> {OUT}")


if __name__ == "__main__":
    main()
