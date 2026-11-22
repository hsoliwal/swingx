// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.synexia.proof;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Parser;
import org.openrewrite.SourceFile;
import org.openrewrite.java.JavaParser;
import org.openrewrite.tree.ParseError;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Independent public parser/printer qualification of already source-sealed
 * SwingX receiver files. This never generates or edits the product sources.
 * It is NOT execution of the private canonical Synexia transformation recipe.
 */
final class StarRatingOpenRewriteParserProofTest {
    private static final String JAVA =
            "swingx-core/src/main/java/org/jdesktop/swingx/JXStarRating.java";
    private static final String JUNIT4 =
            "swingx-core/src/test/java/org/jdesktop/swingx/JXStarRatingTest.java";
    private static final String JAVA_SHA256 =
            "6b79454534ba4aeb33f9390f858d83ca8d84c88ea497aeb8c6bc731160c30b7e";
    private static final String JUNIT4_SHA256 =
            "a789663b7f48e272f38d7f1ce9e80f87c0bbb00d914f58df3110bd96d34a9603";

    private static Path receiverRoot() {
        return Path.of(System.getProperty("m3.receiver.root", "../.."))
                .toAbsolutePath().normalize();
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException unavailable) {
            throw new AssertionError(unavailable);
        }
    }

    private static String exact(String relative, String digest) throws IOException {
        Path path = receiverRoot().resolve(relative);
        byte[] bytes = Files.readAllBytes(path);
        assertEquals(digest, sha256(bytes), "Receiver source or test SHA256 drift: " + relative);
        String text = new String(bytes, StandardCharsets.UTF_8);
        assertEquals(digest, sha256(text.getBytes(StandardCharsets.UTF_8)),
                "Non-UTF8 or lossy source decoding: " + relative);
        return text;
    }

    private static List<SourceFile> parse(Map<String, String> files) {
        var ctx = new InMemoryExecutionContext(error -> {
            throw new AssertionError("OpenRewrite parser produced an error", error);
        });
        var inputs = files.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> Parser.Input.fromString(Path.of(e.getKey()), e.getValue()))
                .toList();
        return JavaParser.fromJavaVersion().build().parseInputs(inputs, null, ctx).toList();
    }

    @Test void productSourceIsExactAndPrintedUnchanged() throws IOException {
        String source = exact(JAVA, JAVA_SHA256);
        List<SourceFile> parsed = parse(Map.of(JAVA, source));
        assertEquals(1, parsed.size());
        assertFalse(parsed.get(0) instanceof ParseError, "Unexpected Java source parse failure");
        assertEquals(Path.of(JAVA), parsed.get(0).getSourcePath());
        assertEquals(source, parsed.get(0).printAll(), "OpenRewrite changed Java source text");
    }

    @Test void realJunit4TestIsExactAndPrintedUnchanged() throws IOException {
        String source = exact(JUNIT4, JUNIT4_SHA256);
        List<SourceFile> parsed = parse(Map.of(JUNIT4, source));
        assertEquals(1, parsed.size());
        assertFalse(parsed.get(0) instanceof ParseError, "Unexpected JUnit4 source parse failure");
        assertEquals(Path.of(JUNIT4), parsed.get(0).getSourcePath());
        assertEquals(source, parsed.get(0).printAll(), "OpenRewrite changed JUnit4 test text");
    }

    @Test void bothReviewedSwingxPathsCoParseWithoutReorderingOrLoss() throws IOException {
        Map<String, String> originals = Map.of(JAVA, exact(JAVA, JAVA_SHA256),
                JUNIT4, exact(JUNIT4, JUNIT4_SHA256));
        List<SourceFile> parsed = parse(originals);
        assertEquals(2, parsed.size(), "Two exact receiver targets must survive co-parse");
        for (SourceFile source : parsed) {
            assertFalse(source instanceof ParseError, "Source was not a Java LST");
            String path = source.getSourcePath().toString().replace('\\', '/');
            assertTrue(originals.containsKey(path), "Parser emitted unreviewed path " + path);
            assertEquals(originals.get(path), source.printAll(), "Co-parse changed source text");
        }
    }

    @Test void noReceiverMutationOrUnreviewedPaths() throws IOException {
        Map<String, String> originals = Map.of(JAVA, exact(JAVA, JAVA_SHA256),
                JUNIT4, exact(JUNIT4, JUNIT4_SHA256));
        parse(originals);
        // Proof is read-only: files on disk retain their exact source seals.
        assertEquals(JAVA_SHA256, sha256(Files.readAllBytes(receiverRoot().resolve(JAVA))));
        assertEquals(JUNIT4_SHA256, sha256(Files.readAllBytes(receiverRoot().resolve(JUNIT4))));
    }
}
