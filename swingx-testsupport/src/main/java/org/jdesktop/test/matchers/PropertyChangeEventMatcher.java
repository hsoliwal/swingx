package org.jdesktop.test.matchers;

import static org.hamcrest.CoreMatchers.is;

import java.beans.PropertyChangeEvent;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.mockito.ArgumentMatcher;

/**
 * extends org.hamcrest.BaseMatcher implements org.mockito.ArgumentMatcher
 * org.hamcrest.BaseMatcher implements org.hamcrest.Matcher
 *                                     org.hamcrest.Matcher<T> extends SelfDescribing
 * <p>
 * abstract {@link BaseMatcher} class ensures that the Matcher API can grow to support
 * new features and remain compatible with all Matcher implementations.
 *
 * @see Matcher
 * @see ArgumentMatcher
 */
class PropertyChangeEventMatcher extends BaseMatcher<PropertyChangeEvent> implements ArgumentMatcher<Object> {

    private final String propertyName;
    private final Object oldValue;
    private final Object newValue;
    
    public PropertyChangeEventMatcher(String propertyName, Object oldValue, Object newValue) {
        this.propertyName = propertyName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
    
    /**
     * {@inheritDoc}
     */
	@Override
	public boolean matches(Object argument) {
        if (argument instanceof PropertyChangeEvent) {
            PropertyChangeEvent pce = (PropertyChangeEvent) argument;
            
            boolean result = propertyName.equals(pce.getPropertyName());
            result &= oldValue == null || pce.getOldValue() == null || is(oldValue).matches(pce.getOldValue()); 
            result &= newValue == null || pce.getNewValue() == null || is(newValue).matches(pce.getNewValue());
            
            return result;
        }
        
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void describeTo(Description description) {
        description.appendText(" " + propertyName);
    }

}
