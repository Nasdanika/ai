package org.nasdanika.ai.openai;

import org.nasdanika.common.Util;

import com.azure.core.http.HttpHeaderName;
import com.azure.core.http.HttpPipelineCallContext;
import com.azure.core.http.HttpPipelineNextPolicy;
import com.azure.core.http.HttpRequest;
import com.azure.core.http.HttpResponse;
import com.azure.core.http.policy.HttpPipelinePolicy;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import reactor.core.publisher.Mono;

public class OpenTelemetryHttpPipelinePolicy implements HttpPipelinePolicy {

//    private static Logger logger = LoggerFactory.getLogger(OpenTelemetryHttpPipelinePolicy.class);
    
	private Tracer tracer;	
	private DoubleHistogram durationHistogram;

	private String endpoint;

	private TextMapPropagator propagator;
	
	public OpenTelemetryHttpPipelinePolicy(OpenTelemetry openTelemetry, String endpoint) {
		String instrumentationScopeName = Util.isBlank(endpoint) ? "openai-http" : endpoint;
		tracer = openTelemetry.getTracer(instrumentationScopeName);   
        propagator = openTelemetry.getPropagators().getTextMapPropagator();
		this.endpoint = endpoint;
		Meter meter = openTelemetry.getMeter(instrumentationScopeName);
		durationHistogram = meter
			.histogramBuilder("duration_histogram")
			.setDescription("OpenAI request duration histogram")
			.setUnit("seconds")
			.build();		
	}
	
	@Override
	public Mono<HttpResponse> process(HttpPipelineCallContext context, HttpPipelineNextPolicy next) {
		return Mono.deferContextual(contextView -> {
			Context parentContext = contextView.getOrDefault(Context.class, Context.current());
		
			long start = System.currentTimeMillis();
			HttpRequest request = context.getHttpRequest();
			String path = request.getUrl().toString();
			if (!Util.isBlank(path) && path.startsWith(endpoint)) {
				path = path.substring(endpoint.length());
			}
			
	        Span requestSpan = tracer
		        	.spanBuilder(request.getHttpMethod().toString() + " " + path)
		        	.setSpanKind(SpanKind.CLIENT)
		        	.setParent(parentContext)
		        	.startSpan();
	                
	        // Trace propagation
	        Context telemetryContext = Context.current().with(requestSpan);
	        propagator.inject(telemetryContext, request, (rq, name, value) -> rq.setHeader(HttpHeaderName.fromString(name), value));
	        
			Mono<HttpResponse> result = next.process();
			
			return 
				result
					.map(response -> {
				        try (Scope scope = requestSpan.makeCurrent()) {
				        	double duration = System.currentTimeMillis() - start;
				        	durationHistogram.record(duration / 1000);
							requestSpan.setAttribute("http.status", response.getStatusCode());
				        	requestSpan.setStatus(StatusCode.OK);
							return response;
				        }
					})
					.onErrorMap(error -> {
				        try (Scope scope = requestSpan.makeCurrent()) {
				        	requestSpan.recordException(error);
				        	requestSpan.setStatus(StatusCode.ERROR);
							return error;
				        }
					}).doFinally(signal -> {
			        	requestSpan.end();					
					});
		});
	}
	
}
