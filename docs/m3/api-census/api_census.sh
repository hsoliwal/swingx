#!/usr/bin/env bash
# Copyright (c) 2026 Synexia contributors.
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
#
# API-CENSUS v1 driver: census every tree with ApiCensus.java, then report every exported
# declaration that a reference tree has and the superset tree lacks.
#
#   api_census.sh <repo> <work-dir> <superset-label=rev> <ref-label=rev>... [-- <chain-label=rev>...]
#
# Writes <work-dir>/<label>.census.tsv per tree and <work-dir>/API_LOSS.raw.tsv with columns
#   fqcn  key  loss_kind  present_in  absent_in_chain  detail  path  written
# loss_kind       = MEMBER_ABSENT (fqcn+key missing from the superset) or
#                   SIGNATURE_CHANGED (fqcn+key present, but the declaration as written differs)
# present_in      = reference labels that declare the member exactly as written
# absent_in_chain = chain labels (superset history, oldest first) that no longer declare it as written
# written         = the reference declaration as written (type parameters, generic types)
# Reads src/$SCOPE/java sources (SCOPE=main by default, SCOPE=test for the test-source census);
# trees are materialised with git archive and deleted after use.
# Requires: git, tar, awk, sort and a JDK 17+ java (override with JAVA=...).
set -euo pipefail
repo=$1 work=$2; shift 2
here=$(cd "$(dirname "$0")" && pwd)
java=${JAVA:-java}
scope=${SCOPE:-main}
mkdir -p "$work"

census() {
  local label=${1%%=*} rev=${1#*=}
  local dir="$work/tree-$label"
  rm -rf "$dir" && mkdir -p "$dir"
  git -C "$repo" archive --format=tar "$rev" -- ":(glob)**/src/$scope/java/**/*.java" | tar -x -C "$dir"
  (cd "$dir" && find . -name '*.java' | sed 's|^\./||' | LC_ALL=C sort) > "$work/$label.files"
  "$java" "$here/ApiCensus.java" "$dir" "$work/$label.files" > "$work/$label.census.tsv"
  rm -rf "$dir"
  printf 'census\t%s\t%s\tfiles=%s\trows=%s\n' "$label" "$(git -C "$repo" rev-parse --short=12 "$rev")" \
    "$(wc -l < "$work/$label.files")" "$(wc -l < "$work/$label.census.tsv")" >&2
}

sup=${1%%=*}; census "$1"; shift
refs=() chain=()
while [ $# -gt 0 ] && [ "$1" != "--" ]; do refs+=("${1%%=*}"); census "$1"; shift; done
[ $# -gt 0 ] && shift
while [ $# -gt 0 ]; do chain+=("${1%%=*}"); census "$1"; shift; done

# k = fqcn+key (member identity), w = k+written form; a reference w the superset lacks is a loss
awk -F'\t' -v OFS='\t' -v sup="$sup" -v reflist="${refs[*]}" -v chainlist="${chain[*]:-}" '
  BEGIN { nr = split(reflist, rf, " "); for (i = 1; i <= nr; i++) isRef[rf[i]] = 1; nc = split(chainlist, ch, " ") }
  FNR == 1 { file = FILENAME; sub(/^.*\//, "", file); sub(/\.census\.tsv$/, "", file) }
  { k = $1 OFS $2; w = k OFS $5 }
  file == sup { supK[k] = 1; supW[w] = 1; next }
  { has[file, w] = 1 }
  isRef[file] && !(w in key) { key[w] = k; detail[w] = $3; path[w] = $4; written[w] = $5 }
  isRef[file] { refsOf[w] = refsOf[w] (refsOf[w] == "" ? "" : ",") file }
  END {
    for (w in refsOf) if (!(w in supW)) {
      gone = ""
      for (i = 1; i <= nc; i++) if (!((ch[i], w) in has)) gone = gone (gone == "" ? "" : ",") ch[i]
      kind = (key[w] in supK) ? "SIGNATURE_CHANGED" : "MEMBER_ABSENT"
      print key[w], kind, refsOf[w], (gone == "" ? "-" : gone), detail[w], path[w], written[w]
    }
  }' "$work/$sup.census.tsv" $(printf "$work/%s.census.tsv " "${refs[@]}" "${chain[@]}") \
  | LC_ALL=C sort > "$work/API_LOSS.raw.tsv"
printf 'loss\trows=%s\tmember_absent=%s\tsignature_changed=%s\n' "$(wc -l < "$work/API_LOSS.raw.tsv")" \
  "$(awk -F'\t' '$3 == "MEMBER_ABSENT"' "$work/API_LOSS.raw.tsv" | wc -l)" \
  "$(awk -F'\t' '$3 == "SIGNATURE_CHANGED"' "$work/API_LOSS.raw.tsv" | wc -l)" >&2
