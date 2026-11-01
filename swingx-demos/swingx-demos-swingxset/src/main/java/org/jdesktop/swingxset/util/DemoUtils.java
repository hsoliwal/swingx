/*
 * Created on 16.11.2009
 *
 */
package org.jdesktop.swingxset.util;

import java.awt.Component;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.security.AllPermission;
import java.security.Permission;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.text.DefaultEditorKit;

import org.jdesktop.application.Application;
import org.pushingpixels.trident.Timeline;
import org.pushingpixels.trident.Timeline.TimelineState;
import org.pushingpixels.trident.callback.UIThreadTimelineCallbackAdapter;

/**
 * Misc. convenience methods ;-) 
 */
public class DemoUtils {

    @SuppressWarnings("unused")
    private static final Logger LOG = Logger.getLogger(DemoUtils.class
            .getName());

    //FLLDCIM: MRL - JNLP (Java Web Start) services resolved reflectively: javax.jnlp is absent from JDK 11+
    /** Java Web Start service manager; present only when running under Web Start. */
    private static final String JNLP_SERVICE_MANAGER = "javax.jnlp.ServiceManager";
    /** Static service lookup method of the service manager. */
    private static final String JNLP_LOOKUP = "lookup";
    /** JNLP service that shows documents in the browser. */
    private static final String JNLP_BASIC_SERVICE = "javax.jnlp.BasicService";
    /** BasicService method telling whether a browser is available. */
    private static final String JNLP_IS_WEB_BROWSER_SUPPORTED = "isWebBrowserSupported";
    /** BasicService method showing a document. */
    private static final String JNLP_SHOW_DOCUMENT = "showDocument";
    /** JNLP clipboard service. */
    private static final String JNLP_CLIPBOARD_SERVICE = "javax.jnlp.ClipboardService";
    /** ClipboardService method setting the clipboard contents. */
    private static final String JNLP_SET_CONTENTS = "setContents";
    
    /**
     * Replaces the editor's default copy action in security restricted
     * environments with one messaging the ClipboardService. Does nothing 
     * if not restricted.
     * 
     * @param editor the editor to replace 
     */
    public static void replaceCopyAction(final JEditorPane editor) {
        if (!isRestricted()) return;
        Action safeCopy = new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    //FLLDCIM: MRL - javax.jnlp.ClipboardService looked up reflectively (no javaws.jar on JDK 11+)
                    Object cs = lookupJnlpService(JNLP_CLIPBOARD_SERVICE);
                    StringSelection transferable = new StringSelection(editor.getSelectedText());
                    Class.forName(JNLP_CLIPBOARD_SERVICE).getMethod(JNLP_SET_CONTENTS, Transferable.class)
                        .invoke(cs, transferable);
                } catch (Exception e1) {
                    // do nothing
                }
            }
        };
        editor.getActionMap().put(DefaultEditorKit.copyAction, safeCopy);
    }
    
    /**
     * Looks a Java Web Start (JNLP) service up by name. Reflective, so that the demos
     * compile and run without javax.jnlp, which JDK 11 and later no longer ship.
     *
     * @param serviceName the JNLP service name, for example {@code javax.jnlp.BasicService}
     * @return the service
     * @throws IOException if Web Start or the service is not available
     */
    public static Object lookupJnlpService(String serviceName) throws IOException {
        try {
            return Class.forName(JNLP_SERVICE_MANAGER).getMethod(JNLP_LOOKUP, String.class)
                .invoke(null, serviceName);
        } catch (ReflectiveOperationException e) {
            throw new IOException(serviceName, e);
        }
    }

    /**
     * Shows the document through the JNLP BasicService when it supports a browser.
     *
     * @param url the document
     * @return whether the document was shown
     * @throws IOException if Web Start or the service is not available
     */
    public static boolean showJnlpDocument(URL url) throws IOException {
        Object service = lookupJnlpService(JNLP_BASIC_SERVICE);
        try {
            Class<?> type = Class.forName(JNLP_BASIC_SERVICE);
            if (!(Boolean) type.getMethod(JNLP_IS_WEB_BROWSER_SUPPORTED).invoke(service)) {
                return false;
            }
            return (Boolean) type.getMethod(JNLP_SHOW_DOCUMENT, URL.class).invoke(service, url);
        } catch (ReflectiveOperationException e) {
            throw new IOException(JNLP_BASIC_SERVICE, e);
        }
    }

    private static boolean isRestricted() {
        SecurityManager manager = System.getSecurityManager();
        if (manager == null) return false;
        try {
        	Permission perm = new AllPermission(); // == SecurityConstants.ALL_PERMISSION
            manager.checkPermission(perm);
            return false;
        } catch (SecurityException e) {
            // nothing to do - not allowed to access
        }
        return true;
    }
//--------------- Application-related convenience methods
    
    /**
     * Injects the resources into the given component instance from
     * the ResourceMap of the component's class.
     */
    public static void injectResources(Component comp) {
        Application.getInstance().getContext().getResourceMap(comp.getClass()).injectComponents(comp);
    }

    /**
     * Injects the resources into the given component instance from
     * the ResourceMap of the parent's class.
     */
    public static void injectResources(Object parent, Component child) {
        Application.getInstance().getContext().getResourceMap(parent.getClass())
            .injectComponents(child);
    }
    
    /**
     * Returns an Icon stored with the given key using the resourceMap of 
     * the baseClass, or null if none is found.
     * 
     */
    public static Icon getResourceIcon(Class<?> baseClass, String key) {
        return Application.getInstance().getContext()
            .getResourceMap(baseClass).getIcon(key);
    }

    /**
     * Returns an String stored with the given key using the resourceMap of 
     * the baseClass, or null if none is found.
     * 
     */
    public static String getResourceString(Class<?> baseClass, String key) {
        return Application.getInstance().getContext()
            .getResourceMap(baseClass).getString(key);
    }

    public static Action getAction(Object actionProvider, String key) {
        return Application.getInstance().getContext().getActionMap(actionProvider).get(key);        
    }
    
//--------------------- swingset specials
    
    public static void setSnippet(String snippet, JComponent... comps) {
        for (JComponent comp : comps) {
            comp.putClientProperty("snippetKey", snippet);
        }
    }

//----------------------- Window transparency convenience support
    
    public static void fadeOutAndDispose(final Window window,
            int fadeOutDuration) {
        fadeOutAndEnd(window, fadeOutDuration, false);
    }

    public static void fadeOutAndExit(Window window, int fadeOutDuration) {
        fadeOutAndEnd(window, fadeOutDuration, true);
    }
    /**
     * @param window
     * @param fadeOutDuration
     */
    private static void fadeOutAndEnd(final Window window, int fadeOutDuration, 
            final boolean exit) {
        Timeline dispose = new Timeline(new WindowFader(window));
        dispose.addPropertyToInterpolate("opacity", 1.0f,
//                AWTUtilitiesWrapper.getWindowOpacity(window), 
                0.0f);
        dispose.addCallback(new UIThreadTimelineCallbackAdapter() {
            @Override
            public void onTimelineStateChanged(TimelineState oldState,
                    TimelineState newState, float durationFraction,
                    float timelinePosition) {
                if (newState == TimelineState.DONE) {
                    if (exit) {
                        Runtime.getRuntime().exit(0);
                    } else {
                        window.dispose();
                    }
                }
            }
        });
        dispose.setDuration(fadeOutDuration);
        dispose.play();
    }
    
    public static class WindowFader {
        private Window window;
        
        public WindowFader(Window window) {
            this.window = window;
        }
        
        public void setOpacity(float opacity) {
            AWTUtilitiesWrapper.setWindowOpacity(window, opacity);
        }
    }

    private DemoUtils() {}
    
    
}
