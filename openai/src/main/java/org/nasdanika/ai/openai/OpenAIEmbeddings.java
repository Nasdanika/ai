package org.nasdanika.ai.openai;

import java.lang.module.ModuleDescriptor.Version;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nasdanika.ai.Embeddings;
import org.nasdanika.common.Util;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.EmbeddingItem;
import com.azure.ai.openai.models.EmbeddingsOptions;
import com.azure.ai.openai.models.EmbeddingsUsage;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

/**
 * Base class for OpenAI embeddings. 
 */
public class OpenAIEmbeddings implements Embeddings {
	
	private OpenAIClient openAIClient;
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
	private DoubleHistogram tokenHistogram;        

	public OpenAIEmbeddings(
			OpenAIClient openAIClient, 
			String model, 
			String provider,
			int dimensions,
			EncodingType encodingType,
			int maxInputTokens,
			OpenTelemetry openTelemetry) {
		this.openAIClient = openAIClient;
		this.model = model;
		this.provider = provider;
		this.dimensions = dimensions;
		
		if (encodingType != null) {
			EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
			encoding = registry.getEncoding(encodingType);
		}
		this.maxInputTokens = maxInputTokens;
				 
		tracer = openTelemetry.getTracer(getInstrumentationScopeName(), getInstrumentationScopeVersion());
		Meter meter = openTelemetry.getMeter(getInstrumentationScopeName());
		tokenHistogram = meter
			.histogramBuilder(provider + "." + model)
			.setDescription("Token usage")
			.setUnit("token")
			.build();
	}

	@Override
	public String getProvider() {
		return provider;
	}

	@Override
	public String getModel() {
		return model;
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
	public List<Float> generate(String input) {
		return generate(Collections.singletonList(input)).get(input);
	}

	@Override
	public Map<String, List<Float>> generate(List<String> input) {
		EmbeddingsOptions embeddingOptions = new EmbeddingsOptions(input);
        Span span = tracer
	        	.spanBuilder("Embeddings " + provider + " " + model)
	        	.startSpan();
	        
	    try (Scope scope = span.makeCurrent()) {		
			com.azure.ai.openai.models.Embeddings embeddings = openAIClient.getEmbeddings(model, embeddingOptions);
			Map<String, List<Float>> ret = new LinkedHashMap<>();
			for (EmbeddingItem ei: embeddings.getData()) {
				String prompt = input.get(ei.getPromptIndex());
				ret.put(prompt, ei.getEmbedding());
			}
			EmbeddingsUsage usage = embeddings.getUsage();
			tokenHistogram.record(usage.getPromptTokens());
			span.setAttribute("tokens", usage.getPromptTokens());
			return ret;
	    } finally {
	    	span.end();
	    }
	}

}
