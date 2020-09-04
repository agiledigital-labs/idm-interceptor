package au.com.agiledigital.idm.interceptor;

import org.apache.felix.dm.annotation.api.AspectService;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.agiledigital.idm.EventType;
import au.com.agiledigital.idm.SharedInterceptorStateService;

@AspectService(ranking = 1, added = "added", changed = "changed", removed = "removed", filter = "(&(objectClass=org.forgerock.json.resource.RequestHandler)(service.factoryPid=org.forgerock.openidm.provisioner.openicf))")
public class ConnectorRequestHandlerInterceptor implements RequestHandler {
	private volatile RequestHandler intercepted;
	private volatile String factoryPid;
	@ServiceDependency
	private volatile EnhancedConfig enhancedConfig;
	
	@ServiceDependency
	private CryptoService cryptoService;
	
	@ServiceDependency
	private SharedInterceptorStateService sharedState;
	
	private static Logger logger = LoggerFactory.getLogger(ConnectorRequestHandlerInterceptor.class);
	
	public RequestHandler getUnderlyingService() {
		return this.intercepted;
	}

	public void added(ServiceReference<RequestHandler> ref, RequestHandler service) {
		String factoryPid = (String) ref.getProperty("config.factory-pid");
		this.factoryPid = factoryPid;
		this.intercepted = service;
		this.sharedState.registerInterceptor(factoryPid, this);

		logger.info("Added Service {}", factoryPid);
	}

	public void changed(ServiceReference<RequestHandler> ref, RequestHandler service) {
		String factoryPid = (String) ref.getProperty("config.factory-pid");
		this.factoryPid = factoryPid;
		this.intercepted = service;
		
		logger.info("Changed Service {}", factoryPid);
	}
	
	public void removed(ServiceReference<RequestHandler> ref, RequestHandler service) {
		String factoryPid = (String) ref.getProperty("config.factory-pid");
		this.intercepted = null;
		this.factoryPid = null;
		
		this.sharedState.unregisterInterceptor(factoryPid, this);
		
		logger.info("Removed Service {}", factoryPid);
	}
	
	private void ensureHasValue() {
		if (intercepted == null || factoryPid == null) {
			throw new IllegalStateException("The intercepted instance is missing, cannot proceed");
		}
	}

	private void logEvent(EventType eventType, Context context, Request request) {
		this.sharedState.logEvent(this.factoryPid, eventType.getName(), context, request);
	}

	@Override
	public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
		ensureHasValue();
		logger.info("About to execute action for {}", factoryPid);
		this.logEvent(EventType.ACTION, context, request);
		return intercepted.handleAction(context, request);
	}

	@Override
	public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
		ensureHasValue();
		logger.info("About to read for {}", factoryPid);
		this.logEvent(EventType.READ, context, request);
		return intercepted.handleRead(context, request);
	}
	
	@Override
	public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
		ensureHasValue();
		logger.info("About to create for {}", factoryPid);
		this.logEvent(EventType.CREATE, context, request);

		return intercepted.handleCreate(context, request);
	}
	
	@Override
	public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
		ensureHasValue();
		logger.info("About to delete for {}", factoryPid);
		this.logEvent(EventType.DELETE, context, request);
		return intercepted.handleDelete(context, request);
	}
	
	@Override
	public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
		ensureHasValue();
		logger.info("About to Patch for {}", factoryPid);
		this.logEvent(EventType.PATCH, context, request);
		return intercepted.handlePatch(context, request);
	}
	
	@Override
	public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
			QueryResourceHandler handler) {
		ensureHasValue();
		logger.info("About to Query for {}", factoryPid);
		this.logEvent(EventType.QUERY, context, request);
		return intercepted.handleQuery(context, request, handler);
	}
	
	@Override
	public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
		ensureHasValue();
		logger.info("About to Update for {}", factoryPid);
		this.logEvent(EventType.UPDATE, context, request);
		return intercepted.handleUpdate(context, request);
	}
}
