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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

/**
 * A native Swing button with a separate trailing popup-menu target.
 *
 * <p>The primary region uses the original {@link JButton} model, UI delegate,
 * focus, actions, text and icon. The arrow region opens an existing
 * {@link JPopupMenu}, never fires the primary action, and respects RTL layout.
 * Toggle mode uses that same button model's selected bit rather than a second
 * state or an embedded button. All Swing mutations follow the EDT convention.
 */
@SuppressWarnings("serial")
public class JXSplitButton extends JButton {
    private static final long serialVersionUID = 1L;
    private static final int ARROW_SLOT = 23;
    private static final int TRIANGLE_WIDTH = 8;

    private JPopupMenu popupMenu;
    private boolean toggleMode;
    private transient boolean arrowArmed;
    private transient boolean arrowHovered;

    /** Creates an empty push-mode split button. */
    @SuppressWarnings("this-escape")
    public JXSplitButton() {
        setRolloverEnabled(true);
    }

    /** Creates a push-mode split button with text. */
    @SuppressWarnings("this-escape")
    public JXSplitButton(String text) {
        this();
        super.setText(text);
    }

    /** Creates a push-mode split button with text and icon. */
    @SuppressWarnings("this-escape")
    public JXSplitButton(String text, Icon icon) {
        this(text);
        super.setIcon(icon);
    }

    /** Returns the existing Swing popup displayed by the arrow region. */
    public JPopupMenu getPopupMenu() {
        return popupMenu;
    }

    /** Installs a popup without copying or owning its menu-item state. */
    public void setPopupMenu(JPopupMenu value) {
        if (popupMenu == value) {
            return;
        }
        JPopupMenu previous = popupMenu;
        popupMenu = value;
        if (value == null) {
            arrowArmed = false;
            arrowHovered = false;
        }
        firePropertyChange("popupMenu", previous, value);
        revalidate();
        repaint();
    }

    /** Whether the primary action toggles the native button-model selection. */
    public boolean isToggleMode() {
        return toggleMode;
    }

    /** Switches primary activation between PUSH and TOGGLE. */
    public void setToggleMode(boolean value) {
        if (toggleMode == value) {
            return;
        }
        boolean previous = toggleMode;
        toggleMode = value;
        firePropertyChange("toggleMode", previous, value);
        repaint();
    }

    @Override
    protected void fireActionPerformed(ActionEvent event) {
        if (toggleMode && isEnabled()) {
            setSelected(!isSelected());
        }
        super.fireActionPerformed(event);
    }

    @Override
    public Insets getInsets() {
        return getInsets(new Insets(0, 0, 0, 0));
    }

    @Override
    public Insets getInsets(Insets insets) {
        Insets result = super.getInsets(insets);
        if (popupMenu != null) {
            if (getComponentOrientation().isLeftToRight()) {
                result.right = saturatingAdd(result.right, ARROW_SLOT);
            } else {
                result.left = saturatingAdd(result.left, ARROW_SLOT);
            }
        }
        return result;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (popupMenu == null || getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        Graphics2D paint = (Graphics2D) graphics.create();
        try {
            Rectangle bounds = arrowBounds();
            Color foreground = getForeground() == null ? Color.DARK_GRAY : getForeground();
            if (!isEnabled()) {
                foreground = foreground.brighter();
            }
            if (arrowHovered && isEnabled()) {
                Color hover = getBackground() == null ? Color.LIGHT_GRAY
                        : getBackground().darker();
                paint.setColor(new Color(hover.getRed(), hover.getGreen(),
                        hover.getBlue(), 48));
                paint.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
            paint.setColor(foreground);
            int divider = getComponentOrientation().isLeftToRight()
                    ? bounds.x : bounds.x + bounds.width - 1;
            int top = Math.max(2, getHeight() / 5);
            paint.drawLine(divider, top, divider, Math.max(top, getHeight() - top));
            int centerX = bounds.x + bounds.width / 2;
            int centerY = bounds.y + bounds.height / 2;
            for (int row = 0; row < TRIANGLE_WIDTH / 2; row++) {
                paint.drawLine(centerX - TRIANGLE_WIDTH / 2 + row,
                        centerY - 2 + row, centerX + TRIANGLE_WIDTH / 2 - row,
                        centerY - 2 + row);
            }
        } finally {
            paint.dispose();
        }
    }

    @Override
    protected void processMouseEvent(MouseEvent event) {
        int id = event.getID();
        boolean onArrow = popupMenu != null && arrowBounds().contains(event.getPoint());
        if (id == MouseEvent.MOUSE_EXITED) {
            setArrowHovered(false);
        } else if (id == MouseEvent.MOUSE_ENTERED) {
            setArrowHovered(onArrow);
        }
        if (arrowArmed && id == MouseEvent.MOUSE_RELEASED) {
            arrowArmed = false;
            if (isEnabled() && onArrow && SwingUtilities.isLeftMouseButton(event)) {
                showPopupMenu();
            }
            repaint();
            event.consume();
            return;
        }
        if (isEnabled() && onArrow && id == MouseEvent.MOUSE_PRESSED
                && SwingUtilities.isLeftMouseButton(event)) {
            arrowArmed = true;
            repaint();
            event.consume();
            return;
        }
        if (onArrow && id == MouseEvent.MOUSE_CLICKED) {
            event.consume();
            return;
        }
        super.processMouseEvent(event);
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent event) {
        setArrowHovered(popupMenu != null && arrowBounds().contains(event.getPoint()));
        super.processMouseMotionEvent(event);
    }

    @Override
    protected void processKeyEvent(KeyEvent event) {
        if (popupMenu != null && isEnabled() && event.getID() == KeyEvent.KEY_PRESSED
                && (event.getKeyCode() == KeyEvent.VK_F4
                        || (event.getKeyCode() == KeyEvent.VK_DOWN && event.isAltDown()))) {
            showPopupMenu();
            event.consume();
            return;
        }
        super.processKeyEvent(event);
    }

    /**
     * Opens the attached menu below the button when showing on screen.
     * Subclasses may customize the placement; nothing opens in headless or
     * unattached components.
     *
     * @return true if a popup was requested while its owner was showing
     */
    public boolean showPopupMenu() {
        JPopupMenu menu = popupMenu;
        if (menu == null || !isEnabled() || !isShowing()) {
            return false;
        }
        int x = getComponentOrientation().isLeftToRight() ? 0
                : Math.max(0, getWidth() - menu.getPreferredSize().width);
        menu.show(this, x, getHeight());
        return true;
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJXSplitButton();
        }
        return accessibleContext;
    }

    /** Preserves the original Swing push action and adds an arrow menu action. */
    protected class AccessibleJXSplitButton extends AccessibleJButton {
        private static final long serialVersionUID = 1L;

        protected AccessibleJXSplitButton() {
            super();
        }

        @Override
        public AccessibleRole getAccessibleRole() {
            return toggleMode ? AccessibleRole.TOGGLE_BUTTON : super.getAccessibleRole();
        }

        @Override
        public int getAccessibleActionCount() {
            return super.getAccessibleActionCount() + (popupMenu == null ? 0 : 1);
        }

        @Override
        public String getAccessibleActionDescription(int index) {
            int ordinary = super.getAccessibleActionCount();
            return popupMenu != null && index == ordinary
                    ? "Open popup menu" : super.getAccessibleActionDescription(index);
        }

        @Override
        public boolean doAccessibleAction(int index) {
            int ordinary = super.getAccessibleActionCount();
            return popupMenu != null && index == ordinary
                    ? showPopupMenu() : super.doAccessibleAction(index);
        }
    }

    private void setArrowHovered(boolean hovered) {
        if (arrowHovered != hovered) {
            arrowHovered = hovered;
            repaint(arrowBounds());
        }
    }

    private Rectangle arrowBounds() {
        int width = Math.min(ARROW_SLOT, Math.max(0, getWidth()));
        int x = getComponentOrientation().isLeftToRight() ? getWidth() - width : 0;
        return new Rectangle(x, 0, width, Math.max(0, getHeight()));
    }

    private static int saturatingAdd(int left, int right) {
        return (int) Math.min(Integer.MAX_VALUE, (long) left + right);
    }
}
