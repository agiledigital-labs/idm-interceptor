package au.com.agiledigital.idm;

import java.util.Optional;

import au.com.agiledigital.idm.connector.api.DummyConnectorApi;

public interface SharedInterceptorStateService {

	void registerConnector(String factoryPid, DummyConnectorApi dummyConnector);

	void unregisterConnector(String factoryPid, DummyConnectorApi dummyConnector);

	void setEventEndpoint(String factoryPid);

	Optional<String> getEventEndpoint();
}