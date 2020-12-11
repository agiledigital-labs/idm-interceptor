package au.com.agiledigital.idm;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;

import au.com.agiledigital.idm.connector.api.DummyConnectorApi;

public interface SharedInterceptorStateService {

	ConcurrentMap<ObjectClass, ConcurrentMap<Uid, Set<Attribute>>> registerConnector(String factoryPid, DummyConnectorApi dummyConnector);

	void unregisterConnector(String factoryPid, DummyConnectorApi dummyConnector);

	void setEventEndpoint(String factoryPid);

	Optional<String> getEventEndpoint();

	Map<String, String> clearAllConnectorData();
}