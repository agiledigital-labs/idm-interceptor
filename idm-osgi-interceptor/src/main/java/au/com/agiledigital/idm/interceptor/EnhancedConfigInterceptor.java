package au.com.agiledigital.idm.interceptor;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.util.Dictionary;
import java.util.Map;

import au.com.agiledigital.idm.SharedInterceptorStateService;
import org.apache.felix.dm.annotation.api.AspectService;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.forgerock.config.util.JsonValuePropertyEvaluator;
import org.forgerock.json.JsonPointer;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.openidm.config.enhanced.EnhancedConfig;
import org.forgerock.openidm.config.enhanced.InternalErrorException;
import org.forgerock.openidm.config.enhanced.InvalidException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AspectService(ranking = 10, added = "added", changed = "changed", removed = "removed")
public class EnhancedConfigInterceptor implements EnhancedConfig {

	private volatile EnhancedConfig intercepted;

	@ServiceDependency
	private SharedInterceptorStateService sharedState;
	
	private static Logger logger = LoggerFactory.getLogger(EnhancedConfigInterceptor.class);
	
	private Map<String, Object> dummyConnectorRef  = object(field("bundleName", "au.com.agiledigital.idm.connector.dummy-connector"),
			field("bundleVersion", "[0.0.0,2)"),
			field("connectorName", "au.com.agiledigital.idm.connector.dummy.DummyConnector"));
	
	public void added(ServiceReference<RequestHandler> ref, EnhancedConfig service) {
		this.intercepted = service;

		logger.info("Added Service Interceptor for {}", service.getClass());
	}

	public void changed(ServiceReference<RequestHandler> ref, EnhancedConfig service) {
		this.intercepted = service;

		logger.info("Changed Service Interceptor for {}", service.getClass());
	}

	public void removed(ServiceReference<RequestHandler> ref, EnhancedConfig service) {
		this.intercepted = null;

		logger.info("Removed Service Interceptor for {}", service.getClass());
	}

	@Override
	public JsonValue getConfiguration(Dictionary<String, Object> dict, BundleContext context, String servicePid)
			throws InvalidException, InternalErrorException {
		// Pass-through
		return this.intercepted.getConfiguration(dict, context, servicePid);
	}

	@Override
	public JsonValue getConfiguration(Dictionary<String, Object> dict, String servicePid, boolean decrypt)
			throws InvalidException, InternalErrorException {
		// Pass-through
		return this.intercepted.getConfiguration(dict, servicePid, decrypt);
	}

	@Override
	public JsonValue getConfigurationAsJson(ComponentContext context) throws InvalidException, InternalErrorException {
		String factoryPid = (String) context.getProperties().get("config.factory-pid");
		JsonValue configuration = this.intercepted.getConfigurationAsJson(context);

		// If connector is configured to bypass, then we won't replace the implementation with the dummy connector.
		JsonValue bypassValue = configuration.get(JsonPointer.ptr("/dummyConnectorProperties/bypass"));
		if (bypassValue != null && bypassValue.asBoolean()) {
			return configuration;
		}

		// Override the connectorRef with the dummy connector
		configuration.put("connectorRef", dummyConnectorRef);
		
		JsonValue configProps = configuration.get("configurationProperties");
		if (configProps.isNull()) {
			configProps = json(object(1));
			configuration.put("configurationProperties", configProps);
		}
		// Store the factory pid inside the json configuration, so the dummy connector
		// knows which instance it is, which makes it more useful for logging.
		configProps.put("dummyConnectorFactoryPid", factoryPid);

		JsonValue resultsHandlerConfig = configuration.get("resultsHandlerConfig");
		if (resultsHandlerConfig.isNull()) {
			resultsHandlerConfig = json(object(1));
			configuration.put("resultsHandlerConfig", resultsHandlerConfig);
		}

		// The dummy connector doesn't support filtering and returns all results, so we need to turn on filtering
		// from the results handler
		resultsHandlerConfig.put("enableFilteredResultsHandler", true);

		// If this is the event endpoint, then register it with the shared state.
		JsonValue eventValue = configuration.get(JsonPointer.ptr("/dummyConnectorProperties/eventEndpoint"));
		if (eventValue != null && eventValue.asBoolean()) {
			sharedState.setEventEndpoint(factoryPid);
		}

		return configuration;
	}

	@Override
	public String getConfigurationFactoryPid(ComponentContext compContext) {
		// Pass-through
		return this.intercepted.getConfigurationFactoryPid(compContext);
	}

	@Override
	public JsonValuePropertyEvaluator getJsonValuePropertyEvaluator() {
		// Pass-through
		return this.intercepted.getJsonValuePropertyEvaluator();
	}

	@Override
	public JsonValue getRawConfiguration(Dictionary<String, Object> dict, String servicePid) throws InvalidException {
		// Pass-through
		return this.intercepted.getRawConfiguration(dict, servicePid);
	}

}
