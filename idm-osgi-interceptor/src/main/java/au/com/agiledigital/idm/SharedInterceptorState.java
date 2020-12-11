package au.com.agiledigital.idm;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.dm.annotation.api.Component;
import org.apache.felix.dm.annotation.api.ServiceScope;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;

import au.com.agiledigital.idm.connector.api.DummyConnectorApi;

@Component(scope = ServiceScope.SINGLETON)
public class SharedInterceptorState implements SharedInterceptorStateService {

	private ConcurrentHashMap<String, ConcurrentMap<DummyConnectorApi, Boolean>> connectorMap = new ConcurrentHashMap<>();
	private ConcurrentHashMap<String, ConcurrentMap<ObjectClass, ConcurrentMap<Uid, Set<Attribute>>>> connectorData = new ConcurrentHashMap<>();

	private String eventEndpoint;

	@Override
	public ConcurrentMap<ObjectClass, ConcurrentMap<Uid, Set<Attribute>>> registerConnector(String factoryPid,
			DummyConnectorApi dummyConnector) {
		connectorMap.computeIfAbsent(factoryPid, key -> new ConcurrentHashMap<>()).put(dummyConnector, true);

		// We create the connector data map here so that state isn't lost when using a
		// poolable connector across multiple threads.
		return connectorData.computeIfAbsent(factoryPid, key -> new ConcurrentHashMap<>());
	}

	@Override
	public void unregisterConnector(String factoryPid, DummyConnectorApi dummyConnector) {
		connectorMap.get(factoryPid).remove(dummyConnector);
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
		
		// Because the connectors use shared state, where we have a pool of connectors, we can simply use the first in the set. 
		this.connectorMap.entrySet().parallelStream().filter(entry -> entry.getValue().size() > 0)
				.forEach(entry -> results.put(entry.getKey(), entry.getValue().keySet().iterator().next().clearData()));
		return results;
	}
}
