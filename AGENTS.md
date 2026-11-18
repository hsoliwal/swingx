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



## Additive historical-convergence owner directive (2026-11-18)

Before every merge or API change, inspect all accessible Git ancestry to its oldest roots, historical branches, tags, and paginated open, merged, rejected and closed PRs including head/merge refs. Compare actual before/after owner file and member/atom source, tests, behavior, performance and native/VM contracts; ancestry-only merges may have lost code. Catalogue bad commits and restore proven capabilities additively, without rewriting develop history.

Select best-of-best correct atoms and combine compatible implementations into an additive superset. Preserve original public APIs, ABI, reflection/serialization, concurrency, UI threading and ownership, performance/resource guarantees and existing tests. Reusable recipes, source history-loss evidence and patternization are owned by `hsoliwal/com.synexia`; receiving product repositories retain eligible postimages and tests.

Prefer Apache-2.0 only for original contributions with sufficient rights. Keep all inherited licenses, GPL/EPL/Apache exceptions, attributions, notices and source lineage. Independently implementing an abstract algorithm can be lawful, but an LLM's code translation is not a license or copyright bypass; check applicable patents.

Record exact audit date/commit and exhaustive root/ref/PR/source coverage in `hsoliwal/com.synexia/docs/M3-SCALE/invariants/SYNEXIA-HISTORY-REVIEW-LAST-CHECKED.tsv`. Label partial scans PARTIAL and never assert complete review without executed proof.
