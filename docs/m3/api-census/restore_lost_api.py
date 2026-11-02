#!/usr/bin/env python3
# Copyright (c) 2026 Synexia contributors.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
"""RESTORE-LOST-API v1: additive restoration of the members listed in API_LOSS.tsv.

Usage: python restore_lost_api.py <repo> <upstream-rev>

Blocks restored verbatim are copied from the upstream blob (git show <upstream-rev>:<path>), byte for byte
except for the target file's line ending; union members (same erasure, two written forms) keep the wider
upstream parameter types and the donor's behaviour.  Every operation checks that its anchor is unique and
skips itself when its result is already present, so a second run is a no-op (fixed point).
Prints one TSV line per operation: op, path, status (APPLIED | ALREADY | FAIL), detail.
"""
import subprocess
import sys

JXTABLE = "swingx-core/src/main/java/org/jdesktop/swingx/JXTable.java"
UTILS = "swingx-core/src/main/java/org/jdesktop/swingx/SwingXUtilities.java"

CTOR_OLD = ["    public JXTable(Vector<? extends Vector> rowData, Vector<?> columnNames) {",
            "        super(rowData, columnNames);"]
CTOR_NEW = ["    @SuppressWarnings({ \"unchecked\", \"rawtypes\" })",
            "    public JXTable(Vector<?> rowData, Vector<?> columnNames) {",
            "        //FLLDCIM: API-UNION - upstream Vector<?> parameter kept; the cast fits the JDK 9+ "
            "JTable(Vector<? extends Vector>, Vector<?>)",
            "        super((Vector<? extends Vector>) rowData, columnNames);"]

SIG2_OLD = ["    public static <T extends JComponent & Mnemonicable> void updateMnemonicBinding(T c, String pressed) {"]
SIG2_NEW = ["    public static void updateMnemonicBinding(JComponent c, String pressed) {"]
SIG3_OLD = ["    public static <T extends JComponent & Mnemonicable> void updateMnemonicBinding(T c, String pressed, "
            "String released) {",
            "        int m = c.getMnemonic();"]
SIG3_NEW = ["    public static void updateMnemonicBinding(JComponent c, String pressed, String released) {",
            "        int m = mnemonicOf(c);"]
DOC_UNION = ["     * <p>",
             "     * Components implementing {@link Mnemonicable} are read directly; any other",
             "     * component must expose a public {@code getMnemonic()} method, read reflectively."]
HELPER = ["    /**",
          "     * Reads the mnemonic of the given component: directly when it implements",
          "     * {@link Mnemonicable}, otherwise through its public {@code getMnemonic()} method.",
          "     */",
          "    //FLLDCIM: API-UNION - Mnemonicable fast path (franksorensen e77c279) + upstream reflective lookup",
          "    private static int mnemonicOf(JComponent c) {",
          "        if (c instanceof Mnemonicable) {",
          "            return ((Mnemonicable) c).getMnemonic();",
          "        }",
          "        try {",
          "            Method mtd = c.getClass().getMethod(\"getMnemonic\");",
          "            return (Integer) mtd.invoke(c);",
          "        } catch (RuntimeException e) {",
          "            throw e;",
          "        } catch (Exception e) {",
          "            throw new IllegalArgumentException(\"unable to access mnemonic\", e);",
          "        }",
          "    }",
          "    "]


class Doc:
    def __init__(self, repo, path):
        self.file = f"{repo}/{path}"
        self.path = path
        with open(self.file, encoding="iso-8859-1", newline="") as f:
            text = f.read()
        self.eol = "\r\n" if "\r\n" in text else "\n"
        self.lines = text.split(self.eol)

    def index(self, line, start=0):
        hits = [i for i, x in enumerate(self.lines) if x == line and i >= start]
        if len(hits) != 1:
            raise SystemExit(f"FAIL\t{self.path}\tanchor matched {len(hits)} times: {line!r}")
        return hits[0]

    def has(self, block):
        n = len(block)
        return any(self.lines[i:i + n] == block for i in range(len(self.lines) - n + 1))

    def save(self):
        with open(self.file, "w", encoding="iso-8859-1", newline="") as f:
            f.write(self.eol.join(self.lines))


def upstream(repo, rev, path):
    out = subprocess.run(["git", "-C", repo, "show", f"{rev}:{path}"], capture_output=True, check=True)
    return out.stdout.decode("iso-8859-1").replace("\r\n", "\n").split("\n")


def between(lines, first, last_excl):
    a = lines.index(first)
    return lines[a:lines.index(last_excl, a)]


def insert(doc, op, block, at):
    if doc.has(block):
        print(f"{op}\t{doc.path}\tALREADY\t{len(block)} lines")
        return
    doc.lines[at:at] = block
    print(f"{op}\t{doc.path}\tAPPLIED\t+{len(block)} lines at {at + 1}")


def replace(doc, op, old, new):
    if doc.has(new):
        print(f"{op}\t{doc.path}\tALREADY\t{len(new)} lines")
        return
    at = doc.index(old[0])
    if doc.lines[at:at + len(old)] != old:
        raise SystemExit(f"FAIL\t{doc.path}\t{op}: old block not found")
    doc.lines[at:at + len(old)] = new
    print(f"{op}\t{doc.path}\tAPPLIED\t-{len(old)} +{len(new)} lines at {at + 1}")


def restore_jxtable(repo, rev):
    u = upstream(repo, rev, JXTABLE)
    d = Doc(repo, JXTABLE)
    insert(d, "import-MouseEvent", ["import java.awt.event.MouseEvent;"],
           d.index("import java.awt.event.ActionEvent;") + 1)
    insert(d, "doc-popup-trigger", between(u, " * <h2>Location of Trigger for ComponentPopupMenu</h2>",
                                           " * <h2>Search</h2>"), d.index(" * <h2>Search</h2>"))
    cycle = "    private SortOrder[] sortOrderCycle;"
    insert(d, "field-popupTriggerLocation", u[u.index(cycle) + 1:u.index("    private Point popupTriggerLocation;") + 1],
           d.index(cycle) + 1)
    control = "     * Returns the column control visible property."
    popup = u[u.index("//---------------------- enhanced component popup support"):u.index(control) - 1]
    insert(d, "popup-trigger-methods", popup, d.index(control) - 2)
    convenience = "    // ---------------------- enhanced TableColumn/Model support: convenience"
    at_point = u.index("     * Returns the TableColumn at the given location or null if") - 1
    insert(d, "getColumn-Point", u[at_point:u.index(convenience)], d.index(convenience))
    replace(d, "ctor-Vector-union", CTOR_OLD, CTOR_NEW)
    d.save()


def restore_utils(repo):
    d = Doc(repo, UTILS)
    insert(d, "import-Method", ["import java.lang.reflect.Method;"],
           d.index("import java.lang.reflect.InvocationTargetException;") + 1)
    replace(d, "updateMnemonicBinding-2-union", SIG2_OLD, SIG2_NEW)
    replace(d, "updateMnemonicBinding-3-union", SIG3_OLD, SIG3_NEW)
    if not d.has(DOC_UNION):
        first = d.index("     * is activated.") + 1
        d.lines[first:first] = DOC_UNION
        second = d.index("     * mnemonic is deactivated.") + 1
        d.lines[second:second] = DOC_UNION
        print(f"doc-mnemonic-union\t{UTILS}\tAPPLIED\t+{2 * len(DOC_UNION)} lines")
    else:
        print(f"doc-mnemonic-union\t{UTILS}\tALREADY\t{len(DOC_UNION)} lines")
    paint = d.index("    static <C extends JComponent & BackgroundPaintable> void paintBackground(C comp, Graphics2D g) {")
    insert(d, "helper-mnemonicOf", HELPER, paint - 1)
    d.save()


if __name__ == "__main__":
    restore_jxtable(sys.argv[1], sys.argv[2])
    restore_utils(sys.argv[1])
