/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */
package org.jdesktop.swingx;

import static org.junit.Assert.assertTrue;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.Test;

/**
 * Every-widget D leaf of JXBusyLabel: a busy frame repaints a region narrower than the label (the icon, not the text),
 * and every pixel that changes from one frame to the next lies inside that region.
 */
public class JXBusyLabelFrameDamageTest {

    /* a busy label that records the clip of every paint */
    static final class Recording extends JXBusyLabel {
        final List<Rectangle> clips = new ArrayList<>();

        Recording() {
            setText("Loading the archive, please wait");
        }

        @Override
        protected void paintComponent(Graphics g) {
            Rectangle clip = g.getClipBounds();
            if (clip != null) {
                synchronized (clips) {
                    clips.add(clip);
                }
            }
            super.paintComponent(g);
        }

        void nextFrame() {
            getBusyPainter().setFrame((getBusyPainter().getFrame() + 1) % getBusyPainter().getPoints());
            frameChanged();
        }

        BufferedImage snapshot() {
            BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics g = image.getGraphics();
            paint(g);
            g.dispose();
            return image;
        }
    }

    @Test
    public void aBusyFrameRepaintsTheIconAndCoversEveryChangedPixel() throws Exception {
        final Recording[] label = new Recording[1];
        final JFrame[] frame = new JFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            label[0] = new Recording();
            label[0].setDelay(3_600_000); // the timer stays quiet: the test steps the frames
            frame[0] = new JFrame("JXBusyLabelFrameDamageTest");
            frame[0].add(label[0]);
            frame[0].pack();
            frame[0].setVisible(true);
            label[0].setBusy(true);
        });
        try {
            Thread.sleep(500);
            for (int i = 0; i < 5; i++) {
                final BufferedImage[] before = new BufferedImage[1];
                SwingUtilities.invokeAndWait(() -> {
                    before[0] = label[0].snapshot();
                    label[0].clips.clear();
                    label[0].nextFrame();
                });
                Thread.sleep(100); // the RepaintManager paints the damaged region
                final BufferedImage[] after = new BufferedImage[1];
                final List<Rectangle> clips = new ArrayList<>();
                SwingUtilities.invokeAndWait(() -> {
                    synchronized (label[0].clips) {
                        clips.addAll(label[0].clips);
                    }
                    after[0] = label[0].snapshot();
                });
                assertTrue("frame " + i + " is painted", !clips.isEmpty());
                Rectangle damaged = new Rectangle(clips.get(0));
                for (Rectangle clip : clips) {
                    damaged = damaged.union(clip);
                }
                int width = label[0].getWidth();
                assertTrue("frame " + i + " repaints " + damaged.width + " of the label's " + width + " px",
                        damaged.width < width);
                for (int y = 0; y < after[0].getHeight(); y++) {
                    for (int x = 0; x < after[0].getWidth(); x++) {
                        if (before[0].getRGB(x, y) != after[0].getRGB(x, y)) {
                            assertTrue("frame " + i + ": changed pixel " + x + "," + y + " lies in " + damaged,
                                    damaged.contains(x, y));
                        }
                    }
                }
            }
        } finally {
            SwingUtilities.invokeAndWait(() -> frame[0].dispose());
        }
    }
}
