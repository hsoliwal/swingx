package org.jdesktop.beans;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.jdesktop.test.SerializableSupport.serialize;
import static org.jdesktop.test.matchers.Matchers.equivalentTo;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.awt.Insets;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hamcrest.Matcher;
import org.jdesktop.test.matchers.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.exceptions.verification.NoInteractionsWanted;

public abstract class AbstractBeanInfoTest<T> {
	
    protected Logger logger = Logger.getLogger(getClass().getName());
    
    protected T instance;
    protected BeanInfo beanInfo;
    protected Map<Class<?>, Object> listeners;
    
    protected void init() throws IntrospectionException {
        instance = createInstance();
        beanInfo = Introspector.getBeanInfo(instance.getClass()); // IntrospectionException
        listeners = new HashMap<Class<?>, Object>();
    }
    
    protected abstract T createInstance();    
//    protected abstract PropertyChangeListener[] getPropertyChangeListener();
    protected PropertyChangeListener[] getPropertyChangeListener() {
    	return null;
    }
    protected void addPropertyChangeListener(PropertyChangeListener pcl) {
    	// to be implemented in subclass
    }

    @Before
    public void setUp() throws Exception {
//        instance = createInstance();
//        beanInfo = Introspector.getBeanInfo(instance.getClass());
//        listeners = new HashMap<Class<?>, Object>();
    	init();
        
//        for (EventSetDescriptor descriptor : beanInfo.getEventSetDescriptors()) {
//            Class<?> listenerType = descriptor.getListenerType(); // besser listenerType eventClass
//            Object listener = Mockito.mock(listenerType); // mock object of given class/type or interface
//            
//            // TODO nur invoke für PropertyChangeListener
//            descriptor.getAddListenerMethod().invoke(instance, listener);
//            listeners.put(listenerType, listener);
//        }
//		logger.log(Level.INFO, "listeners size "+listeners.size());
		
    	PropertyChangeListener[] pcls = getPropertyChangeListener();
    	if(pcls!=null) for(int i=0; i<pcls.length; i++) {
    		logger.log(Level.CONFIG, "i="+i + " "+pcls[i].getClass());
    		PropertyChangeListener l = Mockito.mock(PropertyChangeListener.class);
    		addPropertyChangeListener(l);
//        	getInstance().addPropertyChangeListener(l);
//        	descriptor.getAddListenerMethod().invoke(instance, l);
        	listeners.put(pcls[i].getClass(), l);		
    	}
		logger.log(Level.CONFIG, "listeners size "+listeners.size());
    }
    
    @Test
//    public final void testBoundProperties() throws Exception {
    public void testBoundProperties() {
        logger.log(Level.INFO, "beanInfo:"+beanInfo);
        for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
            logger.log(Level.INFO, "beanInfo.getPropertyDescriptors():"+descriptor);
            if (descriptor.isBound()) {
                if (descriptor.isHidden()) {
                    continue;
                }
                
                if (descriptor.getWriteMethod() == null) {
                    //special-case this read-only property
                    if ("UIClassID".equals(descriptor.getName())) {
                        return;
                    }
                    
                    fail("bound read-only property: " + descriptor.getName());
                }
                
                Class<?> propertyType = descriptor.getPropertyType();
                
                if (isUnhandledType(propertyType)) {
                    //TODO log?
                    continue;
                }
                
                Object defaultValue = null;
				try {
					defaultValue = descriptor.getReadMethod().invoke(instance);
				} catch (IllegalAccessException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IllegalArgumentException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (InvocationTargetException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
                Object newValue = getNewValue(propertyType, defaultValue);
                
                logger.log(Level.INFO, "propertyType:"+propertyType + ", newValue:"+newValue);
                try {
                    descriptor.getWriteMethod().invoke(instance, newValue);
                } catch (InvocationTargetException ex) {
                    logger.log(Level.WARNING, "InvocationTargetException", ex);
                    continue;
                } catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
/*
java.lang.reflect.InvocationTargetException
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:78)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base/java.lang.reflect.Method.invoke(Method.java:567)
	at org.jdesktop.beans.AbstractBeanInfoTest.testBoundProperties(AbstractBeanInfoTest.java:93)
 */                
                logger.log(Level.INFO, "listeners.size():"+listeners.size());
                Object listener = listeners.get(PropertyChangeListener.class);
                if(listener==null) return;
                
                PropertyChangeListener pclMock = (PropertyChangeListener) listener;
//                verify(pcl).propertyChange(argThat(is(property(descriptor.getName(), defaultValue, newValue))));
//                Matcher<PropertyChangeEvent> am = property(descriptor.getName(), defaultValue, newValue);
//                Matcher<PropertyChangeEvent> pce = is(am);
                
                Matcher<PropertyChangeEvent> m = Matchers.property(descriptor.getName(), defaultValue, newValue);
                logger.log(Level.INFO, "Matcher<PropertyChangeEvent> m:"+m.getClass()+" "+m
                	+", descriptor.Name="+descriptor.getName()+", defaultValue:"+defaultValue+", newValue:"+newValue);
                // can cast PropertyChangeEventMatcher to org.mockito.ArgumentMatchers:
                ArgumentMatcher<PropertyChangeEvent> am = (ArgumentMatcher<PropertyChangeEvent>)m;
//                if(m instanceof PropertyChangeEventMatcher) {
//                    ArgumentMatcher<PropertyChangeEvent> am = (ArgumentMatcher<PropertyChangeEvent>)m;
//                    PropertyChangeEvent pce = ArgumentMatchers.argThat(am);
//                    PropertyChangeListener mock = Mockito.verify(pclMock); // Verifies certain behavior happened once. 
//                    mock.propertyChange(pce);
//                }
//                verify(pcl).propertyChange(argThat( (ArgumentMatcher<PropertyChangeEvent>)(is(property(descriptor.getName(), defaultValue, newValue))) ));
                
                PropertyChangeListener mock = Mockito.verify(pclMock); // Verifies certain behavior happened once. 
                mock.propertyChange(argThat(am));
                Mockito.reset(pclMock);
            }
        }
    }
    
    private boolean isUnhandledType(Class<?> type) {
        return type == null;
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Object getNewValue(Class<?> propertyType, Object defaultValue) {
        Object result = null;
        
        if (propertyType.isArray()) {
            int length = defaultValue == null ? 1 : ((Object[]) defaultValue).length + 1;
            result = Array.newInstance(propertyType.getComponentType(), length);
        } else if (propertyType.isEnum()) {
            EnumSet set = EnumSet.allOf((Class<? extends Enum>) propertyType);
            int size = set.size();
            
            if (size == 1) {
                result = defaultValue == null ? set.iterator().next() : null;
            } else {
                int ordinal = ((Enum) defaultValue).ordinal();
                ordinal = ordinal == size - 1 ? 0 : ordinal + 1;
                Iterator iter = set.iterator();
                
                for (int i = 0; i < ordinal + 1; i++) {
                    result = iter.next();
                }
            }
        } else if (propertyType.isPrimitive()) {
            //help short circuit all of these checks
            if (propertyType == boolean.class) {
                result = Boolean.FALSE.equals(defaultValue);
            } else if (propertyType == int.class) {
                result = ((Integer) defaultValue) + 1;
            } else if (propertyType == double.class) {
                result = ((Double) defaultValue) + 1d;
            } else if (propertyType == float.class) {
                result = ((Float) defaultValue) + 1f;
            }
        } else if (propertyType == String.class) {
            result = "original string: " + defaultValue;
        } else if (propertyType == Insets.class) {
            if (new Insets(0, 0, 0, 0).equals(defaultValue)) {
                result = new Insets(1, 1, 1, 1);
            } else {
                result = mock(propertyType);
            }
        } else {
            result = mock(propertyType, RETURNS_MOCKS);
        }
        
        return result;
    }

    /**
     * A simple serialization check. Ensures that the reconstituted object is equivalent to the
     * original.
     */
    @Test
    public void testSerialization() {
        logger.log(Level.INFO, "beanInfo:"+beanInfo);
        if (!Serializable.class.isInstance(instance)) {
            return;
        }
        
        T serialized = serialize(instance);
        
        assertThat(serialized, is(equivalentTo(instance)));
    }
    
    @After
    public void tearDown() {
		logger.log(Level.INFO, "listeners size "+listeners.size());
        for (Object listener : listeners.values()) {
            try {
                logger.log(Level.INFO, "listener "+listener);
                // TODO need a way to handle components that have contained components,
                // like JXComboBox, that cause spurious container events
                verifyNoMoreInteractions(listener);
//                PropertyChangeListener pclMock = (PropertyChangeListener) listener;
//                Mockito.clearInvocations(pclMock);
/*
java.lang.reflect.InvocationTargetException
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:78)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base/java.lang.reflect.Method.invoke(Method.java:567)
	at org.jdesktop.beans.AbstractBeanInfoTest.testBoundProperties(AbstractBeanInfoTest.java:89)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:78)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.base/java.lang.reflect.Method.invoke(Method.java:567)
	at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
	at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
	at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
	at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
	at org.junit.internal.runners.statements.RunBefores.evaluate(RunBefores.java:26)
	at org.junit.internal.runners.statements.RunAfters.evaluate(RunAfters.java:27)
	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
	at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100)
	at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103)
	at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63)
	at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331)
	at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79)
	at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
	at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
	at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293)
	at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
	at org.junit.runners.ParentRunner.run(ParentRunner.java:413)
	at org.eclipse.jdt.internal.junit4.runner.JUnit4TestReference.run(JUnit4TestReference.java:93)
	at org.eclipse.jdt.internal.junit.runner.TestExecution.run(TestExecution.java:40)
	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:529)
	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.runTests(RemoteTestRunner.java:756)
	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.run(RemoteTestRunner.java:452)
	at org.eclipse.jdt.internal.junit.runner.RemoteTestRunner.main(RemoteTestRunner.java:210)
Caused by: java.lang.ClassCastException: class org.mockito.codegen.Object$MockitoMock$1823671394 cannot be cast to class java.lang.Integer (org.mockito.codegen.Object$MockitoMock$1823671394 is in unnamed module of loader 'app'; java.lang.Integer is in module java.base of loader 'bootstrap')
	at java.desktop/javax.swing.AbstractButton.setMnemonicFromAction(AbstractButton.java:1228)
	at java.desktop/javax.swing.AbstractButton.configurePropertiesFromAction(AbstractButton.java:1131)
	at java.desktop/javax.swing.AbstractButton.setAction(AbstractButton.java:1078)
	... 33 more


 */
            } catch (NoInteractionsWanted logAndIgnore) {
                logger.log(Level.WARNING, "unexpected listener notification", logAndIgnore);
            }
        }
    }
}
