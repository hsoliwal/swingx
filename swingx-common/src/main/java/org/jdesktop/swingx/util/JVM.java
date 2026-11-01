/*
 * $Id: JVM.java 4236 2012-08-13 17:32:35Z kschaefe $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.jdesktop.swingx.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

/**
 * Deals with the different version of the Java Virtual Machine. <br>
 */
@SuppressWarnings("nls")
public class JVM {

  public final static int JDK1_0 = 1000;
  public final static int JDK1_1 = 1100;
  public final static int JDK1_2 = 1200;
  public final static int JDK1_3 = 1300;
  public final static int JDK1_4 = 1400;
  public final static int JDK1_5 = 1500;
  public final static int JDK1_6 = 1600;
  public final static int JDK1_6N = 1610;
  public final static int JDK1_7 = 1700;
  public final static int JDK1_8 = 1800;
  public final static int JDK1_9 = 1900;

  /** First feature release numbered by JEP 223 ("9", "11.0.2", "21.0.9", "22-ea"). */
  private static final int FIRST_JEP223_FEATURE = 9;
  /** Distance between two consecutive release constants (JDK1_8 = 1800, JDK1_9 = 1900). */
  private static final int VERSION_STEP = 100;
  /** Returned by {@link #jep223Version(String)} for a string that is not a JEP 223 version. */
  private static final int NOT_JEP223 = 0;
  /** Group of {@link #JEP223_VERSION} holding the feature release number. */
  private static final int FEATURE_GROUP = 1;
  /** A JEP 223 version: feature number, then an optional ".", "+" or "-" suffix. */
  private static final Pattern JEP223_VERSION = Pattern.compile("([1-9][0-9]{0,2})(?:[.+-].*)?");


  private static JVM current;
  static {
    current = new JVM();
  }

  /**
   * @return the current JVM object
   */
  public static JVM current() {
    return current;
  }

  private int jdkVersion;

  /**
   * Creates a new JVM data from the <code>java.version</code>
   * System property
   *  
   */
  public JVM() {
    this(System.getProperty("java.version"));
  }

  /**
   * Constructor for the OS object
   */
  public JVM(String p_JavaVersion) {
    if (p_JavaVersion.startsWith("1.9.")) {
      jdkVersion = JDK1_9;
    } else  if (p_JavaVersion.startsWith("1.8.")) {
      jdkVersion = JDK1_8;
    } else if (p_JavaVersion.startsWith("1.7.")) {
      jdkVersion = JDK1_7;
    } else if (p_JavaVersion.startsWith("1.6.")) {
      for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
          if ("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel".equals(info.getClassName())) {
              jdkVersion = JDK1_6N;
              break;
          }
      }
      
      jdkVersion = jdkVersion == 0 ? JDK1_6 : jdkVersion;
    } else if (p_JavaVersion.startsWith("1.5.")) {
      jdkVersion = JDK1_5;
    } else if (p_JavaVersion.startsWith("1.4.")) {
      jdkVersion = JDK1_4;
    } else if (p_JavaVersion.startsWith("1.3.")) {
      jdkVersion = JDK1_3;
    } else if (p_JavaVersion.startsWith("1.2.")) {
      jdkVersion = JDK1_2;
    } else if (p_JavaVersion.startsWith("1.1.")) {
      jdkVersion = JDK1_1;
    } else if (p_JavaVersion.startsWith("1.0.")) {
      jdkVersion = JDK1_0;
    } else {
      //FLLDCIM: MRL - JEP 223 versions map to JDK1_0 + feature * VERSION_STEP (21.0.9 -> 3100)
      int jep223 = jep223Version(p_JavaVersion);
      // unknown version, assume 1.3
      jdkVersion = jep223 == NOT_JEP223 ? JDK1_3 : jep223;
    }
  }

  /**
   * Returns the release constant of a JEP 223 version string, {@code JDK1_0 + feature * 100}:
   * "9" gives {@link #JDK1_9}, "11.0.2" gives 2100 and "21.0.9" gives 3100.
   *
   * @param p_JavaVersion a {@code java.version} value
   * @return the release constant, or 0 when the value is not a JEP 223 version
   */
  //FLLDCIM: MRL - atom: JEP 223 java.version -> release constant (JVMTest)
  static int jep223Version(String p_JavaVersion) {
    Matcher matcher = JEP223_VERSION.matcher(p_JavaVersion);
    if (!matcher.matches()) {
      return NOT_JEP223;
    }
    int feature = Integer.parseInt(matcher.group(FEATURE_GROUP));
    return feature < FIRST_JEP223_FEATURE ? NOT_JEP223 : JDK1_0 + feature * VERSION_STEP;
  }

  public boolean isOrLater(int p_Version) {
    return jdkVersion >= p_Version;
  }

  public boolean isOneDotOne() {
    return jdkVersion == JDK1_1;
  }

  public boolean isOneDotTwo() {
    return jdkVersion == JDK1_2;
  }

  public boolean isOneDotThree() {
    return jdkVersion == JDK1_3;
  }

  public boolean isOneDotFour() {
    return jdkVersion == JDK1_4;
  }

  public boolean isOneDotFive() {
    return jdkVersion == JDK1_5;
  }

  public boolean isOneDotSix() {
    return jdkVersion == JDK1_6 || isOneDotSixUpdateN();
  }

    /**
     * Determines if the version of JDK1_6 has Nimbus Look and Feel installed.
     * 
     * @return {@code true} if Nimbus is available and the version is 1.6;
     *         {@code false} otherwise
     */
  public boolean isOneDotSixUpdateN() {
      return jdkVersion == JDK1_6N;
  }
  
  public boolean isOneDotSeven() {
      return jdkVersion == JDK1_7;
  }

  public boolean isOneDotEight() {
    return jdkVersion == JDK1_8;
  }

  public boolean isOneDotNine() {
    return jdkVersion == JDK1_9;
  }

}