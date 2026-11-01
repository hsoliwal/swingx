package org.jdesktop.swingx;

import java.beans.PropertyChangeListener;

import org.jdesktop.beans.AbstractBeanInfoTest;

public class JXFormattedTextFieldBeanInfoTest extends AbstractBeanInfoTest<JXFormattedTextField> {
    @Override
    protected JXFormattedTextField createInstance() {
        return new JXFormattedTextField();
    }
    JXFormattedTextField getInstance() {
    	return super.instance;
    }
    protected PropertyChangeListener[] getPropertyChangeListener() {
    	return getInstance().getListeners(PropertyChangeListener.class);
    }
    protected void addPropertyChangeListener(PropertyChangeListener pcl) {
    	getInstance().addPropertyChangeListener(pcl);
    }
}
