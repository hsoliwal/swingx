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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Objects;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleValue;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A native Swing rating control for a bounded number of stars.
 * <p>The canonical selection owner is one {@link BoundedRangeModel} counting
 * half-stars, rather than a per-star widget/object graph. The display supports
 * whole and half-star steps, horizontal and vertical layouts, RTL painting,
 * mouse hover preview, arrow/Home/End keys, and a Swing accessible slider
 * value. The component has no SWT event loop, native peer or own timer: its
 * window integration and input events are ordinary Swing/AWT facilities.
 * <p>Java2D renders the small, clipped star glyphs; a JNI call per star would
 * add work without an established bulk/native performance benefit.
 * <p>Like all Swing components, read and mutate it on the EDT.
 */
public class JXStarRating extends JComponent {
    private static final long serialVersionUID = 1L;
    private static final int MAX_STARS = 256;
    private static final int MIN_STAR_SIZE = 8;
    private static final int MAX_STAR_SIZE = 128;

    private final DefaultBoundedRangeModel selection;
    private int starCount;
    private boolean halfSteps = true;
    private int orientation = SwingConstants.HORIZONTAL;
    private int starSize = 24;
    private int gap = 4;
    private Color selectedColor = new Color(245, 181, 42);
    private Color emptyColor = new Color(164, 164, 164);
    private Color previewColor = new Color(255, 207, 86);
    // -1 means no hover; this is presentation, not a second selection model.
    private transient int hoveredHalves = -1;
    private transient boolean mouseArmed;
    private transient ChangeEvent cachedChangeEvent;
    private transient ChangeListener selectionListener;
    // Value at the last canonical model notification; accessibility needs
    // both the previous and the new numeric rating, not a null old value.
    private transient int lastReportedHalfUnits;

    /** Five-star rating with half-star precision and zero initial value. */
    public JXStarRating() {
        this(5);
    }

    /** Creates a rating control with 1 through 256 stars. */
    @SuppressWarnings("this-escape") // Ordinary Swing constructor LAF/focus initialization.
    public JXStarRating(int stars) {
        requireStarCount(stars);
        starCount = stars;
        selection = new DefaultBoundedRangeModel(0, 0, 0, stars * 2);
        installSelectionListener();
        setOpaque(false);
        setFocusable(true);
        installInteractions();
    }

    private static void requireStarCount(int stars) {
        if (stars < 1 || stars > MAX_STARS) {
            throw new IllegalArgumentException("stars must be in 1.." + MAX_STARS);
        }
    }

    private void installSelectionListener() {
        lastReportedHalfUnits = selection.getValue();
        selectionListener = event -> {
            int oldHalfUnits = lastReportedHalfUnits;
            int newHalfUnits = selection.getValue();
            lastReportedHalfUnits = newHalfUnits;
            // Model events are forwarded without synthesizing ActionEvents.
            fireStateChanged();
            if (accessibleContext != null && oldHalfUnits != newHalfUnits) {
                accessibleContext.firePropertyChange(
                        AccessibleContext.ACCESSIBLE_VALUE_PROPERTY,
                        Double.valueOf(oldHalfUnits / 2.0),
                        Double.valueOf(newHalfUnits / 2.0));
            }
            repaint();
        };
        selection.addChangeListener(selectionListener);
    }

    private void readObject(ObjectInputStream input)
            throws IOException, ClassNotFoundException {
        input.defaultReadObject();
        if (selection == null || starCount < 1 || starCount > MAX_STARS
                || selection.getMinimum() != 0 || selection.getMaximum() != starCount * 2
                || selection.getExtent() != 0 || selection.getValue() < 0
                || selection.getValue() > starCount * 2 || (!halfSteps && (selection.getValue() & 1) != 0)
                || (orientation != SwingConstants.HORIZONTAL && orientation != SwingConstants.VERTICAL)
                || starSize < MIN_STAR_SIZE || starSize > MAX_STAR_SIZE || gap < 0 || gap > 64
                || selectedColor == null || emptyColor == null || previewColor == null) {
            throw new java.io.InvalidObjectException("invalid serialized star-rating state");
        }
        hoveredHalves = -1;
        mouseArmed = false;
        cachedChangeEvent = null;
        installSelectionListener();
        installInteractions();
    }

    /** Gets the selected rating, in 0.5 increments. */
    public double getRating() {
        return selection.getValue() / 2.0;
    }

    /**
     * Updates the rating without posting ActionEvents. Rejects unsupported
     * fractional steps before mutating any model state.
     */
    public void setRating(double rating) {
        if (!Double.isFinite(rating) || rating < 0 || rating > starCount
                || (halfSteps ? rating * 2 != Math.rint(rating * 2)
                              : rating != Math.rint(rating))) {
            throw new IllegalArgumentException("rating must be representable within current steps");
        }
        int next = (int) Math.round(rating * 2);
        int old = selection.getValue();
        if (old != next) {
            selection.setValue(next);
            firePropertyChange("rating", old / 2.0, next / 2.0);
        }
    }

    public int getStarCount() { return starCount; }

    /** Changes maximum stars and clamps the current rating if necessary. */
    public void setStarCount(int stars) {
        requireStarCount(stars);
        if (starCount == stars) return;
        int oldCount = starCount;
        double oldRating = getRating();
        starCount = stars;
        selection.setRangeProperties(Math.min(selection.getValue(), stars * 2),
                0, 0, stars * 2, false);
        hoveredHalves = -1;
        firePropertyChange("starCount", oldCount, stars);
        if (getRating() != oldRating) {
            firePropertyChange("rating", oldRating, getRating());
        }
        revalidate();
        repaint();
    }

    public boolean isHalfSteps() { return halfSteps; }

    /** Whole-only mode rounds an existing half selection downward. */
    public void setHalfSteps(boolean enabled) {
        if (halfSteps == enabled) return;
        boolean old = halfSteps;
        double previous = getRating();
        halfSteps = enabled;
        if (!enabled && (selection.getValue() & 1) != 0) {
            selection.setValue(selection.getValue() - 1);
        }
        hoveredHalves = -1;
        firePropertyChange("halfSteps", old, enabled);
        if (previous != getRating()) {
            firePropertyChange("rating", previous, getRating());
        }
        repaint();
    }

    public int getOrientation() { return orientation; }

    public void setOrientation(int next) {
        if (next != SwingConstants.HORIZONTAL && next != SwingConstants.VERTICAL) {
            throw new IllegalArgumentException("orientation must be horizontal or vertical");
        }
        if (orientation == next) return;
        int old = orientation;
        orientation = next;
        hoveredHalves = -1;
        firePropertyChange("orientation", old, next);
        revalidate();
        repaint();
    }

    public int getStarSize() { return starSize; }
    public void setStarSize(int pixels) {
        if (pixels < MIN_STAR_SIZE || pixels > MAX_STAR_SIZE) {
            throw new IllegalArgumentException("starSize must be in 8..128");
        }
        if (pixels != starSize) {
            int old = starSize;
            starSize = pixels;
            firePropertyChange("starSize", old, pixels);
            revalidate();
            repaint();
        }
    }

    public int getStarGap() { return gap; }
    public void setStarGap(int pixels) {
        if (pixels < 0 || pixels > 64) throw new IllegalArgumentException("starGap must be in 0..64");
        if (pixels != gap) {
            int old = gap;
            gap = pixels;
            firePropertyChange("starGap", old, pixels);
            revalidate();
            repaint();
        }
    }

    public Color getSelectedColor() { return selectedColor; }
    public void setSelectedColor(Color next) {
        Color newColor = Objects.requireNonNull(next, "selectedColor");
        if (!selectedColor.equals(newColor)) {
            Color old = selectedColor;
            selectedColor = newColor;
            firePropertyChange("selectedColor", old, newColor);
            repaint();
        }
    }

    public Color getEmptyColor() { return emptyColor; }
    public void setEmptyColor(Color next) {
        Color newColor = Objects.requireNonNull(next, "emptyColor");
        if (!emptyColor.equals(newColor)) {
            Color old = emptyColor;
            emptyColor = newColor;
            firePropertyChange("emptyColor", old, newColor);
            repaint();
        }
    }

    public Color getPreviewColor() { return previewColor; }
    public void setPreviewColor(Color next) {
        Color newColor = Objects.requireNonNull(next, "previewColor");
        if (!previewColor.equals(newColor)) {
            Color old = previewColor;
            previewColor = newColor;
            firePropertyChange("previewColor", old, newColor);
            repaint();
        }
    }

    /** Returns current pointer preview in half units, or -1 outside the stars. */
    public int getHoverHalfUnits() { return hoveredHalves; }

    public void addChangeListener(ChangeListener listener) {
        listenerList.add(ChangeListener.class, listener);
    }
    public void removeChangeListener(ChangeListener listener) {
        listenerList.remove(ChangeListener.class, listener);
    }
    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }
    protected void fireStateChanged() {
        for (ChangeListener listener : getChangeListeners()) {
            if (cachedChangeEvent == null) cachedChangeEvent = new ChangeEvent(this);
            listener.stateChanged(cachedChangeEvent);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) return super.getPreferredSize();
        Insets insets = getInsets();
        int length = starCount * starSize + (starCount - 1) * gap;
        int w = orientation == SwingConstants.HORIZONTAL ? length : starSize;
        int h = orientation == SwingConstants.HORIZONTAL ? starSize : length;
        return new Dimension(w + insets.left + insets.right,
                h + insets.top + insets.bottom);
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (graphics.getClipBounds() == null) return;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            Insets insets = getInsets();
            int delta = starSize + gap;
            boolean rtl = orientation == SwingConstants.HORIZONTAL
                    && !getComponentOrientation().isLeftToRight();
            int active = hoveredHalves >= 0 && isEnabled() ? hoveredHalves : selection.getValue();
            Color fill = hoveredHalves >= 0 && isEnabled() ? previewColor : selectedColor;
            Path2D star = starShape(starSize);
            for (int i = 0; i < starCount; i++) {
                int slot = rtl ? starCount - i - 1 : i;
                int x = insets.left + (orientation == SwingConstants.HORIZONTAL ? slot * delta : 0);
                int y = insets.top + (orientation == SwingConstants.VERTICAL ? i * delta : 0);
                if (!g.getClipBounds().intersects(x, y, starSize, starSize)) continue;
                Graphics2D cell = (Graphics2D) g.create(x, y, starSize, starSize);
                try {
                    cell.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    cell.setColor(emptyColor);
                    cell.setStroke(new BasicStroke(Math.max(1f, starSize / 18f)));
                    cell.draw(star);
                    // Fill the entire silhouette, then reveal selected prefix.
                    // An explicit cell clip means a half-star uses no retained
                    // per-star image or mutable 2D state.
                    int selected = Math.min(2, Math.max(0, active - i * 2));
                    if (selected > 0) {
                        if (selected == 1) {
                            // Half selection follows the physical input axis:
                            // top half in a vertical stack, leading half in
                            // horizontal LTR, trailing (right) half in RTL.
                            if (orientation == SwingConstants.VERTICAL) {
                                cell.clipRect(0, 0, starSize, (starSize + 1) / 2);
                            } else if (rtl) {
                                cell.clipRect(starSize / 2, 0,
                                        starSize - starSize / 2, starSize);
                            } else {
                                cell.clipRect(0, 0, (starSize + 1) / 2, starSize);
                            }
                        }
                        cell.setColor(isEnabled() ? fill : emptyColor);
                        cell.fill(star);
                    }
                } finally {
                    cell.dispose();
                }
            }
            if (hasFocus()) {
                g.setColor(getForeground() == null ? Color.DARK_GRAY : getForeground());
                int w = getWidth() - insets.left - insets.right;
                int h = getHeight() - insets.top - insets.bottom;
                g.drawRect(insets.left, insets.top, Math.max(0, w - 1), Math.max(0, h - 1));
            }
        } finally {
            g.dispose();
        }
    }

    private static Path2D starShape(int pixels) {
        Path2D.Double path = new Path2D.Double();
        double center = (pixels - 1) / 2.0;
        double outer = pixels * 0.46;
        double inner = outer * 0.47;
        for (int point = 0; point < 10; point++) {
            double angle = Math.PI * (point / 5.0) - Math.PI / 2;
            double radius = (point & 1) == 0 ? outer : inner;
            double x = center + Math.cos(angle) * radius;
            double y = center + Math.sin(angle) * radius;
            if (point == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        path.closePath();
        return path;
    }

    private int halfUnitsAt(int x, int y) {
        Insets insets = getInsets();
        int along = orientation == SwingConstants.HORIZONTAL ? x - insets.left : y - insets.top;
        int cross = orientation == SwingConstants.HORIZONTAL ? y - insets.top : x - insets.left;
        if (cross < 0 || cross >= starSize || along < 0) return -1;
        int length = starCount * starSize + (starCount - 1) * gap;
        if (along >= length) return -1;
        int index = along / (starSize + gap);
        int inCell = along % (starSize + gap);
        // The inter-star gutter is not part of either hit target. SWT's
        // source Star.bounds behaves the same way: a gap click is inert.
        if (inCell >= starSize) return -1;
        boolean rtl = orientation == SwingConstants.HORIZONTAL
                && !getComponentOrientation().isLeftToRight();
        int logical = rtl ? starCount - index - 1 : index;
        int half = halfSteps && (rtl ? inCell >= starSize / 2 : inCell < (starSize + 1) / 2) ? 1 : 2;
        return logical * 2 + half;
    }

    private void installInteractions() {
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent event) { preview(event); }
            @Override public void mouseEntered(MouseEvent event) { preview(event); }
            @Override public void mouseDragged(MouseEvent event) { preview(event); }
            @Override public void mouseExited(MouseEvent event) { setHover(-1); }
            @Override public void mousePressed(MouseEvent event) {
                mouseArmed = isEnabled() && event.getButton() == MouseEvent.BUTTON1
                        && halfUnitsAt(event.getX(), event.getY()) > 0;
                if (mouseArmed) requestFocusInWindow();
            }
            @Override public void mouseReleased(MouseEvent event) {
                if (mouseArmed && isEnabled() && event.getButton() == MouseEvent.BUTTON1) {
                    int next = halfUnitsAt(event.getX(), event.getY());
                    if (next > 0) setRating(next / 2.0);
                }
                mouseArmed = false;
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        InputMap inputs = getInputMap(WHEN_FOCUSED);
        ActionMap actions = getActionMap();
        for (String action : new String[] {"left", "right", "up", "down", "home", "end"}) {
            actions.put("star." + action, new AbstractAction() {
                private static final long serialVersionUID = 1L;
                @Override public void actionPerformed(ActionEvent event) {
                    if (JXStarRating.this.isEnabled()) keyboard(action);
                }
            });
        }
        inputs.put(KeyStroke.getKeyStroke("LEFT"), "star.left");
        inputs.put(KeyStroke.getKeyStroke("RIGHT"), "star.right");
        inputs.put(KeyStroke.getKeyStroke("UP"), "star.up");
        inputs.put(KeyStroke.getKeyStroke("DOWN"), "star.down");
        inputs.put(KeyStroke.getKeyStroke("HOME"), "star.home");
        inputs.put(KeyStroke.getKeyStroke("END"), "star.end");
    }

    private void preview(MouseEvent e) {
        if (isEnabled()) setHover(halfUnitsAt(e.getX(), e.getY()));
    }

    private void setHover(int next) {
        if (hoveredHalves != next) {
            hoveredHalves = next;
            repaint();
        }
    }

    private void keyboard(String action) {
        int delta = halfSteps ? 1 : 2;
        int direction;
        if ("home".equals(action)) { setRating(0); return; }
        if ("end".equals(action)) { setRating(starCount); return; }
        if (orientation == SwingConstants.VERTICAL) {
            // The first (minimum) star is painted at the top; moving down
            // increases the rating and moving up decreases it.
            if ("up".equals(action)) direction = -1;
            else if ("down".equals(action)) direction = 1;
            else return;
        } else {
            boolean rtl = !getComponentOrientation().isLeftToRight();
            if ("right".equals(action)) direction = rtl ? -1 : 1;
            else if ("left".equals(action)) direction = rtl ? 1 : -1;
            else return;
        }
        int target = Math.max(0, Math.min(starCount * 2, selection.getValue() + direction * delta));
        if (!halfSteps && (target & 1) != 0) target--;
        setRating(target / 2.0);
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) accessibleContext = new AccessibleJXStarRating();
        return accessibleContext;
    }

    protected class AccessibleJXStarRating extends AccessibleJComponent implements AccessibleValue {
        private static final long serialVersionUID = 1L;
        @Override public AccessibleRole getAccessibleRole() { return AccessibleRole.SLIDER; }
        @Override public AccessibleValue getAccessibleValue() { return this; }
        @Override public Number getCurrentAccessibleValue() { return Double.valueOf(getRating()); }
        @Override public boolean setCurrentAccessibleValue(Number value) {
            if (value == null || !isEnabled()) return false;
            try { setRating(value.doubleValue()); return true; }
            catch (IllegalArgumentException ex) { return false; }
        }
        @Override public Number getMinimumAccessibleValue() { return Double.valueOf(0); }
        @Override public Number getMaximumAccessibleValue() { return Double.valueOf(starCount); }
    }
}
