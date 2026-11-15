/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jdesktop.swingx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Rectangle;
import java.util.Arrays;
import javax.swing.SwingUtilities;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.junit.Test;

/** Real tree-table renderers with a small model and extreme legal row heights. */
public class JXTreeTableRendererBoundsTest {
    @Test
    public void saturatesRendererHeightWhenPositiveProductOverflows() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JXTreeTable table = tableWithThreeVisibleRows();
            table.setRowHeight(Integer.MAX_VALUE);
            Component renderer = renderer(table);
            renderer.setBounds(19, 23, 5, 7);
            assertEquals(Integer.MAX_VALUE, renderer.getHeight());
            assertEquals(0, renderer.getX());
            assertEquals(0, renderer.getY());
            assertEquals(table.getColumn(0).getWidth(), renderer.getWidth());
            assertSame(renderer, renderer(table));
            assertEquals(3, table.getRowCount());
        });
    }

    @Test
    public void saturatesImmediatelyAboveRepresentableHeight() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JXTreeTable table = tableWithThreeVisibleRows();
            table.setRowHeight(Integer.MAX_VALUE / 3 + 1);
            Component renderer = renderer(table);
            renderer.setBounds(0, 0, 5, 7);
            assertEquals(Integer.MAX_VALUE, renderer.getHeight());
        });
    }

    @Test
    public void preservesExactRepresentableExtentAndColumnWidth() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JXTreeTable table = tableWithThreeVisibleRows();
            Component renderer = renderer(table);
            for (int height : new int[] {1, 18, 1048576, Integer.MAX_VALUE / 3}) {
                table.setRowHeight(height);
                table.getColumn(0).setWidth(137);
                renderer.setBounds(19, 23, 5, 7);
                assertEquals(new Rectangle(0, 0, 137, height * 3), renderer.getBounds());
                assertEquals(height, table.getRowHeight());
                assertSame(renderer, renderer(table));
            }
        });
    }

    @Test
    public void recoversNormalGeometryAfterSaturation() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JXTreeTable table = tableWithThreeVisibleRows();
            Component renderer = renderer(table);
            table.setRowHeight(Integer.MAX_VALUE);
            renderer.setBounds(0, 0, 1, 1);
            assertEquals(Integer.MAX_VALUE, renderer.getHeight());
            table.setRowHeight(20);
            renderer.setBounds(0, 0, 1, 1);
            assertEquals(60, renderer.getHeight());
            table.collapseRow(0);
            renderer.setBounds(0, 0, 1, 1);
            assertEquals(20, renderer.getHeight());
            table.expandRow(0);
            renderer.setBounds(0, 0, 1, 1);
            assertEquals(60, renderer.getHeight());
        });
    }

    @Test
    public void emptyHiddenRootPreservesZeroHeight() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultMutableTreeTableNode root = new DefaultMutableTreeTableNode("root");
            JXTreeTable table = new JXTreeTable(new DefaultTreeTableModel(root, Arrays.asList("Tree")));
            table.setRootVisible(false);
            table.setRowHeight(Integer.MAX_VALUE);
            assertEquals(0, table.getRowCount());
            Component renderer = renderer(table);
            renderer.setBounds(1, 2, 3, 4);
            assertEquals(0, renderer.getHeight());
        });
    }

    @Test
    public void oneVisibleRowPreservesMaximumHeight() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JXTreeTable table = tableWithThreeVisibleRows();
            table.collapseRow(0);
            assertEquals(1, table.getRowCount());
            table.setRowHeight(Integer.MAX_VALUE);
            Component renderer = renderer(table);
            renderer.setBounds(1, 2, 3, 4);
            assertEquals(Integer.MAX_VALUE, renderer.getHeight());
        });
    }

    private static JXTreeTable tableWithThreeVisibleRows() {
        DefaultMutableTreeTableNode root = new DefaultMutableTreeTableNode("root");
        root.add(new DefaultMutableTreeTableNode("first"));
        root.add(new DefaultMutableTreeTableNode("second"));
        JXTreeTable table = new JXTreeTable(new DefaultTreeTableModel(root, Arrays.asList("Tree")));
        table.setRootVisible(true);
        table.expandRow(0);
        assertEquals(3, table.getRowCount());
        return table;
    }

    private static Component renderer(JXTreeTable table) {
        Object renderer = table.getCellRenderer(0, table.getHierarchicalColumn());
        assertTrue(renderer instanceof JXTree);
        return (Component) renderer;
    }
}
