/*
 * Copyright 2026 SwingX contributors.
 * Licensed under the GNU Lesser General Public License, version 2.1 or later.
 */
package org.jdesktop.swingx;

import static org.junit.Assert.*;

import java.awt.Dimension;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.table.AbstractTableModel;

import org.jdesktop.swingx.table.ColumnFactory;
import org.jdesktop.swingx.table.TableColumnExt;
import org.jdesktop.test.EDTRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Regression tests for the original table's scrollable size contract. */
@RunWith(EDTRunner.class)
public class JXTableViewportSizeTest {
    @Test
    public void largeLogicalModelDoesNotMaterializeRowsForViewportSize() {
        JXTable table = new JXTable(new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return Integer.MAX_VALUE;
            }

            @Override
            public int getColumnCount() {
                return 1;
            }

            @Override
            public Object getValueAt(int row, int column) {
                throw new AssertionError("viewport sizing must not fetch logical row " + row);
            }
        });
        table.setRowHeight(19);
        table.setVisibleRowCount(12);
        assertEquals(Integer.MAX_VALUE, table.getRowCount());
        assertEquals(228, table.getPreferredScrollableViewportSize().height);
        table.setVisibleRowCount(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, table.getPreferredScrollableViewportSize().height);
    }

    @Test
    public void largeRowCountsDoNotWrapHeight() {
        JXTable table = new JXTable(0, 1);
        table.setRowHeight(2);
        table.setVisibleRowCount(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, table.getPreferredScrollableViewportSize().height);
        table.setVisibleRowCount(0);
        assertEquals(0, table.getPreferredScrollableViewportSize().height);
        table.setVisibleRowCount(20);
        assertEquals(40, table.getPreferredScrollableViewportSize().height);
    }

    @Test
    public void largeRowHeightsDoNotWrapHeight() {
        JXTable table = new JXTable(0, 1);
        table.setRowHeight(Integer.MAX_VALUE);
        table.setVisibleRowCount(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, table.getPreferredScrollableViewportSize().height);
        table.setVisibleRowCount(1);
        assertEquals(Integer.MAX_VALUE, table.getPreferredScrollableViewportSize().height);
    }

    @Test
    public void rowHeightChangeInvalidatesOnlyHeightBeforeNotification() {
        JXTable table = new JXTable(0, 1);
        table.setRowHeight(12);
        table.setVisibleRowCount(7);
        Dimension before = table.getPreferredScrollableViewportSize();
        table.getColumn(0).setPreferredWidth(before.width + 100);
        AtomicInteger events = new AtomicInteger();
        table.addPropertyChangeListener("rowHeight", event -> {
            events.incrementAndGet();
            assertEquals(147, table.getPreferredScrollableViewportSize().height);
            assertEquals(before.width, table.getPreferredScrollableViewportSize().width);
        });
        table.setRowHeight(21);
        assertEquals(1, events.get());
        assertEquals(147, table.getPreferredScrollableViewportSize().height);
    }

    @Test
    public void explicitSizeStillWinsAndIsDefensivelyCopied() {
        JXTable table = new JXTable(0, 1);
        table.setPreferredScrollableViewportSize(new Dimension(321, 123));
        table.setRowHeight(Integer.MAX_VALUE);
        table.setVisibleRowCount(Integer.MAX_VALUE);
        table.setVisibleColumnCount(Integer.MAX_VALUE);
        Dimension result = table.getPreferredScrollableViewportSize();
        assertEquals(new Dimension(321, 123), result);
        result.setSize(0, 0);
        assertEquals(new Dimension(321, 123), table.getPreferredScrollableViewportSize());
    }

    @Test
    public void wideColumnsDoNotWrapWidth() {
        JXTable table = new JXTable(0, 2);
        table.getColumn(0).setPreferredWidth(Integer.MAX_VALUE);
        table.getColumn(1).setPreferredWidth(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, table.getPreferredScrollableViewportSize().width);
    }

    @Test
    public void missingColumnsDoNotWrapWidth() {
        JXTable table = new JXTable(0, 0);
        table.setVisibleColumnCount(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, table.getPreferredScrollableViewportSize().width);
        table.setVisibleColumnCount(3);
        assertEquals(225, table.getPreferredScrollableViewportSize().width);
        table.setVisibleColumnCount(0);
        assertEquals(0, table.getPreferredScrollableViewportSize().width);
    }

    @Test
    public void hiddenAndReorderedColumnsKeepViewSemantics() {
        JXTable table = new JXTable(0, 3);
        table.getColumnExt(0).setPreferredWidth(80);
        table.getColumnExt(1).setPreferredWidth(120);
        TableColumnExt hidden = table.getColumnExt(2);
        hidden.setPreferredWidth(Integer.MAX_VALUE);
        hidden.setVisible(false);
        table.getColumnModel().moveColumn(1, 0);
        table.setVisibleColumnCount(1);
        assertEquals(120, table.getPreferredScrollableViewportSize().width);
        table.setVisibleColumnCount(4);
        assertEquals(350, table.getPreferredScrollableViewportSize().width);
    }

    @Test
    public void customColumnFactoryRemainsTheWidthOwner() {
        AtomicInteger calls = new AtomicInteger();
        JXTable table = new JXTable(0, 1);
        table.setColumnFactory(new ColumnFactory() {
            @Override
            public int getPreferredScrollableViewportWidth(JXTable ignored) {
                calls.incrementAndGet();
                return 1234;
            }
        });
        table.setVisibleColumnCount(Integer.MAX_VALUE);
        assertEquals(1234, table.getPreferredScrollableViewportSize().width);
        table.setRowHeight(35);
        assertEquals(1234, table.getPreferredScrollableViewportSize().width);
        assertEquals(1, calls.get());
    }

    @Test
    public void normalGeometryAndVisibleCountEventsRemainUnchanged() {
        JXTable table = new JXTable(0, 2);
        table.setRowHeight(17);
        AtomicInteger events = new AtomicInteger();
        table.addPropertyChangeListener("visibleRowCount", event -> events.incrementAndGet());
        table.setVisibleRowCount(11);
        table.setVisibleRowCount(11);
        assertEquals(1, events.get());
        assertEquals(187, table.getPreferredScrollableViewportSize().height);
        try {
            table.setVisibleRowCount(-1);
            fail("negative visible row count must still be rejected");
        } catch (IllegalArgumentException expected) {
            assertEquals(11, table.getVisibleRowCount());
        }
    }
}
