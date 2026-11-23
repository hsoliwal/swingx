# StarRating public OpenRewrite 8.17.1 parse-only proof

**Scope:** Four independent JUnit5 tests of two existing SwingX receiver files,
pinned by SHA-256. These tests use **public** OpenRewrite 8.17.1 JavaParser
and assert that the receiver implementation and JUnit4 test can be parsed
into Java LSTs and printed byte-for-byte identically. No private Synexia
recipe engine is copied, installed, modified, or reimplemented.

This is a diagnostic parser/printer gate ONLY. It is not the canonical
`M3HashPinnedJavaSnapshotRecipe` `Recipe.run`, `Result.getAfter`,
fixed-point, source-refusal or source-promotion proof. Do not merge the
SwingX receiver based only on this gate.

## Run in the public SwingX receiving branch

```sh
mvn -B -ntp -DfailIfNoTests=true -Dm3.receiver.root="$PWD" \
  -f proofs/starrating-openrewrite-parser-20261122/pom.xml test
```

The GitHub Actions workflow runs the same command with Temurin 21 and fails
closed unless Surefire emits exactly four passed JUnit5 tests, zero skipped,
errors or failures. An action artifact preserves the real XML. Independent
full SwingX Maven/JUnit4 receiver proof is run #44992499865 (11/17/21 PASS).

**Privacy:** Source files are already first-party published SwingX GPL/LGPL
receiver candidates. No private Synexia source, credential or inaccessible
repository checkout is needed. All canonical Maven/OpenRewrite and platform
admission remains owned by private Synexia and gated separately.
