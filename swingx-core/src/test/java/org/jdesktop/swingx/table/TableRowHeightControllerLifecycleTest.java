/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * Licensed under the GNU Lesser General Public License, version 2.1 or later.
 */
package org.jdesktop.swingx.table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Checks ownership of deferred row-height work across model and table changes. */
public class TableRowHeightControllerLifecycleTest {
    private final List<Throwable> failures =
            Collections.synchronizedList(new ArrayList<Throwable>());
    private final List<InspectableController> controllers = new ArrayList<>();
    private Thread.UncaughtExceptionHandler previousHandler;

    @Before
    public void captureEventDispatchFailures() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            previousHandler = Thread.currentThread().getUncaughtExceptionHandler();
            Thread.currentThread().setUncaughtExceptionHandler((thread, failure) -> failures.add(failure));
        });
    }

    @After
    public void releaseControllersAndRestoreHandler() throws Exception {
        try {
            SwingUtilities.invokeAndWait(() -> {
                for (InspectableController controller : controllers) {
                    controller.release();
                }
            });
            drainEvents();
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                Thread.currentThread().setUncaughtExceptionHandler(previousHandler);
            });
        }
    }

    @Test
    public void rapidModelReplacementsKeepOneCurrentSubscription() throws Exception {
        InspectableController controller = controller();
        DefaultTableModel first = model(20);
        DefaultTableModel middle = model(30);
        DefaultTableModel current = model(40);
        SwingUtilities.invokeAndWait(() -> {
            JTable table = table(first);
            controller.install(table);
            table.setModel(middle);
            table.setModel(current);
        });
        drainEvents();
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(0, controller.count(first));
            assertEquals(0, controller.count(middle));
            assertEquals(1, controller.count(current));
            controller.release();
            assertEquals(0, controller.count(current));
        });
    }

    @Test
    public void rapidModelReplacementsPreserveEveryProtectedCallback() throws Exception {
        List<TableModel> oldModels = new ArrayList<>();
        InspectableController controller = new InspectableController() {
            @Override
            protected void updateModel(TableModel oldValue) {
                oldModels.add(oldValue);
                super.updateModel(oldValue);
            }
        };
        controllers.add(controller);
        DefaultTableModel first = model(20);
        DefaultTableModel middle = model(30);
        DefaultTableModel current = model(40);
        SwingUtilities.invokeAndWait(() -> {
            JTable table = table(first);
            controller.install(table);
            table.setModel(middle);
            table.setModel(current);
        });
        drainEvents();
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(2, oldModels.size());
            assertSame(first, oldModels.get(0));
            assertSame(middle, oldModels.get(1));
            assertEquals(0, controller.count(first));
            assertEquals(0, controller.count(middle));
            assertEquals(1, controller.count(current));
            controller.release();
            assertEquals(0, controller.count(current));
        });
    }

    @Test
    public void releaseCancelsQueuedModelChangeAndDetachesActualModel() throws Exception {
        InspectableController controller = controller();
        DefaultTableModel first = model(20);
        DefaultTableModel current = model(30);
        SwingUtilities.invokeAndWait(() -> {
            JTable table = table(first);
            controller.install(table);
            table.setModel(current);
            controller.release();
            assertEquals(0, controller.count(first));
            assertEquals(0, controller.count(current));
        });
        drainEvents();
    }

    @Test
    public void releaseCancelsQueuedRowChange() throws Exception {
        InspectableController controller = controller();
        DefaultTableModel current = model(20);
        SwingUtilities.invokeAndWait(() -> {
            controller.install(table(current));
            current.setValueAt(Integer.valueOf(40), 0, 0);
            controller.release();
            assertEquals(0, controller.count(current));
        });
        drainEvents();
    }

    @Test
    public void reinstallRejectsThePreviousTablesQueuedModelChange() throws Exception {
        InspectableController controller = controller();
        DefaultTableModel first = model(20);
        DefaultTableModel obsolete = model(30);
        DefaultTableModel current = model(40);
        SwingUtilities.invokeAndWait(() -> {
            JTable oldTable = table(first);
            controller.install(oldTable);
            oldTable.setModel(obsolete);
            controller.install(table(current));
        });
        drainEvents();
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(0, controller.count(first));
            assertEquals(0, controller.count(obsolete));
            assertEquals(1, controller.count(current));
        });
    }

    @Test
    public void reinstallOnTheSameTableRejectsItsPreviousQueuedRowWork() throws Exception {
        InspectableController controller = controller();
        DefaultTableModel current = model(20);
        int[] rangesAfterInstall = new int[1];
        SwingUtilities.invokeAndWait(() -> {
            JTable table = table(current);
            controller.install(table);
            current.setValueAt(Integer.valueOf(40), 0, 0);
            controller.install(table);
            rangesAfterInstall[0] = controller.rangeUpdates;
            assertEquals(40, table.getRowHeight(0));
        });
        drainEvents();
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(rangesAfterInstall[0], controller.rangeUpdates);
            assertEquals(1, controller.count(current));
        });
    }

    @Test
    public void removedRowsAreNotMeasuredByAnEarlierQueuedUpdate() throws Exception {
        InspectableController controller = controller();
        DefaultTableModel current = model(20, 30);
        JTable[] table = new JTable[1];
        SwingUtilities.invokeAndWait(() -> {
            table[0] = table(current);
            controller.install(table[0]);
            current.setValueAt(Integer.valueOf(40), 1, 0);
            current.removeRow(1);
        });
        drainEvents();
        SwingUtilities.invokeAndWait(() -> assertEquals(20, table[0].getRowHeight(0)));
    }

    @Test
    public void ordinaryUpdatesKeepHeightsAndProtectedCallbacks() throws Exception {
        InspectableController controller = controller();
        DefaultTableModel first = model(20);
        DefaultTableModel current = model(30);
        JTable[] table = new JTable[1];
        SwingUtilities.invokeAndWait(() -> {
            table[0] = table(first);
            controller.install(table[0]);
            assertEquals(20, table[0].getRowHeight(0));
            assertEquals(1, controller.fullUpdates);
            assertEquals(1, controller.rangeUpdates);
            table[0].setModel(current);
        });
        drainEvents();
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(30, table[0].getRowHeight(0));
            assertEquals(1, controller.modelUpdates);
            assertEquals(first, controller.oldModel);
            assertEquals(2, controller.fullUpdates);
            assertEquals(2, controller.rangeUpdates);
            current.setValueAt(Integer.valueOf(40), 0, 0);
        });
        drainEvents();
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(40, table[0].getRowHeight(0));
            assertEquals(2, controller.fullUpdates);
            assertEquals(3, controller.rangeUpdates);
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import org.junit.Test;

/** The original JTable owns its model and viewport; deferred work belongs to one binding. */
public class TableRowHeightControllerLifecycleTest {
    private static void edt(Runnable action) throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(action);
    }

    private static final class TrackingController extends TableRowHeightController {
        int rangeUpdates;

        TrackingController(JTable table) {
            super(table);
        }

        @Override
        protected void updatePreferredRowHeights(int firstRow, int lastRow) {
            rangeUpdates++;
            super.updatePreferredRowHeights(firstRow, lastRow);
        }
    }

    private static JTable table() {
        return new JTable(new DefaultTableModel(
            new Object[][] { { "a" }, { "b" } }, new Object[] { "value" }));
    }

    @Test
    public void queuedRowWorkCannotUpdateAnotherTableAfterRebind() throws Exception {
        final TrackingController[] controller = new TrackingController[1];
        edt(() -> {
            JTable first = table();
            JTable next = table();
            controller[0] = new TrackingController(first);
            first.setValueAt("changed", 0, 0);
            controller[0].install(next);
            controller[0].rangeUpdates = 0;
        });
        edt(() -> assertEquals("old rows must not be visited in the new table",
                0, controller[0].rangeUpdates));
        edt(() -> controller[0].release());
    }

    @Test
    public void releaseDetachesOriginalAndPendingReplacementModel() throws Exception {
        final TrackingController[] controller = new TrackingController[1];
        final DefaultTableModel[] oldModel = new DefaultTableModel[1];
        final DefaultTableModel[] newModel = new DefaultTableModel[1];
        edt(() -> {
            JTable t = table();
            oldModel[0] = (DefaultTableModel) t.getModel();
            controller[0] = new TrackingController(t);
            newModel[0] = new DefaultTableModel(
                    new Object[][] { { "new" } }, new Object[] { "value" });
            t.setModel(newModel[0]);
            controller[0].release();
        });
        edt(() -> {
            for (javax.swing.event.TableModelListener listener : oldModel[0].getTableModelListeners()) {
                assertTrue(listener != controller[0].getTableModelListener());
            }
            for (javax.swing.event.TableModelListener listener : newModel[0].getTableModelListeners()) {
                assertTrue(listener != controller[0].getTableModelListener());
            }
        });
    }

    @Test
    public void currentModelStillReceivesNormalRowUpdates() throws Exception {
        final TrackingController[] controller = new TrackingController[1];
        edt(() -> {
            JTable t = table();
            controller[0] = new TrackingController(t);
            controller[0].rangeUpdates = 0;
            t.setValueAt("updated", 1, 0);
        });
        edt(() -> {
            assertTrue("current table update must be delivered", controller[0].rangeUpdates > 0);
            controller[0].release();
        });
    }

    @Test
    public void releaseDetachesAModelRewiredByAnUpdateModelOverride() throws Exception {
        DefaultTableModel first = model(20);
        DefaultTableModel current = model(30);
        JTable[] table = new JTable[1];
        InspectableController controller = new InspectableController() {
            @Override
            protected void updateModel(TableModel oldValue) {
                oldValue.removeTableModelListener(getTableModelListener());
                table[0].getModel().addTableModelListener(getTableModelListener());
                updatePreferredRowHeights();
            }
        };
        controllers.add(controller);
        SwingUtilities.invokeAndWait(() -> {
            table[0] = table(first);
            controller.install(table[0]);
            table[0].setModel(current);
        });
        drainEvents();
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(1, controller.count(current));
            assertEquals(30, table[0].getRowHeight(0));
            controller.release();
            assertEquals(0, controller.count(current));
        });
    }

    private InspectableController controller() {
        InspectableController controller = new InspectableController();
        controllers.add(controller);
        return controller;
    }

    private void drainEvents() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
        assertTrue("Deferred row-height work failed: " + failures, failures.isEmpty());
    }

    private static DefaultTableModel model(int... heights) {
        Object[][] values = new Object[heights.length][1];
        for (int row = 0; row < heights.length; row++) {
            values[row][0] = Integer.valueOf(heights[row]);
        }
        return new DefaultTableModel(values, new Object[] {"height"});
    }

    private static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focused, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focused, row, column);
                setPreferredSize(new Dimension(30, ((Integer) value).intValue()));
                return this;
            }
        });
        return table;
    }

    private static class InspectableController extends TableRowHeightController {
        int fullUpdates;
        int rangeUpdates;
        int modelUpdates;
        TableModel oldModel;

        @Override
        protected void updateModel(TableModel oldValue) {
            modelUpdates++;
            oldModel = oldValue;
            super.updateModel(oldValue);
        }

        @Override
        protected void updatePreferredRowHeights() {
            fullUpdates++;
            super.updatePreferredRowHeights();
        }

        @Override
        protected void updatePreferredRowHeights(int firstRow, int lastRow) {
            rangeUpdates++;
            super.updatePreferredRowHeights(firstRow, lastRow);
        }

        int count(DefaultTableModel model) {
            int count = 0;
            TableModelListener expected = getTableModelListener();
            for (TableModelListener listener : model.getTableModelListeners()) {
                if (listener == expected) count++;
            }
            return count;
        }
    public void modelChangeKeepsTheNewListenerExactlyOnce() throws Exception {
        final TrackingController[] controller = new TrackingController[1];
        final DefaultTableModel[] newModel = new DefaultTableModel[1];
        edt(() -> {
            JTable t = table();
            controller[0] = new TrackingController(t);
            newModel[0] = new DefaultTableModel(new Object[][] { { "new" } },
                    new Object[] { "value" });
            t.setModel(newModel[0]);
        });
        edt(() -> {
            int matches = 0;
            for (javax.swing.event.TableModelListener listener : newModel[0].getTableModelListeners()) {
                if (listener == controller[0].getTableModelListener()) matches++;
            }
            assertEquals(1, matches);
            controller[0].release();
        });
    }
}
