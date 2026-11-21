/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jdesktop.swingx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.awt.ComponentOrientation;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import org.junit.Test;

/** Actual on-screen mouse routing for the two separate SwingX action targets. */
public class JXChipSplitButtonHeadfulTest {
    private static final int TRAILING_INSET = 12;

    private static <T> T edt(Callable<T> callable) throws Exception {
        FutureTask<T> task = new FutureTask<T>(callable);
        SwingUtilities.invokeAndWait(task);
        return task.get();
    }

    private static void edt(final Runnable action) throws Exception {
        edt(new Callable<Object>() {
            @Override public Object call() {
                action.run();
                return null;
            }
        });
    }

    private static JFrame display(final JComponent component) throws Exception {
        return edt(new Callable<JFrame>() {
            @Override public JFrame call() {
                JFrame frame = new JFrame("SwingX native-action regression");
                frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                JPanel content = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 40));
                content.add(component);
                frame.setContentPane(content);
                frame.setSize(440, 220);
                frame.setLocation(100, 100);
                frame.setVisible(true);
                return frame;
            }
        });
    }

    private static Point target(final JComponent component, final boolean trailing)
            throws Exception {
        return edt(new Callable<Point>() {
            @Override public Point call() {
                Point point = component.getLocationOnScreen();
                boolean ltr = component.getComponentOrientation().isLeftToRight();
                int localX = trailing
                        ? (ltr ? component.getWidth() - TRAILING_INSET : TRAILING_INSET)
                        : component.getWidth() / 4;
                point.translate(localX, component.getHeight() / 2);
                return point;
            }
        });
    }

    private static void click(Robot robot, Point point) {
        robot.mouseMove(point.x, point.y);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.waitForIdle();
    }

    @Test(timeout = 30000)
    public void chipCloseStaysSeparateFromToggleInBothOrientations() throws Exception {
        assumeFalse("Requires an AWT display with a complete native JDK",
                GraphicsEnvironment.isHeadless());
        final JXChip chip = edt(new Callable<JXChip>() {
            @Override public JXChip call() {
                JXChip result = new JXChip("Messages");
                result.setClosable(true);
                return result;
            }
        });
        final AtomicInteger closeRequests = new AtomicInteger();
        final AtomicInteger actions = new AtomicInteger();
        edt(new Runnable() {
            @Override public void run() {
                chip.addCloseListener(event -> {
                    closeRequests.incrementAndGet();
                    event.setAllowed(false);
                });
                chip.addActionListener(event -> actions.incrementAndGet());
            }
        });
        JFrame frame = display(chip);
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(35);
            robot.waitForIdle();
            click(robot, target(chip, true));
            assertEquals("Close gesture must reach only its vetoable listener", 1,
                    closeRequests.get());
            assertEquals(0, actions.get());
            assertFalse(edt(() -> chip.isSelected()));

            click(robot, target(chip, false));
            assertEquals("Primary action must still select the inherited model", 1,
                    actions.get());
            assertTrue(edt(() -> chip.isSelected()));

            edt(() -> chip.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT));
            robot.waitForIdle();
            click(robot, target(chip, true));
            assertEquals("RTL trailing close must use the leading screen edge", 2,
                    closeRequests.get());
            assertEquals(1, actions.get());
            assertTrue(edt(() -> chip.isSelected()));
        } finally {
            edt(() -> frame.dispose());
        }
    }

    @Test(timeout = 30000)
    public void splitArrowOpensNativePopupWithoutPrimaryAction() throws Exception {
        assumeFalse("Requires an AWT display with a complete native JDK",
                GraphicsEnvironment.isHeadless());
        final JPopupMenu menu = new JPopupMenu();
        final JXSplitButton button = edt(new Callable<JXSplitButton>() {
            @Override public JXSplitButton call() {
                JXSplitButton result = new JXSplitButton("Run action");
                menu.add(new JMenuItem("Dropdown action"));
                result.setPopupMenu(menu);
                return result;
            }
        });
        final AtomicInteger actions = new AtomicInteger();
        edt(() -> button.addActionListener(event -> actions.incrementAndGet()));
        JFrame frame = display(button);
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(35);
            robot.waitForIdle();
            click(robot, target(button, true));
            assertTrue("The arrow should display the real JPopupMenu",
                    edt(() -> menu.isVisible()));
            assertEquals("Opening the popup cannot dispatch the primary action", 0,
                    actions.get());

            edt(() -> menu.setVisible(false));
            click(robot, target(button, false));
            assertEquals("The main region retains the original JButton action", 1,
                    actions.get());

            edt(() -> button.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT));
            robot.waitForIdle();
            click(robot, target(button, true));
            assertTrue("RTL arrow should still open its original popup",
                    edt(() -> menu.isVisible()));
            assertEquals(1, actions.get());
        } finally {
            edt(() -> {
                menu.setVisible(false);
                frame.dispose();
            });
        }
    }
}
