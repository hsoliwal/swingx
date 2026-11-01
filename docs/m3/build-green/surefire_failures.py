#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""BLLSE extractor for surefire XML reports: one TSV row per failing or erroring test case.

Usage: python surefire_failures.py <reactor-root> [label] [newer-than-epoch-seconds]

Reads every <module>/target/surefire-reports/TEST-*.xml under the reactor root (optionally only the
reports written after the given time, so stale reports of an earlier run are not counted).
Columns: label, module, class, method, kind (FAILURE | ERROR), exception, message (first line, collapsed).
"""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

KINDS = ("failure", "error")
MESSAGE_LIMIT = 220
REPORTS = "**/target/surefire-reports/TEST-*.xml"
HEADER = ("label", "module", "class", "method", "kind", "exception", "message")
LF = "\n"


def rows(root, label, newer_than):
    for report in sorted(Path(root).glob(REPORTS)):
        if report.stat().st_mtime < newer_than:
            continue
        module = report.parent.parent.parent.relative_to(root).as_posix()
        for case in ET.parse(report).getroot().iter("testcase"):
            for kind in KINDS:
                node = case.find(kind)
                if node is None:
                    continue
                message = node.get("message") or (node.text or "").strip().split("\n")[0]
                yield (label, module, case.get("classname", ""), case.get("name", ""),
                       kind.upper(), node.get("type", ""), " ".join(message.split())[:MESSAGE_LIMIT])


def main(argv):
    root = Path(argv[1]).resolve()
    label = argv[2] if len(argv) > 2 else "run"
    newer_than = float(argv[3]) if len(argv) > 3 else 0.0
    out = sys.stdout
    out.reconfigure(newline=LF)  # LF on every platform, also on Windows consoles and pipes
    out.write("\t".join(HEADER) + "\n")
    for row in rows(root, label, newer_than):
        out.write("\t".join(row) + "\n")


if __name__ == "__main__":
    main(sys.argv)
