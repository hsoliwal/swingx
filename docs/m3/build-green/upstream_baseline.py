#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""BUILD-GREEN v1 upstream baseline: adapt an upstream master worktree so that swingx-core and its tests
compile on JDK 17/21, then run the tests listed in TEST_BASELINE.tsv there.

Usage: python upstream_baseline.py <upstream-worktree> <superset-rev>
then:  mvn -B -ntp -pl swingx-core -am -Dtest=<classes> -DfailIfNoTests=false -Dmaven.test.failure.ignore=true test

Upstream master (9e33bc0) targets Java 6 and does not compile on JDK 11+. The adaptation is the smallest
one that compiles, applied to the scratch worktree only (never committed):
  - pom.xml: source/target 1.6 -> 11 (JDK 17/21 javac rejects 6; --add-exports needs 9+), metainf-services 1.3 -> 1.11 (the 1.3
    processor fails on the JDK 9+ NoSuchFileException);
  - swingx-core/pom.xml: export com.sun.java.swing.plaf.motif to the test compile (RendererIssues);
  - JXTable(Vector<?>, Vector<?>): cast for the JDK 9+ JTable(Vector<? extends Vector>, Vector<?>);
  - OVERLAY files: the superset's JDK 9+ compile fixes, taken verbatim from <superset-rev>.
Every tested class (the test and the main class it exercises) stays as upstream wrote it.
Each step checks its anchor and skips itself when already applied; prints one TSV line per step.
"""
import subprocess
import sys
from pathlib import Path

OVERLAY = (
    # JDK 11 adds Collection.toArray(IntFunction): toArray(null) is ambiguous
    "swingx-core/src/main/java/org/jdesktop/swingx/error/ErrorSupport.java",
    # JDK 9 TreeNode.children() returns Enumeration<? extends TreeNode>
    "swingx-core/src/main/java/org/jdesktop/swingx/tree/TreeUtilities.java",
    "swingx-core/src/test/java/org/jdesktop/swingx/test/TreeTableHelper.java",
    "swingx-core/src/test/java/org/jdesktop/swingx/test/TreeTableUtils.java",
    # JDK 9+ hides com.sun.java.swing and sun.font (interactive *VisualCheck classes, not run by surefire)
    "swingx-core/src/test/java/org/jdesktop/swingx/JXPanelVisualCheck.java",
    "swingx-core/src/test/java/org/jdesktop/swingx/JXListVisualCheck.java",
)
POM = "pom.xml"
POM_EDITS = (("<source>1.6</source>", "<source>11</source>"), ("<target>1.6</target>", "<target>11</target>"),
             # metainf-services 1.3 fails on JDK 9+ Filer.getResource (NoSuchFileException); the superset has 1.11
             ("<artifactId>metainf-services</artifactId>\n      <version>1.3</version>",
              "<artifactId>metainf-services</artifactId>\n      <version>1.11</version>"))
CORE_POM = "swingx-core/pom.xml"
CORE_POM_ANCHOR = "    <plugins>"
CORE_POM_PLUGIN = (
    "      <plugin>\n"
    "        <groupId>org.apache.maven.plugins</groupId>\n"
    "        <artifactId>maven-compiler-plugin</artifactId>\n"
    "        <configuration>\n"
    "          <testCompilerArgument>--add-exports=java.desktop/com.sun.java.swing.plaf.motif=ALL-UNNAMED"
    "</testCompilerArgument>\n"
    "        </configuration>\n"
    "      </plugin>\n")
JXTABLE = "swingx-core/src/main/java/org/jdesktop/swingx/JXTable.java"
JXTABLE_CTOR = "public JXTable(Vector<?> rowData, Vector<?> columnNames) {"
JXTABLE_SUPER = "super(rowData, columnNames);"
JXTABLE_CAST = "super((Vector) rowData, columnNames);"
ENCODING = "iso-8859-1"


def read(tree, path):
    return (Path(tree) / path).read_text(encoding=ENCODING).replace("\r\n", "\n")


def write(tree, path, text):
    (Path(tree) / path).write_text(text, encoding=ENCODING, newline="\n")


def report(step, path, status):
    print(f"{step}\t{path}\t{status}")


def overlay(tree, rev):
    for path in OVERLAY:
        blob = subprocess.run(["git", "-C", tree, "show", f"{rev}:{path}"], capture_output=True, check=True)
        text = blob.stdout.decode(ENCODING).replace("\r\n", "\n")
        status = "ALREADY" if read(tree, path) == text else "APPLIED"
        write(tree, path, text)
        report("overlay", path, status)


def edit_pom(tree):
    text = read(tree, POM)
    for old, new in POM_EDITS:
        step = "pom-" + new.split("\n")[-1].strip()
        if new in text and old not in text:
            report(step, POM, "ALREADY")
            continue
        if text.count(old) != 1:
            raise SystemExit(f"FAIL\t{POM}\t{old!r} matched {text.count(old)} times")
        text = text.replace(old, new)
        report(step, POM, "APPLIED")
    write(tree, POM, text)


def edit_core_pom(tree):
    text = read(tree, CORE_POM)
    if CORE_POM_PLUGIN in text:
        report("core-pom-motif-export", CORE_POM, "ALREADY")
        return
    anchor = CORE_POM_ANCHOR + "\n"
    if text.count(anchor) != 1:
        raise SystemExit(f"FAIL\t{CORE_POM}\tanchor matched {text.count(anchor)} times")
    write(tree, CORE_POM, text.replace(anchor, anchor + CORE_POM_PLUGIN))
    report("core-pom-motif-export", CORE_POM, "APPLIED")


def edit_jxtable(tree):
    text = read(tree, JXTABLE)
    lines = text.split("\n")
    at = [i for i, line in enumerate(lines) if line.strip() == JXTABLE_CTOR]
    if len(at) != 1:
        raise SystemExit(f"FAIL\t{JXTABLE}\tconstructor matched {len(at)} times")
    body = lines[at[0] + 1]
    if body.strip() == JXTABLE_CAST:
        report("jxtable-vector-cast", JXTABLE, "ALREADY")
        return
    if body.strip() != JXTABLE_SUPER:
        raise SystemExit(f"FAIL\t{JXTABLE}\tunexpected constructor body {body!r}")
    lines[at[0] + 1] = body.replace(JXTABLE_SUPER, JXTABLE_CAST)
    write(tree, JXTABLE, "\n".join(lines))
    report("jxtable-vector-cast", JXTABLE, "APPLIED")


if __name__ == "__main__":
    worktree, superset_rev = sys.argv[1], sys.argv[2]
    edit_pom(worktree)
    edit_core_pom(worktree)
    edit_jxtable(worktree)
    overlay(worktree, superset_rev)
