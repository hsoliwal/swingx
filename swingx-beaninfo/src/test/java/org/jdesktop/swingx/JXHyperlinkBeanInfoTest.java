package org.jdesktop.swingx;

import java.beans.PropertyChangeListener;

import org.jdesktop.beans.AbstractBeanInfoTest;

public class JXHyperlinkBeanInfoTest extends AbstractBeanInfoTest<JXHyperlink> {
    @Override
    protected JXHyperlink createInstance() {
        return new JXHyperlink();
    }
    JXHyperlink getInstance() {
    	return super.instance;
    }
    protected PropertyChangeListener[] getPropertyChangeListener() {
    	return getInstance().getListeners(PropertyChangeListener.class);
    }
    protected void addPropertyChangeListener(PropertyChangeListener pcl) {
    	getInstance().addPropertyChangeListener(pcl);
    }
}
