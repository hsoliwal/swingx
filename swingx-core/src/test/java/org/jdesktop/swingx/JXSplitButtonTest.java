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

import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
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
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.Test;

/** Native Swing split-button input, model, menu isolation and paint. */
public class JXSplitButtonTest {
    private static final class PopupProbe extends JXSplitButton {
        private static final long serialVersionUID = 1L;
        private int opened;

        PopupProbe(String text) {
            super(text);
        }

        @Override
        public boolean showPopupMenu() {
            if (getPopupMenu() == null || !isEnabled()) {
                return false;
            }
            opened++;
            return true;
        }

        void pressKey(int code, int modifiers) {
            processKeyEvent(new KeyEvent(this, KeyEvent.KEY_PRESSED,
                    System.currentTimeMillis(), modifiers, code, KeyEvent.CHAR_UNDEFINED));
        }
    }

    private static void edt(Runnable runnable) throws Exception {
        SwingUtilities.invokeAndWait(runnable);
    }

    private static BufferedImage render(JXSplitButton button) {
        Dimension size = button.getPreferredSize();
        button.setSize(size);
        BufferedImage image = new BufferedImage(size.width, size.height,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            button.paint(graphics);
        } finally {
            graphics.dispose();
        }
        return image;
    }

    private static void mouse(JXSplitButton button, int kind, int x, int y) {
        button.dispatchEvent(new MouseEvent(button, kind, System.currentTimeMillis(),
                0, x, y, 1, false, MouseEvent.BUTTON1));
    }

    @Test
    public void inheritsOriginalJButtonPushAction() throws Exception {
        edt(() -> {
            JXSplitButton button = new JXSplitButton("Save");
            assertEquals(new JButton().getModel().getClass(), button.getModel().getClass());
            assertEquals(AccessibleRole.PUSH_BUTTON,
                    button.getAccessibleContext().getAccessibleRole());
            AtomicInteger actions = new AtomicInteger();
            button.addActionListener(event -> actions.incrementAndGet());
            button.doClick();
            button.doClick();
            assertEquals(2, actions.get());
            assertFalse(button.isSelected());
            assertFalse(button.isToggleMode());
            assertEquals(0, button.getComponentCount());
        });
    }

    @Test
    public void nativeButtonModelSupportsToggleWithoutParallelState() throws Exception {
        edt(() -> {
            JXSplitButton button = new JXSplitButton("Apply");
            AtomicInteger items = new AtomicInteger();
            AtomicInteger actions = new AtomicInteger();
            button.addItemListener(event -> items.incrementAndGet());
            button.addActionListener(event -> actions.incrementAndGet());
            button.setToggleMode(true);
            assertEquals(AccessibleRole.TOGGLE_BUTTON,
                    button.getAccessibleContext().getAccessibleRole());
            button.doClick();
            assertTrue(button.isSelected());
            button.doClick();
            assertFalse(button.isSelected());
            assertEquals(2, items.get());
            assertEquals(2, actions.get());
            button.setToggleMode(false);
            assertEquals(AccessibleRole.PUSH_BUTTON,
                    button.getAccessibleContext().getAccessibleRole());
            button.doClick();
            assertEquals(3, actions.get());
            assertFalse(button.isSelected());
        });
    }

    @Test
    public void menuOwnershipIconTextAndRtlLayout() throws Exception {
        edt(() -> {
            BufferedImage icon = new BufferedImage(12, 12, BufferedImage.TYPE_INT_ARGB);
            JXSplitButton button = new JXSplitButton("Save", new ImageIcon(icon));
            Dimension plain = button.getPreferredSize();
            JPopupMenu popup = new JPopupMenu();
            popup.add(new JMenuItem("Save as"));
            button.setPopupMenu(popup);
            assertEquals(popup, button.getPopupMenu());
            assertTrue(button.getPreferredSize().width > plain.width);
            assertTrue(button.getInsets().right > 0);
            assertEquals("Save", button.getText());
            assertNotNull(button.getIcon());
            assertEquals(0, button.getComponentCount());
            button.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            assertTrue(button.getInsets().left > 0);
            button.setPopupMenu(null);
            assertEquals(plain, button.getPreferredSize());
            assertFalse(button.showPopupMenu());
        });
    }

    @Test
    public void menuMouseRegionDoesNotFirePrimaryButton() throws Exception {
        edt(() -> {
            PopupProbe button = new PopupProbe("Save");
            button.setPopupMenu(new JPopupMenu());
            render(button);
            AtomicInteger actions = new AtomicInteger();
            button.addActionListener(event -> actions.incrementAndGet());
            int menuX = button.getWidth() - 10;
            int middleY = button.getHeight() / 2;
            mouse(button, MouseEvent.MOUSE_PRESSED, menuX, middleY);
            mouse(button, MouseEvent.MOUSE_RELEASED, menuX, middleY);
            mouse(button, MouseEvent.MOUSE_CLICKED, menuX, middleY);
            assertEquals(1, button.opened);
            assertEquals(0, actions.get());
            assertFalse(button.isSelected());
            mouse(button, MouseEvent.MOUSE_PRESSED, 8, middleY);
            mouse(button, MouseEvent.MOUSE_RELEASED, 8, middleY);
            mouse(button, MouseEvent.MOUSE_CLICKED, 8, middleY);
            assertEquals(1, actions.get());
            assertEquals(1, button.opened);
            button.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            mouse(button, MouseEvent.MOUSE_PRESSED, 10, middleY);
            mouse(button, MouseEvent.MOUSE_RELEASED, 10, middleY);
            mouse(button, MouseEvent.MOUSE_CLICKED, 10, middleY);
            assertEquals(2, button.opened);
            assertEquals(1, actions.get());
            button.setEnabled(false);
            mouse(button, MouseEvent.MOUSE_PRESSED, 10, middleY);
            mouse(button, MouseEvent.MOUSE_RELEASED, 10, middleY);
            assertEquals(2, button.opened);
        });
    }

    @Test
    public void popupKeyboardShortcutsAreActionNeutral() throws Exception {
        edt(() -> {
            PopupProbe button = new PopupProbe("Save");
            button.setPopupMenu(new JPopupMenu());
            AtomicInteger actions = new AtomicInteger();
            button.addActionListener(event -> actions.incrementAndGet());
            button.pressKey(KeyEvent.VK_F4, 0);
            button.pressKey(KeyEvent.VK_DOWN, InputEvent.ALT_DOWN_MASK);
            assertEquals(2, button.opened);
            assertEquals(0, actions.get());
            button.setEnabled(false);
            button.pressKey(KeyEvent.VK_F4, 0);
            assertEquals(2, button.opened);
        });
    }

    @Test
    public void accessiblePrimaryActionAndSeparatePopupAction() throws Exception {
        edt(() -> {
            PopupProbe button = new PopupProbe("Save");
            var accessible = button.getAccessibleContext().getAccessibleAction();
            int ordinary = accessible.getAccessibleActionCount();
            button.setPopupMenu(new JPopupMenu());
            assertEquals(ordinary + 1, accessible.getAccessibleActionCount());
            assertEquals("Open popup menu", accessible.getAccessibleActionDescription(ordinary));
            AtomicInteger actions = new AtomicInteger();
            button.addActionListener(event -> actions.incrementAndGet());
            assertTrue(accessible.doAccessibleAction(0));
            assertEquals(1, actions.get());
            assertTrue(accessible.doAccessibleAction(ordinary));
            assertEquals(1, button.opened);
            assertEquals(1, actions.get());
        });
    }

    @Test
    public void detachedMenuCannotPopUpButIsNotLost() throws Exception {
        edt(() -> {
            JXSplitButton button = new JXSplitButton("Save");
            JPopupMenu popup = new JPopupMenu();
            button.setPopupMenu(popup);
            assertFalse(button.showPopupMenu());
            assertEquals(popup, button.getPopupMenu());
            button.setEnabled(false);
            assertFalse(button.showPopupMenu());
        });
    }

    @Test
    public void paintsWithinClipAndUnderMetalNimbus() throws Exception {
        edt(() -> {
            var old = UIManager.getLookAndFeel();
            try {
                for (String name : new String[] {
                        "javax.swing.plaf.metal.MetalLookAndFeel",
                        "javax.swing.plaf.nimbus.NimbusLookAndFeel"}) {
                    try {
                        UIManager.setLookAndFeel(name);
                    } catch (Exception ex) {
                        throw new AssertionError(ex);
                    }
                    JXSplitButton button = new JXSplitButton("Save");
                    button.setPopupMenu(new JPopupMenu());
                    BufferedImage image = render(button);
                    assertTrue(image.getWidth() > 23);
                    assertTrue(image.getHeight() > 0);
                    BufferedImage clipped = new BufferedImage(image.getWidth(),
                            image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D graphics = clipped.createGraphics();
                    try {
                        graphics.setClip(0, 0, image.getWidth() / 2, image.getHeight());
                        button.paint(graphics);
                    } finally {
                        graphics.dispose();
                    }
                    assertEquals(0, clipped.getRGB(image.getWidth() - 1,
                            image.getHeight() / 2));
                }
            } finally {
                try {
                    UIManager.setLookAndFeel(old);
                } catch (Exception ex) {
                    throw new AssertionError(ex);
                }
            }
        });
    }

    @Test
    public void serializablePopupAndSelectionState() throws Exception {
        final JXSplitButton[] source = new JXSplitButton[1];
        edt(() -> {
            JXSplitButton button = new JXSplitButton("Save");
            button.setPopupMenu(new JPopupMenu());
            button.setToggleMode(true);
            button.doClick();
            source[0] = button;
        });
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(source[0]);
        }
        JXSplitButton restored;
        try (ObjectInputStream in = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (JXSplitButton) in.readObject();
        }
        assertEquals("Save", restored.getText());
        assertTrue(restored.isToggleMode());
        assertTrue(restored.isSelected());
        assertNotNull(restored.getPopupMenu());
    }
}
