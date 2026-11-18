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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.BorderFactory;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.Test;

/** Pure-Swing LED/colon widget tests, independent of an SWT runtime. */
public class JXSevenSegmentDisplayTest {

    private static void onEdt(Runnable work) throws Exception {
        SwingUtilities.invokeAndWait(work);
    }

    private static void expectIllegal(Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("accepted invalid input");
        } catch (IllegalArgumentException expected) {
            // Required transactional refusal.
        }
    }

    private static void expectNull(Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("accepted null color");
        } catch (NullPointerException expected) {
            // Required non-null paint-state contract.
        }
    }

    private static BufferedImage render(JXSevenSegmentDisplay display) {
        Dimension size = display.getPreferredSize();
        display.setSize(size);
        BufferedImage image = new BufferedImage(size.width, size.height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            display.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static int difference(BufferedImage left, BufferedImage right) {
        int differences = 0;
        for (int row = 0; row < left.getHeight(); row++) {
            for (int col = 0; col < left.getWidth(); col++) {
                if (left.getRGB(col, row) != right.getRGB(col, row)) {
                    differences++;
                }
            }
        }
        return differences;
    }

    @Test
    public void acceptedCharactersAndDigitValidation() throws Exception {
        onEdt(() -> {
            JXSevenSegmentDisplay display = new JXSevenSegmentDisplay('a');
            assertEquals((int) 'A', (int) display.getCharacter());
            display.setDigit(8);
            assertEquals((int) '8', (int) display.getCharacter());
            for (char current : "0123456789ABCDEF- :".toCharArray()) {
                display.setCharacter(current);
                assertEquals((int) current, (int) display.getCharacter());
            }
            expectIllegal(() -> display.setCharacter('@'));
            expectIllegal(() -> display.setDigit(-1));
            expectIllegal(() -> display.setDigit(10));
            expectIllegal(() -> new JXSevenSegmentDisplay('\n'));
            assertEquals((int) ':', (int) display.getCharacter());
        });
    }

    @Test
    public void propertiesDoNotEmitDuplicates() throws Exception {
        onEdt(() -> {
            JXSevenSegmentDisplay display = new JXSevenSegmentDisplay('0');
            AtomicInteger characters = new AtomicInteger();
            AtomicInteger colors = new AtomicInteger();
            display.addPropertyChangeListener("character", event -> characters.incrementAndGet());
            display.addPropertyChangeListener("litColor", event -> colors.incrementAndGet());
            display.setCharacter('0');
            display.setCharacter('2');
            display.setCharacter('2');
            assertEquals(1, characters.get());
            display.setLitColor(Color.YELLOW);
            display.setLitColor(Color.YELLOW);
            assertEquals(1, colors.get());
            assertEquals(Color.YELLOW, display.getLitColor());
            display.setUnlitColor(Color.BLUE);
            assertEquals(Color.BLUE, display.getUnlitColor());
            display.setDecimalPointLit(true);
            assertTrue(display.isDecimalPointLit());
            expectNull(() -> display.setLitColor(null));
            expectNull(() -> display.setUnlitColor(null));
            assertEquals(Color.YELLOW, display.getLitColor());
            assertEquals(Color.BLUE, display.getUnlitColor());
        });
    }

    @Test
    public void sizeBorderDecimalPointAndColon() throws Exception {
        onEdt(() -> {
            JXSevenSegmentDisplay display = new JXSevenSegmentDisplay('5');
            assertEquals(new Dimension(60, 100), display.getPreferredSize());
            display.setShowDecimalPoint(true);
            assertTrue(display.isShowDecimalPoint());
            assertEquals(new Dimension(75, 100), display.getPreferredSize());
            display.setBorder(BorderFactory.createEmptyBorder(2, 3, 4, 5));
            assertEquals(new Dimension(83, 106), display.getPreferredSize());
            display.setCharacter(':');
            assertEquals(new Dimension(38, 106), display.getPreferredSize());
            display.setShowDecimalPoint(false);
            assertEquals(new Dimension(38, 106), display.getPreferredSize());
            display.setPreferredSize(new Dimension(45, 88));
            assertEquals(new Dimension(45, 88), display.getPreferredSize());
            assertEquals(0, display.getComponentCount());
        });
    }

    @Test
    public void nativeSevenSegmentPixelsAndOptionalDecimalPoint() throws Exception {
        onEdt(() -> {
            JXSevenSegmentDisplay display = new JXSevenSegmentDisplay('0');
            BufferedImage zero = render(display);
            display.setCharacter('8');
            BufferedImage eight = render(display);
            assertTrue(difference(zero, eight) > 20);
            assertEquals(display.getLitColor().getRGB(), eight.getRGB(30, 8));
            assertEquals(display.getLitColor().getRGB(), eight.getRGB(30, 50));
            display.setCharacter(' ');
            BufferedImage blank = render(display);
            assertTrue(difference(blank, eight) > 200);
            display.setCharacter('8');
            display.setShowDecimalPoint(true);
            display.setDecimalPointLit(false);
            BufferedImage dotOff = render(display);
            display.setDecimalPointLit(true);
            BufferedImage dotOn = render(display);
            assertTrue(difference(dotOff, dotOn) > 20);
            assertEquals(display.getLitColor().getRGB(), dotOn.getRGB(66, 90));
        });
    }

    @Test
    public void colonRendersTwoIndependentDots() throws Exception {
        onEdt(() -> {
            JXSevenSegmentDisplay display = new JXSevenSegmentDisplay(':');
            BufferedImage colon = render(display);
            assertEquals(display.getLitColor().getRGB(), colon.getRGB(15, 30));
            assertEquals(display.getLitColor().getRGB(), colon.getRGB(15, 70));
            assertEquals(Color.BLACK.getRGB(), colon.getRGB(15, 50));
            display.setShowDecimalPoint(true);
            assertEquals(new Dimension(30, 100), display.getPreferredSize());
        });
    }

    @Test
    public void dirtyRegionClipIsPreserved() throws Exception {
        onEdt(() -> {
            JXSevenSegmentDisplay display = new JXSevenSegmentDisplay('8');
            display.setSize(60, 100);
            BufferedImage image = new BufferedImage(60, 100,
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setColor(Color.MAGENTA);
                graphics.fillRect(0, 0, 60, 100);
                graphics.setClip(0, 0, 5, 5);
                display.paint(graphics);
            } finally {
                graphics.dispose();
            }
            assertEquals(Color.MAGENTA.getRGB(), image.getRGB(30, 50));
            assertEquals(Color.MAGENTA.getRGB(), image.getRGB(30, 8));
        });
    }

    @Test
    public void accessibleLabelAndDynamicName() throws Exception {
        onEdt(() -> {
            JXSevenSegmentDisplay display = new JXSevenSegmentDisplay('2');
            AccessibleContext context = display.getAccessibleContext();
            assertNotNull(context);
            assertEquals(AccessibleRole.LABEL, context.getAccessibleRole());
            assertEquals("Seven-segment display: 2", context.getAccessibleName());
            AtomicInteger notifications = new AtomicInteger();
            context.addPropertyChangeListener(event -> {
                if (AccessibleContext.ACCESSIBLE_NAME_PROPERTY
                        .equals(event.getPropertyName())) {
                    notifications.incrementAndGet();
                }
            });
            display.setCharacter('3');
            display.setShowDecimalPoint(true);
            display.setDecimalPointLit(true);
            assertEquals("Seven-segment display: 3.", context.getAccessibleName());
            assertEquals(2, notifications.get());
            display.setCharacter(':');
            assertEquals("Seven-segment display: :", context.getAccessibleName());
            assertEquals(3, notifications.get());
            context.setAccessibleName("Counter");
            display.setCharacter('9');
            assertEquals("Counter", context.getAccessibleName());
        });
    }

    @Test
    public void disabledStateAndObjectSerialization() throws Exception {
        onEdt(() -> {
            JXSevenSegmentDisplay display = new JXSevenSegmentDisplay('9');
            BufferedImage enabled = render(display);
            display.setEnabled(false);
            BufferedImage disabled = render(display);
            assertTrue(difference(enabled, disabled) > 20);
            assertFalse(display.isEnabled());
            try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    ObjectOutputStream output = new ObjectOutputStream(bytes)) {
                display.setShowDecimalPoint(true);
                display.setDecimalPointLit(true);
                output.writeObject(display);
                output.flush();
                try (ObjectInputStream input = new ObjectInputStream(
                        new ByteArrayInputStream(bytes.toByteArray()))) {
                    JXSevenSegmentDisplay restored =
                            (JXSevenSegmentDisplay) input.readObject();
                    assertEquals((int) '9', (int) restored.getCharacter());
                    assertTrue(restored.isShowDecimalPoint());
                    assertTrue(restored.isDecimalPointLit());
                    assertFalse(restored.isEnabled());
                    assertEquals(AccessibleRole.LABEL,
                            restored.getAccessibleContext().getAccessibleRole());
                }
            } catch (Exception failure) {
                throw new AssertionError(failure);
            }
        });
    }

    @Test
    public void installedSwingLookAndFeelsPaint() throws Exception {
        onEdt(() -> {
            LookAndFeel original = UIManager.getLookAndFeel();
            int families = 0;
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if (!info.getName().equals("Metal") && !info.getName().equals("Nimbus")) {
                        continue;
                    }
                    UIManager.setLookAndFeel(info.getClassName());
                    families++;
                    JXSevenSegmentDisplay display = new JXSevenSegmentDisplay('F');
                    assertTrue(render(display).getWidth() > 0);
                    display.setCharacter(':');
                    assertTrue(render(display).getHeight() > 0);
                }
                assertEquals(2, families);
            } catch (Exception failure) {
                throw new AssertionError(failure);
            } finally {
                try {
                    UIManager.setLookAndFeel(original);
                } catch (Exception failure) {
                    throw new AssertionError(failure);
                }
            }
        });
    }
}
