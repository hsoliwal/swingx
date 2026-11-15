/*
 * Copyright 2026 SwingX contributors.
 * Licensed under the GNU Lesser General Public License, version 2.1 or later.
 */
package org.jdesktop.swingx.treetable;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

import org.jdesktop.test.EDTRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Compatibility checks for paths used by tree-table model notifications. */
@RunWith(EDTRunner.class)
public class DefaultTreeTableModelPathTest {
    @Test
    public void deepPathKeepsEveryNodeInRootToLeafOrder() {
        DefaultMutableTreeTableNode[] nodes = new DefaultMutableTreeTableNode[16384];
        nodes[0] = new DefaultMutableTreeTableNode();
        for (int i = 1; i < nodes.length; i++) {
            nodes[i] = new DefaultMutableTreeTableNode();
            nodes[i - 1].add(nodes[i]);
        }
        DefaultTreeTableModel model = model(nodes[0]);
        TreeTableNode[] path = model.getPathToRoot(nodes[nodes.length - 1]);
        assertEquals(nodes.length, path.length);
        for (int i = 0; i < nodes.length; i++) {
            assertSame("path node " + i, nodes[i], path[i]);
        }
    }

    @Test
    public void readsEachParentOnceAndStopsAtTheModelRoot() {
        List<String> calls = new ArrayList<String>();
        ObservedNode above = new ObservedNode("above", calls);
        ObservedNode root = new ObservedNode("root", calls);
        ObservedNode branch = new ObservedNode("branch", calls);
        ObservedNode leaf = new ObservedNode("leaf", calls);
        above.add(root);
        root.add(branch);
        branch.add(leaf);
        DefaultTreeTableModel model = model(root);
        root.onParentRead = () -> fail("the model root's parent must not be read");
        calls.clear();

        assertArrayEquals(new TreeTableNode[] {root, branch, leaf}, model.getPathToRoot(leaf));
        assertEquals(Arrays.asList("leaf", "branch"), calls);
        calls.clear();
        assertArrayEquals(new TreeTableNode[] {root}, model.getPathToRoot(root));
        assertTrue(calls.isEmpty());
    }

    @Test
    public void returnedPathsAreDetachedAndObserveLaterReparenting() {
        DefaultMutableTreeTableNode root = new DefaultMutableTreeTableNode();
        DefaultMutableTreeTableNode left = new DefaultMutableTreeTableNode();
        DefaultMutableTreeTableNode right = new DefaultMutableTreeTableNode();
        DefaultMutableTreeTableNode leaf = new DefaultMutableTreeTableNode();
        root.add(left);
        root.add(right);
        left.add(leaf);
        DefaultTreeTableModel model = model(root);

        TreeTableNode[] first = model.getPathToRoot(leaf);
        TreeTableNode[] second = model.getPathToRoot(leaf);
        assertNotSame(first, second);
        first[0] = null;
        assertArrayEquals(new TreeTableNode[] {root, left, leaf}, second);
        right.add(leaf);
        assertArrayEquals(new TreeTableNode[] {root, right, leaf}, model.getPathToRoot(leaf));
        assertArrayEquals(new TreeTableNode[] {root, left, leaf}, second);
    }

    @Test
    public void nullAndUnmanagedNodesKeepExistingFailures() {
        DefaultTreeTableModel model = model(new DefaultMutableTreeTableNode());
        assertNullPointer(() -> model.getPathToRoot(null));
        DefaultMutableTreeTableNode foreignRoot = new DefaultMutableTreeTableNode();
        DefaultMutableTreeTableNode foreignLeaf = new DefaultMutableTreeTableNode();
        foreignRoot.add(foreignLeaf);
        assertNullPointer(() -> model.getPathToRoot(foreignLeaf));
    }

    @Test
    public void nullRootKeepsItsExistingTerminalNull() {
        DefaultTreeTableModel model = model(null);
        assertArrayEquals(new TreeTableNode[] {null}, model.getPathToRoot(null));
        DefaultMutableTreeTableNode parent = new DefaultMutableTreeTableNode();
        DefaultMutableTreeTableNode leaf = new DefaultMutableTreeTableNode();
        parent.add(leaf);
        assertArrayEquals(new TreeTableNode[] {null, parent, leaf}, model.getPathToRoot(leaf));
    }

    @Test
    public void parentReadObservesAReentrantRootChange() {
        List<String> calls = new ArrayList<String>();
        ObservedNode root = new ObservedNode("root", calls);
        ObservedNode branch = new ObservedNode("branch", calls);
        ObservedNode leaf = new ObservedNode("leaf", calls);
        root.add(branch);
        branch.add(leaf);
        DefaultTreeTableModel model = model(root);
        leaf.onParentRead = () -> model.setRoot(branch);
        calls.clear();

        assertArrayEquals(new TreeTableNode[] {branch, leaf}, model.getPathToRoot(leaf));
        assertEquals(Collections.singletonList("leaf"), calls);
        assertSame(branch, model.getRoot());
    }

    @Test
    public void reentrantPathQueriesHaveIndependentWorkingState() {
        List<String> calls = new ArrayList<String>();
        ObservedNode root = new ObservedNode("root", calls);
        ObservedNode branch = new ObservedNode("branch", calls);
        ObservedNode leaf = new ObservedNode("leaf", calls);
        root.add(branch);
        branch.add(leaf);
        DefaultTreeTableModel model = model(root);
        leaf.onParentRead = () -> assertArrayEquals(
                new TreeTableNode[] {root, branch}, model.getPathToRoot(branch));
        calls.clear();

        assertArrayEquals(new TreeTableNode[] {root, branch, leaf}, model.getPathToRoot(leaf));
        assertEquals(Arrays.asList("leaf", "branch", "branch"), calls);
    }

    @Test
    public void parentGetterFailurePropagatesWithoutFurtherTraversal() {
        List<String> calls = new ArrayList<String>();
        ObservedNode root = new ObservedNode("root", calls);
        ObservedNode leaf = new ObservedNode("leaf", calls);
        root.add(leaf);
        DefaultTreeTableModel model = model(root);
        RuntimeException failure = new IllegalStateException("parent access failed");
        leaf.onParentRead = () -> { throw failure; };
        calls.clear();
        try {
            model.getPathToRoot(leaf);
            fail("parent getter failure must propagate");
        } catch (RuntimeException actual) {
            assertSame(failure, actual);
        }
        assertEquals(Collections.singletonList("leaf"), calls);
    }

    @Test
    public void modelEventsKeepTheirPathsAndPermitReentrantQueriesOnTheEdt() {
        DefaultMutableTreeTableNode root = new DefaultMutableTreeTableNode("root");
        DefaultMutableTreeTableNode parent = new DefaultMutableTreeTableNode("parent");
        DefaultMutableTreeTableNode child = new DefaultMutableTreeTableNode("child");
        root.add(parent);
        DefaultTreeTableModel model = model(root);
        List<String> events = new ArrayList<String>();
        model.addTreeModelListener(new TreeModelListener() {
            private void check(TreeModelEvent event, String kind) {
                assertTrue(SwingUtilities.isEventDispatchThread());
                assertSame(model, event.getSource());
                assertArrayEquals(new Object[] {root, parent}, event.getPath());
                assertArrayEquals(new int[] {0}, event.getChildIndices());
                assertArrayEquals(new Object[] {child}, event.getChildren());
                assertArrayEquals(new TreeTableNode[] {root, parent}, model.getPathToRoot(parent));
                events.add(kind);
            }

            @Override
            public void treeNodesInserted(TreeModelEvent event) {
                check(event, "inserted");
                assertSame(parent, child.getParent());
            }

            @Override
            public void treeNodesChanged(TreeModelEvent event) {
                check(event, "changed");
                assertEquals("changed child", child.getUserObject());
            }

            @Override
            public void treeNodesRemoved(TreeModelEvent event) {
                check(event, "removed");
                assertNull(child.getParent());
            }

            @Override
            public void treeStructureChanged(TreeModelEvent event) {
                fail("path construction must not add a structure-change event");
            }
        });

        model.insertNodeInto(child, parent, 0);
        model.setUserObject(child, "changed child");
        model.removeNodeFromParent(child);
        assertEquals(Arrays.asList("inserted", "changed", "removed"), events);
    }

    private static DefaultTreeTableModel model(TreeTableNode root) {
        return new DefaultTreeTableModel(root, Collections.singletonList("value"));
    }

    private static void assertNullPointer(Runnable action) {
        try {
            action.run();
            fail("existing null-parent failure must be retained");
        } catch (NullPointerException expected) {
            // Existing contract, including a node outside the model's ancestry.
        }
    }

    private static class ObservedNode extends DefaultMutableTreeTableNode {
        private final String name;
        private final List<String> calls;
        private Runnable onParentRead;

        ObservedNode(String name, List<String> calls) {
            this.name = name;
            this.calls = calls;
        }

        @Override
        public TreeTableNode getParent() {
            calls.add(name);
            if (onParentRead != null) {
                onParentRead.run();
            }
            return super.getParent();
        }
    }
}
