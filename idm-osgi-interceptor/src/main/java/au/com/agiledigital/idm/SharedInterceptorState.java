package au.com.agiledigital.idm;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceScope;

import au.com.agiledigital.idm.connector.api.DummyConnectorApi;

@Component(scope = ServiceScope.SINGLETON)
public class SharedInterceptorState implements SharedInterceptorStateService {

	private ConcurrentHashMap<String, DummyConnectorApi> connectorMap = new ConcurrentHashMap<>();

	private String eventEndpoint;

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
	public Optional<String> getEventEndpoint() {
		return Optional.ofNullable(this.eventEndpoint);
	}

	@Override
	public Map<String, String> clearAllConnectorData() {
		Map<String, String> results = new ConcurrentHashMap<>();
		this.connectorMap.entrySet().parallelStream().forEach(entry -> results.put(entry.getKey(), entry.getValue().clearData()));
		return results;
	}
}
