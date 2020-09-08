package au.com.agiledigital.idm.connector.dummy;

import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.identityconnectors.common.script.ScriptExecutor;
import org.identityconnectors.common.script.ScriptExecutorFactory;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.PreconditionFailedException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.identityconnectors.framework.spi.operations.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.agiledigital.idm.SharedInterceptorStateService;
import au.com.agiledigital.idm.connector.api.DummyConnectorApi;

@ConnectorClass(displayNameKey = "DummyConnector", configurationClass = DummyConfiguration.class)
public class DummyConnector implements DummyConnectorApi, PoolableConnector, CreateOp, DeleteOp, SearchOp<String>, UpdateOp, TestOp, ScriptOnConnectorOp, ScriptOnResourceOp {

	private ConcurrentMap<ObjectClass, ConcurrentMap<Uid, Set<Attribute>>> data = new ConcurrentHashMap<>();

	private DummyConfiguration _configuration = null;

	private String factoryPid;

	private static Logger logger = LoggerFactory.getLogger(DummyConnector.class);

	private SharedInterceptorStateService sharedState;

	public DummyConnector() {

	}

	public void dispose() {
		this.sharedState.unregisterConnector(this.factoryPid, this);
	}

	public void checkAlive() {
	}

	private ConcurrentMap<Uid, Set<Attribute>> getObjectClassMap(ObjectClass objectClass) {
		ConcurrentMap<Uid, Set<Attribute>> classMap = data.computeIfAbsent(objectClass,
				key -> new ConcurrentHashMap<>());

		return classMap;
	}

	public Configuration getConfiguration() {
		return _configuration;
	}

	public void init(Configuration cfg) {
		_configuration = (DummyConfiguration) cfg;

		if (_configuration.getDummyConnectorFactoryPid() == null) {
			logger.warn("Factory pid is missing cannot fully initialise");
		} else {

			// Lookup the shared state service
			BundleContext context = FrameworkUtil.getBundle(SharedInterceptorStateService.class).getBundleContext();
			ServiceReference<SharedInterceptorStateService> reference = context
					.getServiceReference(SharedInterceptorStateService.class);
			this.sharedState = context.getService(reference);

			if (this.sharedState == null) {
				throw new IllegalStateException("Shared Interceptor State instance is missing");
			}

			this.factoryPid = _configuration.getDummyConnectorFactoryPid();

			this.sharedState.registerConnector(this.factoryPid, this);

			logger.info("Initialising the dummy connector with factory pid: {}", this.factoryPid);
		}
	}

	public Uid create(ObjectClass objClass, Set<Attribute> attrs, OperationOptions options) {
		ConcurrentMap<Uid, Set<Attribute>> objectClassMap = getObjectClassMap(objClass);

		// Find a Uid or generate one.
		Uid uid = attrs.stream().filter(attr -> attr.getName().equals(Uid.NAME))
				.map(attr -> new Uid(AttributeUtil.getAsStringValue(attr))).findFirst()
				.orElse(new Uid(UUID.randomUUID().toString()));
		Set<Attribute> existingAttrs = objectClassMap.putIfAbsent(uid, attrs);

		if (existingAttrs != null) {
			throw new AlreadyExistsException(String.format("%s: %s: %s already exists.", this.factoryPid,
					objClass.toString(), uid.getUidValue()));
		}

		return uid;
	}

	public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
		ConcurrentMap<Uid, Set<Attribute>> objectClassMap = getObjectClassMap(objClass);
		Set<Attribute> deleted = objectClassMap.remove(uid);
		if (deleted == null) {
			throw new UnknownUidException(uid, objClass);
		}
	}

	public FilterTranslator<String> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
		return new DummyFilterTranslator();
	}

	public void executeQuery(ObjectClass objClass, String query, ResultsHandler handler, OperationOptions options) {
		ConnectorObjectBuilder builder;
		ConcurrentMap<Uid, Set<Attribute>> objectClassMap = getObjectClassMap(objClass);
		for (Entry<Uid, Set<Attribute>> result : objectClassMap.entrySet()) {
			builder = new ConnectorObjectBuilder();
			builder.addAttributes(result.getValue());
			builder.setUid(result.getKey());

			handler.handle(builder.build());
		}

		if (handler instanceof SearchResultsHandler && objectClassMap.entrySet().size() > -1) {
			SearchResult searchResult = new SearchResult();
			((SearchResultsHandler) handler).handleResult(searchResult);
		}
	}

	public Uid update(ObjectClass objClass, Uid uid, Set<Attribute> attrs, OperationOptions options) {
		ConcurrentMap<Uid, Set<Attribute>> objectClassMap = getObjectClassMap(objClass);

		Set<Attribute> object = objectClassMap.get(uid);
		if (object == null) {
			throw new UnknownUidException(uid, objClass);
		}

		Map<String, Attribute> attrMap = new HashMap<String, Attribute>(AttributeUtil.toMap(object));

		attrMap.remove(OperationalAttributeInfos.CURRENT_PASSWORD.getName());
		object.addAll(attrMap.values());

		return uid;
	}

	@Override
	public Object runScriptOnConnector(ScriptContext scriptContext, OperationOptions operationOptions) {
		logger.info("Running script on connector");
		return runScript(scriptContext, operationOptions);
	}

	private Object runScript(ScriptContext scriptContext, OperationOptions operationOptions) {
		if ("fixed".equals(scriptContext.getScriptLanguage())) {
			return runFixedAction(scriptContext, operationOptions);
		} else {
			return executeScript(scriptContext, operationOptions);
		}
	}

	@Override
	public Object runScriptOnResource(ScriptContext scriptContext, OperationOptions operationOptions) {
		logger.info("Running script on resource");
		return runScript(scriptContext, operationOptions);
	}

	private Object runFixedAction(ScriptContext scriptContext, OperationOptions operationOptions) {
		switch(FixedAction.valueOf(scriptContext.getScriptText())) {
			case CLEAR_DATA:
				return this.clearData();
			case CLEAR_ALL_DUMMY_CONNECTOR_DATA:
				return this.sharedState.clearAllConnectorData();
			default:
				throw new PreconditionFailedException("Unsupported FixedAction: " + scriptContext.getScriptText());
		}
	}

	@Override
	public String clearData() {
		int numElements = this.data.entrySet().stream().mapToInt(e -> e.getValue().size()).reduce(0, (subtotal, size) -> subtotal + size);
		this.data.clear();
		return "Removed " + numElements;
	}

	private Object executeScript(ScriptContext request, OperationOptions operationOptions) {
		ScriptExecutorFactory factory = ScriptExecutorFactory.newInstance(request.getScriptLanguage());
		ScriptExecutor executor = factory.newScriptExecutor(this.getClass().getClassLoader(), request.getScriptText(), true);
		Map<String, Object> bindings = new HashMap();
		for (Entry<String, Object> arg : request.getScriptArguments().entrySet()) {
			bindings.put(arg.getKey(), arg.getValue());
		}

		bindings.put("connector", this);

		try {
			return executor.execute(bindings);
		} catch (Exception ex) {
			logger.error("Failed to execute Script", ex);
			throw ConnectorException.wrap(ex);
		}
	}



	static class MethodComparator implements Comparator<Method> {
		public int compare(Method o1, Method o2) {
			return o1.getName().compareTo(o2.getName());
		}

	}

	@Override
	public void test() {
		// Nothing to do
	}

}

enum FixedAction {
	CLEAR_DATA,
	CLEAR_ALL_DUMMY_CONNECTOR_DATA
}