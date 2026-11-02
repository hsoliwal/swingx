#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""BUILD-GREEN v1: derive TEST_BASELINE.tsv from the surefire failure TSVs in runs/.

Usage: python baseline_table.py <build-green-dir>

runs/superset-before-jdk21.tsv  superset head before the recipe (mvn verify, JDK 21)
runs/upstream-jdk21.tsv         upstream master 9e33bc0 + upstream_baseline.py adaptation (JDK 21)
runs/upstream-jdk17.tsv         the same on JDK 17
(written by surefire_failures.py; the upstream runs covered the classes in CLASSES).
Every test that failed in any run gets one row; a test absent from a run of its class passed there.
After the recipe, `mvn clean verify` passed on JDK 21 and JDK 17: every row is PASS there except the
EXCLUDED ones, which the assumption reports as SKIPPED.
"""
import csv
import sys
from pathlib import Path

RUNS = ("superset-before-jdk21", "upstream-jdk21", "upstream-jdk17")
PASS = "PASS"
SKIPPED = "SKIPPED"
EXCLUDED = "EXCLUDED_JDK9_PLUS"
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
}


def load(directory, run):
    with open(Path(directory) / "runs" / f"{run}.tsv", encoding="utf-8", newline="") as f:
        return {f"{r['class']}#{r['method']}": f"{r['kind']} {r['message']}" for r in csv.DictReader(f, delimiter="\t")}


def cause(test):
    return CAUSES.get(test) or CAUSES[test.split("#")[0]]


def main(directory):
    results = {run: load(directory, run) for run in RUNS}
    tests = sorted(set().union(*results.values()))
    with open(Path(directory) / "TEST_BASELINE.tsv", "w", encoding="utf-8", newline="\n") as out:
        out.write("\t".join(HEADER) + "\n")
        for test in tests:
            why, resolution, op = cause(test)
            after = SKIPPED if resolution == EXCLUDED else PASS
            row = (test,) + tuple(results[run].get(test, PASS) for run in RUNS) + (why, resolution, op, after, after)
            out.write("\t".join(row) + "\n")
    print(f"TEST_BASELINE.tsv\trows={len(tests)}")


if __name__ == "__main__":
    main(sys.argv[1])
