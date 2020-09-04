package au.com.agiledigital.idm.filter;

import static org.forgerock.json.JsonValue.field;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.json.JsonValue.object;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.*;
import org.forgerock.openidm.router.RouterFilterRegistration;
import org.forgerock.services.context.Context;
import org.forgerock.util.Reject;
import org.forgerock.util.promise.Promise;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.agiledigital.idm.EventType;
import au.com.agiledigital.idm.SharedInterceptorStateService;

@Component(name = ConnectorLoggerFilter.PID, policy = ConfigurationPolicy.IGNORE, immediate = true)
@Service
@Properties({ @Property(name = "service.vendor", value = { "Agile Digital" }),
		@Property(name = "service.description", value = {
				"" + "Connector Logger which logs to the event endpoint connector" }) })
public class ConnectorLoggerFilter implements Filter {

	static final String PID = "au.com.agiledigital.idm.filter";
	private static final Logger logger = LoggerFactory.getLogger(ConnectorLoggerFilter.class);

	private ServiceTracker<ConnectionFactory, ConnectionFactory> serviceTracker = null;
	ConnectionFactory connectionFactory = null;

	@Reference
	private RouterFilterRegistration filterRego;

	@Reference
	private SharedInterceptorStateService sharedState;

	private static final Connection NULL_CONNECTION = new AbstractAsynchronousConnection() {
		public Promise<ActionResponse, ResourceException> actionAsync(Context context, ActionRequest request) {
			return null;
		}

		public void close() {
		}

		public Promise<ResourceResponse, ResourceException> createAsync(Context context, CreateRequest request) {
			ConnectorLoggerFilter.logger
					.error("No connection factory to log connector traffic : " + request.getContent().toString());
			return Responses.newResourceResponse((String) null, (String) null, request.getContent()).asPromise();
		}

		public Promise<ResourceResponse, ResourceException> deleteAsync(Context context, DeleteRequest request) {
			return null;
		}

		public boolean isClosed() {
			return false;
		}

		public boolean isValid() {
			return false;
		}

		public Promise<ResourceResponse, ResourceException> patchAsync(Context context, PatchRequest request) {
			return null;
		}

		public Promise<QueryResponse, ResourceException> queryAsync(Context context, QueryRequest request,
				QueryResourceHandler handler) {
			return null;
		}

		public Promise<ResourceResponse, ResourceException> readAsync(Context context, ReadRequest request) {
			return null;
		}

		public Promise<ResourceResponse, ResourceException> updateAsync(Context context, UpdateRequest request) {
			return null;
		}
	};

	private final FilterCondition isConnector = (context, request) -> {
		ResourcePath pathObject = request.getResourcePathObject();
		Optional<String> eventEndpoint = this.sharedState.getEventEndpoint();

		// We only care about connectors and we don't want to filter the event endpoint
		// otherwise we'd get into a loop.
		return this.sharedState.getEventEndpoint().map(endpoint -> pathObject.size() > 1
				&& "system".equals(pathObject.get(0)) && !endpoint.equals(pathObject.get(1))).orElse(false);
	};

	@Activate
	void activate(ComponentContext componentContext) throws InvalidSyntaxException {
		logger.info("Activating the Connector Logger Filter");
		BundleContext bundleContext = componentContext.getBundleContext();
		org.osgi.framework.Filter connectionFactoryFilter = bundleContext
				.createFilter("(service.pid=org.forgerock.openidm.router.internal)");
		this.serviceTracker = new ServiceTracker<ConnectionFactory, ConnectionFactory>(bundleContext,
				connectionFactoryFilter, (ServiceTrackerCustomizer<ConnectionFactory, ConnectionFactory>) null);
		this.serviceTracker.open(true);

		this.filterRego.addFilter(Filters.conditionalFilter(isConnector, this));
	}

	@Deactivate
	void deactivate(ComponentContext componentContext) {
		logger.info("Deactivating the Connector Logger Filter");
		this.connectionFactory = null;
		if (this.serviceTracker != null) {
			this.serviceTracker.close();
		}

		this.filterRego.removeFilter(this);
	}

	private Connection getConnection() throws ResourceException {
		if (this.connectionFactory == null) {
			this.connectionFactory = this.serviceTracker.getService();
		}

		return this.connectionFactory != null ? this.connectionFactory.getConnection() : NULL_CONNECTION;
	}

	@Override
	public Promise<ActionResponse, ResourceException> filterAction(Context context, ActionRequest request,
			RequestHandler next) {
		return this.logConnectorAccessEntry(context, request, EventType.ACTION, () -> {
			return next.handleAction(context, request);
		});
	}

	@Override
	public Promise<ResourceResponse, ResourceException> filterCreate(Context context, CreateRequest request,
			RequestHandler next) {
		return this.logConnectorAccessEntry(context, request, EventType.CREATE, () -> {
			return next.handleCreate(context, request);
		});
	}

	@Override
	public Promise<ResourceResponse, ResourceException> filterDelete(Context context, DeleteRequest request,
			RequestHandler next) {
		return this.logConnectorAccessEntry(context, request, EventType.DELETE, () -> {
			return next.handleDelete(context, request);
		});
	}

	@Override
	public Promise<ResourceResponse, ResourceException> filterPatch(Context context, PatchRequest request,
			RequestHandler next) {
		return this.logConnectorAccessEntry(context, request, EventType.PATCH, () -> {
			return next.handlePatch(context, request);
		});
	}

	@Override
	public Promise<QueryResponse, ResourceException> filterQuery(Context context, QueryRequest request,
			QueryResourceHandler handler, RequestHandler next) {
		return this.logConnectorAccessEntry(context, request, EventType.QUERY, () -> {
			return next.handleQuery(context, request, handler);
		});
	}

	@Override
	public Promise<ResourceResponse, ResourceException> filterRead(Context context, ReadRequest request,
			RequestHandler next) {
		return this.logConnectorAccessEntry(context, request, EventType.READ, () -> {
			return next.handleRead(context, request);
		});
	}

	@Override
	public Promise<ResourceResponse, ResourceException> filterUpdate(Context context, UpdateRequest request,
			RequestHandler next) {
		return this.logConnectorAccessEntry(context, request, EventType.UPDATE, () -> {
			return next.handleUpdate(context, request);
		});
	}

	<R extends Response> Promise<R, ResourceException> logConnectorAccessEntry(Context context, Request request,
																			   EventType eventType, Supplier<Promise<R, ResourceException>> requestPromise) {
		long actionTime = System.currentTimeMillis();
		Promise<R, ResourceException> promise = requestPromise.get();
		ConnectorLoggerFilter.ConnectorEventBuilder connectorLoggerEventBuilder = new ConnectorLoggerFilter.ConnectorEventBuilder(
				context, request);
		promise.thenOnResultOrException((result) -> {
			long now = System.currentTimeMillis();
			long elapsedTime = now - actionTime;
			connectorLoggerEventBuilder.response(ResponseStatus.SUCCESSFUL, result, null, elapsedTime,
					TimeUnit.MILLISECONDS);
		}, (resourceException) -> {
			long now = System.currentTimeMillis();
			long elapsedTime = now - actionTime;
			connectorLoggerEventBuilder.responseWithDetail(ResponseStatus.FAILED,
					String.valueOf(resourceException.getCode()), elapsedTime, TimeUnit.MILLISECONDS,
					resourceException.toJsonValue());
		}).thenAlways(() -> {
			try {
				String event_endpoint = this.sharedState.getEventEndpoint()
						.orElseThrow(() -> new PreconditionFailedException("Missing Event Endpoint"));
				this.getConnection().create(context,
						Requests.newCreateRequest("system/" + event_endpoint + "/" + eventType.getName(),
								connectorLoggerEventBuilder.getJsonValue()));
			} catch (ResourceException e) {
				logger.error("Failed to log connector event", e);
			}

		});

		return promise;
	}

	private class ConnectorEventBuilder {
		JsonValue jsonValue;

		ConnectorEventBuilder(Context context, Request request) {
			this.jsonValue = json(
					object(field("objectType", request.getResourcePath()), field("request", request.toJsonValue())));
		}

		public ConnectorEventBuilder response(ResponseStatus responseStatus, Response response, String statusCode,
				long elapsedTime, TimeUnit elapsedTimeUnits) {
			this.addTimestamp();
			Map<String, Object> responseVal = object(field("status", responseStatus.toString()),
					field("content", this.getJsonContent(response)), field("elapsedTime", elapsedTime),
					field("elapsedTimeUnits", elapsedTimeUnits == null ? null : elapsedTimeUnits.name()));

			this.jsonValue.put("response", responseVal);
			return this;
		}

		public final ConnectorEventBuilder responseWithDetail(ResponseStatus status, String statusCode,
				long elapsedTime, TimeUnit elapsedTimeUnits, JsonValue detail) {
			Reject.ifNull(detail);
			this.addTimestamp();
			Object response = object(field("status", status == null ? null : status.toString()),
					field("statusCode", statusCode), field("elapsedTime", elapsedTime),
					field("elapsedTimeUnits", elapsedTimeUnits == null ? null : elapsedTimeUnits.name()),
					field("detail", detail.getObject()));
			this.jsonValue.put("response", response);
			return this;
		}

		private JsonValue getJsonContent(Response response) {
			if (response instanceof ResourceResponse) {
				return ((ResourceResponse) response).getContent();
			} else if (response instanceof ActionResponse) {
				return ((ActionResponse) response).getJsonContent();
			} else if (response instanceof QueryResponse) {
				// FIXME: Figure out how to get the query response.
				return json(object(0));
			} else {
				return json(object(0));
			}
		}

		private void addTimestamp() {
			this.jsonValue.put("responseTime", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()));
		}

		public JsonValue getJsonValue() {
			return jsonValue;
		}
	}

	private enum ResponseStatus {
		SUCCESSFUL, FAILED
	}

}
