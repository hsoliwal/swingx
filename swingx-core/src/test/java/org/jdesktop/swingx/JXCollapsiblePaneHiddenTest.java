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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.After;
import org.junit.Test;

/**
 * JXCollapsiblePane hidden gate (every-widget H): a slide that finds the pane hidden ends at its target at once.
 */
public class JXCollapsiblePaneHiddenTest {

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
    public void slideHiddenMidwayEndsAtItsTargetAtOnce() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        AtomicInteger collapsedEvents = new AtomicInteger();
        JPanel[] holder = new JPanel[1];
        JXCollapsiblePane[] pane = new JXCollapsiblePane[1];
        SwingUtilities.invokeAndWait(() -> {
            frame = new JFrame("collapsible");
            holder[0] = new JPanel(new BorderLayout());
            pane[0] = new JXCollapsiblePane();
            JLabel content = new JLabel("content");
            content.setPreferredSize(new Dimension(200, 400));
            pane[0].add(content);
            pane[0].addPropertyChangeListener("collapsed", e -> collapsedEvents.incrementAndGet());
            holder[0].add(pane[0], BorderLayout.NORTH);
            frame.add(holder[0]);
            frame.setSize(300, 500);
            frame.setVisible(true);
        });
        Thread.sleep(300);
        SwingUtilities.invokeAndWait(() -> {
            pane[0].setCollapsed(true); // a slide of about ten 30 ms frames
            holder[0].setVisible(false);
        });
        Thread.sleep(120);
        SwingUtilities.invokeAndWait(() -> { });
        assertEquals("the slide ended at once when the pane was found hidden", 1, collapsedEvents.get());
        assertTrue(pane[0].isCollapsed());
        SwingUtilities.invokeAndWait(() -> pane[0].setCollapsed(false)); // hidden: no slide starts (stock)
        assertEquals(2, collapsedEvents.get());
    }
}
