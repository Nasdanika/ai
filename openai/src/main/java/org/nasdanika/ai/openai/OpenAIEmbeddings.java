package org.nasdanika.ai.openai;

import java.lang.module.ModuleDescriptor.Version;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.nasdanika.ai.Embeddings;
import org.nasdanika.common.Util;

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
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import reactor.core.publisher.Mono;

/**
 * Base class for OpenAI embeddings. 
 */
public class OpenAIEmbeddings implements Embeddings {

//    private static Logger logger = LoggerFactory.getLogger(OpenAIEmbeddings.class);
	
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
	private LongCounter tokenCounter;
	private String version;
	private int batchSize;

	/**
	 * 
	 * @param openAIClient
	 * @param openAIAsyncClient
	 * @param provider
	 * @param model
	 * @param version
	 * @param dimensions
	 * @param encodingType
	 * @param maxInputTokens
	 * @param batchSize Maximum number of input strings in a single request
	 * @param openTelemetry
	 */
	public OpenAIEmbeddings(
			OpenAIClient openAIClient, 
			OpenAIAsyncClient openAIAsyncClient, 
			String provider,
			String model,
			String version,
			int dimensions,
			EncodingType encodingType,
			int maxInputTokens,
			int batchSize,
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
		String tokenCounterName = provider + "_" + model;
		if (!Util.isBlank(version)) {
			tokenCounterName += "_" + version;
		}
		tokenCounter = meter
			.counterBuilder(tokenCounterName)
			.setDescription("Token usage")
			.setUnit("token")
			.build();
		
		this.batchSize = batchSize;
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
	public List<List<Float>> generate(String input) {
		return generate(Collections.singletonList(input)).get(input);
	}
	
	protected List<String> chunk(String input) {
		return Collections.singletonList(input);
	}

	@Override
	public Map<String, List<List<Float>>> generate(List<String> input) {
        String spanName = "Embeddings " + provider + " " + model;
        if (!Util.isBlank(version)) {
        	spanName += " " + version;
        }
		Span span = tracer
	        	.spanBuilder(spanName)
	        	.setSpanKind(SpanKind.CLIENT)
	        	.setAllAttributes(filterSpanInputAttributes(input))
	        	.startSpan();
	        
	    try (Scope scope = span.makeCurrent()) {
			Map<String, List<List<Float>>> ret = new LinkedHashMap<>();
	    	for (int i = 0; i < input.size(); i += batchSize) {
	    		List<String> batch = input.subList(i, Math.min(i + batchSize, input.size()));
				EmbeddingsOptions embeddingOptions = new EmbeddingsOptions(batch);
				com.azure.ai.openai.models.Embeddings embeddings = openAIClient.getEmbeddings(model, embeddingOptions);
				for (EmbeddingItem ei: embeddings.getData()) {
					String prompt = input.get(i + ei.getPromptIndex());
					ret.put(prompt, Collections.singletonList(ei.getEmbedding()));
				}
				EmbeddingsUsage usage = embeddings.getUsage();
				tokenCounter.add(usage.getPromptTokens());
				span.setAttribute("tokens", usage.getPromptTokens());
				span.setStatus(StatusCode.OK);
	    	}
			return ret;
	    } catch (RuntimeException e) {
	    	span.setStatus(StatusCode.ERROR);
	    	span.recordException(e);
	    	throw e;
	    } finally {
	    	span.end();
	    }
	}

	@Override
	public int getMaxInputTokens() {
		return maxInputTokens;
	}

	@Override
	public Mono<List<List<Float>>> generateAsync(String input) {
		return generateAsync(Collections.singletonList(input)).map(result -> result.get(input));
	}	
	
	/**
	 * Override to modify what is set as span's input attribute - suppress (return null), truncate, anonymize
	 * @param input
	 * @return
	 */
	protected String filterSpanInputAttributeElement(String input) {
		return input;
	}

	protected Attributes filterSpanInputAttributes(List<String> input) {
		List<String> filteredInput = input.stream()
			.map(this::filterSpanInputAttributeElement)
			.filter(s -> !Util.isBlank(s))
			.toList();
		
		if (filteredInput.isEmpty()) {
			return Attributes.empty();
		}
        return Attributes.of(AttributeKey.stringArrayKey("input"), filteredInput);
	}

	@Override
	public Mono<Map<String, List<List<Float>>>> generateAsync(List<String> input) {
		return Mono.deferContextual(contextView -> {
			Context parentContext = contextView.getOrDefault(Context.class, Context.current());

	        String spanName = "Embeddings " + provider + " " + model;
	        if (!Util.isBlank(version)) {
	        	spanName += " " + version;
	        }
			Span span = tracer
		        	.spanBuilder(spanName)
		        	.setSpanKind(SpanKind.CLIENT)
		        	.setParent(parentContext)
		        	.setAllAttributes(filterSpanInputAttributes(input))
		        	.startSpan();
			
			List<Mono<Map.Entry<Integer,com.azure.ai.openai.models.Embeddings>>> batchResults = new ArrayList<>();
	    	for (int i = 0; i < input.size(); i += batchSize) {
	    		List<String> batch = input.subList(i, Math.min(i + batchSize, input.size()));														
				EmbeddingsOptions embeddingOptions = new EmbeddingsOptions(batch);
				final int offset = i;
				Mono<Map.Entry<Integer,com.azure.ai.openai.models.Embeddings>> batchResult = openAIAsyncClient
						.getEmbeddings(model, embeddingOptions)
						.map(e -> Map.entry(offset, e))
		        		.contextWrite(reactor.util.context.Context.of(Context.class, Context.current().with(span)));
				batchResults.add(batchResult);
	    	}
	    	
			Mono<Map<String, List<List<Float>>>> result = Mono.zip(batchResults, embeddingsArray -> {
		        try (Scope scope = span.makeCurrent()) {
					Map<String, List<List<Float>>> ret = new LinkedHashMap<>();
					int promptTokens = 0;
					for (Object ae: (Object[]) embeddingsArray) {
						@SuppressWarnings("unchecked")
						Map.Entry<Integer,com.azure.ai.openai.models.Embeddings> embeddingsEntry = (Map.Entry<Integer,com.azure.ai.openai.models.Embeddings>) ae;
						for (EmbeddingItem ei: embeddingsEntry.getValue().getData()) {
							String prompt = input.get(embeddingsEntry.getKey() + ei.getPromptIndex());
							ret.put(prompt, Collections.singletonList(ei.getEmbedding()));
						}
						EmbeddingsUsage usage = embeddingsEntry.getValue().getUsage();
						promptTokens += usage.getPromptTokens();
					}
					tokenCounter.add(promptTokens);
					span.setAttribute("tokens", promptTokens);
		        	span.setStatus(StatusCode.OK);
					return ret;
		        }				
			})
			.onErrorMap(error -> {
		        try (Scope scope = span.makeCurrent()) {
		        	span.recordException(error);
		        	span.setStatus(StatusCode.ERROR);
					return error;
		        }
			})
			.doFinally(signal -> {
				span.end();
			});
			return result;
		});
		
	}

}
