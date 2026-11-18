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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.util.Objects;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JComponent;

/**
 * A seven-segment LED character display with an optional decimal point.
 *
 * <p>The colon character displays the two-dot separator used between LED
 * digits; regular characters are digits, hexadecimal letters, blank, or minus.
 * This is one pure-Swing {@code JComponent}: its ordinary EDT, border,
 * orientation, enabled state, clipping and serialization conventions apply.
 * It never owns an SWT {@code Display}, graphics context or native handle.
 * The default colors resemble an illuminated green segmented instrument.
 */
@SuppressWarnings("serial")
public class JXSevenSegmentDisplay extends JComponent {
    private static final long serialVersionUID = 1L;
    private static final int CHARACTER_WIDTH = 60;
    private static final int COLON_WIDTH = 30;
    private static final int CHARACTER_HEIGHT = 100;
    private static final int DECIMAL_SLOT = 15;
    private static final int SEGMENT_COUNT = 7;

    // Seven polygons in logical 60x100 coordinates: top, upper right, lower
    // right, bottom, lower left, upper left, middle. The mask bit is the index.
    private static final int[][][] SEGMENTS = {
        {{12, 3}, {48, 3}, {53, 8}, {48, 13}, {12, 13}, {7, 8}},
        {{53, 12}, {58, 7}, {58, 45}, {53, 49}, {48, 44}, {48, 17}},
        {{53, 52}, {58, 56}, {58, 93}, {53, 98}, {48, 88}, {48, 57}},
        {{12, 87}, {48, 87}, {53, 92}, {48, 97}, {12, 97}, {7, 92}},
        {{7, 52}, {12, 57}, {12, 88}, {7, 98}, {2, 93}, {2, 56}},
        {{7, 12}, {12, 17}, {12, 44}, {7, 49}, {2, 45}, {2, 7}},
        {{12, 45}, {48, 45}, {53, 50}, {48, 55}, {12, 55}, {7, 50}}
    };

    private char character = ' ';
    private Color litColor = new Color(148, 237, 147);
    private Color unlitColor = new Color(0, 29, 29);
    private boolean showDecimalPoint;
    private boolean decimalPointLit;

    /** Creates a blank, dark-backed instrument display. */
    @SuppressWarnings("this-escape")
    public JXSevenSegmentDisplay() {
        setOpaque(true);
        setBackground(Color.BLACK);
    }

    /** Creates a display for a supported character (0-9, A-F, minus, colon). */
    public JXSevenSegmentDisplay(char initialCharacter) {
        this();
        char normalized = Character.toUpperCase(initialCharacter);
        if (segmentMask(normalized) < 0) {
            throw new IllegalArgumentException(
                    "unsupported seven-segment character: " + initialCharacter);
        }
        character = normalized;
    }

    /** Returns the character currently represented on the display. */
    public char getCharacter() {
        return character;
    }

    /**
     * Sets the character to display. Lowercase hexadecimal letters normalize
     * to uppercase. Unsupported characters are rejected before mutation.
     */
    public void setCharacter(char value) {
        char next = Character.toUpperCase(value);
        if (segmentMask(next) < 0) {
            throw new IllegalArgumentException("unsupported seven-segment character: " + value);
        }
        if (character == next) {
            return;
        }
        String previousName = accessibleContext == null
                ? null : accessibleContext.getAccessibleName();
        char old = character;
        character = next;
        firePropertyChange("character", Character.valueOf(old), Character.valueOf(next));
        if (accessibleContext != null) {
            accessibleContext.firePropertyChange(AccessibleContext.ACCESSIBLE_NAME_PROPERTY,
                    previousName, accessibleContext.getAccessibleName());
        }
        if (old == ':' || next == ':') {
            revalidate();
        }
        repaint();
    }

    /** Sets a decimal digit from 0 through 9. */
    public void setDigit(int value) {
        if (value < 0 || value > 9) {
            throw new IllegalArgumentException("digit must be between 0 and 9: " + value);
        }
        setCharacter((char) ('0' + value));
    }

    /** Color used for illuminated segments. */
    public Color getLitColor() {
        return litColor;
    }

    /** Sets the illuminated-segment color. */
    public void setLitColor(Color value) {
        Color next = Objects.requireNonNull(value, "value");
        Color old = litColor;
        litColor = next;
        firePropertyChange("litColor", old, next);
        repaint();
    }

    /** Color used for segments that are switched off. */
    public Color getUnlitColor() {
        return unlitColor;
    }

    /** Sets the switched-off segment color. */
    public void setUnlitColor(Color value) {
        Color next = Objects.requireNonNull(value, "value");
        Color old = unlitColor;
        unlitColor = next;
        firePropertyChange("unlitColor", old, next);
        repaint();
    }

    /** Whether a decimal-point slot is reserved next to an ordinary digit. */
    public boolean isShowDecimalPoint() {
        return showDecimalPoint;
    }

    /** Shows or removes the decimal-point slot, recalculating preferred width. */
    public void setShowDecimalPoint(boolean value) {
        if (showDecimalPoint == value) {
            return;
        }
        String previousName = accessibleContext == null
                ? null : accessibleContext.getAccessibleName();
        boolean old = showDecimalPoint;
        showDecimalPoint = value;
        firePropertyChange("showDecimalPoint", old, value);
        if (accessibleContext != null) {
            accessibleContext.firePropertyChange(AccessibleContext.ACCESSIBLE_NAME_PROPERTY,
                    previousName, accessibleContext.getAccessibleName());
        }
        revalidate();
        repaint();
    }

    /** Whether the decimal point is illuminated (even if its slot is hidden). */
    public boolean isDecimalPointLit() {
        return decimalPointLit;
    }

    /** Turns the optional decimal point on or off. */
    public void setDecimalPointLit(boolean value) {
        if (decimalPointLit == value) {
            return;
        }
        String previousName = accessibleContext == null
                ? null : accessibleContext.getAccessibleName();
        boolean old = decimalPointLit;
        decimalPointLit = value;
        firePropertyChange("decimalPointLit", old, value);
        if (accessibleContext != null) {
            accessibleContext.firePropertyChange(AccessibleContext.ACCESSIBLE_NAME_PROPERTY,
                    previousName, accessibleContext.getAccessibleName());
        }
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        Insets insets = getInsets();
        int width = character == ':' ? COLON_WIDTH
                : CHARACTER_WIDTH + (showDecimalPoint ? DECIMAL_SLOT : 0);
        return new Dimension(width + insets.left + insets.right,
                CHARACTER_HEIGHT + insets.top + insets.bottom);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (getWidth() <= 0 || getHeight() <= 0) {
            return;
        }
        Graphics2D painting = (Graphics2D) graphics.create();
        try {
            if (isOpaque()) {
                painting.setColor(getBackground());
                painting.fillRect(0, 0, getWidth(), getHeight());
            }
            Insets insets = getInsets();
            int width = Math.max(0, getWidth() - insets.left - insets.right);
            int height = Math.max(0, getHeight() - insets.top - insets.bottom);
            if (width <= 0 || height <= 0) {
                return;
            }
            painting.translate(insets.left, insets.top);
            int designWidth = character == ':' ? COLON_WIDTH
                    : CHARACTER_WIDTH + (showDecimalPoint ? DECIMAL_SLOT : 0);
            painting.scale((double) width / designWidth,
                    (double) height / CHARACTER_HEIGHT);
            painting.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            if (character == ':') {
                painting.setColor(litOrDisabled(litColor));
                painting.fillOval(10, 25, 10, 10);
                painting.fillOval(10, 65, 10, 10);
            } else {
                int mask = segmentMask(character);
                for (int index = 0; index < SEGMENT_COUNT; index++) {
                    painting.setColor((mask & (1 << index)) != 0
                            ? litOrDisabled(litColor) : unlitColor);
                    int[][] points = SEGMENTS[index];
                    Polygon polygon = new Polygon();
                    for (int[] point : points) {
                        polygon.addPoint(point[0], point[1]);
                    }
                    painting.fillPolygon(polygon);
                }
                if (showDecimalPoint) {
                    painting.setColor(decimalPointLit
                            ? litOrDisabled(litColor) : unlitColor);
                    painting.fillOval(61, 85, 11, 11);
                }
            }
        } finally {
            painting.dispose();
        }
    }

    /** Exposes the illuminated character as a label, not an interactive input. */
    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJXSevenSegmentDisplay();
        }
        return accessibleContext;
    }

    protected class AccessibleJXSevenSegmentDisplay extends AccessibleJComponent {
        private static final long serialVersionUID = 1L;

        /** Creates the accessible context for this instrument. */
        protected AccessibleJXSevenSegmentDisplay() {
            super();
        }

        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.LABEL;
        }

        @Override
        public String getAccessibleName() {
            String explicit = super.getAccessibleName();
            if (explicit != null) {
                return explicit;
            }
            return "Seven-segment display: "
                    + (character == ' ' ? "blank" : String.valueOf(character))
                    + (showDecimalPoint && decimalPointLit && character != ':'
                            ? "." : "");
        }
    }

    private Color litOrDisabled(Color lit) {
        if (isEnabled()) {
            return lit;
        }
        return new Color((lit.getRed() + unlitColor.getRed()) / 2,
                (lit.getGreen() + unlitColor.getGreen()) / 2,
                (lit.getBlue() + unlitColor.getBlue()) / 2);
    }

    private static int segmentMask(char ch) {
        switch (ch) {
            case '0': return 0x3f;
            case '1': return 0x06;
            case '2': return 0x5b;
            case '3': return 0x4f;
            case '4': return 0x66;
            case '5': return 0x6d;
            case '6': return 0x7d;
            case '7': return 0x07;
            case '8': return 0x7f;
            case '9': return 0x6f;
            case 'A': return 0x77;
            case 'B': return 0x7c;
            case 'C': return 0x39;
            case 'D': return 0x5e;
            case 'E': return 0x79;
            case 'F': return 0x71;
            case '-': return 0x40;
            case ' ': case ':': return 0;
            default: return -1;
        }
    }
}
