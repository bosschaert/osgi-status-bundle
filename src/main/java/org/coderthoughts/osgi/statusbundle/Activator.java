package org.coderthoughts.osgi.statusbundle;

import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
	private FrameworkListener listener;
	private AtomicBoolean reported = new AtomicBoolean(false);

    public void start(final BundleContext bundleContext) throws Exception {
        reported.set(false);

        listener = new FrameworkListener() {
            public void frameworkEvent(FrameworkEvent event) {
                if (event.getType() == FrameworkEvent.STARTED) {
                    try {
                        reportStatus(bundleContext);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
	    bundleContext.addFrameworkListener(listener);

	    if (bundleContext.getBundle(0).getState() == Bundle.ACTIVE)
	        reportStatus(bundleContext);
	}

	private void reportStatus(BundleContext bundleContext) throws Exception {
	    // There is a window of opportunity where this method can be called twice in a row
	    // This can happen when the listener fires that the Framework was started *and* the
	    // system bundle was active when the start method checked above.
	    // If this is the case we want to return as there is no need for two reports.
	    if (reported.getAndSet(true))
	        // has already been reported
	        return;

	    ServiceTracker st = new ServiceTracker(bundleContext, StartLevel.class.getName(), null);
	    st.open();
	    StartLevel sl = (StartLevel) st.waitForService(5000);
	    System.out.println("Framework Start Level: " + sl.getStartLevel());
	    listBundles(bundleContext, sl);
	    st.close();

	    // Make it an optional import
	    ServiceTracker st2 = new ServiceTracker(bundleContext, ConfigurationAdmin.class.getName(), null);
	    st2.open();
	    ConfigurationAdmin cas = (ConfigurationAdmin) st2.waitForService(100);
	    if (cas != null)
	        listConfigurations(cas);
	    st2.close();
	}

    private void listBundles(BundleContext bc, StartLevel sl) {
	    for (Bundle b : bc.getBundles()) {
	        System.out.printf("Bundle %d %s:%s (start level %d)\n",
	                b.getBundleId(),
	                b.getSymbolicName(),
	                getBundleState(b),
	                sl.getBundleStartLevel(b));
	    }
    }

    private String getBundleState(Bundle b) {
        switch (b.getState()) {
        case Bundle.ACTIVE:
            return "ACTIVE";
        case Bundle.INSTALLED:
            return "INSTALLED";
        case Bundle.RESOLVED:
            return "RESOLVED";
        case Bundle.STARTING:
            return "STARTING";
        case Bundle.STOPPING:
            return "STOPPING";
        case Bundle.UNINSTALLED:
            return "UNINSTALLED";
        default:
            return "unknown";
        }
    }

    private void listConfigurations(ConfigurationAdmin cas) throws Exception {
        Configuration[] configurations = cas.listConfigurations(null);
        if (configurations == null)
            return;

        for (Configuration c : configurations) {
            System.out.println("Configuration PID: " + c.getPid());

        }
    }

    public void stop(BundleContext bundleContext) throws Exception {
        bundleContext.removeFrameworkListener(listener);
        reported.set(false);
	}
}
