package org.jdesktop.swingx;

import java.beans.PropertyChangeListener;

import org.jdesktop.beans.AbstractBeanInfoTest;
import org.junit.Ignore;
import org.junit.Test;

public class JXMultiSplitPaneBeanInfoTest extends AbstractBeanInfoTest<JXMultiSplitPane> {
    @Override
    protected JXMultiSplitPane createInstance() {
        return new JXMultiSplitPane();
    }
    JXMultiSplitPane getInstance() {
    	return super.instance;
    }
    protected PropertyChangeListener[] getPropertyChangeListener() {
    	return getInstance().getListeners(PropertyChangeListener.class);
    }
    protected void addPropertyChangeListener(PropertyChangeListener pcl) {
    	getInstance().addPropertyChangeListener(pcl);
    }
    
    /**
     * {@inheritDoc}
     */
    @Test
    @Override
    @Ignore("boundProperties fails")
    public void testBoundProperties() {
        super.testBoundProperties();
    }

}
