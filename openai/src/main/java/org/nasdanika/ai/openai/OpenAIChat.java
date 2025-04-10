package org.nasdanika.ai.openai;

import java.lang.module.ModuleDescriptor.Version;
import java.util.ArrayList;
import java.util.List;

import org.nasdanika.ai.Chat;
import org.nasdanika.common.Util;

import com.azure.ai.openai.OpenAIAsyncClient;
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
	private LongCounter totalTokenCounter;
	private LongCounter promptTokenCounter;
	private LongCounter completionTokenCounter;
	private int maxInputTokens;
	private int maxOutputTokens;
	private String version;
	private OpenAIAsyncClient openAIAsyncClient;        

	public OpenAIChat(
			OpenAIClient openAIClient, 
			OpenAIAsyncClient openAIAsyncClient, 
			String provider,
			String model,
			String version,
			int maxInputTokens,
			int maxOutputTokens,
			OpenTelemetry openTelemetry) {
		this.openAIClient = openAIClient;
		this.openAIAsyncClient = openAIAsyncClient;
		this.provider = provider;
		this.model = model;
		this.version = version;
		this.maxInputTokens = maxInputTokens;
		this.maxOutputTokens = maxOutputTokens;
						 
		tracer = openTelemetry.getTracer(getInstrumentationScopeName(), getInstrumentationScopeVersion());
		Meter meter = openTelemetry.getMeter(getInstrumentationScopeName());

		totalTokenCounter = meter
			.counterBuilder(provider + "_" + model + "_total")
			.setDescription("Total token usage")
			.setUnit("token")
			.build();

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
	
	@Override
	public List<ResponseMessage> chat(List<Message> messages) {
        Span span = tracer
	        	.spanBuilder(spanName())
	        	.setSpanKind(SpanKind.CLIENT)
	        	.startSpan();
		
	    try (Scope scope = span.makeCurrent()) {
			for (Message message: messages) {
				span.addEvent(
					"input." + message.getRole(), 
					Attributes.of(AttributeKey.stringKey("content"), message.getContent()));
			}
	        ChatCompletions chatCompletions = openAIClient.getChatCompletions(model, createChatCompletionOptions(messages));
			List<ResponseMessage> response = mapCompletions(chatCompletions, span);
			for (ResponseMessage message: response) {
				Attributes messageAttributes = Attributes.builder()
					.put("content", message.getContent())
					.put("finishReason", message.getFinishReason())
					.put("refusal", message.getRefusal())
					.build();					
				span.addEvent("response." + message.getRole(), messageAttributes);
			}									
			return response;
	    } catch (RuntimeException e) {
			span.setStatus(StatusCode.ERROR);
			span.recordException(e);
			throw e;	    	
	    } finally {
	    	span.end();
	    }
	}

	protected ChatCompletionsOptions createChatCompletionOptions(List<Message> messages) {
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
		return chatCompletionOptions;
	}

	@Override
	public Mono<List<ResponseMessage>> chatAsync(List<Message> messages) {
		return Mono.deferContextual(contextView -> {
			Context parentContext = contextView.getOrDefault(Context.class, Context.current());
		
	        Span span = tracer
		        	.spanBuilder(spanName())
		        	.setSpanKind(SpanKind.CLIENT)
		        	.setParent(parentContext)
		        	.startSpan();
	
		    try (Scope scope = span.makeCurrent()) {
				for (Message message: messages) {
					span.addEvent(
						"input." + message.getRole(), 
						Attributes.of(AttributeKey.stringKey("content"), message.getContent()));
				}
		        Mono<ChatCompletions> chatCompletionsMono = openAIAsyncClient.getChatCompletions(model, createChatCompletionOptions(messages));
		        return chatCompletionsMono
					.map(result -> {
						List<ResponseMessage> response = mapCompletions(result, span);
						for (ResponseMessage message: response) {
							Attributes messageAttributes = Attributes.builder()
								.put("content", message.getContent())
								.put("finishReason", message.getFinishReason())
								.put("refusal", message.getRefusal())
								.build();					
							span.addEvent("response." + message.getRole(), messageAttributes);
						}												
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

	protected String spanName() {
		String spanName = "Chat " + provider + " " + model;
        if (!Util.isBlank(version)) {
        	spanName += " " + version;
        }
		return spanName;
	}
	
	protected List<ResponseMessage> mapCompletions(ChatCompletions chatCompletions, Span span) {

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
		
		int promptTokens = usage.getPromptTokens();
		promptTokenCounter.add(promptTokens);
		span.setAttribute("prompt-tokens", promptTokens);
		
		int completionTokens = usage.getCompletionTokens();
		completionTokenCounter.add(completionTokens);
		span.setAttribute("completion-tokens", completionTokens);
		
		int totalTokens = usage.getTotalTokens();
		totalTokenCounter.add(totalTokens);
		span.setAttribute("total-tokens", totalTokens);
		span.setStatus(StatusCode.OK);
		return ret;		
	}
	
}
