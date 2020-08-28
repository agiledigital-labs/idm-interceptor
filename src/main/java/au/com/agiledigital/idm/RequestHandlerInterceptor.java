package au.com.agiledigital.idm;

import java.util.Map;
import java.util.Set;

import org.apache.felix.dm.annotation.api.AspectService;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.forgerock.json.JsonValue;
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
import org.forgerock.json.resource.ResourcePath;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.json.resource.UpdateRequest;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.crypto.CryptoService;
import org.forgerock.openidm.provisioner.openicf.commons.ConnectorUtil;
import org.forgerock.openidm.provisioner.openicf.commons.ObjectClassInfoHelper;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.identityconnectors.framework.common.objects.Attribute;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AspectService(ranking = 10, added = "added", changed = "changed", removed = "removed", filter = "(&(objectClass=org.forgerock.json.resource.RequestHandler)(service.factoryPid=org.forgerock.openidm.provisioner.openicf))")
public class RequestHandlerInterceptor implements RequestHandler {
	private volatile RequestHandler intercepted;
	private volatile String factoryPid;
	@ServiceDependency
	private volatile EnhancedConfig enhancedConfig;
	
	@ServiceDependency
	private CryptoService cryptoService;
	
	private Map<String, ObjectClassInfoHelper> objectClassInfoMap;

	private static Logger logger = LoggerFactory.getLogger(RequestHandlerInterceptor.class);
	
	private Map<String, ObjectClassInfoHelper> generateObjectClassInfo(ServiceReference<?> ref) {
		JsonValue jsonConfig = this.enhancedConfig.getConfigurationAsJson(new ComponentContextAdapter(ref));
		Map<String, ObjectClassInfoHelper> objectTypes = ConnectorUtil.getObjectTypes(jsonConfig);
		
		return objectTypes;
	}
	
	private ObjectClassInfoHelper getObjectClassInfoHelper(ResourcePath resourcePath) {
		return objectClassInfoMap.get(resourcePath.get(0));
	}

	public void added(ServiceReference<RequestHandler> ref, RequestHandler service) {
		String factoryPid = (String) ref.getProperty("config.factory-pid");
		this.objectClassInfoMap = generateObjectClassInfo(ref);
		this.factoryPid = factoryPid;
		this.intercepted = service;

		logger.info("Added Service {}", factoryPid);
	}

	public void changed(ServiceReference<RequestHandler> ref, RequestHandler service) {
		String factoryPid = (String) ref.getProperty("config.factory-pid");
		this.objectClassInfoMap = generateObjectClassInfo(ref);
		this.factoryPid = factoryPid;
		this.intercepted = service;
		
		logger.info("Changed Service {}", factoryPid);
	}
	
	public void removed(ServiceReference<RequestHandler> ref, RequestHandler service) {
		String factoryPid = (String) ref.getProperty("config.factory-pid");
		this.intercepted = null;
		this.factoryPid = null;
		this.objectClassInfoMap = null;
		
		logger.info("Removed Service {}", factoryPid);
	}
	
	private void ensureHasValue() {
		if (intercepted == null || factoryPid == null) {
			throw new IllegalStateException("The intercepted instance is missing, cannot proceed");
		}
	}

	@Override
	public Promise<ActionResponse, ResourceException> handleAction(Context context, ActionRequest request) {
		ensureHasValue();
		logger.info("About to execute action for {}", factoryPid);
		return intercepted.handleAction(context, request);
	}

	@Override
	public Promise<ResourceResponse, ResourceException> handleRead(Context context, ReadRequest request) {
		ensureHasValue();
		logger.info("About to read for {}", factoryPid);
		Promise<ResourceResponse, ResourceException> handleRead = intercepted.handleRead(context, request);
		return handleRead;
	}
	
	@Override
	public Promise<ResourceResponse, ResourceException> handleCreate(Context context, CreateRequest request) {
		ensureHasValue();
		logger.info("About to create for {}", factoryPid);
		
		ObjectClassInfoHelper helper = getObjectClassInfoHelper(request.getResourcePathObject());
		
		try {
			Set<Attribute> createAttributes = helper.getCreateAttributes(request, this.cryptoService);
			logger.info("Create attributes {}", createAttributes);
		} catch (ResourceException e) {
			return e.asPromise();
		}
		
		return intercepted.handleCreate(context, request);
	}
	
	@Override
	public Promise<ResourceResponse, ResourceException> handleDelete(Context context, DeleteRequest request) {
		ensureHasValue();
		logger.info("About to delete for {}", factoryPid);
		return intercepted.handleDelete(context, request);
	}
	
	@Override
	public Promise<ResourceResponse, ResourceException> handlePatch(Context context, PatchRequest request) {
		ensureHasValue();
		logger.info("About to Patch for {}", factoryPid);
		return intercepted.handlePatch(context, request);
	}
	
	@Override
	public Promise<QueryResponse, ResourceException> handleQuery(Context context, QueryRequest request,
			QueryResourceHandler handler) {
		ensureHasValue();
		logger.info("About to Query for {}", factoryPid);
		return intercepted.handleQuery(context, request, handler);
	}
	
	@Override
	public Promise<ResourceResponse, ResourceException> handleUpdate(Context context, UpdateRequest request) {
		ensureHasValue();
		logger.info("About to Update for {}", factoryPid);
		return intercepted.handleUpdate(context, request);
	}
}
