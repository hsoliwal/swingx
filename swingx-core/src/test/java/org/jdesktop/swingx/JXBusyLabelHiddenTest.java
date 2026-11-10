/*
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package org.jdesktop.swingx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.After;
import org.junit.Test;

/**
 * JXBusyLabel hidden gate (every-widget H): no animation frames while the label cannot be seen, the animation resumes
 * when it is shown, and isBusy() stays true throughout.
 */
public class JXBusyLabelHiddenTest {

    private JFrame frame;

    @After
    public void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            if (frame != null) {
                frame.dispose();
            }
        });
    }

    @Test
    public void hiddenLabelParksItsAnimationAndResumesWhenShown() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        AtomicInteger frames = new AtomicInteger();
        JPanel[] panel = new JPanel[1];
        JXBusyLabel[] label = new JXBusyLabel[1];
        SwingUtilities.invokeAndWait(() -> {
            frame = new JFrame("busy");
            panel[0] = new JPanel();
            label[0] = new JXBusyLabel() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void frameChanged() {
                    frames.incrementAndGet();
                    super.frameChanged();
                }
            };
            panel[0].add(label[0]);
            frame.add(panel[0]);
            frame.pack();
            frame.setVisible(true);
            label[0].setBusy(true);
        });
        Thread.sleep(500);
        assertTrue("the animation runs while shown", frames.get() > 0);
        SwingUtilities.invokeAndWait(() -> panel[0].setVisible(false));
        Thread.sleep(200);
        frames.set(0);
        Thread.sleep(500);
        assertEquals("no animation frame while an ancestor is hidden", 0, frames.get());
        assertTrue("still busy", label[0].isBusy());
        SwingUtilities.invokeAndWait(() -> panel[0].setVisible(true));
        Thread.sleep(500);
        assertTrue("the animation resumes when shown", frames.get() > 0);
        SwingUtilities.invokeAndWait(() -> label[0].setBusy(false));
        Thread.sleep(200);
        frames.set(0);
        Thread.sleep(300);
        assertEquals("stopped", 0, frames.get());
    }
}
