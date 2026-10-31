package org.jdesktop.swingx;

import java.beans.Introspector;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.logging.Level;

import org.jdesktop.beans.AbstractBeanInfoTest;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

public class JXRadioGroupBeanInfoTest extends AbstractBeanInfoTest<JXRadioGroup> {
    @Override
    protected JXRadioGroup createInstance() {
        return new JXRadioGroup();
    }
    JXRadioGroup getInstance() {
    	return super.instance;
    }
    protected PropertyChangeListener[] getPropertyChangeListener() {
    	return getInstance().getListeners(PropertyChangeListener.class);
    }
    protected void addPropertyChangeListener(PropertyChangeListener pcl) {
    	getInstance().addPropertyChangeListener(pcl);
    }
//    public void setUp() throws Exception {
//	//	init();
//	    instance = createInstance();
//	    beanInfo = Introspector.getBeanInfo(JXRadioGroup.class); // IntrospectionException
//	    listeners = new HashMap<Class<?>, Object>();
//	    
//		PropertyChangeListener[] pcls = getInstance().getListeners(PropertyChangeListener.class);
//		if(pcls!=null) for(int i=0; i<pcls.length; i++) {
//			logger.log(Level.CONFIG, "i="+i + " "+pcls[i].getClass());
//			PropertyChangeListener l = Mockito.mock(PropertyChangeListener.class);
//	    	getInstance().addPropertyChangeListener(l);
//	    	listeners.put(pcls[i].getClass(), l);		
//		}
//		logger.log(Level.CONFIG, "listeners size "+listeners.size());
//    }
    
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
