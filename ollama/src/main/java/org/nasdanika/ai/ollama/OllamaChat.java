package org.nasdanika.ai.ollama;

import java.lang.module.ModuleDescriptor.Version;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.nasdanika.ai.Chat;
import org.nasdanika.common.NasdanikaException;
import org.nasdanika.common.Util;

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
import io.opentelemetry.context.propagation.TextMapPropagator;
import reactor.core.publisher.Mono;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.client.HttpClient;

/**
 * Base class for OpenAI embeddings. 
 */
public class OllamaChat implements Chat {
	
	private static final String ERROR_KEY = "error";
	private static final String CONTENT_KEY = "content";
	private static final String IMAGES_KEY = "images";
	private static final String ROLE_KEY = "role";
	private static final String BASE_64 = ";base64,";
	private String provider;
	private String model;
	
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
	private LongCounter promptTokenCounter;
	private LongCounter completionTokenCounter;
	private int maxInputTokens;
	private int maxOutputTokens;
	private String version;
	private String endpoint;
	private TextMapPropagator propagator;        

	public OllamaChat(
			String endpoint, 
			String provider,
			String model,
			String version,
			int maxInputTokens,
			int maxOutputTokens,
			OpenTelemetry openTelemetry) {
		this.endpoint = endpoint;
		this.provider = provider;
		this.model = model;
		this.version = version;
		this.maxInputTokens = maxInputTokens;
		this.maxOutputTokens = maxOutputTokens;
						 
		tracer = openTelemetry.getTracer(getInstrumentationScopeName(), getInstrumentationScopeVersion());
		propagator = openTelemetry.getPropagators().getTextMapPropagator();
		Meter meter = openTelemetry.getMeter(getInstrumentationScopeName());

		promptTokenCounter = meter
				.counterBuilder(provider + "_" + model + "_prompt")
				.setDescription("Prompt token usage")
				.setUnit("token")
				.build();

		completionTokenCounter = meter
				.counterBuilder(provider + "_" + model + "_completion")
				.setDescription("Completion token usage")
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
	public int getMaxInputTokens() {
		return maxInputTokens;
	}

	@Override
	public int getMaxOutputTokens() {
		return maxOutputTokens;
	}

	@Override
	public String getVersion() {
		return version;
	}
	
	protected String spanName() {
		String spanName = "Chat " + provider + " " + model + " " + endpoint;
        if (!Util.isBlank(version)) {
        	spanName += " " + version;
        }
		return spanName;
	}	
	
	protected JSONObject marshal(List<Message> messages) {
		JSONObject ret = new JSONObject();
		ret.put("model", model);
		ret.put("stream", false);
		JSONArray jMessages = new JSONArray();
		ret.put("messages", jMessages);
		for (Message message: messages) {
			JSONObject jMessage = new JSONObject();
			jMessages.put(jMessage);
			jMessage.put(ROLE_KEY, message.getRole());
			if (!Util.isBlank(message.getContent())) {
				jMessage.put(CONTENT_KEY, message.getContent());
			}
			if (!message.getImages().isEmpty()) {
				JSONArray jImages = new JSONArray();
				jMessage.put(IMAGES_KEY, jImages);
				for (String img: message.getImages()) {
					int base64Idx = img.indexOf(BASE_64);
					if (base64Idx != -1) {
						img = img.substring(base64Idx + BASE_64.length());
					}
					jImages.put(img);
				}
			}
		}						
		return ret;
	}
	
	protected List<ResponseMessage> unmarshal(String response, Span span) {
		JSONObject jResponse = new JSONObject(response);
		if (jResponse.has(ERROR_KEY)) {
			throw new NasdanikaException(jResponse.getString(ERROR_KEY));
		}
		JSONObject jMessage = jResponse.getJSONObject("message");		
		
		int promptTokens = jResponse.getInt("prompt_eval_count");
		promptTokenCounter.add(promptTokens);
		span.setAttribute("prompt-tokens", promptTokens);
		
		int completionTokens = jResponse.getInt("eval_count");
		completionTokenCounter.add(completionTokens);
		span.setAttribute("completion-tokens", completionTokens);
		
		return Collections.singletonList(new ResponseMessage() {
			
			@Override
			public String getRole() {
				return jMessage.getString(ROLE_KEY);
			}
			
			@Override
			public String getContent() {
				return jMessage.getString(CONTENT_KEY);
			}
			
			@Override
			public String getRefusal() {
				return null;
			}
			
			@Override
			public String getFinishReason() {
				return jResponse.getString("done_reason");
			}
		});
	}

	@Override
	public Mono<List<ResponseMessage>> chatAsync(List<Message> messages) {
		return Mono.deferContextual(contextView -> {
			Context parentContext = contextView.getOrDefault(Context.class, Context.current());
		
			Span span = tracer.spanBuilder(spanName())
					.setSpanKind(SpanKind.CLIENT)
					.setParent(parentContext)
					.startSpan();
				
			try (Scope scope = span.makeCurrent()) {									
				for (Message message: messages) {
					span.addEvent(
						"input." + message.getRole(), 
						Attributes.of(AttributeKey.stringKey("content"), message.getContent()));
				}
				
				HttpClient client = HttpClient.create()
						.headers(headerBuilder -> {
							propagator.inject(Context.current(), headerBuilder, (c, k, v) -> c.set(k,v));	
						})
						.followRedirect(true);
				
				return client.post()
					.uri(endpoint + "chat")		
					.send(ByteBufFlux.fromString(Mono.just(marshal(messages).toString(2))))
					.responseContent()
					.aggregate()
					.asString()
					.map(result -> {
						List<ResponseMessage> response = unmarshal(result, span);
						for (ResponseMessage message: response) {
							Attributes messageAttributes = Attributes.builder()
								.put("content", message.getContent())
								.put("finishReason", message.getFinishReason())
								.put("refusal", message.getRefusal())
								.build();					
							span.addEvent("response." + message.getRole(), messageAttributes);
						}		
						span.setStatus(StatusCode.OK);
						return response;
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
