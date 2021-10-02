package org.jdesktop.swingx;

import java.beans.PropertyChangeListener;

import org.jdesktop.beans.AbstractBeanInfoTest;

public class JXPanelBeanInfoTest extends AbstractBeanInfoTest<JXPanel> {
    @Override
    protected JXPanel createInstance() {
        return new JXPanel();
    }
    JXPanel getInstance() {
    	return super.instance;
    }
    protected PropertyChangeListener[] getPropertyChangeListener() {
    	return getInstance().getListeners(PropertyChangeListener.class);
    }
    protected void addPropertyChangeListener(PropertyChangeListener pcl) {
    	getInstance().addPropertyChangeListener(pcl);
    }
}
