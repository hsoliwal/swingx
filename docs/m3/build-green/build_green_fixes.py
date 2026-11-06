#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
# Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
"""BUILD-GREEN v1: anchored, idempotent corrections that make `mvn verify` pass on JDK 11, 17 and 21.

Usage: python build_green_fixes.py <repo>

One operation per RECIPE_MEMORY.tsv row (column op); TEST_BASELINE.tsv records every test the recipe
touches, with its status on the superset and on upstream master.  The Doc/insert/replace helpers are the
ones of ../api-census/restore_lost_api.py: every anchor must match exactly once, and an operation whose
result is already present prints ALREADY, so a second run is a no-op (fixed point).
Prints one TSV line per operation: op, path, status (APPLIED | ALREADY | FAIL), detail.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "api-census"))
from restore_lost_api import Doc, insert, replace  # noqa: E402  (shared recipe kernel)

POM = "pom.xml"
JVM = "swingx-common/src/main/java/org/jdesktop/swingx/util/JVM.java"
EDITOR = "swingx-core/src/main/java/org/jdesktop/swingx/JXEditorPane.java"
HEADER_TEST = "swingx-core/src/test/java/org/jdesktop/swingx/JXHeaderTest.java"
TREETABLE_TEST = "swingx-core/src/test/java/org/jdesktop/swingx/JXTreeTableUnitTest.java"
BUDDY_TEST = "swingx-core/src/test/java/org/jdesktop/swingx/plaf/BuddyLayoutAndBorderTest.java"
SANDBOX_TEST = "swingx-plaf/src/test/java/org/jdesktop/swingx/plaf/LookAndFeelAddonsSandboxTest.java"

# R1 surefire: pin the test JVM's locale data, locale and time zone (CI = UTC, en_US)
POM_FORK = "          <forkMode>always</forkMode>"
POM_ARGLINE = [
    "          <!-- BUILD-GREEN v1 (docs/m3/build-green): the tests were written for JDK 6 locale data. Pin the",
    "               test JVM so results do not depend on the machine: CLDR, the JDK 9+ default, gives",
    "               Locale.GERMAN minimalDaysInFirstWeek 1 instead of 4 (CalendarUtilsTest, BasicMonthViewUITest),",
    "               and JXMonthViewTest needs a zone whose raw offset minus three hours exists (none for",
    "               Asia/Kolkata). COMPAT exists on JDK 9 to 22. -->",
    "          <argLine>${swingx.test.argLine}</argLine>"]
POM_PROPERTY_ANCHOR = ("    <project.generatedAnnotations>${project.generatedSourcesDirectory}/annotations"
                       "</project.generatedAnnotations>")
POM_PROPERTY = ["    <swingx.test.argLine>-Djava.locale.providers=COMPAT,CLDR -Duser.timezone=UTC -Duser.language=en "
                "-Duser.country=US</swingx.test.argLine>"]

# R2 JVM: JEP 223 version strings ("9", "11.0.2", "21.0.9", "22-ea") were "unknown, assume 1.3"
JVM_IMPORTS = ["import java.util.regex.Matcher;", "import java.util.regex.Pattern;", ""]
JVM_CONSTANT_ANCHOR = "  public final static int JDK1_9 = 1900;"
JVM_CONSTANTS = [
    "",
    "  /** First feature release numbered by JEP 223 (\"9\", \"11.0.2\", \"21.0.9\", \"22-ea\"). */",
    "  private static final int FIRST_JEP223_FEATURE = 9;",
    "  /** Distance between two consecutive release constants (JDK1_8 = 1800, JDK1_9 = 1900). */",
    "  private static final int VERSION_STEP = 100;",
    "  /** Returned by {@link #jep223Version(String)} for a string that is not a JEP 223 version. */",
    "  private static final int NOT_JEP223 = 0;",
    "  /** Group of {@link #JEP223_VERSION} holding the feature release number. */",
    "  private static final int FEATURE_GROUP = 1;",
    "  /** A JEP 223 version: feature number, then an optional \".\", \"+\" or \"-\" suffix. */",
    "  private static final Pattern JEP223_VERSION = Pattern.compile(\"([1-9][0-9]{0,2})(?:[.+-].*)?\");"]
JVM_FALLBACK_OLD = ["      // unknown version, assume 1.3", "      jdkVersion = JDK1_3;"]
JVM_FALLBACK_NEW = [
    "      //FLLDCIM: MRL - JEP 223 versions map to JDK1_0 + feature * VERSION_STEP (21.0.9 -> 3100)",
    "      int jep223 = jep223Version(p_JavaVersion);",
    "      // unknown version, assume 1.3",
    "      jdkVersion = jep223 == NOT_JEP223 ? JDK1_3 : jep223;"]
JVM_HELPER_ANCHOR = "  public boolean isOrLater(int p_Version) {"
JVM_HELPER = [
    "  /**",
    "   * Returns the release constant of a JEP 223 version string, {@code JDK1_0 + feature * 100}:",
    "   * \"9\" gives {@link #JDK1_9}, \"11.0.2\" gives 2100 and \"21.0.9\" gives 3100.",
    "   *",
    "   * @param p_JavaVersion a {@code java.version} value",
    "   * @return the release constant, or 0 when the value is not a JEP 223 version",
    "   */",
    "  //FLLDCIM: MRL - atom: JEP 223 java.version -> release constant (JVMTest)",
    "  static int jep223Version(String p_JavaVersion) {",
    "    Matcher matcher = JEP223_VERSION.matcher(p_JavaVersion);",
    "    if (!matcher.matches()) {",
    "      return NOT_JEP223;",
    "    }",
    "    int feature = Integer.parseInt(matcher.group(FEATURE_GROUP));",
    "    return feature < FIRST_JEP223_FEATURE ? NOT_JEP223 : JDK1_0 + feature * VERSION_STEP;",
    "  }",
    ""]

# R3 JXEditorPane: JDK 9+ Matcher.toMatchResult() is not a Matcher; klst-de 4173f8f then skipped the state update
EDITOR_SEARCH_OLD = ["                updateStateAfterFound(currentResult, start);", "            } else {"]
EDITOR_SEARCH_NEW = ["                updateStateAfterFound(currentResult, start, pattern);", "            } else {"]
EDITOR_EXTENDED_OLD = ["                    updateStateAfterFound(currentResult, start);", "                    return true;"]
EDITOR_EXTENDED_NEW = ["                    updateStateAfterFound(currentResult, start, pattern);",
                       "                    return true;"]
EDITOR_HELPER_ANCHOR = "         * @param useFirst whether or not to return after the first match is found."
EDITOR_HELPER = [
    "        /**",
    "         * Updates the state after a match of the given pattern. Since JDK 9",
    "         * {@link Matcher#toMatchResult()} no longer returns a {@code Matcher}: the match",
    "         * result is kept as is and the searched regex is read from the pattern.",
    "         *",
    "         * @param currentResult the match",
    "         * @param offset the document offset of the searched segment",
    "         * @param pattern the pattern that produced the match",
    "         * @return the start position of the selected text",
    "         */",
    "        //FLLDCIM: MRL - incremental search state on JDK 9+ (upstream semantics + klst-de JDK 9 guard)",
    "        private int updateStateAfterFound(MatchResult currentResult, final int offset, Pattern pattern) {",
    "            int found = updateStateAfterFound(currentResult, offset);",
    "            lastMatchResult = currentResult;",
    "            lastRegEx = pattern.pattern();",
    "            return found;",
    "        }",
    ""]

# R4 JXHeaderTest: @RunWith(JUnit4) never calls TestCase.setUp(), which loads the header addon
HEADER_IMPORT_ANCHOR = "import org.junit.Test;"
HEADER_IMPORT = ["import org.junit.Before;"]
HEADER_SETUP_ANCHOR = "        // forcing load of headerAddon"
HEADER_BEFORE = [
    "",
    "    //FLLDCIM: MRL - JUnit 4 runner skips TestCase.setUp(); JUnit 4.11+ method order runs the B tests first",
    "    @Before",
    "    public void setUpJ4() throws Exception {",
    "        setUp();",
    "    }"]

# R5 JXTreeTableUnitTest: FileSystemModel's first root child may be a file (F:\ on Windows) -> not expandable
TREETABLE_TESTS = ("    public void testExpansionListenerSourceExpanded() {",
                   "    public void testExpansionListenerSourceCollapsed() {")
TREETABLE_OLD = "        JXTreeTable table = new JXTreeTable(new FileSystemModel());"
TREETABLE_NEW = [
    "        //FLLDCIM: MRL - deterministic model: row 0 of the file system root may be a file (leaf)",
    "        JXTreeTable table = new JXTreeTable(simpleTreeTableModel);"]

# R6 BuddyLayoutAndBorderTest: JDK 9+ BasicTextUI adds a caret margin to the preferred width (upstream too)
BUDDY_IMPORT_ANCHOR = "import static org.junit.Assert.assertThat;"
BUDDY_IMPORTS = ["import static org.junit.Assume.assumeFalse;"]
BUDDY_JVM_ANCHOR = "import org.jdesktop.swingx.prompt.BuddySupport;"
BUDDY_JVM_IMPORT = ["import org.jdesktop.swingx.util.JVM;"]
BUDDY_TEST_ANCHOR = "\tpublic void testPreferredWidth() throws Exception {"
BUDDY_ASSUME = [
    "\t\t//FLLDCIM: MRL - excluded on JDK 9+ with upstream evidence (docs/m3/build-green/TEST_BASELINE.tsv)",
    "\t\tassumeFalse(\"JDK 9+ BasicTextUI adds caretMargin to the preferred width; see TEST_BASELINE.tsv\",",
    "\t\t\t\tJVM.current().isOrLater(JVM.JDK1_9));"]

# R7 demos: bsaf's javax.jnlp system dependency (${java.home}/lib/javaws.jar) exists only up to JDK 8
DEMOS_POM = "swingx-demos/pom.xml"
DEMO_UTILITIES = "swingx-demos/swingx-demos-swingxset/src/main/java/com/sun/swingset3/demos/DemoUtilities.java"
DEMO_UTILS = "swingx-demos/swingx-demos-swingxset/src/main/java/org/jdesktop/swingxset/util/DemoUtils.java"
DEMOS_BSAF_ANCHOR = "    \t<version>1.9.2</version>"
DEMOS_EXCLUSION = [
    "        <exclusions>",
    "          <!-- BUILD-GREEN v1: bsaf declares javax.jnlp:jnlp as a system dependency on",
    "               ${java.home}/lib/javaws.jar, which only JDK 8 and earlier ship; the demos look",
    "               the JNLP services up reflectively (DemoUtils.lookupJnlpService) -->",
    "          <exclusion>",
    "            <groupId>javax.jnlp</groupId>",
    "            <artifactId>jnlp</artifactId>",
    "          </exclusion>",
    "        </exclusions>"]
UTILITIES_IMPORTS_OLD = ["import javax.jnlp.BasicService;", "import javax.jnlp.ServiceManager;",
                         "import javax.jnlp.UnavailableServiceException;", "import javax.swing.SwingConstants;",
                         "import javax.swing.SwingUtilities;", ""]
UTILITIES_IMPORTS_NEW = ["import javax.swing.SwingConstants;", "import javax.swing.SwingUtilities;", "",
                         "import org.jdesktop.swingxset.util.DemoUtils;", ""]
BROWSE_OLD = [
    "    public static boolean browse(URI uri) throws IOException , UnavailableServiceException {",
    "        // Try using the Desktop api first",
    "        try {",
    "            Desktop desktop = Desktop.getDesktop();",
    "            desktop.browse(uri);",
    "",
    "            return true;",
    "        } catch (SecurityException e) {",
    "//             Running in sandbox, try using WebStart service",
    "            BasicService basicService =",
    "                    (BasicService) ServiceManager.lookup(\"javax.jnlp.BasicService\");",
    "",
    "            if (basicService.isWebBrowserSupported()) {",
    "                return basicService.showDocument(uri.toURL());",
    "            }",
    "        }"]
BROWSE_NEW = [
    "    public static boolean browse(URI uri) throws IOException {",
    "        // Try using the Desktop api first",
    "        try {",
    "            Desktop desktop = Desktop.getDesktop();",
    "            desktop.browse(uri);",
    "",
    "            return true;",
    "        } catch (SecurityException e) {",
    "//             Running in sandbox, try using WebStart service",
    "            //FLLDCIM: MRL - javax.jnlp.BasicService looked up reflectively: no javaws.jar on JDK 11+",
    "            if (DemoUtils.showJnlpDocument(uri.toURL())) {",
    "                return true;",
    "            }",
    "        }"]
UTILS_IMPORTS_OLD = ["import java.awt.datatransfer.StringSelection;", "import java.awt.event.ActionEvent;",
                     "import java.security.AllPermission;", "import java.security.Permission;",
                     "import java.util.logging.Logger;", "", "import javax.jnlp.ClipboardService;",
                     "import javax.jnlp.ServiceManager;", "import javax.swing.AbstractAction;"]
UTILS_IMPORTS_NEW = ["import java.awt.datatransfer.StringSelection;", "import java.awt.datatransfer.Transferable;",
                     "import java.awt.event.ActionEvent;", "import java.io.IOException;", "import java.net.URL;",
                     "import java.security.AllPermission;", "import java.security.Permission;",
                     "import java.util.logging.Logger;", "", "import javax.swing.AbstractAction;"]
UTILS_LOG_ANCHOR = "            .getName());"
UTILS_CONSTANTS = [
    "",
    "    //FLLDCIM: MRL - JNLP (Java Web Start) services resolved reflectively: javax.jnlp is absent from JDK 11+",
    "    /** Java Web Start service manager; present only when running under Web Start. */",
    "    private static final String JNLP_SERVICE_MANAGER = \"javax.jnlp.ServiceManager\";",
    "    /** Static service lookup method of the service manager. */",
    "    private static final String JNLP_LOOKUP = \"lookup\";",
    "    /** JNLP service that shows documents in the browser. */",
    "    private static final String JNLP_BASIC_SERVICE = \"javax.jnlp.BasicService\";",
    "    /** BasicService method telling whether a browser is available. */",
    "    private static final String JNLP_IS_WEB_BROWSER_SUPPORTED = \"isWebBrowserSupported\";",
    "    /** BasicService method showing a document. */",
    "    private static final String JNLP_SHOW_DOCUMENT = \"showDocument\";",
    "    /** JNLP clipboard service. */",
    "    private static final String JNLP_CLIPBOARD_SERVICE = \"javax.jnlp.ClipboardService\";",
    "    /** ClipboardService method setting the clipboard contents. */",
    "    private static final String JNLP_SET_CONTENTS = \"setContents\";"]
CLIPBOARD_OLD = [
    "                    ClipboardService cs = (ClipboardService)ServiceManager.lookup",
    "                        (\"javax.jnlp.ClipboardService\");",
    "                    StringSelection transferable = new StringSelection(editor.getSelectedText());",
    "                    cs.setContents(transferable);"]
CLIPBOARD_NEW = [
    "                    //FLLDCIM: MRL - javax.jnlp.ClipboardService looked up reflectively (no javaws.jar on JDK 11+)",
    "                    Object cs = lookupJnlpService(JNLP_CLIPBOARD_SERVICE);",
    "                    StringSelection transferable = new StringSelection(editor.getSelectedText());",
    "                    Class.forName(JNLP_CLIPBOARD_SERVICE).getMethod(JNLP_SET_CONTENTS, Transferable.class)",
    "                        .invoke(cs, transferable);"]
UTILS_RESTRICTED_ANCHOR = "    private static boolean isRestricted() {"
UTILS_JNLP_METHODS = [
    "    /**",
    "     * Looks a Java Web Start (JNLP) service up by name. Reflective, so that the demos",
    "     * compile and run without javax.jnlp, which JDK 11 and later no longer ship.",
    "     *",
    "     * @param serviceName the JNLP service name, for example {@code javax.jnlp.BasicService}",
    "     * @return the service",
    "     * @throws IOException if Web Start or the service is not available",
    "     */",
    "    public static Object lookupJnlpService(String serviceName) throws IOException {",
    "        try {",
    "            return Class.forName(JNLP_SERVICE_MANAGER).getMethod(JNLP_LOOKUP, String.class)",
    "                .invoke(null, serviceName);",
    "        } catch (ReflectiveOperationException e) {",
    "            throw new IOException(serviceName, e);",
    "        }",
    "    }",
    "",
    "    /**",
    "     * Shows the document through the JNLP BasicService when it supports a browser.",
    "     *",
    "     * @param url the document",
    "     * @return whether the document was shown",
    "     * @throws IOException if Web Start or the service is not available",
    "     */",
    "    public static boolean showJnlpDocument(URL url) throws IOException {",
    "        Object service = lookupJnlpService(JNLP_BASIC_SERVICE);",
    "        try {",
    "            Class<?> type = Class.forName(JNLP_BASIC_SERVICE);",
    "            if (!(Boolean) type.getMethod(JNLP_IS_WEB_BROWSER_SUPPORTED).invoke(service)) {",
    "                return false;",
    "            }",
    "            return (Boolean) type.getMethod(JNLP_SHOW_DOCUMENT, URL.class).invoke(service, url);",
    "        } catch (ReflectiveOperationException e) {",
    "            throw new IOException(JNLP_BASIC_SERVICE, e);",
    "        }",
    "    }",
    ""]

# R8 LookAndFeelAddonsSandboxTest.testSystemAddon assumes that the system LAF is not the cross-platform LAF.
# On Linux without a GNOME desktop (XDG_CURRENT_DESKTOP) or GTK the JDK reports Metal for both, and
# getBestMatchAddonClassName() checks the cross-platform name first, so the Metal addon is the correct answer.
SANDBOX_IMPORT_ANCHOR = "import java.util.logging.Logger;"
SANDBOX_IMPORTS = ["import static org.junit.Assume.assumeFalse;", ""]
SANDBOX_TEST_ANCHOR = "    public void testSystemAddon() {"
SANDBOX_ASSUME = [
    "        //FLLDCIM: MRL - premise: the system LAF is not the cross-platform LAF (docs/m3/build-green/TEST_BASELINE.tsv)",
    "        // Linux without a GNOME desktop or GTK reports Metal for both, so the best-match addon is the Metal addon.",
    "        assumeFalse(\"system LAF is the cross-platform LAF (Linux without GNOME/GTK); see TEST_BASELINE.tsv\",",
    "                UIManager.getSystemLookAndFeelClassName().equals(UIManager.getCrossPlatformLookAndFeelClassName()));"]

def after(doc, anchor, offset=1):
    return doc.index(anchor) + offset


def fix_pom(repo):
    d = Doc(repo, POM)
    insert(d, "R1-surefire-argLine", POM_ARGLINE, after(d, POM_FORK))
    insert(d, "R1-argLine-property", POM_PROPERTY, after(d, POM_PROPERTY_ANCHOR))
    d.save()


def fix_jvm(repo):
    d = Doc(repo, JVM)
    insert(d, "R2-regex-imports", JVM_IMPORTS, d.index("import javax.swing.UIManager;"))
    insert(d, "R2-jep223-constants", JVM_CONSTANTS, after(d, JVM_CONSTANT_ANCHOR))
    replace(d, "R2-jep223-fallback", JVM_FALLBACK_OLD, JVM_FALLBACK_NEW)
    insert(d, "R2-jep223-helper", JVM_HELPER, d.index(JVM_HELPER_ANCHOR))
    d.save()


def fix_editor(repo):
    d = Doc(repo, EDITOR)
    replace(d, "R3-search-pattern", EDITOR_SEARCH_OLD, EDITOR_SEARCH_NEW)
    replace(d, "R3-extended-pattern", EDITOR_EXTENDED_OLD, EDITOR_EXTENDED_NEW)
    insert(d, "R3-state-overload", EDITOR_HELPER, d.index(EDITOR_HELPER_ANCHOR) - 2)
    d.save()


def fix_header_test(repo):
    d = Doc(repo, HEADER_TEST)
    insert(d, "R4-import-Before", HEADER_IMPORT, d.index(HEADER_IMPORT_ANCHOR))
    insert(d, "R4-before-setUp", HEADER_BEFORE, after(d, HEADER_SETUP_ANCHOR, 3))
    d.save()


def fix_treetable_test(repo):
    d = Doc(repo, TREETABLE_TEST)
    for name in TREETABLE_TESTS:
        line = after(d, name)
        if d.lines[line:line + len(TREETABLE_NEW)] == TREETABLE_NEW:
            print(f"R5-model-{name.strip()[12:-4]}\t{TREETABLE_TEST}\tALREADY\t{len(TREETABLE_NEW)} lines")
            continue
        if d.lines[line] != TREETABLE_OLD:
            raise SystemExit(f"FAIL\t{TREETABLE_TEST}\tR5: unexpected line after {name.strip()!r}")
        d.lines[line:line + 1] = TREETABLE_NEW
        print(f"R5-model-{name.strip()[12:-4]}\t{TREETABLE_TEST}\tAPPLIED\t-1 +{len(TREETABLE_NEW)} lines at {line + 1}")
    d.save()


def fix_buddy_test(repo):
    d = Doc(repo, BUDDY_TEST)
    insert(d, "R6-import-assumeFalse", BUDDY_IMPORTS, after(d, BUDDY_IMPORT_ANCHOR))
    insert(d, "R6-import-JVM", BUDDY_JVM_IMPORT, after(d, BUDDY_JVM_ANCHOR))
    insert(d, "R6-assume-jdk9", BUDDY_ASSUME, after(d, BUDDY_TEST_ANCHOR))
    d.save()


def fix_demos(repo):
    d = Doc(repo, DEMOS_POM)
    insert(d, "R7-bsaf-exclude-jnlp", DEMOS_EXCLUSION, after(d, DEMOS_BSAF_ANCHOR, 3))
    d.save()
    d = Doc(repo, DEMO_UTILITIES)
    replace(d, "R7-utilities-imports", UTILITIES_IMPORTS_OLD, UTILITIES_IMPORTS_NEW)
    replace(d, "R7-browse-reflective", BROWSE_OLD, BROWSE_NEW)
    d.save()
    d = Doc(repo, DEMO_UTILS)
    replace(d, "R7-utils-imports", UTILS_IMPORTS_OLD, UTILS_IMPORTS_NEW)
    insert(d, "R7-jnlp-constants", UTILS_CONSTANTS, after(d, UTILS_LOG_ANCHOR))
    replace(d, "R7-clipboard-reflective", CLIPBOARD_OLD, CLIPBOARD_NEW)
    insert(d, "R7-jnlp-lookup", UTILS_JNLP_METHODS, d.index(UTILS_RESTRICTED_ANCHOR))
    d.save()


def fix_sandbox_test(repo):
    d = Doc(repo, SANDBOX_TEST)
    insert(d, "R8-import-assumeFalse", SANDBOX_IMPORTS, d.index(SANDBOX_IMPORT_ANCHOR))
    insert(d, "R8-assume-distinct-system-laf", SANDBOX_ASSUME, after(d, SANDBOX_TEST_ANCHOR))
    d.save()

if __name__ == "__main__":
    root = sys.argv[1]
    fix_pom(root)
    fix_jvm(root)
    fix_editor(root)
    fix_header_test(root)
    fix_treetable_test(root)
    fix_buddy_test(root)
    fix_demos(root)
    fix_sandbox_test(root)
