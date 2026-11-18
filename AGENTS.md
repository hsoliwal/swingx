# SwingX agent instructions

## Clean output rule (owner directive, 2026-11-14) - read first

This rule overrides any older text in this file.

- This fork is clean output: one coherent SwingX repository. M3 work lives
  inside the project's own classes and packages, written the way the project
  writes its code. It is not stamped across folders.
- Do not add `m3/`, `.m3/`, `docs/m3/` or `tools/m3*` folders.
- Do not add any of the following. They belong in `hsoliwal/com.synexia`, the
  convergence workspace:
  - qualification bundles;
  - evidence, receipts and benchmark tables;
  - work orders;
  - donor copies;
  - recipe sources or recipe resources.
- Do not add parallel `M3`-prefixed types or `*.m3` packages where an existing
  class owns the behaviour. Change that class in place, keep it internal unless
  the owner admits new API, and follow the project's naming.
- A pull request here contains only the product change, plus its tests in the
  project's own test layout. Put the proof material in the paired `com.synexia`
  pull request and link to it.
- This fork's merged pull-request history shows the intended direction.



## Priority 0 — universal additive superset, full-history merge and last-checked rule (2026-11-18)

Preserve the **backward-compatible optimized superset** of all applicable accepted upstream/fork capabilities, source/binary/public APIs, behavior, fixes, platform/runtime contracts and previous qualified optimizations. Compare candidate algorithms and branches on differential correctness, worst-case complexity, CPU/heap/native cost, scalability and maintainability; choose the best **proven** implementation, allowing compatible additive API extensions but no silent removal. Before merge, inspect the earliest reachable ancestors across every relevant branch/tag, old merged/closed/reverted PR, merge base and both merge parents, to identify bad commits, lost code, removed APIs and lost optimizations; never rebase `develop`. Persist last-checked date, precise ref/head SHA, scope, oldest ancestor checked, last verified SHA, branch/PR coverage, loss findings and PENDING/PARTIAL/VERIFIED/HOLD in the canonical Synexia checkpoint register. An observed HEAD is **not** an exhaustive history audit. First-party separable new work prefers Apache-2.0; preserve original upstream and derivative license/NOTICE obligations. LLM atomization/patternization does not by itself change copyright or code licensing. Source-changing work follows canonical Synexia Maven/OpenRewrite recipe/historical-proof gates; public product forks remain clean postimage-only receivers. Policy and initial explicitly **PENDING** repository inventory: [Synexia PR #18764](https://github.com/hsoliwal/com.synexia/pull/18764).



## Superset merge and history-audit rule (2026-11-18)

Read [.github/MERGE_RULE.md](.github/MERGE_RULE.md) before changing an existing implementation or integrating any branch or PR.
Preserve the supported additive API/behavior superset and select implementations by correctness, predictability, measured performance, then elegance.
Audit all reachable history back to every root, all branches/tags and all PR states for lost code or regressions; preserve append-only history.
Record actual UTC coverage and remaining work in [.github/HISTORY_AUDIT.tsv](.github/HISTORY_AUDIT.tsv); policy installation is not a completed source-history audit.
Prefer verified Apache-2.0 sources and independent implementations while preserving all existing third-party rights; LLM rewriting does not remove license obligations.
Detailed private recipe/proof records remain in canonical Synexia; the compact local policy and checkpoint make the rule discoverable here.
<!-- MERGE-SUPERSET-HISTORY-20261118-2 -->

## Additive historical-convergence owner directive (2026-11-18)

Before every merge or API change, inspect all accessible Git ancestry to its oldest roots, historical branches, tags, and paginated open, merged, rejected and closed PRs including head/merge refs. Compare actual before/after owner file and member/atom source, tests, behavior, performance and native/VM contracts; ancestry-only merges may have lost code. Catalogue bad commits and restore proven capabilities additively, without rewriting develop history.

Select best-of-best correct atoms and combine compatible implementations into an additive superset. Preserve original public APIs, ABI, reflection/serialization, concurrency, UI threading and ownership, performance/resource guarantees and existing tests. Reusable recipes, source history-loss evidence and patternization are owned by `hsoliwal/com.synexia`; receiving product repositories retain eligible postimages and tests.

Prefer Apache-2.0 only for original contributions with sufficient rights. Keep all inherited licenses, GPL/EPL/Apache exceptions, attributions, notices and source lineage. Independently implementing an abstract algorithm can be lawful, but an LLM's code translation is not a license or copyright bypass; check applicable patents.

Record exact audit date/commit and exhaustive root/ref/PR/source coverage in `hsoliwal/com.synexia/docs/M3-SCALE/invariants/SYNEXIA-HISTORY-REVIEW-LAST-CHECKED.tsv`. Label partial scans PARTIAL and never assert complete review without executed proof.

## MOP SDK — documented semantic atoms and machine-classified patterns (2026-11-18)

Before any source-changing task, follow the [canonical Synexia atom/pattern law](https://github.com/hsoliwal/com.synexia/blob/develop/docs/M3-SCALE/invariants/SYNEXIA-UNIVERSAL-API-ATOM-PATTERN-EVOLUTION-1.md) and [MOP SDK lattice proposal](https://github.com/hsoliwal/com.synexia/pull/18784). Descend repository -> project -> module -> package -> file -> declarations -> meaningful behavioral atoms; preserve original API/ABI, identity, effects, ordering, resource, native, serialization and concurrency contracts. Recognize existing algorithms/patterns by versioned catalogue ID, typed roles, applicability and proof. Where no qualified match exists, retain the code and register `UNKNOWN_ALGORITHM`, `UNKNOWN_PATTERN` or `UNPROVEN_MATCH` with source pin, owner/atom, effects, dependencies, rationale and evidence status. Link this typed residue from source-bound documentation or an **already supported** annotation; never create an uncompilable marker or invent an admitted pattern. Recompose upward through the actual affected dependency closure, with parallel independent file analysis and serial proof-gated canonical mutation. Preserve best-qualified additive history and source/provenance; pattern labels, hashes and LLM output are never equivalence proof. Canonical recipe, catalogue, evidence and correction memory stay in Synexia; clean product targets receive only qualified postimages. **Policy binding only**: full history, compiler, recipe, test, runtime and pattern coverage are NOT_RUN unless proven separately.
