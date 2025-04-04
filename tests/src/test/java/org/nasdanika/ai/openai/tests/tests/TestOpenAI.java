package org.nasdanika.ai.openai.tests.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.nasdanika.ai.Chat;
import org.nasdanika.ai.Chat.ResponseMessage;
import org.nasdanika.capability.CapabilityLoader;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.capability.ServiceCapabilityFactory.Requirement;
import org.nasdanika.common.PrintStreamProgressMonitor;
import org.nasdanika.common.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.azure.ai.openai.OpenAIAsyncClient;
import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatChoice;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatRequestAssistantMessage;
import com.azure.ai.openai.models.ChatRequestMessage;
import com.azure.ai.openai.models.ChatRequestSystemMessage;
import com.azure.ai.openai.models.ChatRequestUserMessage;
import com.azure.ai.openai.models.ChatResponseMessage;
import com.azure.ai.openai.models.CompletionsUsage;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import reactor.core.publisher.Mono;

public class TestOpenAI {
	
	@Test
	public void testOpenAIChat() {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
		try {
			Requirement<String, OpenAIClientBuilder> requirement = ServiceCapabilityFactory.createRequirement(
					OpenAIClientBuilder.class,
					null,
					"https://api.openai.com/v1/");
			
			OpenAIClientBuilder builder = capabilityLoader.loadOne(requirement, progressMonitor);
			assertNotNull(builder);
			
			OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
			assertNotNull(openTelemetry);
			
	        OpenAIClient client = builder.buildClient();
	
	        Tracer tracer = openTelemetry.getTracer("openai");        
	        Span span = tracer
	        	.spanBuilder("Chat")
	        	.startSpan();
	        
	        try (Scope scope = span.makeCurrent()) {
	        	long start = System.currentTimeMillis();
		        List<ChatRequestMessage> chatMessages = new ArrayList<>();
		        chatMessages.add(new ChatRequestSystemMessage("You are a helpful assistant. You will talk like a pirate."));
		        chatMessages.add(new ChatRequestUserMessage("Can you help me?"));
		        chatMessages.add(new ChatRequestAssistantMessage("Of course, me hearty! What can I do for ye?"));
		        chatMessages.add(new ChatRequestUserMessage("What's the best way to train a parrot?"));
		
		        String deploymentOrModelId = "gpt-3.5-turbo";
		        ChatCompletions chatCompletions = client.getChatCompletions(deploymentOrModelId, new ChatCompletionsOptions(chatMessages));
		
		        System.out.printf("Model ID=%s is created at %s.%n", chatCompletions.getId(), chatCompletions.getCreatedAt());
		        for (ChatChoice choice : chatCompletions.getChoices()) {
		            ChatResponseMessage message = choice.getMessage();
		            System.out.printf("Index: %d, Chat Role: %s.%n", choice.getIndex(), message.getRole());
		            System.out.println("Message:");
		            System.out.println(message.getContent());
		        }
		
		        System.out.println();
		        CompletionsUsage usage = chatCompletions.getUsage();
		        System.out.printf("Usage: number of prompt token is %d, "
		                + "number of completion token is %d, and number of total tokens in request and response is %d.%n",
		            usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
				
		        Meter meter = openTelemetry.getMeter("openai.chat");
		        meter.counterBuilder("prompt").setDescription("Prompt tokens").setUnit("token").build().add(usage.getPromptTokens());
		        meter.counterBuilder("completion").setDescription("Completion tokens").setUnit("token").build().add(usage.getCompletionTokens());
		        meter.counterBuilder("total").setDescription("Total tokens").setUnit("token").build().add(usage.getTotalTokens());		        
				
		        Logger logger = LoggerFactory.getLogger(TestOpenAI.class);
		        logger.info("My test message");
		        
		        long duration = System.currentTimeMillis() - start;		        
		        meter
		        	.histogramBuilder("duration.histogram")
		        	.setDescription("Request duration histogram")
		        	.setUnit("milliseconds")
		        	.build()
		        	.record(duration);
		        
		        meter
		        	.gaugeBuilder("duration.gauge")
		        	.setDescription("Request duration gauge")
		        	.setUnit("milliseconds")
		        	.build()
		        	.set(duration);		        
		        
	        } finally {
	        	span.end();
	        }
		} finally {
			capabilityLoader.close(progressMonitor);
		}
	}
	
	@Test
	public void testOpenAIChatAsync() {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
		try {
			Requirement<String, OpenAIClientBuilder> requirement = ServiceCapabilityFactory.createRequirement(
					OpenAIClientBuilder.class,
					null,
					"https://api.openai.com/v1/chat/completions");
			
			OpenAIClientBuilder builder = capabilityLoader.loadOne(requirement, progressMonitor);
			assertNotNull(builder);
			
			OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
			assertNotNull(openTelemetry);
			
	        OpenAIAsyncClient client = builder.buildAsyncClient();
	
	        Tracer tracer = openTelemetry.getTracer("openai");        
	        Span span = tracer
	        	.spanBuilder("Chat")
	        	.startSpan();
	        
	        try (Scope scope = span.makeCurrent()) {
	        	long start = System.currentTimeMillis();
		        List<ChatRequestMessage> chatMessages = new ArrayList<>();
		        chatMessages.add(new ChatRequestSystemMessage("You are a helpful assistant. You will talk like a pirate."));
		        chatMessages.add(new ChatRequestUserMessage("Can you help me?"));
		        chatMessages.add(new ChatRequestAssistantMessage("Of course, me hearty! What can I do for ye?"));
		        chatMessages.add(new ChatRequestUserMessage("What's the best way to train a parrot?"));
		
		        String deploymentOrModelId = "gpt-3.5-turbo";
		        Mono<ChatCompletions> chatCompletionsMono = client.getChatCompletions(deploymentOrModelId, new ChatCompletionsOptions(chatMessages));
		        chatCompletionsMono.subscribe(chatCompletions -> {
			        
		    		
			        System.out.printf("Model ID=%s is created at %s.%n", chatCompletions.getId(), chatCompletions.getCreatedAt());
			        for (ChatChoice choice : chatCompletions.getChoices()) {
			            ChatResponseMessage message = choice.getMessage();
			            System.out.printf("Index: %d, Chat Role: %s.%n", choice.getIndex(), message.getRole());
			            System.out.println("Message:");
			            System.out.println(message.getContent());
			        }
			
			        System.out.println();
			        CompletionsUsage usage = chatCompletions.getUsage();
			        System.out.printf("Usage: number of prompt token is %d, "
			                + "number of completion token is %d, and number of total tokens in request and response is %d.%n",
			            usage.getPromptTokens(), usage.getCompletionTokens(), usage.getTotalTokens());
					
			        Meter meter = openTelemetry.getMeter("openai.chat");
			        meter.counterBuilder("prompt").setDescription("Prompt tokens").setUnit("token").build().add(usage.getPromptTokens());
			        meter.counterBuilder("completion").setDescription("Completion tokens").setUnit("token").build().add(usage.getCompletionTokens());
			        meter.counterBuilder("total").setDescription("Total tokens").setUnit("token").build().add(usage.getTotalTokens());		        
					
			        Logger logger = LoggerFactory.getLogger(TestOpenAI.class);
			        logger.info("My test message");
			        
			        long duration = System.currentTimeMillis() - start;		        
			        meter
			        	.histogramBuilder("duration.histogram")
			        	.setDescription("Request duration histogram")
			        	.setUnit("milliseconds")
			        	.build()
			        	.record(duration);
			        
			        meter
			        	.gaugeBuilder("duration.gauge")
			        	.setDescription("Request duration gauge")
			        	.setUnit("milliseconds")
			        	.build()
			        	.set(duration);
			      span.end();  			        
		        });		        
	        }
		} finally {
			capabilityLoader.close(progressMonitor);
		}
	}
	
	@Test
	public void testOpenAIEmbeddings() throws Exception {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
		try {
			Requirement<Void, org.nasdanika.ai.Embeddings> requirement = ServiceCapabilityFactory.createRequirement(org.nasdanika.ai.Embeddings.class);			
			org.nasdanika.ai.Embeddings embeddings = capabilityLoader.loadOne(requirement, progressMonitor);
			assertNotNull(embeddings);
			assertEquals("text-embedding-ada-002", embeddings.getName());
			assertEquals("OpenAI", embeddings.getProvider());
			assertEquals(1536, embeddings.getDimensions());
			
			OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
			assertNotNull(openTelemetry);
	
	        Tracer tracer = openTelemetry.getTracer("test.openai");        
	        Span span = tracer
	        	.spanBuilder("Embeddings")
	        	.startSpan();
	        
	        try (Scope scope = span.makeCurrent()) {
	        	Thread.sleep(200);
	        	List<Float> vector = embeddings.generate("Hello world!");
	        	System.out.println(vector.size());
	        } finally {
	        	span.end();
	        }
		} finally {
			capabilityLoader.close(progressMonitor);
		}
	}
	
	@Test
	public void testOpenAIAsyncEmbeddings() throws InterruptedException {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
		try {
			Requirement<Void, org.nasdanika.ai.Embeddings> requirement = ServiceCapabilityFactory.createRequirement(org.nasdanika.ai.Embeddings.class);			
			org.nasdanika.ai.Embeddings embeddings = capabilityLoader.loadOne(requirement, progressMonitor);
			assertNotNull(embeddings);
			assertEquals("text-embedding-ada-002", embeddings.getName());
			assertEquals("OpenAI", embeddings.getProvider());
			assertEquals(1536, embeddings.getDimensions());
			
			OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
			assertNotNull(openTelemetry);
	
	        Tracer tracer = openTelemetry.getTracer("test.openai");        
	        Span span = tracer
	        	.spanBuilder("Embeddings")
	        	.startSpan();
	        
        	embeddings
        		.generateAsync("Hello world!")
        		.contextWrite(reactor.util.context.Context.of(Context.class, Context.current().with(span)))
        		.doFinally(signal -> span.end())
        		.subscribe(vector -> System.out.println(vector.size()));
        	
        	Thread.sleep(5000);
		} finally {
			capabilityLoader.close(progressMonitor);
		}
	}
	
	@Test
	public void testOpenAIAsyncEmbeddingsPropagation() throws InterruptedException {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
		try {
			Requirement<Void, org.nasdanika.ai.Embeddings> requirement = ServiceCapabilityFactory.createRequirement(org.nasdanika.ai.Embeddings.class);			
			org.nasdanika.ai.Embeddings embeddings = capabilityLoader.loadOne(requirement, progressMonitor);
			assertNotNull(embeddings);
			assertEquals("text-embedding-ada-002", embeddings.getName());
			assertEquals("OpenAI", embeddings.getProvider());
			assertEquals(1536, embeddings.getDimensions());
			
			OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
			assertNotNull(openTelemetry);
						
	        Tracer rootTracer = openTelemetry.getTracer("test.openai.caller");
	        Span rootSpan = rootTracer
		        	.spanBuilder("Root span")
		        	.setNoParent()
		        	.startSpan();
	        Thread.sleep(200);
	        
	        Context rootSpanContext = Context.current().with(rootSpan);
	        try {	        		
		        Tracer tracer = openTelemetry.getTracer("test.openai");        
		        Span span = tracer
		        	.spanBuilder("Embeddings")
		        	.setParent(rootSpanContext)
		        	.startSpan();
		        
		        try (Scope scope = span.makeCurrent()) {
		        	List<Float> vector = embeddings
		        		.generateAsync("Hello world!")
		        		.contextWrite(reactor.util.context.Context.of(Context.class, Context.current().with(span)))
		        		.block();
		        	System.out.println(vector.size());
		        } finally {
		        	span.end();
		        }
		        Thread.sleep(200);
	        } finally {
	        	rootSpan.end();
	        }
		} finally {
			capabilityLoader.close(progressMonitor);
		}
	}	
	
	@Test
	public void testNasdanikaOpenAIChat() {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
		try {
			Requirement<Void, Chat> requirement = ServiceCapabilityFactory.createRequirement(Chat.class);			
			org.nasdanika.ai.Chat chat = capabilityLoader.loadOne(requirement, progressMonitor);
			assertNotNull(chat);
			assertEquals("gpt-3.5-turbo", chat.getName());
			assertEquals("OpenAI", chat.getProvider());
			assertEquals(16385, chat.getMaxInputTokens());
			assertEquals(4096, chat.getMaxOutputTokens());
			
			OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
			assertNotNull(openTelemetry);
	
	        Tracer tracer = openTelemetry.getTracer("test.openai");        
	        Span span = tracer
	        	.spanBuilder("Chat")
	        	.startSpan();
	        
	        try (Scope scope = span.makeCurrent()) {
	        	List<ResponseMessage> responses = chat.chat(
	        		Chat.Role.system.createMessage("You are a helpful assistant. You will talk like a pirate."),
	        		Chat.Role.user.createMessage("Can you help me?"),
	        		Chat.Role.system.createMessage("Of course, me hearty! What can I do for ye?"),
	        		Chat.Role.user.createMessage("What's the best way to train a parrot?")
	        	);
	        	
	        	for (ResponseMessage response: responses) {
	        		System.out.println(response.getContent());
	        	}
	        } finally {
	        	span.end();
	        }
		} finally {
			capabilityLoader.close(progressMonitor);
		}
	}
	
}
