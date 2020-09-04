package au.com.agiledigital.idm.connector.dummy;

import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.agiledigital.idm.SharedInterceptorStateService;
import au.com.agiledigital.idm.connector.api.DummyConnectorApi;

@ConnectorClass(displayNameKey = "DummyConnector", configurationClass = DummyConfiguration.class)
public class DummyConnector implements DummyConnectorApi, PoolableConnector, CreateOp, DeleteOp, SearchOp<String>, UpdateOp, TestOp {

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
