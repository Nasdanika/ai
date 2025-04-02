package org.nasdanika.ai.openai;

import java.lang.module.ModuleDescriptor.Version;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nasdanika.ai.Embeddings;
import org.nasdanika.common.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.ai.openai.OpenAIAsyncClient;
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
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import reactor.core.publisher.Mono;

/**
 * Base class for OpenAI embeddings. 
 */
public class OpenAIEmbeddings implements Embeddings {

    private static Logger logger = LoggerFactory.getLogger(OpenAIEmbeddings.class);
	
	protected OpenAIClient openAIClient;
	protected OpenAIAsyncClient openAIAsyncClient;
	
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
	private DoubleHistogram durationHistogram;

	public OpenAIEmbeddings(
			OpenAIClient openAIClient, 
			OpenAIAsyncClient openAIAsyncClient, 
			String provider,
			String model,
			String version,
			int dimensions,
			EncodingType encodingType,
			int maxInputTokens,
			OpenTelemetry openTelemetry) {
		this.openAIClient = openAIClient;
		this.openAIAsyncClient = openAIAsyncClient;
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
		String tokenHistogramName = provider + "." + model;
		if (!Util.isBlank(version)) {
			tokenHistogramName += "." + version;
		}
		tokenHistogram = meter
			.histogramBuilder(tokenHistogramName)
			.setDescription("Token usage")
			.setUnit("token")
			.build();
		
		durationHistogram = meter
			.histogramBuilder(tokenHistogramName + ".duration")
			.setDescription("Duration histogram")
			.setUnit("seconds")
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

	@Override
	public Mono<List<Float>> generateAsync(String input) {
		return generateAsync(Collections.singletonList(input)).map(result -> result.get(input));
	}

	@Override
	public Mono<Map<String, List<Float>>> generateAsync(List<String> input) {
		return Mono.deferContextual(contextView -> {
			Context parentContext = contextView.getOrDefault(Context.class, Context.current());

			long start = System.currentTimeMillis();
			EmbeddingsOptions embeddingOptions = new EmbeddingsOptions(input);
	        String spanName = "Embeddings " + provider + " " + model;
	        if (!Util.isBlank(version)) {
	        	spanName += " " + version;
	        }
			Span span = tracer
		        	.spanBuilder(spanName)
		        	.setAttribute("request.thread", Thread.currentThread().getName())
		        	.setParent(parentContext)
		        	.startSpan();
					
			Mono<com.azure.ai.openai.models.Embeddings> result = openAIAsyncClient
					.getEmbeddings(model, embeddingOptions)
	        		.contextWrite(reactor.util.context.Context.of(Context.class, Context.current().with(span)));
			
			return 
				result
					.map(embeddings -> {
				        try (Scope scope = span.makeCurrent()) {
				        	double duration = System.currentTimeMillis() - start;
				        	durationHistogram.record(duration / 1000);
				        	
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
				        }
					})
					.onErrorMap(error -> {
				        try (Scope scope = span.makeCurrent()) {
					        logger.error("Embedding generation failed: " + error , error);
				        	span.setStatus(StatusCode.ERROR);
							return error;
				        }
					}).doFinally(signal -> {
						span.setAttribute("result.thread", Thread.currentThread().getName());
						span.end();
					});			
		});
		
	}

}
