package org.nasdanika.ai.openai;

import java.lang.module.ModuleDescriptor.Version;
import java.util.ArrayList;
import java.util.List;

import org.nasdanika.ai.Chat;
import org.nasdanika.common.Util;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.ChatChoice;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestAssistantMessage;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.azure.ai.openai.models.ChatResponseMessage;
import com.azure.ai.openai.models.ChatRole;
import com.azure.ai.openai.models.CompletionsUsage;

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
public class OpenAIChat implements Chat {
	
	private OpenAIClient openAIClient;
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
	private DoubleHistogram totalTokenHistogram;
	private DoubleHistogram promptTokenHistogram;
	private DoubleHistogram completionTokenHistogram;
	private int maxInputTokens;
	private int maxOutputTokens;
	private String version;        

	public OpenAIChat(
			OpenAIClient openAIClient, 
			String provider,
			String model,
			String version,
			int maxInputTokens,
			int maxOutputTokens,
			OpenTelemetry openTelemetry) {
		this.openAIClient = openAIClient;
		this.provider = provider;
		this.model = model;
		this.version = version;
		this.maxInputTokens = maxInputTokens;
		this.maxOutputTokens = maxOutputTokens;
						 
		tracer = openTelemetry.getTracer(getInstrumentationScopeName(), getInstrumentationScopeVersion());
		Meter meter = openTelemetry.getMeter(getInstrumentationScopeName());

		totalTokenHistogram = meter
			.histogramBuilder(provider + "." + model + ".total")
			.setDescription("Token usage")
			.setUnit("token")
			.build();

		promptTokenHistogram = meter
				.histogramBuilder(provider + "." + model + ".prompt")
				.setDescription("Token usage")
				.setUnit("token")
				.build();

		completionTokenHistogram = meter
				.histogramBuilder(provider + "." + model + ".completion")
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
	
	@Override
	public List<ResponseMessage> chat(List<Message> messages) {
        String spanName = "Chat " + provider + " " + model;
        if (!Util.isBlank(version)) {
        	spanName += " " + version;
        }
		Span span = tracer
	        	.spanBuilder(spanName)
	        	.startSpan();

		span.setStatus(StatusCode.ERROR); // We set OK at before return		
		
	    try (Scope scope = span.makeCurrent()) {
	        List<ChatRequestMessage> chatMessages = new ArrayList<>();
	        for (Message message: messages) {
	        	String role = message.getRole();
	        	if (Util.isBlank(role)) {
	        		role = Role.user.name();
	        	}
	        	ChatRequestMessage chatMessage = 
		        	switch (role) {
		        	case "assistant" -> new ChatRequestAssistantMessage(message.getContent());
//		        	case "developer" -> new ChatRequestDeveloperMessage(message.getContent());
//		        	case "function" -> new ChatRequestFunctionMessage(message.getContent());
		        	case "system" -> new ChatRequestSystemMessage(message.getContent());
//		        	case "tool" -> new ChatRequestToolMessage(message.getContent());
		        	case "user" ->  new ChatRequestUserMessage(message.getContent());	
		        	default ->  new ChatRequestUserMessage(message.getContent());
		        	};
		        chatMessages.add(chatMessage);		        	
	        }
	        
	        ChatCompletionsOptions chatCompletionOptions = new ChatCompletionsOptions(chatMessages);
	        chatCompletionOptions.setModel(model);
	        ChatCompletions chatCompletions = openAIClient.getChatCompletions(model, new ChatCompletionsOptions(chatMessages));

	        interface IndexedResponseMessage extends ResponseMessage {
            	
            	int getIndex();
            	
            }
			
	        List<ResponseMessage> ret = new ArrayList<>();
	        for (ChatChoice choice : chatCompletions.getChoices()) {
	            ChatResponseMessage message = choice.getMessage();
	            ret.add(new IndexedResponseMessage() {

					@Override
					public String getRole() {
						ChatRole role = message.getRole();
						return role == null ? null : role.getValue();
					}

					@Override
					public String getContent() {
						return message.getContent();
					}

					@Override
					public String getRefusal() {
						return message.getRefusal();
					}

					@Override
					public String getFinishReason() {
						return choice.getFinishReason().getValue();
					}

					@Override
					public int getIndex() {
						return choice.getIndex();
					}
	            	
	            });
	        }
	        
	        ret.sort((a, b) -> ((IndexedResponseMessage) a).getIndex() - ((IndexedResponseMessage) b).getIndex());
			CompletionsUsage usage = chatCompletions.getUsage();
			
			promptTokenHistogram.record(usage.getPromptTokens());
			span.setAttribute("prompt-tokens", usage.getPromptTokens());
			
			completionTokenHistogram.record(usage.getCompletionTokens());
			span.setAttribute("completion-tokens", usage.getCompletionTokens());
			
			totalTokenHistogram.record(usage.getTotalTokens());
			span.setAttribute("total-tokens", usage.getTotalTokens());
			span.setStatus(StatusCode.OK);
			return ret;
	    } finally {
	    	span.end();
	    }
	}
	
}
