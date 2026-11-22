/*
 * Copyright (c) 2026, Hitesh Soliwal and contributors. All rights reserved.
 * This library is free software under the GNU Lesser General Public License,
 * version 2.1 or (at your option) any later version. See LICENSE in SwingX.
 */
package org.jdesktop.swingx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleValue;
import javax.swing.BorderFactory;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import org.junit.Test;

/** Original EDT regression suite for StarRating; no SWT native control is loaded. */
public class JXStarRatingTest {
    private static void edt(Runnable action) throws Exception {
        SwingUtilities.invokeAndWait(action);
    }

    private static void invalid(Runnable action) {
        try { action.run(); fail("accepted invalid value"); }
        catch (IllegalArgumentException expected) { /* transactional refusal */ }
    }

    private static BufferedImage render(JXStarRating star, int width, int height) {
        star.setSize(width, height);
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try { star.paint(g); } finally { g.dispose(); }
        return img;
    }

    private static int differences(BufferedImage a, BufferedImage b) {
        int difference = 0;
        for (int y = 0; y < a.getHeight(); y++) {
            for (int x = 0; x < a.getWidth(); x++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) difference++;
            }
        }
        return difference;
    }

    private static void mouse(JXStarRating star, int id, int x, int y, int button) {
        MouseEvent event = new MouseEvent(star, id, 123, 0, x, y,
                1, false, button);
        star.dispatchEvent(event);
    }

    private static void key(JXStarRating star, String action) {
        star.getActionMap().get("star." + action)
                .actionPerformed(new ActionEvent(star, ActionEvent.ACTION_PERFORMED, action));
    }

    private static JXStarRating copy(JXStarRating source) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream stream = new ObjectOutputStream(bytes)) {
            stream.writeObject(source);
        }
        try (ObjectInputStream stream = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray()))) {
            return (JXStarRating) stream.readObject();
        }
    }

    @Test public void defaultsAndMaximumValidation() throws Exception {
        edt(() -> {
            JXStarRating star = new JXStarRating();
            assertEquals(5, star.getStarCount());
            assertEquals(0.0, star.getRating(), 0.0);
            assertTrue(star.isHalfSteps());
            assertEquals(SwingConstants.HORIZONTAL, star.getOrientation());
            assertTrue(star.isFocusable());
            assertFalse(star.isOpaque());
            assertEquals(136, star.getPreferredSize().width);
            assertEquals(24, star.getPreferredSize().height);
            invalid(() -> new JXStarRating(0));
            invalid(() -> new JXStarRating(257));
            invalid(() -> star.setStarCount(-1));
            invalid(() -> star.setStarCount(257));
            assertEquals(5, star.getStarCount());
        });
    }

    @Test public void canonicalModelChangeEventsAndNoDuplicates() throws Exception {
        edt(() -> {
            JXStarRating star = new JXStarRating(5);
            AtomicInteger changes = new AtomicInteger();
            AtomicInteger beanChanges = new AtomicInteger();
            star.addChangeListener(e -> changes.incrementAndGet());
            star.addPropertyChangeListener("rating", e -> beanChanges.incrementAndGet());
            star.setRating(1.5);
            assertEquals(1.5, star.getRating(), 0);
            star.setRating(1.5);
            assertEquals(1, changes.get());
            assertEquals(1, beanChanges.get());
            star.setRating(5.0);
            assertEquals(2, changes.get());
            star.setStarCount(2);
            assertEquals(2.0, star.getRating(), 0);
            assertEquals(3, changes.get());
            assertEquals(2, star.getStarCount());
            assertEquals(3, beanChanges.get());
            assertEquals(0, star.getChangeListeners().length - 1);
        });
    }

    @Test public void halfWholePrecisionAndTransactionalRefusal() throws Exception {
        edt(() -> {
            JXStarRating star = new JXStarRating(5);
            star.setRating(2.5);
            assertEquals(2.5, star.getRating(), 0);
            invalid(() -> star.setRating(2.25));
            invalid(() -> star.setRating(5.5));
            invalid(() -> star.setRating(Double.NaN));
            invalid(() -> star.setRating(Double.POSITIVE_INFINITY));
            invalid(() -> star.setRating(-1));
            assertEquals(2.5, star.getRating(), 0);
            star.setHalfSteps(false);
            assertEquals(2.0, star.getRating(), 0);
            invalid(() -> star.setRating(2.5));
            star.setRating(4);
            star.setHalfSteps(true);
            star.setRating(4.5);
            assertEquals(4.5, star.getRating(), 0);
        });
    }

    @Test public void geometryHorizontalVerticalAndBorder() throws Exception {
        edt(() -> {
            JXStarRating star = new JXStarRating(4);
            assertEquals(new Dimension(108, 24), star.getPreferredSize());
            star.setOrientation(SwingConstants.VERTICAL);
            assertEquals(new Dimension(24, 108), star.getPreferredSize());
            star.setStarSize(32);
            star.setStarGap(8);
            assertEquals(new Dimension(32, 152), star.getPreferredSize());
            star.setBorder(BorderFactory.createEmptyBorder(1, 2, 3, 4));
            assertEquals(new Dimension(38, 156), star.getPreferredSize());
            assertEquals(new Insets(1, 2, 3, 4), star.getInsets());
            invalid(() -> star.setStarSize(7));
            invalid(() -> star.setStarGap(-1));
            invalid(() -> star.setOrientation(99));
            assertEquals(SwingConstants.VERTICAL, star.getOrientation());
        });
    }

    @Test public void mouseHoverPreviewAndClickSelect() throws Exception {
        edt(() -> {
            JXStarRating star = new JXStarRating();
            star.setSize(star.getPreferredSize());
            mouse(star, MouseEvent.MOUSE_MOVED, 8, 10, MouseEvent.NOBUTTON);
            assertEquals(1, star.getHoverHalfUnits());
            assertEquals(0.0, star.getRating(), 0);
            mouse(star, MouseEvent.MOUSE_PRESSED, 35, 10, MouseEvent.BUTTON1);
            mouse(star, MouseEvent.MOUSE_RELEASED, 35, 10, MouseEvent.BUTTON1);
            assertEquals(1.5, star.getRating(), 0);
            mouse(star, MouseEvent.MOUSE_EXITED, -5, -5, MouseEvent.NOBUTTON);
            assertEquals(-1, star.getHoverHalfUnits());
            mouse(star, MouseEvent.MOUSE_PRESSED, 72, 10, MouseEvent.BUTTON1);
            mouse(star, MouseEvent.MOUSE_RELEASED, -10, 10, MouseEvent.BUTTON1);
            assertEquals(1.5, star.getRating(), 0);
            star.setEnabled(false);
            mouse(star, MouseEvent.MOUSE_PRESSED, 115, 10, MouseEvent.BUTTON1);
            mouse(star, MouseEvent.MOUSE_RELEASED, 115, 10, MouseEvent.BUTTON1);
            assertEquals(1.5, star.getRating(), 0);
        });
    }

    @Test public void rtlHitTestsAndVisualOrientation() throws Exception {
        edt(() -> {
            JXStarRating ltr = new JXStarRating(3);
            JXStarRating rtl = new JXStarRating(3);
            rtl.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            ltr.setRating(1.0);
            rtl.setRating(1.0);
            Dimension size = ltr.getPreferredSize();
            BufferedImage left = render(ltr, size.width, size.height);
            BufferedImage right = render(rtl, size.width, size.height);
            assertTrue(differences(left, right) > 10);
            mouse(rtl, MouseEvent.MOUSE_PRESSED, 72, 10, MouseEvent.BUTTON1);
            mouse(rtl, MouseEvent.MOUSE_RELEASED, 72, 10, MouseEvent.BUTTON1);
            assertEquals(0.5, rtl.getRating(), 0);
            mouse(rtl, MouseEvent.MOUSE_PRESSED, 8, 10, MouseEvent.BUTTON1);
            mouse(rtl, MouseEvent.MOUSE_RELEASED, 8, 10, MouseEvent.BUTTON1);
            assertEquals(3.0, rtl.getRating(), 0);
        });
    }

    @Test public void keyboardHorizontalVerticalAndRtl() throws Exception {
        edt(() -> {
            JXStarRating star = new JXStarRating(4);
            key(star, "right");
            assertEquals(0.5, star.getRating(), 0);
            key(star, "end");
            assertEquals(4.0, star.getRating(), 0);
            key(star, "right");
            assertEquals(4.0, star.getRating(), 0);
            key(star, "left");
            assertEquals(3.5, star.getRating(), 0);
            star.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            key(star, "left");
            assertEquals(4.0, star.getRating(), 0);
            star.setOrientation(SwingConstants.VERTICAL);
            key(star, "down");
            assertEquals(4.0, star.getRating(), 0); // last star, clamp
            key(star, "home");
            assertEquals(0.0, star.getRating(), 0);
            key(star, "up");
            assertEquals(0.0, star.getRating(), 0); // top star, clamp
            key(star, "down");
            assertEquals(0.5, star.getRating(), 0);
            star.setHalfSteps(false);
            key(star, "down");
            assertEquals(1.0, star.getRating(), 0);
            star.setEnabled(false);
            key(star, "end");
            assertEquals(1.0, star.getRating(), 0);
        });
    }

    @Test public void verticalNavigationMatchesPhysicalTopToBottomOrder() throws Exception {
        edt(() -> {
            JXStarRating star = new JXStarRating(5);
            star.setOrientation(SwingConstants.VERTICAL);
            star.setRating(2.0);
            key(star, "up");
            assertEquals(1.5, star.getRating(), 0);
            key(star, "down");
            assertEquals(2.0, star.getRating(), 0);
            star.setHalfSteps(false);
            key(star, "down");
            assertEquals(3.0, star.getRating(), 0);
            key(star, "up");
            assertEquals(2.0, star.getRating(), 0);
        });
    }

    @Test public void interStarGuttersDoNotChangeRatings() throws Exception {
        edt(() -> {
            JXStarRating star = new JXStarRating(4);
            star.setSize(star.getPreferredSize());
            star.setRating(1.5);
            // Default star width 24, gap 4. x=25 is in the first gutter.
            mouse(star, MouseEvent.MOUSE_MOVED, 25, 10, MouseEvent.NOBUTTON);
            assertEquals(-1, star.getHoverHalfUnits());
            mouse(star, MouseEvent.MOUSE_PRESSED, 25, 10, MouseEvent.BUTTON1);
            mouse(star, MouseEvent.MOUSE_RELEASED, 25, 10, MouseEvent.BUTTON1);
            assertEquals(1.5, star.getRating(), 0);
            star.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            mouse(star, MouseEvent.MOUSE_MOVED, 53, 10, MouseEvent.NOBUTTON);
            assertEquals(-1, star.getHoverHalfUnits());
            star.setOrientation(SwingConstants.VERTICAL);
            star.setSize(star.getPreferredSize());
            mouse(star, MouseEvent.MOUSE_PRESSED, 10, 25, MouseEvent.BUTTON1);
            mouse(star, MouseEvent.MOUSE_RELEASED, 10, 25, MouseEvent.BUTTON1);
            assertEquals(1.5, star.getRating(), 0);
        });
    }

    @Test public void verticalMouseAndBoundedImagePainting() throws Exception {
        edt(() -> {
            JXStarRating star = new JXStarRating(4);
            star.setOrientation(SwingConstants.VERTICAL);
            Dimension size = star.getPreferredSize();
            star.setSize(size);
            mouse(star, MouseEvent.MOUSE_PRESSED, 12, 45, MouseEvent.BUTTON1);
            mouse(star, MouseEvent.MOUSE_RELEASED, 12, 45, MouseEvent.BUTTON1);
            assertEquals(2.0, star.getRating(), 0);
            BufferedImage image = render(star, size.width, size.height);
            assertNotNull(image);
            assertTrue(image.getWidth() < 150);
            assertTrue(image.getHeight() < 150);
        });
    }

    @Test public void halfStarPaintingMatchesPhysicalVerticalAndRtlAxes() throws Exception {
        edt(() -> {
            for (int axis : new int[] {SwingConstants.VERTICAL, SwingConstants.HORIZONTAL}) {
                for (ComponentOrientation direction : new ComponentOrientation[] {
                        ComponentOrientation.LEFT_TO_RIGHT, ComponentOrientation.RIGHT_TO_LEFT}) {
                    JXStarRating star = new JXStarRating(1);
                    star.setOrientation(axis);
                    star.setComponentOrientation(direction);
                    star.setStarSize(80);
                    star.setSelectedColor(Color.RED);
                    star.setEmptyColor(Color.BLUE);
                    star.setRating(0.5);
                    Dimension size = star.getPreferredSize();
                    BufferedImage pixels = render(star, size.width, size.height);
                    int first = 0;
                    int second = 0;
                    for (int y = 0; y < size.height; y++) {
                        for (int x = 0; x < size.width; x++) {
                            int value = pixels.getRGB(x, y);
                            int red = (value >>> 16) & 255;
                            int green = (value >>> 8) & 255;
                            int blue = value & 255;
                            if (red > 180 && green < 80 && blue < 80) {
                                int position = axis == SwingConstants.VERTICAL ? y : x;
                                if (position < 40) first++;
                                else second++;
                            }
                        }
                    }
                    boolean firstSelected = axis == SwingConstants.VERTICAL
                            || direction.isLeftToRight();
                    if (firstSelected) {
                        assertTrue("vertical top / LTR left half must be illuminated", first > 150);
                        assertEquals(0, second);
                    } else {
                        assertEquals(0, first);
                        assertTrue("RTL right half must be illuminated", second > 150);
                    }
                }
            }
        });
    }

    @Test public void accessibleValueEventsReportOldAndNewNumericRatings() throws Exception {
        final JXStarRating[] saved = new JXStarRating[1];
        edt(() -> {
            JXStarRating star = new JXStarRating(5);
            ArrayList<PropertyChangeEvent> values = new ArrayList<>();
            star.getAccessibleContext().addPropertyChangeListener(event -> {
                if (AccessibleContext.ACCESSIBLE_VALUE_PROPERTY.equals(event.getPropertyName())) {
                    values.add(event);
                }
            });
            star.setRating(1.0);
            star.setRating(1.5);
            star.setRating(1.5);
            star.setStarCount(6); // max changes, selected value does not
            assertEquals(2, values.size());
            assertEquals(Double.valueOf(0.0), values.get(0).getOldValue());
            assertEquals(Double.valueOf(1.0), values.get(0).getNewValue());
            assertEquals(Double.valueOf(1.0), values.get(1).getOldValue());
            assertEquals(Double.valueOf(1.5), values.get(1).getNewValue());
            star.setHalfSteps(false); // rounds 1.5 down to 1.0
            assertEquals(3, values.size());
            assertEquals(Double.valueOf(1.5), values.get(2).getOldValue());
            assertEquals(Double.valueOf(1.0), values.get(2).getNewValue());
            saved[0] = star;
        });
        JXStarRating copy = copy(saved[0]);
        edt(() -> {
            ArrayList<PropertyChangeEvent> values = new ArrayList<>();
            copy.getAccessibleContext().addPropertyChangeListener(event -> {
                if (AccessibleContext.ACCESSIBLE_VALUE_PROPERTY.equals(event.getPropertyName())) {
                    values.add(event);
                }
            });
            copy.setRating(2.0);
            assertEquals(1, values.size());
            assertEquals(Double.valueOf(1.0), values.get(0).getOldValue());
            assertEquals(Double.valueOf(2.0), values.get(0).getNewValue());
        });
    }

    @Test public void colorChangesClipAndDisabledRendering() throws Exception {
        edt(() -> {
            JXStarRating star = new JXStarRating(4);
            Dimension d = star.getPreferredSize();
            star.setRating(2.5);
            BufferedImage a = render(star, d.width, d.height);
            AtomicInteger colors = new AtomicInteger();
            star.addPropertyChangeListener("selectedColor", e -> colors.incrementAndGet());
            star.setSelectedColor(Color.BLUE);
            star.setSelectedColor(Color.BLUE);
            assertEquals(1, colors.get());
            BufferedImage b = render(star, d.width, d.height);
            assertTrue(differences(a,b) > 5);
            star.setEnabled(false);
            BufferedImage c = render(star, d.width, d.height);
            assertTrue(differences(b,c) > 5);
            try { star.setEmptyColor(null); fail("null color accepted"); }
            catch (NullPointerException ok) { /* expected */ }
        });
    }

    @Test public void accessibleRoleAndNumericValue() throws Exception {
        edt(() -> {
            JXStarRating star = new JXStarRating(5);
            AccessibleContext context = star.getAccessibleContext();
            assertEquals(AccessibleRole.SLIDER, context.getAccessibleRole());
            AccessibleValue accessible = context.getAccessibleValue();
            assertNotNull(accessible);
            assertEquals(0.0, accessible.getCurrentAccessibleValue().doubleValue(), 0);
            assertTrue(accessible.setCurrentAccessibleValue(Double.valueOf(3.5)));
            assertEquals(3.5, star.getRating(), 0);
            assertFalse(accessible.setCurrentAccessibleValue(Double.valueOf(3.25)));
            assertFalse(accessible.setCurrentAccessibleValue(null));
            assertEquals(5.0, accessible.getMaximumAccessibleValue().doubleValue(), 0);
            star.setEnabled(false);
            assertFalse(accessible.setCurrentAccessibleValue(Double.valueOf(4.0)));
        });
    }

    @Test public void serialRoundtripKeepsStateAndNewEdtEvents() throws Exception {
        final JXStarRating[] saved = new JXStarRating[1];
        edt(() -> {
            JXStarRating star = new JXStarRating(6);
            star.setRating(4.5);
            star.setOrientation(SwingConstants.VERTICAL);
            star.setSelectedColor(Color.ORANGE);
            saved[0] = star;
        });
        JXStarRating restored = copy(saved[0]);
        edt(() -> {
            assertEquals(6, restored.getStarCount());
            assertEquals(4.5, restored.getRating(), 0);
            assertEquals(SwingConstants.VERTICAL, restored.getOrientation());
            assertEquals(Color.ORANGE, restored.getSelectedColor());
            AtomicInteger changes = new AtomicInteger();
            restored.addChangeListener(e -> changes.incrementAndGet());
            restored.setRating(5.0);
            assertEquals(1, changes.get());
            assertEquals(5.0, restored.getRating(), 0);
            assertEquals(-1, restored.getHoverHalfUnits());
        });
    }

    @Test public void metalAndNimbusBothPaintStars() throws Exception {
        edt(() -> {
            LookAndFeel prior = UIManager.getLookAndFeel();
            try {
                for (String laf : new String[] {"javax.swing.plaf.metal.MetalLookAndFeel",
                        "javax.swing.plaf.nimbus.NimbusLookAndFeel"}) {
                    try { UIManager.setLookAndFeel(laf); }
                    catch (Exception unavailable) { throw new AssertionError(laf, unavailable); }
                    JXStarRating star = new JXStarRating();
                    star.setRating(3.0);
                    Dimension d = star.getPreferredSize();
                    BufferedImage image = render(star, d.width, d.height);
                    boolean any = false;
                    for (int y = 0; y < image.getHeight(); y++) {
                        for (int x = 0; x < image.getWidth(); x++) {
                            any |= image.getRGB(x,y) != 0;
                        }
                    }
                    assertTrue(laf + " produced nonblank stars", any);
                }
            } finally {
                try { UIManager.setLookAndFeel(prior); }
                catch (Exception failure) { throw new AssertionError(failure); }
            }
        });
    }
}
