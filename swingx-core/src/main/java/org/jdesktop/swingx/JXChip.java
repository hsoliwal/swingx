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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventListener;
import java.util.EventObject;
import java.util.Objects;
import javax.accessibility.AccessibleContext;
import javax.swing.Icon;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

/**
 * A lightweight, selectable pill with an optional independent close target.
 *
 * <p>Selection, actions, items, focus, button model and keyboard activation are
 * inherited from {@link JToggleButton}. A close gesture is separate and
 * vetoable; it does not select/deselect the chip or remove it automatically.
 * No nested button, native SWT handle, second UI delegate or viewport exists.
 * All operations follow the ordinary Swing EDT convention.
 */
@SuppressWarnings("serial")
public class JXChip extends JToggleButton {
    private static final long serialVersionUID = 1L;
    private static final int CLOSE_SLOT = 22;
    private static final int CLOSE_DIAMETER = 16;
    private static final int CLOSE_PADDING = 5;
    private static final Color DEFAULT_BACKGROUND = new Color(224, 224, 224);
    private static final Color DEFAULT_SELECTED_BACKGROUND = new Color(66, 77, 112);

    private boolean closable;
    private transient boolean closeHovered;
    private transient boolean closeArmed;
    private Color selectedBackground = DEFAULT_SELECTED_BACKGROUND;
    private Color closeBackground = new Color(100, 100, 100);
    private Color closeHoverBackground = new Color(64, 64, 64);

    /** Creates an empty selectable chip. */
    @SuppressWarnings("this-escape")
    public JXChip() {
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setBackground(DEFAULT_BACKGROUND);
        setRolloverEnabled(true);
    }

    /** Creates a chip with the usual button text. */
    @SuppressWarnings("this-escape")
    public JXChip(String text) {
        this();
        // The superclass owns its text; constructor-time assignment is private.
        super.setText(text);
    }

    /** Creates a chip with the usual button text and icon. */
    @SuppressWarnings("this-escape")
    public JXChip(String text, Icon icon) {
        this(text);
        super.setIcon(icon);
    }

    /** Whether a separate trailing close glyph is displayed. */
    public boolean isClosable() {
        return closable;
    }

    /** Enables or hides the independent close glyph and its layout space. */
    public void setClosable(boolean value) {
        if (closable == value) {
            return;
        }
        boolean old = closable;
        closable = value;
        if (!closable) {
            closeHovered = false;
            closeArmed = false;
        }
        firePropertyChange("closable", old, value);
        revalidate();
        repaint();
    }

    /** Selected chip pill background, independent of Swing's ButtonModel. */
    public Color getSelectedBackground() {
        return selectedBackground;
    }

    /** Updates the selected chip pill background. */
    public void setSelectedBackground(Color color) {
        Color next = Objects.requireNonNull(color, "color");
        Color old = selectedBackground;
        selectedBackground = next;
        firePropertyChange("selectedBackground", old, next);
        repaint();
    }

    /** Normal background of the close target. */
    public Color getCloseBackground() {
        return closeBackground;
    }

    /** Updates the close target's normal background. */
    public void setCloseBackground(Color color) {
        Color next = Objects.requireNonNull(color, "color");
        Color old = closeBackground;
        closeBackground = next;
        firePropertyChange("closeBackground", old, next);
        repaint();
    }

    /** Hovered close target background. */
    public Color getCloseHoverBackground() {
        return closeHoverBackground;
    }

    /** Updates the hovered close target background. */
    public void setCloseHoverBackground(Color color) {
        Color next = Objects.requireNonNull(color, "color");
        Color old = closeHoverBackground;
        closeHoverBackground = next;
        firePropertyChange("closeHoverBackground", old, next);
        repaint();
    }

    /** Registers a veto-capable close listener, without changing button actions. */
    public void addCloseListener(CloseListener listener) {
        listenerList.add(CloseListener.class, Objects.requireNonNull(listener, "listener"));
    }

    /** Removes an existing close listener. */
    public void removeCloseListener(CloseListener listener) {
        listenerList.remove(CloseListener.class, Objects.requireNonNull(listener, "listener"));
    }

    /** Returns the registered close listeners. */
    public CloseListener[] getCloseListeners() {
        return listenerList.getListeners(CloseListener.class);
    }

    /**
     * Requests close without removing or disposing the component. Listeners may
     * veto subsequent delivery by setting {@link CloseEvent#setAllowed(boolean)}.
     *
     * @return true if all reached listeners allowed the close request
     */
    public boolean requestClose() {
        if (!isEnabled() || !closable) {
            return false;
        }
        CloseEvent event = new CloseEvent(this);
        for (CloseListener listener : getCloseListeners()) {
            listener.onClose(event);
            if (!event.isAllowed()) {
                break;
            }
        }
        return event.isAllowed();
    }

    /** A handler for the independent close action. */
    @FunctionalInterface
    public interface CloseListener extends EventListener {
        void onClose(CloseEvent event);
    }

    /** A close request that can be vetoed, separate from item selection. */
    public static final class CloseEvent extends EventObject {
        private static final long serialVersionUID = 1L;
        private boolean allowed = true;

        /** Creates a close request from its chip. */
        public CloseEvent(JXChip source) {
            super(Objects.requireNonNull(source, "source"));
        }

        /** Whether the close request remains allowed. */
        public boolean isAllowed() {
            return allowed;
        }

        /** Vetoes or re-allows the close request. */
        public void setAllowed(boolean value) {
            allowed = value;
        }

        /** Returns the source as its original chip type. */
        public JXChip getChip() {
            return (JXChip) getSource();
        }
    }

    @Override
    public Insets getInsets() {
        return getInsets(new Insets(0, 0, 0, 0));
    }

    @Override
    public Insets getInsets(Insets insets) {
        Insets result = super.getInsets(insets);
        if (closable) {
            if (getComponentOrientation().isLeftToRight()) {
                result.right = saturatingAdd(result.right, CLOSE_SLOT);
            } else {
                result.left = saturatingAdd(result.left, CLOSE_SLOT);
            }
        }
        return result;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        Graphics2D painting = (Graphics2D) graphics.create();
        try {
            painting.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            Color surface = isSelected() ? selectedBackground
                    : (getBackground() == null ? DEFAULT_BACKGROUND : getBackground());
            if (!isEnabled()) {
                surface = surface.brighter();
            } else if (getModel().isPressed()) {
                surface = surface.darker();
            } else if (getModel().isRollover()) {
                surface = surface.brighter();
            }
            painting.setColor(surface);
            painting.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1,
                    getHeight(), getHeight());
        } finally {
            painting.dispose();
        }

        super.paintComponent(graphics);

        if (!closable) {
            return;
        }
        Graphics2D closePainter = (Graphics2D) graphics.create();
        try {
            closePainter.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle bounds = closeBounds();
            closePainter.setColor(closeHovered ? closeHoverBackground : closeBackground);
            closePainter.fillOval(bounds.x, bounds.y, bounds.width, bounds.height);
            closePainter.setColor(isEnabled() ? Color.WHITE : Color.LIGHT_GRAY);
            closePainter.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND));
            int centerX = bounds.x + bounds.width / 2;
            int centerY = bounds.y + bounds.height / 2;
            closePainter.drawLine(centerX - 3, centerY - 3,
                    centerX + 3, centerY + 3);
            closePainter.drawLine(centerX + 3, centerY - 3,
                    centerX - 3, centerY + 3);
        } finally {
            closePainter.dispose();
        }
    }

    @Override
    protected void processMouseEvent(MouseEvent event) {
        int id = event.getID();
        boolean onClose = closable && closeBounds().contains(event.getPoint());
        if (id == MouseEvent.MOUSE_EXITED) {
            updateCloseHover(false);
        } else if (id == MouseEvent.MOUSE_ENTERED) {
            updateCloseHover(onClose);
        }
        if (closeArmed && id == MouseEvent.MOUSE_RELEASED) {
            closeArmed = false;
            if (isEnabled() && onClose && SwingUtilities.isLeftMouseButton(event)) {
                requestClose();
            }
            repaint();
            event.consume();
            return;
        }
        if (isEnabled() && onClose && id == MouseEvent.MOUSE_PRESSED
                && SwingUtilities.isLeftMouseButton(event)) {
            closeArmed = true;
            repaint();
            event.consume();
            return;
        }
        if (onClose && id == MouseEvent.MOUSE_CLICKED) {
            event.consume();
            return;
        }
        super.processMouseEvent(event);
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent event) {
        updateCloseHover(closable && closeBounds().contains(event.getPoint()));
        super.processMouseMotionEvent(event);
    }

    @Override
    protected void processKeyEvent(KeyEvent event) {
        if (closable && isEnabled() && event.getID() == KeyEvent.KEY_PRESSED
                && (event.getKeyCode() == KeyEvent.VK_DELETE
                        || event.getKeyCode() == KeyEvent.VK_BACK_SPACE)) {
            requestClose();
            event.consume();
            return;
        }
        super.processKeyEvent(event);
    }

    /** Adds a second accessible action for closing, preserving toggle action 0. */
    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJXChip();
        }
        return accessibleContext;
    }

    protected class AccessibleJXChip extends AccessibleJToggleButton {
        private static final long serialVersionUID = 1L;

        /** Creates the accessible context for this chip. */
        protected AccessibleJXChip() {
            super();
        }

        @Override
        public int getAccessibleActionCount() {
            return super.getAccessibleActionCount() + (closable ? 1 : 0);
        }

        @Override
        public String getAccessibleActionDescription(int index) {
            int original = super.getAccessibleActionCount();
            return closable && index == original
                    ? "Close chip" : super.getAccessibleActionDescription(index);
        }

        @Override
        public boolean doAccessibleAction(int index) {
            int original = super.getAccessibleActionCount();
            return closable && index == original
                    ? requestClose() : super.doAccessibleAction(index);
        }
    }

    private void updateCloseHover(boolean value) {
        if (closeHovered != value) {
            closeHovered = value;
            repaint();
        }
    }

    private Rectangle closeBounds() {
        int size = Math.min(CLOSE_DIAMETER, Math.max(0, getHeight() - 2 * CLOSE_PADDING));
        int x = getComponentOrientation().isLeftToRight()
                ? getWidth() - CLOSE_PADDING - size : CLOSE_PADDING;
        return new Rectangle(x, (getHeight() - size) / 2, size, size);
    }

    private static int saturatingAdd(int left, int right) {
        return (int) Math.min(Integer.MAX_VALUE, (long) left + right);
    }
}
