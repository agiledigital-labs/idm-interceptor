package au.com.agiledigital.idm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.dm.annotation.api.AspectService;
import org.forgerock.json.resource.ActionRequest;
import org.forgerock.json.resource.ActionResponse;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.DeleteRequest;
import org.forgerock.json.resource.PatchRequest;
import org.forgerock.json.resource.QueryRequest;
import org.forgerock.json.resource.QueryResourceHandler;
import org.forgerock.json.resource.QueryResponse;
import org.forgerock.json.resource.ReadRequest;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AspectService(ranking = 10, added = "added", changed = "changed", removed = "removed", filter = "(&(objectClass=org.forgerock.json.resource.RequestHandler)(service.factoryPid=org.forgerock.openidm.provisioner.openicf))")
public class RequestHandlerInterceptor implements RequestHandler {
	// The service we are intercepting (injected by reflection)
	private volatile RequestHandler intercepted;
	private String factoryPid;

	private static Logger logger = LoggerFactory.getLogger(RequestHandlerInterceptor.class);

	private Map<String, RequestHandler> nameConnectors = new ConcurrentHashMap<>();
	private Map<RequestHandler, String> handlerConnectors = new ConcurrentHashMap<>();

	public void added(ServiceReference<RequestHandler> ref, RequestHandler service) {
		String factoryPid = (String) ref.getProperty("config.factory-pid");
		nameConnectors.put(factoryPid, service);
		handlerConnectors.put(service, factoryPid);
		this.factoryPid = factoryPid;
		this.intercepted = service;

		logger.info("Added Service {}", factoryPid);
	}

	public void changed(ServiceReference<RequestHandler> ref, RequestHandler service) {
		String factoryPid = (String) ref.getProperty("config.factory-pid");
		RequestHandler originalHandler = nameConnectors.remove(factoryPid);
		handlerConnectors.remove(originalHandler);
		nameConnectors.put(factoryPid, service);
		handlerConnectors.put(service, factoryPid);
		this.factoryPid = factoryPid;
		this.intercepted = service;
		
		logger.info("Changed Service {}", factoryPid);
	}
	
	public void removed(ServiceReference<RequestHandler> ref, RequestHandler service) {
		String factoryPid = (String) ref.getProperty("config.factory-pid");
		RequestHandler originalHandler = nameConnectors.remove(factoryPid);
		handlerConnectors.remove(originalHandler);
		
		logger.info("Removed Service {}", factoryPid);
	}

	public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
		String factoryPid = handlerConnectors.get(intercepted);
		logger.info("About to execute action for {}", factoryPid);
		return intercepted.handleAction(context, request);
	}

	public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
		String factoryPid = handlerConnectors.get(intercepted);
		logger.info("About to Read for {}", factoryPid);
		Promise<ResourceResponse, ResourceException> handleRead = intercepted.handleRead(context, request);
		return handleRead;
	}
	
	@Override
	public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
		// TODO Auto-generated method stub
		return intercepted.handleCreate(context, request);
	}
	
	@Override
	public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
		// TODO Auto-generated method stub
		return intercepted.handleDelete(context, request);
	}
	
	@Override
	public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
		// TODO Auto-generated method stub
		return intercepted.handlePatch(context, request);
	}
	
	@Override
	public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
			QueryResourceHandler handler) {
		// TODO Auto-generated method stub
		return intercepted.handleQuery(context, request, handler);
	}
	
	@Override
	public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
		// TODO Auto-generated method stub
		return intercepted.handleUpdate(context, request);
	}
}
