/*
 * Copyright (c) 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jdesktop.swingx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.junit.Test;

/** Delay changes remain effective when hidden animation is parked. */
public class JXBusyLabelDelayTest {

    @Test
    public void delaySetBeforeStartingIsUsedByTheTimer() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            VisibleLabel label = new VisibleLabel();
            try {
                label.setDelay(75);
                label.setBusy(true);
                assertEquals(75, animation(label).getDelay());
                assertTrue(label.isBusy());
            } finally {
                label.setBusy(false);
            }
        });
    }

    @Test
    public void runningAnimationKeepsItsTimerAndAcceptsTheNewDelay() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            VisibleLabel label = new VisibleLabel();
            label.showing = true;
            try {
                label.setBusy(true);
                Timer timer = animation(label);
                label.setDelay(75);
                assertSame(timer, animation(label));
                assertEquals(75, timer.getDelay());
                assertTrue(timer.isRunning());
                assertTrue(label.isBusy());
            } finally {
                label.setBusy(false);
            }
        });
    }

    @Test
    public void inactiveNegativeDelayKeepsTheOriginalPropertyNotification() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            VisibleLabel label = new VisibleLabel();
            AtomicInteger notifications = new AtomicInteger();
            label.addPropertyChangeListener("delay", event -> notifications.incrementAndGet());
            label.setDelay(-1);
            assertEquals(-1, label.getDelay());
            assertEquals(1, notifications.get());
            assertFalse(label.isBusy());
            label.setDelay(200);
            assertEquals(2, notifications.get());
            try {
                label.setBusy(true);
                assertEquals(200, animation(label).getDelay());
            } finally {
                label.setBusy(false);
            }
        });
    }

    @Test
    public void runningNegativeDelayKeepsTheOriginalExceptionAndNotificationOrder() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            VisibleLabel label = new VisibleLabel();
            label.showing = true;
            label.setDelay(200);
            label.setBusy(true);
            try {
                Timer timer = animation(label);
                AtomicInteger notifications = new AtomicInteger();
                label.addPropertyChangeListener("delay", event -> notifications.incrementAndGet());
                assertThrows(IllegalArgumentException.class, () -> label.setDelay(-1));
                assertEquals(-1, label.getDelay());
                assertEquals(200, timer.getDelay());
                assertEquals(0, notifications.get());
                assertTrue(timer.isRunning());
                assertTrue(label.isBusy());
            } finally {
                label.setBusy(false);
            }
        });
    }

    @Test
    public void stoppedNonParkedTimerKeepsTheOriginalDelaySemantics() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            VisibleLabel label = new VisibleLabel();
            label.showing = true;
            label.setDelay(200);
            label.setBusy(true);
            try {
                Timer timer = animation(label);
                // A stopped timer without the new hidden-animation park is a distinct state.
                timer.stop();
                AtomicInteger notifications = new AtomicInteger();
                label.addPropertyChangeListener("delay", event -> notifications.incrementAndGet());
                label.setDelay(25);
                assertEquals(25, label.getDelay());
                assertEquals(200, timer.getDelay());
                label.setDelay(-1);
                assertEquals(-1, label.getDelay());
                assertEquals(200, timer.getDelay());
                assertEquals(2, notifications.get());
                assertFalse(timer.isRunning());
                assertTrue(label.isBusy());
            } finally {
                label.setBusy(false);
            }
        });
    }

    @Test
    public void parkedNegativeDelayUsesTheRunningAnimationValidation() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            VisibleLabel label = new VisibleLabel();
            label.setDelay(200);
            label.setBusy(true);
            try {
                Timer timer = animation(label);
                timer.getActionListeners()[0].actionPerformed(
                        new ActionEvent(timer, ActionEvent.ACTION_PERFORMED, "hidden frame"));
                assertFalse(timer.isRunning());
                AtomicInteger notifications = new AtomicInteger();
                label.addPropertyChangeListener("delay", event -> notifications.incrementAndGet());
                assertThrows(IllegalArgumentException.class, () -> label.setDelay(-1));
                assertEquals(-1, label.getDelay());
                assertEquals(200, timer.getDelay());
                assertEquals(0, notifications.get());
                assertFalse(timer.isRunning());
                assertTrue(label.isBusy());
            } finally {
                label.setBusy(false);
            }
        });
    }

    @Test
    public void parkedAnimationUpdatesItsDelayWithoutRestartingAndResumes() throws Exception {
        VisibleLabel[] reference = new VisibleLabel[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                VisibleLabel label = new VisibleLabel();
                reference[0] = label;
                label.setDelay(200);
                label.setBusy(true);
                Timer timer = animation(label);

                // Execute the real timer callback while hidden, without a display or sleeps.
                timer.getActionListeners()[0].actionPerformed(
                        new ActionEvent(timer, ActionEvent.ACTION_PERFORMED, "hidden frame"));
                assertFalse(timer.isRunning());
                assertTrue(label.isBusy());
                assertEquals(0, label.frames.get());

                AtomicInteger notifications = new AtomicInteger();
                label.addPropertyChangeListener("delay", event -> {
                    assertEquals(200, event.getOldValue());
                    assertEquals(25, event.getNewValue());
                    notifications.incrementAndGet();
                });
                label.setDelay(25);
                assertEquals(25, label.getDelay());
                assertEquals("the parked timer uses the new cadence", 25, timer.getDelay());
                assertSame(timer, animation(label));
                assertFalse("changing delay does not wake hidden animation", timer.isRunning());
                assertTrue(label.isBusy());
                assertEquals(1, notifications.get());

                label.showing = true;
                label.dispatchEvent(new HierarchyEvent(label, HierarchyEvent.HIERARCHY_CHANGED,
                        label, null, HierarchyEvent.SHOWING_CHANGED));
                assertTrue("showing resumes the existing animation", timer.isRunning());
                assertSame(timer, animation(label));
            });
            assertTrue("the resumed Swing timer delivers frames", reference[0].resumed.await(3, TimeUnit.SECONDS));
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (reference[0] != null) {
                    reference[0].setBusy(false);
                    assertFalse(reference[0].isBusy());
                }
            });
        }
    }

    private static Timer animation(JXBusyLabel label) {
        try {
            Field field = JXBusyLabel.class.getDeclaredField("busy");
            field.setAccessible(true);
            return (Timer) field.get(label);
        } catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static final class VisibleLabel extends JXBusyLabel {
        private static final long serialVersionUID = 1L;
        final AtomicInteger frames = new AtomicInteger();
        final transient CountDownLatch resumed = new CountDownLatch(2);
        boolean showing;

        @Override
        public boolean isShowing() {
            return showing;
        }

        @Override
        protected void frameChanged() {
            frames.incrementAndGet();
            resumed.countDown();
            super.frameChanged();
        }
    }
}
