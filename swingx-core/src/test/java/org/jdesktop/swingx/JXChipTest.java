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
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import javax.accessibility.AccessibleRole;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.Test;

/** Headless Swing-chip model, clipped painting and close event contract. */
public class JXChipTest {
    private static final class KeyboardProbe extends JXChip {
        private static final long serialVersionUID = 1L;

        KeyboardProbe(String text) {
            super(text);
        }

        void pressKey(int code) {
            processKeyEvent(new KeyEvent(this, KeyEvent.KEY_PRESSED,
                    System.currentTimeMillis(), 0, code, KeyEvent.CHAR_UNDEFINED));
        }
    }

    private static void edt(Runnable runnable) throws Exception {
        SwingUtilities.invokeAndWait(runnable);
    }

    private static BufferedImage render(JXChip chip) {
        Dimension size = chip.getPreferredSize();
        chip.setSize(size);
        BufferedImage image = new BufferedImage(size.width, size.height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            chip.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static int difference(BufferedImage first, BufferedImage second) {
        int pixels = 0;
        for (int y = 0; y < first.getHeight(); y++) {
            for (int x = 0; x < first.getWidth(); x++) {
                if (first.getRGB(x, y) != second.getRGB(x, y)) {
                    pixels++;
                }
            }
        }
        return pixels;
    }

    private static void mouse(JXChip chip, int kind, int x, int y) {
        chip.dispatchEvent(new MouseEvent(chip, kind, System.currentTimeMillis(),
                0, x, y, 1, false, MouseEvent.BUTTON1));
    }

    @Test
    public void inheritedButtonModelAndItemActions() throws Exception {
        edt(() -> {
            JXChip chip = new JXChip("Tag");
            JToggleButton baseline = new JToggleButton("Tag");
            assertEquals(baseline.getModel().getClass(), chip.getModel().getClass());
            AtomicInteger items = new AtomicInteger();
            AtomicInteger actions = new AtomicInteger();
            chip.addItemListener(event -> items.incrementAndGet());
            chip.addActionListener(event -> actions.incrementAndGet());
            chip.doClick();
            assertTrue(chip.isSelected());
            assertEquals(1, items.get());
            assertEquals(1, actions.get());
            chip.doClick();
            assertFalse(chip.isSelected());
            assertEquals(2, items.get());
            assertEquals(2, actions.get());
            assertEquals(0, chip.getComponentCount());
        });
    }

    @Test
    public void closableGeometryAndOriginalIconText() throws Exception {
        edt(() -> {
            BufferedImage icon = new BufferedImage(12, 12, BufferedImage.TYPE_INT_ARGB);
            JXChip chip = new JXChip("Inbox", new ImageIcon(icon));
            Dimension normal = chip.getPreferredSize();
            chip.setClosable(true);
            assertTrue(chip.isClosable());
            assertTrue(chip.getPreferredSize().width > normal.width);
            assertEquals("Inbox", chip.getText());
            assertNotNull(chip.getIcon());
            assertEquals(0, chip.getComponentCount());
            chip.setClosable(false);
            assertEquals(normal, chip.getPreferredSize());
            chip.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            chip.setClosable(true);
            assertTrue(chip.getInsets().left > 0);
        });
    }

    @Test
    public void closeListenerIsVetoableAndSelectionNeutral() throws Exception {
        edt(() -> {
            JXChip chip = new JXChip("Mail");
            chip.setClosable(true);
            chip.setSelected(true);
            AtomicInteger calls = new AtomicInteger();
            JXChip.CloseListener veto = event -> {
                assertEquals(chip, event.getChip());
                calls.incrementAndGet();
                event.setAllowed(false);
            };
            chip.addCloseListener(veto);
            assertEquals(1, chip.getCloseListeners().length);
            assertFalse(chip.requestClose());
            assertEquals(1, calls.get());
            assertTrue(chip.isSelected());
            chip.removeCloseListener(veto);
            assertEquals(0, chip.getCloseListeners().length);
            assertTrue(chip.requestClose());
            assertTrue(chip.isSelected());
            chip.setClosable(false);
            assertFalse(chip.requestClose());
        });
    }

    @Test
    public void closeMouseTargetDoesNotToggleSelection() throws Exception {
        edt(() -> {
            JXChip chip = new JXChip("Mail");
            chip.setClosable(true);
            render(chip);
            AtomicInteger closed = new AtomicInteger();
            AtomicInteger selected = new AtomicInteger();
            chip.addCloseListener(event -> closed.incrementAndGet());
            chip.addItemListener(event -> selected.incrementAndGet());
            int x = chip.getWidth() - 13;
            int y = chip.getHeight() / 2;
            mouse(chip, MouseEvent.MOUSE_PRESSED, x, y);
            mouse(chip, MouseEvent.MOUSE_RELEASED, x, y);
            mouse(chip, MouseEvent.MOUSE_CLICKED, x, y);
            assertEquals(1, closed.get());
            assertEquals(0, selected.get());
            assertFalse(chip.isSelected());
            chip.setEnabled(false);
            mouse(chip, MouseEvent.MOUSE_PRESSED, x, y);
            mouse(chip, MouseEvent.MOUSE_RELEASED, x, y);
            assertEquals(1, closed.get());
        });
    }

    @Test
    public void deleteAndBackspaceCloseWithoutButtonAction() throws Exception {
        edt(() -> {
            KeyboardProbe chip = new KeyboardProbe("Mail");
            chip.setClosable(true);
            AtomicInteger closed = new AtomicInteger();
            AtomicInteger actions = new AtomicInteger();
            chip.addCloseListener(event -> closed.incrementAndGet());
            chip.addActionListener(event -> actions.incrementAndGet());
            chip.pressKey(KeyEvent.VK_DELETE);
            chip.pressKey(KeyEvent.VK_BACK_SPACE);
            assertEquals(2, closed.get());
            assertEquals(0, actions.get());
            assertFalse(chip.isSelected());
            chip.setEnabled(false);
            chip.pressKey(KeyEvent.VK_DELETE);
            assertEquals(2, closed.get());
        });
    }

    @Test
    public void accessibleTogglePreservedAndCloseActionAdded() throws Exception {
        edt(() -> {
            JXChip chip = new JXChip("Mail");
            assertEquals(AccessibleRole.TOGGLE_BUTTON,
                    chip.getAccessibleContext().getAccessibleRole());
            int original = chip.getAccessibleContext().getAccessibleAction()
                    .getAccessibleActionCount();
            chip.setClosable(true);
            var actions = chip.getAccessibleContext().getAccessibleAction();
            assertEquals(original + 1, actions.getAccessibleActionCount());
            assertEquals("Close chip", actions.getAccessibleActionDescription(original));
            AtomicInteger closes = new AtomicInteger();
            chip.addCloseListener(event -> closes.incrementAndGet());
            assertTrue(actions.doAccessibleAction(original));
            assertEquals(1, closes.get());
            assertFalse(chip.isSelected());
        });
    }

    @Test
    public void colorsAndSelectedOrHoverPainting() throws Exception {
        edt(() -> {
            JXChip chip = new JXChip("Mail");
            chip.setClosable(true);
            BufferedImage ordinary = render(chip);
            chip.setSelected(true);
            BufferedImage selected = render(chip);
            assertTrue(difference(ordinary, selected) > 20);
            chip.setSelectedBackground(Color.RED);
            chip.setCloseBackground(Color.BLUE);
            chip.setCloseHoverBackground(Color.YELLOW);
            assertEquals(Color.RED, chip.getSelectedBackground());
            assertEquals(Color.BLUE, chip.getCloseBackground());
            assertEquals(Color.YELLOW, chip.getCloseHoverBackground());
            BufferedImage recolored = render(chip);
            assertTrue(difference(selected, recolored) > 20);
            chip.setEnabled(false);
            assertTrue(difference(recolored, render(chip)) > 20);
            chip.setSelected(false);
            chip.setBackground(null);
            assertTrue(render(chip).getWidth() > 0);
        });
    }

    @Test
    public void clippingAndSerializationRemainSwingNative() throws Exception {
        edt(() -> {
            JXChip chip = new JXChip("Mail");
            chip.setClosable(true);
            render(chip);
            BufferedImage image = new BufferedImage(chip.getWidth(), chip.getHeight(),
                    BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setColor(Color.MAGENTA);
                graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
                graphics.setClip(0, 0, 3, 3);
                chip.paint(graphics);
            } finally {
                graphics.dispose();
            }
            assertEquals(Color.MAGENTA.getRGB(),
                    image.getRGB(image.getWidth() - 5, image.getHeight() / 2));
            try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    ObjectOutputStream output = new ObjectOutputStream(bytes)) {
                chip.setSelected(true);
                output.writeObject(chip);
                output.flush();
                try (ObjectInputStream input = new ObjectInputStream(
                        new ByteArrayInputStream(bytes.toByteArray()))) {
                    JXChip restored = (JXChip) input.readObject();
                    assertEquals("Mail", restored.getText());
                    assertTrue(restored.isSelected());
                    assertTrue(restored.isClosable());
                    assertEquals(AccessibleRole.TOGGLE_BUTTON,
                            restored.getAccessibleContext().getAccessibleRole());
                }
            } catch (Exception failure) {
                throw new AssertionError(failure);
            }
        });
    }

    @Test
    public void metalAndNimbusRenderInHeadlessSwing() throws Exception {
        edt(() -> {
            var initial = UIManager.getLookAndFeel();
            int count = 0;
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if (!info.getName().equals("Metal") && !info.getName().equals("Nimbus")) {
                        continue;
                    }
                    UIManager.setLookAndFeel(info.getClassName());
                    count++;
                    JXChip chip = new JXChip("Mail");
                    chip.setClosable(true);
                    assertTrue(render(chip).getWidth() > 0);
                    chip.doClick();
                    assertTrue(render(chip).getHeight() > 0);
                }
                assertEquals(2, count);
            } catch (Exception failure) {
                throw new AssertionError(failure);
            } finally {
                try {
                    UIManager.setLookAndFeel(initial);
                } catch (Exception failure) {
                    throw new AssertionError(failure);
                }
            }
        });
    }
}
