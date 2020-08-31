package au.com.agiledigital.idm.interceptor;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.object;

import java.util.List;

import org.apache.felix.dm.annotation.api.AspectService;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.RequestHandler;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openidm.provisioner.openicf.ConnectorInfoProvider;
import org.forgerock.openidm.provisioner.openicf.ConnectorReference;
import org.forgerock.openidm.provisioner.openicf.commons.ConnectorUtil;
import org.forgerock.util.promise.Promise;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AspectService(ranking = 10, added = "added", changed = "changed", removed = "removed")
public class ConnectorInfoProviderInterceptor implements ConnectorInfoProvider {

	private volatile ConnectorInfoProvider intercepted;

	private ConnectorReference dummyConnectorReference = createDummyConnectorReference();

	private static Logger logger = LoggerFactory.getLogger(ConnectorInfoProviderInterceptor.class);

	private ConnectorReference createDummyConnectorReference() {

		JsonValue dummyJson = JsonValue.json(object(field("connectorRef",
				object(field("bundleName", "org.forgerock.openicf.connectors.dummy-connector"),
						field("bundleVersion", "[0.0.0,2)"),
						field("connectorName", "org.identityconnectors.dummy.DummyConnector")))));

		return ConnectorUtil.getConnectorReference(dummyJson);
	}

	public void added(ServiceReference<RequestHandler> ref, ConnectorInfoProvider service) {
		this.intercepted = service;

		logger.info("Added Service Interceptor for {}", service.getClass());
	}

	public void changed(ServiceReference<RequestHandler> ref, ConnectorInfoProvider service) {
		this.intercepted = service;

		logger.info("Changed Service Interceptor for {}", service.getClass());
	}

	public void removed(ServiceReference<RequestHandler> ref, ConnectorInfoProvider service) {
		this.intercepted = null;

		logger.info("Removed Service Interceptor for {}", service.getClass());
	}

	@Override
	public ConnectorFacade createConnectorFacade(APIConfiguration paramAPIConfiguration) {
		// Pass-through
		return this.intercepted.createConnectorFacade(paramAPIConfiguration);
	}

	@Override
	public JsonValue createSystemConfiguration(ConnectorReference paramConnectorReference,
			APIConfiguration paramAPIConfiguration) throws ResourceException {
		// Pass-through
		return null;
	}

	@Override
	public ConnectorInfo findConnectorInfo(ConnectorReference paramConnectorReference) {
		// Pass-through
		return null;
	}

	@Override
	public Promise<ConnectorInfo, RuntimeException> findConnectorInfoAsync(ConnectorReference paramConnectorReference) {
		// Override connector info lookup with the dummy connector instead.
		logger.info("Redirecting ConnectorReference\n{}\nto Dummy:\n{}", paramConnectorReference.toString(),
				dummyConnectorReference.toString());
		return this.intercepted.findConnectorInfoAsync(this.dummyConnectorReference);
	}

	@Override
	public List<ConnectorInfo> getAllConnectorInfo() {
		// Pass-through
		return this.getAllConnectorInfo();
	}

	@Override
	public void testConnector(APIConfiguration paramAPIConfiguration) throws ResourceException {
		// Pass-through
		this.intercepted.testConnector(paramAPIConfiguration);
	}

}
