#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""BUILD-GREEN v1: derive TEST_BASELINE.tsv from the surefire failure TSVs in runs/.

Usage: python baseline_table.py <build-green-dir>

runs/superset-before-jdk21.tsv  superset head before the recipe (mvn verify, JDK 21)
runs/upstream-jdk21.tsv         upstream master 9e33bc0 + upstream_baseline.py adaptation (JDK 21)
runs/upstream-jdk17.tsv         the same on JDK 17
(written by surefire_failures.py; the upstream runs covered the classes in CLASSES).
runs/linux-*.tsv                R8/R9: the same runs of LookAndFeelAddonsSandboxTest and HyperlinkActionTest on
                                Linux without a GNOME desktop (WSL Ubuntu 24.04, Temurin 21.0.13 and 17.0.21,
                                xvfb, no registered browser or mail client); the runs/linux-*-gnome.tsv files
                                are the same runs with XDG_CURRENT_DESKTOP=GNOME and GTK 3 (the sandbox test
                                then passes, the system LAF being GTKLookAndFeel; HyperlinkActionTest fails
                                the same three tests either way)
runs/linux-monthview-10x.tsv    R10: BasicMonthViewUITest 10 times each on the superset and on upstream master,
                                Temurin 17 and 21 (one row per run: tests, failures, errors, skipped, failed
                                tests); the CI-only failure of testFirstMonthLocation is a CI_ONLY row below
runs/linux-rowheight-10x.tsv    R11: the same for TableRowHeightControllerTest, before (superset, upstream) and
                                after the recipe (superset); the CI-only error is a CI_ONLY row below
Every test that failed in any run gets one row; a test absent from a run of its class passed there. A test
that failed only in the Linux runs takes its run cells from them, prefixed "linux: " ("linux: NOT_RUN" for a
column without a Linux run).
After the recipe, `mvn clean verify` passed on JDK 21 and JDK 17: every row is PASS there except the
EXCLUDED ones, which the assumption reports as SKIPPED. The R8 row's after cells name both Linux cases
(skipped without a GNOME desktop, run and passed with XDG_CURRENT_DESKTOP=GNOME and GTK 3) and Windows.
"""
import csv
import sys
from pathlib import Path

RUNS = ("superset-before-jdk21", "upstream-jdk21", "upstream-jdk17")
PASS = "PASS"
SKIPPED = "SKIPPED"
EXCLUDED = "EXCLUDED_JDK9_PLUS"
EXCLUDED_NO_SYSTEM_LAF = "EXCLUDED_NO_SYSTEM_LAF"
EXCLUDED_NO_DESKTOP_ACTION = "EXCLUDED_NO_DESKTOP_ACTION"
FIXED_TEST = "FIXED_TEST"
TENFOLD = "runs/linux-monthview-10x.tsv"
ROWHEIGHT_TENFOLD = "runs/linux-rowheight-10x.tsv"
# R10: a failure seen on CI only; its run cells come from here (the Linux 10x runs in TENFOLD all passed)
CI_ONLY = {
    "org.jdesktop.swingx.plaf.basic.BasicMonthViewUITest#testFirstMonthLocation": (
        f"ci: FAILURE no hit - bounds must be null (pull_request run 42158910043, Temurin 17; the push run "
        f"42158006639 of the same commit passed); linux 10x Temurin 17 and 21: PASS ({TENFOLD})",
        f"linux 10x Temurin 21: PASS ({TENFOLD})", f"linux 10x Temurin 17: PASS ({TENFOLD})"),
    "org.jdesktop.swingx.table.TableRowHeightControllerTest#testSetModelRewiresModelListeners": (
        "ci: ERROR InvocationTargetException wrapping assertEquals(smallRowHeight, table.getRowHeight(0)) at line "
        "228 (push runs 42156150045 and 42173303266, Temurin 17 only; the other jobs and the pull_request runs of "
        f"the same commits passed); linux 10x Temurin 17 and 21: PASS ({ROWHEIGHT_TENFOLD})",
        f"linux 10x Temurin 21: PASS ({ROWHEIGHT_TENFOLD})", f"linux 10x Temurin 17: PASS ({ROWHEIGHT_TENFOLD})"),
}
# R8/R9: Linux run per run column (WSL Ubuntu 24.04 without a GNOME desktop); cells prefixed LINUX
LINUX_RUNS = {"superset-before-jdk21": "linux-superset-before-jdk21", "upstream-jdk21": "linux-upstream-jdk21",
              "upstream-jdk17": "linux-upstream-jdk17"}
LINUX = "linux: "
NOT_RUN = "NOT_RUN"
# after-jdk21 and after-jdk17 cell per resolution; PASS for every other resolution
AFTER = {EXCLUDED: SKIPPED,
         EXCLUDED_NO_SYSTEM_LAF: "linux: SKIPPED without a GNOME desktop, PASS with XDG_CURRENT_DESKTOP=GNOME and "
                                 "GTK 3 (maven.yml); windows: PASS",
         EXCLUDED_NO_DESKTOP_ACTION: "linux: SKIPPED where Desktop.isSupported(BROWSE or MAIL) is false (WSL: both, "
                                     "with or without GNOME); windows: PASS"}
HEADER = ("test",) + RUNS + ("cause", "resolution", "op", "after-jdk21", "after-jdk17")
# class or class#method -> (cause, resolution, recipe op)
CAUSES = {
    "org.jdesktop.swingx.calendar.CalendarUtilsTest": (
        "JDK 9+ CLDR locale data: Locale.GERMAN minimalDaysInFirstWeek 1 (JDK 6 data: 4)", "FIXED_ENV", "R1"),
    "org.jdesktop.swingx.plaf.basic.BasicMonthViewUITest": (
        "JDK 9+ CLDR locale data: Locale.GERMAN minimalDaysInFirstWeek 1 (JDK 6 data: 4)", "FIXED_ENV", "R1"),
    "org.jdesktop.swingx.JXMonthViewTest": (
        "machine zone Asia/Kolkata (+05:30): getTimeZone(tz, THREE_HOURS) finds no zone at +02:30", "FIXED_ENV", "R1"),
    "org.jdesktop.swingx.JXHeaderTest": (
        "TestCase.setUp() (loads the header addon) is never called under @RunWith(JUnit4); JUnit 4.13.2 method "
        "order runs the B tests first (JUnit 4.10 order hid it)", "FIXED_TEST", "R4"),
    "org.jdesktop.swingx.JXTreeTableUnitTest": (
        "FileSystemModel() roots at the working directory's drive; its first child is a file, so row 0 is a leaf",
        "FIXED_TEST", "R5"),
    "org.jdesktop.swingx.plaf.BuddyLayoutAndBorderTest#testPreferredWidth": (
        "JDK 9+ BasicTextUI adds caretMargin (1 px) to the text field preferred width; the layout reports insets "
        "only", EXCLUDED, "R6"),
    "org.jdesktop.swingx.plaf.PromptTextAreaUITest": (
        "JVM parsed JEP 223 java.version as unknown (assume 1.3): isOrLater(JDK1_6) false", "FIXED_MAIN", "R2"),
    "org.jdesktop.swingx.plaf.PromptTextFieldUITest": (
        "JVM parsed JEP 223 java.version as unknown (assume 1.3): isOrLater(JDK1_6) false", "FIXED_MAIN", "R2"),
    "org.jdesktop.swingx.search.FindTest#testEditorIncremental": (
        "klst-de 4173f8f JDK 9 guard (toMatchResult is not a Matcher) stopped recording lastMatchResult/lastRegEx",
        "FIXED_MAIN", "R3"),
    "org.jdesktop.swingx.search.FindTest": (
        "upstream JXEditorPane casts the JDK 9+ ImmutableMatchResult to Matcher", "SUPERSET_ALREADY_FIXED", "-"),
    "org.jdesktop.swingx.FindTest": (
        "upstream JXEditorPane cast (as above) and JXEditorPane resource lookup", "SUPERSET_ALREADY_FIXED", "-"),
    "org.jdesktop.swingx.plaf.LookAndFeelAddonsSandboxTest#testSystemAddon": (
        "Linux without a GNOME desktop (XDG_CURRENT_DESKTOP) or GTK: the system LAF is Metal, the cross-platform "
        "LAF, and getBestMatchAddonClassName() checks the cross-platform name first, so the Metal addon is the "
        "correct answer; the test's premise (system LAF distinct from cross-platform) does not hold there",
        EXCLUDED_NO_SYSTEM_LAF, "R8"),
    "org.jdesktop.swingx.hyperlink.HyperlinkActionTest#testVisited": (
        "Linux without a registered browser: Desktop.isSupported(BROWSE) is false (OPEN is the only supported "
        "action), the action is disabled and actionPerformed() never visits", EXCLUDED_NO_DESKTOP_ACTION, "R9"),
    "org.jdesktop.swingx.hyperlink.HyperlinkActionTest#testURIActionFactoryMail": (
        "Linux without a registered mail client: Desktop.isSupported(MAIL) is false, so the mailto: action is "
        "disabled", EXCLUDED_NO_DESKTOP_ACTION, "R9"),
    "org.jdesktop.swingx.hyperlink.HyperlinkActionTest#testURIActionNullMailEnabled": (
        "Linux without a registered mail client: Desktop.isSupported(MAIL) is false, so the MAIL action is "
        "disabled", EXCLUDED_NO_DESKTOP_ACTION, "R9"),
    "org.jdesktop.swingx.plaf.basic.BasicMonthViewUITest#testFirstMonthLocation": (
        "getRealizedMonthViewUI() packed, enlarged and showed the frame on the test thread, and the test read "
        "calendarGrid before the EDT had re-centred it with the 40 px slack (a hit at (19, 20))", FIXED_TEST, "R10"),
    "org.jdesktop.swingx.table.TableRowHeightControllerTest#testSetModelRewiresModelListeners": (
        "table.setModel() ran on the test thread; JXTable.setModel fires the model property change (the controller "
        "posts invokeLater(updateModel)) before it replaces the row sorter, so updateModel() could run on the EDT "
        "while the sorter was in transition and apply no row height", FIXED_TEST, "R11"),
}


def load(directory, run):
    path = Path(directory) / "runs" / f"{run}.tsv"
    if not path.exists():
        return None
    with open(path, encoding="utf-8", newline="") as f:
        return {f"{r['class']}#{r['method']}": f"{r['kind']} {r['message']}" for r in csv.DictReader(f, delimiter="\t")}


def cause(test):
    return CAUSES.get(test) or CAUSES[test.split("#")[0]]


def linux_cell(linux, run, test):
    if linux[run] is None:
        return LINUX + NOT_RUN
    return LINUX + linux[run].get(test, PASS)


def cells(test, results, linux):
    if test in CI_ONLY:
        return CI_ONLY[test]
    if any(test in results[run] for run in RUNS):
        return tuple(results[run].get(test, PASS) for run in RUNS)
    return tuple(linux_cell(linux, run, test) for run in RUNS)


def main(directory):
    results = {run: load(directory, run) for run in RUNS}
    linux = {run: load(directory, name) for run, name in LINUX_RUNS.items()}
    tests = sorted(set().union(*results.values(), *(r for r in linux.values() if r), CI_ONLY))
    with open(Path(directory) / "TEST_BASELINE.tsv", "w", encoding="utf-8", newline="\n") as out:
        out.write("\t".join(HEADER) + "\n")
        for test in tests:
            why, resolution, op = cause(test)
            after = AFTER.get(resolution, PASS)
            row = (test,) + cells(test, results, linux) + (why, resolution, op, after, after)
            out.write("\t".join(row) + "\n")
    print(f"TEST_BASELINE.tsv\trows={len(tests)}")


if __name__ == "__main__":
    main(sys.argv[1])
