package au.com.agiledigital.idm;

import au.com.agiledigital.idm.connector.api.DummyConnectorApi;

import java.util.Map;
import java.util.Optional;

public interface SharedInterceptorStateService {

	void registerConnector(String factoryPid, DummyConnectorApi dummyConnector);

	void unregisterConnector(String factoryPid, DummyConnectorApi dummyConnector);

	void setEventEndpoint(String factoryPid);

	Optional<String> getEventEndpoint();

	Map<String, String> clearAllConnectorData();
}