/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */
package org.jdesktop.swingx.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.stream.Stream;

import org.junit.Test;

/**
 * Unit test for {@code JVM}: release detection for the 1.x and the JEP 223 version schemes
 * (BUILD-GREEN v1 R2, docs/m3/build-green/RECIPE_MEMORY.tsv).
 */
@SuppressWarnings("nls")
public class JVMTest {
    private static final String JAVA_6 = "1.6.0_45";
    private static final String JAVA_8 = "1.8.0_292";
    private static final String JAVA_9 = "9";
    private static final String JAVA_9_BUILD = "9+181";
    private static final String JAVA_11 = "11.0.2";
    private static final String JAVA_17 = "17.0.19";
    private static final String JAVA_21 = "21.0.9";
    private static final String JAVA_22_EA = "22-ea";
    private static final String FEATURE_8 = "8";
    private static final String UNKNOWN = "unknown";
    /** Release constant of JDK 11: JDK1_0 + 11 * 100. */
    private static final int JDK11 = 2100;
    /** Release constant of JDK 21: JDK1_0 + 21 * 100. */
    private static final int JDK21 = 3100;
    /** Result of {@code JVM.jep223Version} for a string outside the JEP 223 scheme. */
    private static final int NOT_JEP223 = 0;

    //FLLDCIM: TOP - atom JVM.jep223Version: feature release -> release constant
    @Test
    public void testJep223VersionMapsFeatureRelease() {
        assertEquals(JVM.JDK1_9, JVM.jep223Version(JAVA_9));
        assertEquals(JVM.JDK1_9, JVM.jep223Version(JAVA_9_BUILD));
        assertEquals(JDK11, JVM.jep223Version(JAVA_11));
        assertEquals(JDK21, JVM.jep223Version(JAVA_21));
    }

    @Test
    public void testJep223VersionRejectsOtherStrings() {
        assertEquals(NOT_JEP223, JVM.jep223Version(JAVA_8));
        assertEquals(NOT_JEP223, JVM.jep223Version(FEATURE_8));
        assertEquals(NOT_JEP223, JVM.jep223Version(UNKNOWN));
    }

    @Test
    public void testJep223ReleasesAreLaterThanJava9() {
        Stream.of(JAVA_9, JAVA_11, JAVA_17, JAVA_21, JAVA_22_EA).map(JVM::new).forEach(jvm -> {
            assertTrue(jvm.isOrLater(JVM.JDK1_7));
            assertTrue(jvm.isOrLater(JVM.JDK1_9));
            assertFalse(jvm.isOneDotThree());
        });
    }

    @Test
    public void testOneDotNineOnlyForJava9() {
        assertTrue(new JVM(JAVA_9).isOneDotNine());
        assertFalse(new JVM(JAVA_21).isOneDotNine());
    }

    @Test
    public void testLegacyAndUnknownVersionsUnchanged() {
        assertTrue(new JVM(JAVA_6).isOneDotSix());
        assertTrue(new JVM(JAVA_8).isOneDotEight());
        assertFalse(new JVM(JAVA_8).isOrLater(JVM.JDK1_9));
        assertTrue(new JVM(UNKNOWN).isOneDotThree());
    }

    @Test
    public void testCurrentJvmIsJep223Release() {
        // the build compiles for Java 11, so the test JVM always reports a JEP 223 version
        assertTrue(JVM.current().isOrLater(JVM.JDK1_9));
    }
}
