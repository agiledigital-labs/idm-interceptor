package au.com.agiledigital.idm;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceScope;
import org.forgerock.http.routing.UriRouterContext;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.CreateRequest;
import org.forgerock.json.resource.Request;
import org.forgerock.json.resource.Requests;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.json.resource.ResourceResponse;
import org.forgerock.services.context.Context;
import org.forgerock.util.promise.Promise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.agiledigital.idm.connector.api.DummyConnectorApi;
import au.com.agiledigital.idm.interceptor.ConnectorRequestHandlerInterceptor;

@Component(scope = ServiceScope.SINGLETON)
public class SharedInterceptorState implements SharedInterceptorStateService {

	private static Logger logger = LoggerFactory.getLogger(SharedInterceptorState.class);

	private ConcurrentHashMap<String, ConnectorRequestHandlerInterceptor> interceptorMap = new ConcurrentHashMap<>();

	private ConcurrentHashMap<String, DummyConnectorApi> connectorMap = new ConcurrentHashMap<>();

	private String eventEndpoint;

	@Override
	public void registerInterceptor(String factoryPid, ConnectorRequestHandlerInterceptor interceptor) {
		interceptorMap.put(factoryPid, interceptor);
	}

	@Override
	public void unregisterInterceptor(String factoryPid, ConnectorRequestHandlerInterceptor interceptor) {
		interceptorMap.remove(factoryPid);
	}

	@Override
	public void registerConnector(String factoryPid, DummyConnectorApi dummyConnector) {
		connectorMap.put(factoryPid, dummyConnector);
	}

	@Override
	public void unregisterConnector(String factoryPid, DummyConnectorApi dummyConnector) {
		connectorMap.remove(factoryPid);
	}

	@Override
	public void setEventEndpoint(String factoryPid) {
		this.eventEndpoint = factoryPid;
	}

	@Override
	public void logEvent(String factoryPid, String eventType, Context context, Request request) {
		if (factoryPid.equals(this.eventEndpoint)) {
			// We don't want to log access to the event endpoint
			return;
		}

		String currentTimestamp = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
		JsonValue content = json(object(field("objectType", "system/" + factoryPid + "/" + request.getResourcePath()), field("requestTimestamp", currentTimestamp),field("request", request.toJsonValue())));
		CreateRequest eventRequest = Requests.newCreateRequest(eventType, content);

		UriRouterContext eventCreateContext = new UriRouterContext(context, "system/" + this.eventEndpoint,
				eventType, Collections.emptyMap());

		ConnectorRequestHandlerInterceptor connectorRequestHandlerInterceptor = this.interceptorMap
				.get(this.eventEndpoint);
		if (connectorRequestHandlerInterceptor == null) {
			throw new IllegalStateException(
					"Unable to find the event endpoint interceptor, is there an event endpoint registered?");
		}

		Promise<ResourceResponse, ResourceException> responsePromise = connectorRequestHandlerInterceptor.getUnderlyingService().handleCreate(eventCreateContext, eventRequest);
		responsePromise.thenOnException( e -> {
			logger.error("Failed to log event", e);
		});
	}
}
