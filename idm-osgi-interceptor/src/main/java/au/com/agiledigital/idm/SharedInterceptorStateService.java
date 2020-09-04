package au.com.agiledigital.idm;

import org.forgerock.json.resource.Request;
import org.forgerock.services.context.Context;

import au.com.agiledigital.idm.connector.api.DummyConnectorApi;
import au.com.agiledigital.idm.interceptor.ConnectorRequestHandlerInterceptor;

public interface SharedInterceptorStateService {

	void registerInterceptor(String factoryPid, ConnectorRequestHandlerInterceptor interceptor);

	void unregisterInterceptor(String factoryPid, ConnectorRequestHandlerInterceptor interceptor);

	void registerConnector(String factoryPid, DummyConnectorApi dummyConnector);

	void unregisterConnector(String factoryPid, DummyConnectorApi dummyConnector);

	void setEventEndpoint(String factoryPid);

	void logEvent(String factoryPid, String eventType, Context context, Request request);
}