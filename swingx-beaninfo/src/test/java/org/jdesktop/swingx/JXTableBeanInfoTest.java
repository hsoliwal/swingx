package org.jdesktop.swingx;

import java.beans.PropertyChangeListener;

import org.jdesktop.beans.AbstractBeanInfoTest;

public class JXTableBeanInfoTest extends AbstractBeanInfoTest<JXTable> {
    @Override
    protected JXTable createInstance() {
        return new JXTable();
    }
    JXTable getInstance() {
    	return super.instance;
    }
    protected PropertyChangeListener[] getPropertyChangeListener() {
    	return getInstance().getListeners(PropertyChangeListener.class);
    }
    protected void addPropertyChangeListener(PropertyChangeListener pcl) {
    	getInstance().addPropertyChangeListener(pcl);
    }
}
