/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */
package org.jdesktop.swingx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.AWTEventListenerProxy;
import java.lang.reflect.Field;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.junit.Assume;
import org.junit.Test;

/**
 * Every-widget leaf of JXFrame: the key preview and idle listeners are global toolkit listeners; a disposed frame
 * leaves the toolkit (which would otherwise keep it and run its key listeners and idle timer for every other window),
 * and a frame made displayable again registers them again.
 */
public class JXFrameToolkitListenerTest {

    @Test
    public void aDisposedFrameLeavesTheToolkit() throws Exception {
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());
        Throwable[] failure = new Throwable[1];
        SwingUtilities.invokeAndWait(() -> {
            try {
                check();
            } catch (Throwable t) {
                failure[0] = t;
            }
        });
        if (failure[0] instanceof Exception) {
            throw (Exception) failure[0];
        }
        if (failure[0] != null) {
            throw (Error) failure[0];
        }
    }

    private static void check() throws Exception {
        JXFrame frame = new JXFrame("toolkit listeners");
        frame.setKeyPreview(true);
        frame.setIdleThreshold(60_000);
        frame.pack();
        assertEquals("key preview and idle listeners while displayable", 2, registered(frame));

        frame.dispose();
        assertEquals("a disposed frame leaves the toolkit", 0, registered(frame));
        assertFalse("the idle timer of a disposed frame stops", ((Timer) field(frame, "idleTimer")).isRunning());

        frame.pack();
        assertEquals("a frame made displayable again registers both again", 2, registered(frame));
        assertTrue(((Timer) field(frame, "idleTimer")).isRunning());
        frame.dispose();
        assertEquals(0, registered(frame));
    }

    private static int registered(JXFrame frame) throws Exception {
        Object key = field(frame, "keyEventListener");
        Object idle = field(frame, "idleListener");
        int found = 0;
        for (AWTEventListener listener : Toolkit.getDefaultToolkit().getAWTEventListeners()) {
            Object target = listener instanceof AWTEventListenerProxy
                    ? ((AWTEventListenerProxy) listener).getListener() : listener;
            if (target == key || target == idle) {
                found++;
            }
        }
        return found;
    }

    private static Object field(JXFrame frame, String name) throws Exception {
        Field field = JXFrame.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(frame);
    }
}
