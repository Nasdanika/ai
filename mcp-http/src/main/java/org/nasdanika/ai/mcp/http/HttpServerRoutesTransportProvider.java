package org.nasdanika.ai.mcp.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerSession;
import io.modelcontextprotocol.spec.McpServerTransport;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import io.modelcontextprotocol.util.Assert;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.opentelemetry.api.OpenTelemetry;
import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

/**
 * Reactor Netty implementation of {@link McpServerTransportProvider}.
 */
public class HttpServerRoutesTransportProvider implements McpServerTransportProvider {
	
	private static final Logger logger = LoggerFactory.getLogger(HttpServerRoutesTransportProviderBak.class);	

	public static final String DEFAULT_SSE_ENDPOINT = "/sse";

	public static final String DEFAULT_BASE_URL = "";
		
	private static final String SESSION_ID_PARAMETER = "sessionId";
	public static final String MESSAGE_EVENT_TYPE = "message";
	public static final String ENDPOINT_EVENT_TYPE = "endpoint";	

	private final ObjectMapper objectMapper;

	private final String baseUrl;

	private final String messageEndpoint;

	private final String sseEndpoint;

	private McpServerSession.Factory sessionFactory;

	private final Map<String, McpServerSession> sessions = new ConcurrentHashMap<>();

	private volatile boolean isClosing = false;

	public HttpServerRoutesTransportProvider(
			ObjectMapper objectMapper, 
			String messageEndpoint, 
			HttpServerRoutes httpServerRoutes,
			OpenTelemetry openTelemetry) {
		
		this(objectMapper, messageEndpoint, DEFAULT_SSE_ENDPOINT, httpServerRoutes, openTelemetry);
	}

	public HttpServerRoutesTransportProvider(
			ObjectMapper objectMapper, 
			String messageEndpoint, 
			String sseEndpoint,
			HttpServerRoutes httpServerRoutes,
			OpenTelemetry openTelemetry) {
		
		this(objectMapper, DEFAULT_BASE_URL, messageEndpoint, sseEndpoint, httpServerRoutes, openTelemetry);
	}

	public HttpServerRoutesTransportProvider(
			ObjectMapper objectMapper, 
			String baseUrl, 
			String messageEndpoint,
			String sseEndpoint,
			HttpServerRoutes httpServerRoutes,
			OpenTelemetry openTelemetry) {

		this.objectMapper = objectMapper;
		this.baseUrl = baseUrl;
		this.messageEndpoint = messageEndpoint;
		this.sseEndpoint = sseEndpoint;
		httpServerRoutes
			.get(this.sseEndpoint, serveSse())
			.post(this.messageEndpoint, this::processMessage);
	}
	
	public static class Builder {

		private ObjectMapper objectMapper;

		private String baseUrl = DEFAULT_BASE_URL;

		private String messageEndpoint;

		private String sseEndpoint = DEFAULT_SSE_ENDPOINT;

		public Builder objectMapper(ObjectMapper objectMapper) {
			Assert.notNull(objectMapper, "ObjectMapper must not be null");
			this.objectMapper = objectMapper;
			return this;
		}

		public Builder basePath(String baseUrl) {
			Assert.notNull(baseUrl, "basePath must not be null");
			this.baseUrl = baseUrl;
			return this;
		}

		public Builder messageEndpoint(String messageEndpoint) {
			Assert.notNull(messageEndpoint, "Message endpoint must not be null");
			this.messageEndpoint = messageEndpoint;
			return this;
		}

		public Builder sseEndpoint(String sseEndpoint) {
			Assert.notNull(sseEndpoint, "SSE endpoint must not be null");
			this.sseEndpoint = sseEndpoint;
			return this;
		}

		public HttpServerRoutesTransportProviderBak build(HttpServerRoutes httpServerRoutes, OpenTelemetry openTelemetry) {
			return new HttpServerRoutesTransportProviderBak(
					objectMapper, 
					baseUrl, 
					messageEndpoint, 
					sseEndpoint,
					httpServerRoutes,
					openTelemetry);
		}

	}
	
	public static Builder builder() {
		return new Builder();
	}	
		
	private record ServerSentEvent(String event, String data) {}	
	
	private class HttpServerRoutesSessionTransport implements McpServerTransport {

		private final FluxSink<ServerSentEvent> sink;

		public HttpServerRoutesSessionTransport(FluxSink<ServerSentEvent> sink) {
			this.sink = sink;
		}

		@Override
		public Mono<Void> sendMessage(McpSchema.JSONRPCMessage message) {
			return Mono.fromSupplier(() -> {
				try {
					return objectMapper.writeValueAsString(message);
				}
				catch (IOException e) {
					throw Exceptions.propagate(e);
				}
			}).doOnNext(jsonText -> {
				sink.next(new ServerSentEvent("message", jsonText));
			}).doOnError(e -> {
				Throwable exception = Exceptions.unwrap(e);
				sink.error(exception);
			}).then();
		}

		@Override
		public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
			return objectMapper.convertValue(data, typeRef);
		}

		@Override
		public Mono<Void> closeGracefully() {
			return Mono.fromRunnable(sink::complete);
		}

		@Override
		public void close() {
			sink.complete();
		}

	}
	
	private BiFunction<HttpServerRequest, HttpServerResponse, Publisher<Void>> serveSse() {
		Flux<ServerSentEvent> flux = Flux.create(sink -> {
			HttpServerRoutesSessionTransport sessionTransport = new HttpServerRoutesSessionTransport(sink);
			McpServerSession session = sessionFactory.create(sessionTransport);
			String sessionId = session.getId();

			logger.debug("Created new SSE connection for session: {}", sessionId);
			sessions.put(sessionId, session);

			logger.debug("Sending initial endpoint event to session: {}", sessionId);
			sink.next(new ServerSentEvent("endpoint", this.baseUrl + this.messageEndpoint + "?sessionId=" + sessionId));
			sink.onCancel(() -> {
				logger.debug("Session {} cancelled", sessionId);
				sessions.remove(sessionId);
			});			
		});
		return (request, response) ->
		        response.sse()
		                .send(flux.map(this::toByteBuf), b -> true);
	}
	
	/**
	 * Transforms the Object to ByteBuf following the expected SSE format.
	 */
	private ByteBuf toByteBuf(ServerSentEvent event) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try (Writer writer = new OutputStreamWriter(out)) {
			writer.write("event: ");
			writer.write(event.event());
			writer.write("\n");
			writer.write("data: ");
			writer.write(event.data());
			writer.write("\n\n");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return ByteBufAllocator.DEFAULT
		                       .buffer()
		                       .writeBytes(out.toByteArray());
	}	

	@Override
	public void setSessionFactory(McpServerSession.Factory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}
	
	private Publisher<Void> processMessage(HttpServerRequest request, HttpServerResponse response) {
		if (isClosing) {
			return response
					.status(HttpResponseStatus.SERVICE_UNAVAILABLE)
					.sendString(Mono.just("Server is shutting down"))
					.then();
		}
		QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
		if (!decoder.parameters().containsKey(SESSION_ID_PARAMETER)) {
			return response
					.status(HttpResponseStatus.BAD_REQUEST)
					.sendString(Mono.just("Session ID is missing"))
					.then();
		}
		
		McpServerSession session = sessions.get(decoder.parameters().get(SESSION_ID_PARAMETER).get(0));
		Mono<String> requestBody = request
				.receive()
				.aggregate()
				.asString();
		
		return requestBody		
				.flatMap(body -> {
					try {
						McpSchema.JSONRPCMessage message = McpSchema.deserializeJsonRpcMessage(objectMapper, body);
						Mono<Void> handled = session.handle(message);
						return handled
								.flatMap(rsp -> response.status(HttpResponseStatus.OK).then().onErrorResume(error -> {
									logger.error("Error processing  message: {}", error.getMessage());
									McpError mcpError = new McpError(error.getMessage());
									return response
											.status(HttpResponseStatus.BAD_REQUEST)
											.sendString(Mono.just(mcpError.getJsonRpcError().toString()))
											.then();
									
								}));
					} catch (IllegalArgumentException | IOException e) {
						// TODO - span record error
						logger.error("Failed to deserialize message: {}", e.getMessage());
						McpError mcpError = new McpError("Invalid message format");
						return response
								.status(HttpResponseStatus.BAD_REQUEST)
								.sendString(Mono.just(mcpError.getJsonRpcError().toString()))
								.then();
					}
				});
		
	}	
	
	@Override
	public Mono<Void> notifyClients(String method, Map<String, Object> params) {
		// TODO - telemetry span
		if (sessions.isEmpty()) {
			logger.debug("No active sessions to broadcast message to");
			return Mono.empty();
		}

		logger.debug("Attempting to broadcast message to {} active sessions", sessions.size());

		return Flux.fromIterable(sessions.values())
			.flatMap(session -> session.sendNotification(method, params)
				.doOnError(
						e -> logger.error("Failed to send message to session {}: {}", session.getId(), e.getMessage()))
				.onErrorComplete())
			.then();
	}

	@Override
	public Mono<Void> closeGracefully() {
		// TODO - telemetry span
		return Flux.fromIterable(sessions.values())
			.doFirst(() -> logger.debug("Initiating graceful shutdown with {} active sessions", sessions.size()))
			.flatMap(McpServerSession::closeGracefully)
			.then();
	}

}
