/*
 * Copyright 2026 SwingX contributors.
 * Licensed under the GNU Lesser General Public License, version 2.1 or later.
 */
package org.jdesktop.swingx;

import static org.junit.Assert.*;

import java.awt.Cursor;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.event.MouseInputAdapter;

import org.jdesktop.test.EDTRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/** The editable property must not multiply image drags or retain disabled input. */
@RunWith(EDTRunner.class)
public class JXImageViewEditableTest {
    @Test
    public void repeatedEnableExportsOnlyOncePerDrag() {
        JXImageView view = new JXImageView();
        for (int i = 0; i < 4; i++) {
            view.setEditable(true);
        }
        AtomicInteger exports = new AtomicInteger();
        view.setTransferHandler(new TransferHandler() {
            @Override
            public void exportAsDrag(JComponent component, InputEvent event, int action) {
                assertSame(view, component);
                assertEquals(TransferHandler.COPY, action);
                exports.incrementAndGet();
            }
        });
        view.setDragEnabled(true);
        drag(view);
        assertEquals(1, exports.get());
    }

    @Test
    public void disableAfterRepeatedEnableStopsImageMovement() {
        JXImageView view = new JXImageView();
        view.setImage(new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB));
        view.setImageLocation(new Point2D.Double(10, 20));
        view.setEditable(true);
        view.setEditable(true);
        view.setEditable(false);
        drag(view);
        assertEquals(new Point2D.Double(10, 20), view.getImageLocation());
        assertNull(view.getTransferHandler());
        assertEquals(Cursor.DEFAULT_CURSOR, view.getCursor().getType());
    }

    @Test
    public void ordinaryDraggingAndClientListenersSurviveToggle() {
        JXImageView view = new JXImageView();
        view.setImageLocation(new Point2D.Double(10, 20));
        AtomicInteger clientDrags = new AtomicInteger();
        view.addMouseMotionListener(new MouseInputAdapter() {
            @Override
            public void mouseDragged(MouseEvent event) {
                clientDrags.incrementAndGet();
            }
        });
        AtomicInteger editableEvents = new AtomicInteger();
        view.addPropertyChangeListener("editable", event -> editableEvents.incrementAndGet());
        view.setEditable(false);
        view.setEditable(true);
        view.setEditable(true);
        drag(view);
        assertEquals(new Point2D.Double(22, 26), view.getImageLocation());
        assertEquals(1, clientDrags.get());
        assertEquals(2, editableEvents.get());
        assertEquals(Cursor.HAND_CURSOR, view.getCursor().getType());
        assertNotNull(view.getTransferHandler());
    }

    private static void drag(JXImageView view) {
        view.dispatchEvent(new MouseEvent(view, MouseEvent.MOUSE_PRESSED, 1L,
                InputEvent.BUTTON1_DOWN_MASK, 2, 3, 1, false, MouseEvent.BUTTON1));
        view.dispatchEvent(new MouseEvent(view, MouseEvent.MOUSE_DRAGGED, 2L,
                InputEvent.BUTTON1_DOWN_MASK, 14, 9, 0, false, MouseEvent.NOBUTTON));
        view.dispatchEvent(new MouseEvent(view, MouseEvent.MOUSE_RELEASED, 3L,
                0, 14, 9, 1, false, MouseEvent.BUTTON1));
    }
}
