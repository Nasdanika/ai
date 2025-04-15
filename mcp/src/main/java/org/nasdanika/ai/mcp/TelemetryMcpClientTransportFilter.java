package org.nasdanika.ai.mcp;

import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;

import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema.JSONRPCMessage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import reactor.core.publisher.Mono;

/**
 * Creates {@link Span}s for transport method calls. 
 */
public class TelemetryMcpClientTransportFilter implements McpClientTransport {
	
	private McpClientTransport target;
	private Tracer tracer;
	private Context context;

	public TelemetryMcpClientTransportFilter(
			McpClientTransport target,
			Tracer tracer,
			Context context) {
		
		this.target = target;
		this.tracer = tracer;
		this.context = context;
	}
	
	public TelemetryMcpClientTransportFilter(
			McpClientTransport target,
			Tracer tracer) {
		
		this.target = target;
		this.tracer = tracer;
		this.context = Context.current();
	}	
	
	public void setContext(Context context) {
		this.context = context;
	}

	@Override
	public Mono<Void> closeGracefully() {
		return Mono.deferContextual(contextView -> {
			Context parentContext = contextView.getOrDefault(Context.class, getContext());
	        Span span = tracer
		        	.spanBuilder("closeGracefully")
		        	.setSpanKind(SpanKind.CLIENT)
		        	.setParent(parentContext)
		        	.startSpan();

			try (Scope scope = span.makeCurrent()) {				
				return 
					target.closeGracefully()
						.map(result -> {
					        span.setStatus(StatusCode.OK);
							return result;
						})
						.onErrorMap(error -> {
			        		span.recordException(error);
					       	span.setStatus(StatusCode.ERROR);
							return error;
						})
						.contextWrite(reactor.util.context.Context.of(Context.class, getContext().with(span)))
						.doFinally(signal -> span.end());
			}
		});		
	}

	@Override
	public Mono<Void> sendMessage(JSONRPCMessage message) {
		return Mono.deferContextual(contextView -> {
			Context parentContext = contextView.getOrDefault(Context.class, getContext());
			
	        Span span = tracer
		        	.spanBuilder("sendMessage")
		        	.setSpanKind(SpanKind.CLIENT)
		        	.setParent(parentContext)
		        	.setAttribute("message", message.toString())
		        	.startSpan();
	        
	        try (Scope scope = span.makeCurrent()) { 
				return 
					target.sendMessage(message)
						.map(result -> {
					       	span.setStatus(StatusCode.OK);
							return result;
						})
						.onErrorMap(error -> {
			        		span.recordException(error);
					       	span.setStatus(StatusCode.ERROR);
							return error;
						})
						.contextWrite(reactor.util.context.Context.of(Context.class, getContext().with(span)))
						.doFinally(signal -> {
							span.end();
						});
	        }
		});		
	}

	protected Context getContext() {
		return context == null ? Context.current() : context;
	}

	@Override
	public <T> T unmarshalFrom(Object data, TypeReference<T> typeRef) {
		return target.unmarshalFrom(data, typeRef);
	}

	@Override
	public Mono<Void> connect(Function<Mono<JSONRPCMessage>, Mono<JSONRPCMessage>> handler) {
		return Mono.deferContextual(contextView -> {
			Context parentContext = contextView.getOrDefault(Context.class, getContext());
	        Span span = tracer
		        	.spanBuilder("connect")
		        	.setSpanKind(SpanKind.CLIENT)
		        	.setParent(parentContext)
		        	.startSpan();

	        try (Scope scope = span.makeCurrent()) {
				return 
					target.connect(filterHandler(handler))
						.doOnNext(result -> {
					       	span.setStatus(StatusCode.OK);						
						})
						.onErrorMap(error -> {
			        		span.recordException(error);
					       	span.setStatus(StatusCode.ERROR);
							return error;
						})
						.contextWrite(reactor.util.context.Context.of(Context.class, getContext().with(span)))
						.doFinally(signal -> {
							span.end();
						});
	        }
		});		
		
	}

	private Function<Mono<JSONRPCMessage>, Mono<JSONRPCMessage>> filterHandler(Function<Mono<JSONRPCMessage>, Mono<JSONRPCMessage>> handler) {
		return requestMono -> {
			return Mono.deferContextual(contextView -> {
				Context parentContext = contextView.getOrDefault(Context.class, getContext());
		        Span span = tracer
			        	.spanBuilder("handle")
			        	.setSpanKind(SpanKind.CLIENT)
			        	.setParent(parentContext)
			        	.startSpan();
		        
		        try (Scope scope = span.makeCurrent()) {
					return 
						handler.apply(requestMono)
							.map(result -> {
						       	span.setStatus(StatusCode.OK);
						       	span.setAttribute("response", result.toString());
								return result;
							})
							.onErrorMap(error -> {
				        		span.recordException(error);
						       	span.setStatus(StatusCode.ERROR);
								return error;
							})
							.contextWrite(reactor.util.context.Context.of(Context.class, getContext().with(span)))
							.doFinally(signal -> span.end());
		        }
			});		
			
		};
	}

}
