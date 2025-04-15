/*
 * This class is an adaptation of https://github.com/Nasdanika/mcp-java-sdk/blob/main/mcp-spring/mcp-spring-webflux/src/main/java/io/modelcontextprotocol/server/transport/WebFluxSseServerTransportProvider.java
 * to Reactor Netty and OpenTelemetry.
 */

package org.nasdanika.ai.mcp.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.json.JSONObject;
import org.nasdanika.http.TelemetryFilter;
import org.nasdanika.telemetry.TelemetryUtil;
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
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
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
	
	private static final String ID_KEY = "id";

	/**
	 * Maps message IDs to telemetry contexts injected in POST to be extracted in sendMessage. 
	 */
	private Map<String, Map<String,String>> contextMap = new ConcurrentHashMap<>();
	
	private static final Logger logger = LoggerFactory.getLogger(HttpServerRoutesTransportProvider.class);	

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

	private Tracer tracer;
	private TextMapPropagator propagator;
	private TelemetryFilter telemetryFilter;

	public HttpServerRoutesTransportProvider(
			ObjectMapper objectMapper, 
			String messageEndpoint, 
			HttpServerRoutes httpServerRoutes,
			Tracer tracer,
			boolean resolveRemoteHostName,
			TextMapPropagator propagator, 
			BiConsumer<String, Long> durationConsumer) {
		
		this(
			objectMapper, 
			messageEndpoint, 
			DEFAULT_SSE_ENDPOINT, 
			httpServerRoutes, 
			tracer, 
			resolveRemoteHostName,
			propagator,
			durationConsumer);
	}

	public HttpServerRoutesTransportProvider(
			ObjectMapper objectMapper, 
			String messageEndpoint, 
			String sseEndpoint,
			HttpServerRoutes httpServerRoutes,
			Tracer tracer,
			boolean resolveRemoteHostName,
			TextMapPropagator propagator, 
			BiConsumer<String, Long> durationConsumer) {
		
		this(
			objectMapper, 
			DEFAULT_BASE_URL, 
			messageEndpoint, 
			sseEndpoint, 
			httpServerRoutes, 
			tracer, 
			resolveRemoteHostName,
			propagator,
			durationConsumer);
	}

	public HttpServerRoutesTransportProvider(
			ObjectMapper objectMapper, 
			String baseUrl, 
			String messageEndpoint,
			String sseEndpoint,
			HttpServerRoutes httpServerRoutes,
			Tracer tracer,
			boolean resolveRemoteHostName,
			TextMapPropagator propagator, 
			BiConsumer<String, Long> durationConsumer) {

		this.objectMapper = objectMapper;
		this.baseUrl = baseUrl;
		this.messageEndpoint = messageEndpoint;
		this.sseEndpoint = sseEndpoint;
		
		this.tracer = tracer;
		this.propagator = propagator;
		this.telemetryFilter = new TelemetryFilter(
				tracer, 
				propagator, 
				durationConsumer, 
				resolveRemoteHostName);				
		httpServerRoutes
			.get(this.sseEndpoint, serveSse())
			.post(this.messageEndpoint, this::processMessage);
	}
	
	public static class Builder {

		private ObjectMapper objectMapper;
		private String baseUrl = DEFAULT_BASE_URL;
		private String messageEndpoint;
		private String sseEndpoint = DEFAULT_SSE_ENDPOINT;
		private Tracer tracer;
		private boolean resolveRemoteHostName;
		private TextMapPropagator propagator;
		private BiConsumer<String, Long> durationConsumer;
				
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
		
		public Builder tracer(Tracer tracer) {
			this.tracer = tracer;
			return this;
		}
		
		public Builder resolveRemoteHostName(boolean resolveRemoteHostName) {
			this.resolveRemoteHostName = resolveRemoteHostName;
			return this;
		}
		
		public Builder propagator(TextMapPropagator propagator) {
			this.propagator = propagator;
			return this;
		}
		
		/**
		 * Consumer of path, duration in milliseconds. E.g. histogram
		 * @param durationConsumer
		 * @return
		 */
		public Builder setDurationConsumer(BiConsumer<String, Long> durationConsumer) {
			this.durationConsumer = durationConsumer;
			return this;
		}

		public HttpServerRoutesTransportProvider build(HttpServerRoutes httpServerRoutes) {
			return new HttpServerRoutesTransportProvider(
					objectMapper, 
					baseUrl, 
					messageEndpoint, 
					sseEndpoint,
					httpServerRoutes,
					tracer,
					resolveRemoteHostName,
					propagator,
					durationConsumer);
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
			AtomicReference<Span> spanRef = new AtomicReference<>();
			
			Context contextDelegate = new Context() {
				
				private Context getTarget() {
					Context target = Context.current();
					Span span = spanRef.get();
					if (span == null) {
						return target;
					}
					return target.with(span);
				}
				
				@Override
				public <V> Context with(ContextKey<V> k1, V v1) {
					return getTarget().with(k1, v1);
				}
				
				@Override
				public <V> V get(ContextKey<V> key) {
					return getTarget().get(key);
				}
			};
			
			return Mono.fromSupplier(() -> {
				try {
					SpanBuilder spanBuilder = TelemetryUtil.buildSpan(tracer.spanBuilder("sessionTransport.sendMessage"));
					String jsonText = objectMapper.writeValueAsString(message);
					JSONObject jObj = new JSONObject(jsonText);
					if (jObj.has(ID_KEY)) {
						Map<String,String> parentSpanData = contextMap.remove(jObj.getString(ID_KEY));
						if (parentSpanData != null) {
							TextMapGetter<Map<String, String>> mapper = new TextMapGetter<Map<String,String>>() {

								@Override
								public Iterable<String> keys(Map<String, String> carrier) {
									return carrier.keySet();
								}

								@Override
								public String get(Map<String, String> carrier, String key) {
									return carrier.get(key);
								}
				
							};
							Context telemetrycontext = propagator.extract(
									Context.current(),
									parentSpanData,
									mapper);
							spanBuilder.setParent(telemetrycontext);
						}
					}
					Span span = spanBuilder.startSpan();
					spanRef.set(span);
					return jsonText;
				}
				catch (IOException e) {
					throw Exceptions.propagate(e);
				}
			})			
			.doOnNext(jsonText -> {
				sink.next(new ServerSentEvent("message", jsonText));
				Span span = spanRef.get();
				if (span != null) {
					span.setAttribute("message", jsonText);
					span.setStatus(StatusCode.OK);
				}
			})
			.doOnError(e -> {
				Throwable exception = Exceptions.unwrap(e);
				Span span = spanRef.get();
				if (span != null) {
					span.recordException(exception);
					span.setStatus(StatusCode.ERROR);
				}
				sink.error(exception);
			})
    		.contextWrite(reactor.util.context.Context.of(Context.class, contextDelegate))
			.doFinally(signal -> {
				Span span = spanRef.get();
				if (span != null) {
					span.end();
				}
			})
			.then();
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
		Mono<String> requestBody = Mono.deferContextual(contextView -> {
			Context context = contextView.getOrDefault(Context.class, Context.current());
			Span span = Span.fromContext(context);
			
			return request
				.receive()
				.aggregate()
				.asString()
				.doOnNext(rb -> {
					if (span != null) {
						span.setAttribute("request", rb);
						JSONObject jRequest = new JSONObject(rb);
						if (jRequest.has(ID_KEY)) {
							Map<String,String> carrier = new HashMap<>();
							propagator.inject(context, carrier, (cr, name, value) -> cr.put(name, value));		
							if (!carrier.isEmpty()) {
								contextMap.put(jRequest.getString(ID_KEY), carrier);
							}
						}
					}					
				});			
		});
				
		return telemetryFilter.filter(request, requestBody)		
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
