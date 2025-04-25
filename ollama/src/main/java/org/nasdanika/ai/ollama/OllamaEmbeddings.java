package org.nasdanika.ai.ollama;

import java.lang.module.ModuleDescriptor.Version;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.nasdanika.ai.Embeddings;
import org.nasdanika.common.NasdanikaException;
import org.nasdanika.common.Util;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;

/**
 * Base class for OpenAI embeddings. 
 */
public class OllamaEmbeddings implements Embeddings {

//    private static Logger logger = LoggerFactory.getLogger(OllamaEmbeddings.class);
	
	private static final String ERROR_KEY = "error";

	protected String endpoint;
	
	private String provider;
	private String model;
	private int dimensions;
	private Encoding encoding;
	private int maxInputTokens;
	
	protected String getInstrumentationScopeName() {
		return getClass().getName();
	}
	
	protected String getInstrumentationScopeVersion() {
		return 
			getClass()
			.getModule()
			.getDescriptor()
			.version()
			.map(Version::toString)
			.orElse("undefined");
	}

    Tracer tracer;
	private LongCounter tokenCounter;
	private String version;
	private TextMapPropagator propagator;

	public OllamaEmbeddings(
			String endpoint, 
			String provider,
			String model,
			String version,
			int dimensions,
			EncodingType encodingType,
			int maxInputTokens,
			OpenTelemetry openTelemetry) {
		this.endpoint = endpoint;
		this.provider = provider;
		this.model = model;
		this.version = version;
		this.dimensions = dimensions;
		propagator = openTelemetry.getPropagators().getTextMapPropagator();
		
		if (encodingType != null) {
			EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
			encoding = registry.getEncoding(encodingType);
		}
		this.maxInputTokens = maxInputTokens;
				 
		tracer = openTelemetry.getTracer(getInstrumentationScopeName(), getInstrumentationScopeVersion());
		Meter meter = openTelemetry.getMeter(getInstrumentationScopeName());
		String tokenCounterName = provider + "_" + model;
		if (!Util.isBlank(version)) {
			tokenCounterName += "_" + version;
		}
		tokenCounter = meter
			.counterBuilder(tokenCounterName)
			.setDescription("Token usage")
			.setUnit("token")
			.build();		
	}

	@Override
	public String getProvider() {
		return provider;
	}

	@Override
	public String getName() {
		return model;
	}
	
	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public boolean isTooLong(String input) {
		if (Util.isBlank(input)) {
			return false;
		}
		if (encoding == null) {
			return false;
		}
		IntArrayList encoded = encoding.encode(input);
		return encoded.size() < maxInputTokens;
	}

	@Override
	public int getDimensions() {
		return dimensions;
	}

	@Override
	public int getMaxInputTokens() {
		return maxInputTokens;
	}
	
	protected String spanName() {
		String spanName = "Embeddings " + provider + " " + model + " " + endpoint;
        if (!Util.isBlank(version)) {
        	spanName += " " + version;
        }
		return spanName;
	}	
	
	protected JSONObject marshal(String input) {
		JSONObject ret = new JSONObject();
		ret.put("model", model);
		ret.put("input", input);
		return ret;
	}
	
	protected List<List<Float>> unmarshal(String response, Span span) {
		JSONObject jResponse = new JSONObject(response);
		if (jResponse.has(ERROR_KEY)) {
			throw new NasdanikaException(jResponse.getString(ERROR_KEY));
		}
		JSONArray jEmbeddings = jResponse.getJSONArray("embeddings");
		List<List<Float>> ret = new ArrayList<>();
		for (int i = 0; i < jEmbeddings.length(); ++i) {
			List<Float> vector = new ArrayList<>();
			JSONArray jVector = jEmbeddings.getJSONArray(i);
			for (int j = 0; j < jVector.length(); ++j) {
				vector.add(jVector.getFloat(j));
			}
			ret.add(vector);
		}
		
		int promptTokens = jResponse.getInt("prompt_eval_count");
		tokenCounter.add(promptTokens);
		span.setAttribute("tokens", promptTokens);
		return ret;
	}

	@Override
	public Mono<List<List<Float>>> generateAsync(String input) {
		return Mono.deferContextual(contextView -> {
			Context parentContext = contextView.getOrDefault(Context.class, Context.current());
		
			Span span = tracer.spanBuilder(spanName())
					.setSpanKind(SpanKind.CLIENT)
					.setParent(parentContext)
					.setAttribute("input", input)
					.startSpan();
				
			try (Scope scope = span.makeCurrent()) {			
				HttpClient client = HttpClient.create()
						.headers(headerBuilder -> {
							propagator.inject(Context.current(), headerBuilder, (c, k, v) -> c.set(k,v));	
						})
						.followRedirect(true);
				
				return client.post()
					.uri(endpoint + "embed")		
					.send(ByteBufFlux.fromString(Mono.just(marshal(input).toString(2))))
					.responseContent()
					.aggregate()
					.asString()
					.map(result -> {
						span.setStatus(StatusCode.OK);
						return unmarshal(result, span);
					})
					.onErrorMap(error -> {
						span.recordException(error);
						span.setStatus(StatusCode.ERROR);
						return error;
					})
					.doFinally(signal -> span.end());
			}						
		});
	}	

}
