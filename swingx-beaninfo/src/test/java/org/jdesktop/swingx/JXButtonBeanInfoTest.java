package org.jdesktop.swingx;

import java.beans.PropertyChangeListener;

import org.jdesktop.beans.AbstractBeanInfoTest;

public class JXButtonBeanInfoTest extends AbstractBeanInfoTest<JXButton> {
    @Override
    protected JXButton createInstance() {
        return new JXButton();
    }
    JXButton getInstance() {
    	return super.instance;
    }
    protected PropertyChangeListener[] getPropertyChangeListener() {
    	return getInstance().getListeners(PropertyChangeListener.class);
    }
    protected void addPropertyChangeListener(PropertyChangeListener pcl) {
    	getInstance().addPropertyChangeListener(pcl);
    }
//    public void setUp() throws Exception {
//    	init();
//        
//    	PropertyChangeListener[] pcls = getInstance().getListeners(PropertyChangeListener.class);
//    	for(int i=0; i<pcls.length; i++) {
//    		logger.log(Level.CONFIG, "i="+i + " "+pcls[i].getClass());
//    		PropertyChangeListener l = Mockito.mock(PropertyChangeListener.class);
//        	getInstance().addPropertyChangeListener(l);
//        	listeners.put(pcls[i].getClass(), l);		
//    	}
//		logger.log(Level.CONFIG, "listeners size "+listeners.size());
//    }
}
