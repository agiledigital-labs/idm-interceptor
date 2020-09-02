package au.com.agiledigital.idm;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.ComponentInstance;

public class ComponentContextAdapter implements ComponentContext {
	
	BundleContext bundleContext;
	Dictionary<String, Object> properties;

	public ComponentContextAdapter(ServiceReference<?> ref) {
		super();
		this.bundleContext = ref.getBundle().getBundleContext();
		
		Hashtable<String, Object> properties = new Hashtable<String, Object>();
		for (String key : ref.getPropertyKeys()) {
			properties.put(key, ref.getProperty(key));
		}
		this.properties = properties;
	}

	@Override
	public void disableComponent(String arg0) {
		throw new IllegalStateException("Not implemented");
	}

	@Override
	public void enableComponent(String arg0) {
		throw new IllegalStateException("Not implemented");	}

	@Override
	public BundleContext getBundleContext() {
		return bundleContext;
	}

	@Override
	public ComponentInstance getComponentInstance() {
		throw new IllegalStateException("Not implemented");
	}

	@Override
	public Dictionary<String, Object> getProperties() {
		return this.properties;
	}

	@Override
	public ServiceReference<?> getServiceReference() {
		throw new IllegalStateException("Not implemented");
	}

	@Override
	public Bundle getUsingBundle() {
		throw new IllegalStateException("Not implemented");
	}

	@Override
	public Object locateService(String arg0) {
		throw new IllegalStateException("Not implemented");
	}

	@Override
	public <S> S locateService(String arg0, ServiceReference<S> arg1) {
		throw new IllegalStateException("Not implemented");
	}

	@Override
	public Object[] locateServices(String arg0) {
		throw new IllegalStateException("Not implemented");
	}
}
