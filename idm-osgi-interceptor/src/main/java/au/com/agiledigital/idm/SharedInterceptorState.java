package au.com.agiledigital.idm;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceScope;

import au.com.agiledigital.idm.connector.api.DummyConnectorApi;
import au.com.agiledigital.idm.interceptor.ConnectorRequestHandlerInterceptor;

@Component(scope = ServiceScope.SINGLETON)
public class SharedInterceptorState implements SharedInterceptorStateService {
	
	private ConcurrentHashMap<String, ConnectorRequestHandlerInterceptor> interceptorMap = new ConcurrentHashMap<>();

	private ConcurrentHashMap<String, DummyConnectorApi> connectorMap = new ConcurrentHashMap<>();

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
}
