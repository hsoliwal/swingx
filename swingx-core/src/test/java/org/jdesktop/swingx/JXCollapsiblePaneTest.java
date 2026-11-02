/*
 * $Id: JXCollapsiblePaneTest.java 3950 2011-03-14 20:17:23Z kschaefe $
 *
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 */
package org.jdesktop.swingx;


import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.hamcrest.core.Is.isA;
import static org.hamcrest.core.IsInstanceOf.instanceOf;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Logger;

import javax.swing.JLabel;

import org.hamcrest.Matcher;
import org.hamcrest.core.Is;
import org.jdesktop.test.EDTRunner;
import org.jdesktop.test.matchers.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;

/**
 * Unit test for <code>JXCollapsiblePane</code>.
 * <p>
 * 
 * All test methods in this class are expected to pass. 
 * 
 * @author Karl Schaefer
 */
@RunWith(EDTRunner.class)
public class JXCollapsiblePaneTest {
	
	private static final Logger LOG = Logger.getLogger(JXCollapsiblePaneTest.class.getName());
    /**
     * SWINGX-1412: Ensure that we do not animate when not showing.
     */
    @Test
    public void testCollapsingNonShowingPane() {
        JXCollapsiblePane pane = new JXCollapsiblePane();
        pane.add(new JLabel("some content"));
        
        PropertyChangeListener pcl = mock(PropertyChangeListener.class);
        pane.addPropertyChangeListener("collapsed", pcl);
        
        pane.setCollapsed(true);
        
        // argThat fails if we animate because the property change
        // will enqueue on the EDT after this check
        
        Matcher<PropertyChangeEvent> m = Matchers.property("collapsed", false, true);
        LOG.info("Matcher<PropertyChangeEvent> m:"+m.getClass()+" "+m);
        // can cast PropertyChangeEventMatcher to org.mockito.ArgumentMatchers:
        ArgumentMatcher<PropertyChangeEvent> am = (ArgumentMatcher<PropertyChangeEvent>)m;       
        // but cannot cast Decorator org.hamcrest.core.Is to org.mockito.ArgumentMatchers
//        Matcher<PropertyChangeEvent> pcem = Is.is(Matchers.property("collapsed", false, true));
//        LOG.info("Matcher<PropertyChangeEvent> pcem:"+pcem.getClass()+" "+pcem);
//        ArgumentMatcher<PropertyChangeEvent> fails = (ArgumentMatcher<PropertyChangeEvent>)pcem;

        verify(pcl).propertyChange(argThat( am ));
                
    }
 }
