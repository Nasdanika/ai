package org.nasdanika.ai.mcp;

import java.util.function.BiConsumer;

import io.modelcontextprotocol.server.McpServerFeatures.AsyncResourceSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import reactor.core.publisher.Mono;

/**
 * Filters (wraps) Mcp features for collecting telemetry
 */
public class McpTelemetryFilter {
	
	protected Tracer tracer;
	protected BiConsumer<String,Long> durationConsumer;
	
	public McpTelemetryFilter(Tracer tracer, BiConsumer<String,Long> durationConsumer) {
		
		this.tracer = tracer;
		this.durationConsumer = durationConsumer;
	}
			
	public SyncToolSpecification filter(SyncToolSpecification syncToolSpecification) {
		return new SyncToolSpecification(
			syncToolSpecification.tool(), 
			(exchange, request) -> {
				long start = System.currentTimeMillis();
				Span span = tracer.spanBuilder("Sync tool " + syncToolSpecification.tool().name())
					.setAttribute("description", syncToolSpecification.tool().description())
					.startSpan();
				
				try (Scope scope = span.makeCurrent()) {				
					CallToolResult result = syncToolSpecification.call().apply(exchange, request);
					span.setStatus(StatusCode.OK);
					return result;
				} catch (RuntimeException e) {
					span.recordException(e);
					span.setStatus(StatusCode.ERROR);
					throw e;
				} finally {
					if (durationConsumer != null) {
						durationConsumer.accept("tool.sync." + syncToolSpecification.tool().name(), System.currentTimeMillis() - start);
					}
					span.end();
				}								
			});						
	}
	
	public AsyncToolSpecification filter(AsyncToolSpecification asyncToolSpecification) {
		return new AsyncToolSpecification(
			asyncToolSpecification.tool(),				
			(exchange, request) -> {				
				return Mono.deferContextual(contextView -> {
					Context parentContext = contextView.getOrDefault(Context.class, Context.current());
				
					long start = System.currentTimeMillis();					
					Span span = tracer.spanBuilder("Async tool " + asyncToolSpecification.tool().name())
						.setAttribute("description", asyncToolSpecification.tool().description())
						.setParent(parentContext)
						.startSpan();
				
					Mono<CallToolResult> publisher = asyncToolSpecification.call().apply(exchange, request);
					return publisher
						.map(result -> {
				        	span.setStatus(StatusCode.OK);
							return result;
						})
						.onErrorMap(error -> {
				        	span.recordException(error);
				        	span.setStatus(StatusCode.ERROR);
							return error;
						})
			    		.contextWrite(reactor.util.context.Context.of(Context.class, Context.current().with(span)))
						.doFinally(signal -> {
							if (durationConsumer != null) {
								durationConsumer.accept("tool.sync." + asyncToolSpecification.tool().name(), System.currentTimeMillis() - start);
							}
							span.end();
						});
				});
			});						
	}
		
	public SyncResourceSpecification filter(SyncResourceSpecification syncResourceSpecification) {
		return new SyncResourceSpecification(
			syncResourceSpecification.resource(), 
			(exchange, request) -> {
				long start = System.currentTimeMillis();
				Span span = tracer.spanBuilder("Sync resource " + syncResourceSpecification.resource().name())
					.setAttribute("description", syncResourceSpecification.resource().description())
					.setAttribute("uri", syncResourceSpecification.resource().uri())
					.setAttribute("mime-type", syncResourceSpecification.resource().mimeType())
					.startSpan();
				
				try (Scope scope = span.makeCurrent()) {				
					ReadResourceResult result = syncResourceSpecification.readHandler().apply(exchange, request);
					span.setStatus(StatusCode.OK);
					return result;
				} catch (RuntimeException e) {
					span.recordException(e);
					span.setStatus(StatusCode.ERROR);
					throw e;
				} finally {
					if (durationConsumer != null) {
						durationConsumer.accept("tool.sync." + syncResourceSpecification.resource().name(), System.currentTimeMillis() - start);
					}
					span.end();
				}								
			});						
	}
	
	public AsyncResourceSpecification filter(AsyncResourceSpecification asyncResourceSpecification) {
		return new AsyncResourceSpecification(
			asyncResourceSpecification.resource(),				
			(exchange, request) -> {
				return Mono.deferContextual(contextView -> {
					Context parentContext = contextView.getOrDefault(Context.class, Context.current());
				
					long start = System.currentTimeMillis();					
					Span span = tracer.spanBuilder("Async tool " + asyncResourceSpecification.resource().name())
						.setAttribute("description", asyncResourceSpecification.resource().description())
						.setAttribute("uri", asyncResourceSpecification.resource().uri())
						.setAttribute("mime-type", asyncResourceSpecification.resource().mimeType())
						.setParent(parentContext)
						.startSpan();
					
					Mono<ReadResourceResult> publisher = asyncResourceSpecification.readHandler().apply(exchange, request);
					return publisher
						.map(result -> {
				        	span.setStatus(StatusCode.OK);
							return result;
						})
						.onErrorMap(error -> {
				        	span.recordException(error);
				        	span.setStatus(StatusCode.ERROR);
							return error;
						})
			    		.contextWrite(reactor.util.context.Context.of(Context.class, Context.current().with(span)))
						.doFinally(signal -> {
							if (durationConsumer != null) {
								durationConsumer.accept("tool.sync." + asyncResourceSpecification.resource().name(), System.currentTimeMillis() - start);
							}
							span.end();
						});									
				});
			});
	}

}
