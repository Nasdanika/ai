package org.nasdanika.ai.mcp;

import java.util.Map.Entry;
import java.util.function.BiConsumer;

import io.modelcontextprotocol.server.McpServerFeatures.AsyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncResourceSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.AsyncToolSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncPromptSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncResourceSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.GetPromptResult;
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
				
				for (Entry<String, Object> re: request.entrySet()) {
					span.setAttribute("request." + re.getKey(), String.valueOf(re.getValue()));
				}
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
					
					for (Entry<String, Object> re: request.entrySet()) {
						span.setAttribute("request." + re.getKey(), String.valueOf(re.getValue()));
					}
				
					try (Scope scope = span.makeCurrent()) {
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
					}
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
					.setAttribute("resource-uri", syncResourceSpecification.resource().uri())
					.setAttribute("request-uri", request.uri())
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
					Span span = tracer.spanBuilder("Async resource " + asyncResourceSpecification.resource().name())
						.setAttribute("description", asyncResourceSpecification.resource().description())
						.setAttribute("resource-uri", asyncResourceSpecification.resource().uri())
						.setAttribute("request-uri", request.uri())
						.setAttribute("mime-type", asyncResourceSpecification.resource().mimeType())
						.setParent(parentContext)
						.startSpan();

					try (Scope scope = span.makeCurrent()) {				
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
					}
				});
			});
	}
	
	public SyncPromptSpecification filter(SyncPromptSpecification syncPromptSpecification) {
	return new SyncPromptSpecification(
		syncPromptSpecification.prompt(), 
		(exchange, request) -> {
			long start = System.currentTimeMillis();
			Span span = tracer.spanBuilder("Sync prompt " + syncPromptSpecification.prompt().name())
				.setAttribute("description", syncPromptSpecification.prompt().description())
				.startSpan();
			
			try (Scope scope = span.makeCurrent()) {				
				GetPromptResult result = syncPromptSpecification.promptHandler().apply(exchange, request);
				span.setStatus(StatusCode.OK);
				return result;
			} catch (RuntimeException e) {
				span.recordException(e);
				span.setStatus(StatusCode.ERROR);
				throw e;
			} finally {
				if (durationConsumer != null) {
					durationConsumer.accept("tool.sync." + syncPromptSpecification.prompt().name(), System.currentTimeMillis() - start);
				}
				span.end();
			}								
		});						
	}
	
	public AsyncPromptSpecification filter(AsyncPromptSpecification asyncPromptSpecification) {
	return new AsyncPromptSpecification(
		asyncPromptSpecification.prompt(),				
		(exchange, request) -> {				
			return Mono.deferContextual(contextView -> {
				Context parentContext = contextView.getOrDefault(Context.class, Context.current());
			
				long start = System.currentTimeMillis();					
				Span span = tracer.spanBuilder("Async prompt " + asyncPromptSpecification.prompt().name())
					.setAttribute("description", asyncPromptSpecification.prompt().description())
					.setParent(parentContext)
					.startSpan();
			
				try (Scope scope = span.makeCurrent()) {
					Mono<GetPromptResult> publisher = asyncPromptSpecification.promptHandler().apply(exchange, request);
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
								durationConsumer.accept("tool.sync." + asyncPromptSpecification.prompt().name(), System.currentTimeMillis() - start);
							}
							span.end();
						});
				}
			});
		});						
	}	
	
}
