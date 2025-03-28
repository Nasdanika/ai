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
import io.opentelemetry.api.trace.StatusCode;
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
	private String version;        

	public OpenAIEmbeddings(
			OpenAIClient openAIClient, 
			String provider,
			String model,
			String version,
			int dimensions,
			EncodingType encodingType,
			int maxInputTokens,
			OpenTelemetry openTelemetry) {
		this.openAIClient = openAIClient;
		this.provider = provider;
		this.model = model;
		this.version = version;
		this.dimensions = dimensions;
		
		if (encodingType != null) {
			EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
			encoding = registry.getEncoding(encodingType);
		}
		this.maxInputTokens = maxInputTokens;
				 
		tracer = openTelemetry.getTracer(getInstrumentationScopeName(), getInstrumentationScopeVersion());
		Meter meter = openTelemetry.getMeter(getInstrumentationScopeName());
		String histogramName = provider + "." + model;
		if (!Util.isBlank(version)) {
			histogramName += "." + version;
		}
		tokenHistogram = meter
			.histogramBuilder(histogramName)
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
	public List<Float> generate(String input) {
		return generate(Collections.singletonList(input)).get(input);
	}

	@Override
	public Map<String, List<Float>> generate(List<String> input) {
		EmbeddingsOptions embeddingOptions = new EmbeddingsOptions(input);
        String spanName = "Embeddings " + provider + " " + model;
        if (!Util.isBlank(version)) {
        	spanName += " " + version;
        }
		Span span = tracer
	        	.spanBuilder(spanName)
	        	.startSpan();

		span.setStatus(StatusCode.ERROR); // We set OK at before return		
	        
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
			span.setStatus(StatusCode.OK);
			return ret;
	    } finally {
	    	span.end();
	    }
	}

	@Override
	public int getMaxInputTokens() {
		return maxInputTokens;
	}

}
