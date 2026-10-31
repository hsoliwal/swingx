package org.jdesktop.swingx;

import java.beans.PropertyChangeListener;

import org.jdesktop.beans.AbstractBeanInfoTest;

public class JXTitledSeparatorBeanInfoTest extends AbstractBeanInfoTest<JXTitledSeparator> {
    @Override
    protected JXTitledSeparator createInstance() {
        return new JXTitledSeparator();
    }
    JXTitledSeparator getInstance() {
    	return super.instance;
    }
    protected PropertyChangeListener[] getPropertyChangeListener() {
    	return getInstance().getListeners(PropertyChangeListener.class);
    }
    protected void addPropertyChangeListener(PropertyChangeListener pcl) {
    	getInstance().addPropertyChangeListener(pcl);
    }
}
