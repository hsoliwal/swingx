/*
 * Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * Licensed under the GNU Lesser General Public License, version 2.1 or later.
 */
package org.jdesktop.swingx.table;

import static org.junit.Assert.assertEquals;
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
