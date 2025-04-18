package org.nasdanika.ai.openai.tests.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.nasdanika.ai.Chat;
import org.nasdanika.ai.Chat.ResponseMessage;
import org.nasdanika.ai.Embeddings;
import org.nasdanika.capability.CapabilityLoader;
import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.capability.ServiceCapabilityFactory.Requirement;
import org.nasdanika.common.PrintStreamProgressMonitor;
import org.nasdanika.common.ProgressMonitor;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

public class TestAI {
	
	@Test
	public void testEmbeddings() throws Exception {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();				
		try {
			OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
			assertNotNull(openTelemetry);			
			
			Requirement<Embeddings.Requirement, Embeddings> requirement = ServiceCapabilityFactory.createRequirement(Embeddings.class);			
			Iterable<CapabilityProvider<Embeddings>> embeddingsProviders = capabilityLoader.load(requirement, progressMonitor);
			List<Embeddings> allEmbeddings = new ArrayList<>();
			embeddingsProviders.forEach(ep -> allEmbeddings.addAll(ep.getPublisher().collect(Collectors.toList()).block()));
	        Tracer tracer = openTelemetry.getTracer("test.ai");        
	        Span span = tracer
	        	.spanBuilder("Embeddings")
	        	.startSpan();
	        
	        try (Scope scope = span.makeCurrent()) {
				for (Embeddings embeddings: allEmbeddings) {				
					assertNotNull(embeddings);
					System.out.println("=== Embeddings ===");
					System.out.println("Name:\t" + embeddings.getName());
					System.out.println("Provider:\t" + embeddings.getProvider());
					System.out.println("Max input:\t" + embeddings.getMaxInputTokens());
					System.out.println("Dimensions:\t" + embeddings.getDimensions());
							
		        	for (Entry<String, List<List<Float>>> vectors: embeddings.generate(List.of("Hello world!", "Hello universe!")).entrySet()) {		
		        		System.out.println("\t" + vectors.getKey());
		        		for (List<Float> vector: vectors.getValue()) {
		        			System.out.println("\t\t" + vector.size());
		        		}
		        	}
				}
	        } finally {
	        	span.end();
	        }
		} finally {
			capabilityLoader.close(progressMonitor);
		}
	}
	
	// -Dotel.java.global-autoconfigure.enabled=true -Dotel.service.name=test.embeddings.ollama
	
	@Test
	public void testOllamaEmbeddings() throws Exception {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();				
		try {
			OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
			assertNotNull(openTelemetry);			
			
			Embeddings.Requirement eReq = new Embeddings.Requirement("Ollama", null, null, 0, 0);
			Requirement<Embeddings.Requirement, Embeddings> requirement = ServiceCapabilityFactory.createRequirement(Embeddings.class, null, eReq);			
			Iterable<CapabilityProvider<Embeddings>> embeddingsProviders = capabilityLoader.load(requirement, progressMonitor);
			List<Embeddings> allEmbeddings = new ArrayList<>();
			embeddingsProviders.forEach(ep -> ep.getPublisher().subscribe(allEmbeddings::add));
			for (Embeddings embeddings: allEmbeddings) {				
				assertNotNull(embeddings);
				System.out.println("=== Embeddings ===");
				System.out.println("Name:\t" + embeddings.getName());
				System.out.println("Provider:\t" + embeddings.getProvider());
				System.out.println("Max input:\t" + embeddings.getMaxInputTokens());
				System.out.println("Dimensions:\t" + embeddings.getDimensions());
						
		        Tracer tracer = openTelemetry.getTracer("test.ai");        
		        Span span = tracer
		        	.spanBuilder("Embeddings")
		        	.startSpan();
		        
		        try (Scope scope = span.makeCurrent()) {
		        	Thread.sleep(200);
		        	for (Entry<String, List<List<Float>>> vectors: embeddings.generate(List.of("Hello world!", "Hello universe!")).entrySet()) {		
		        		System.out.println("\t" + vectors.getKey());
		        		for (List<Float> vector: vectors.getValue()) {
		        			System.out.println("\t\t" + vector.size());
		        		}
		        	}
		        	span.setStatus(StatusCode.OK);
		        } finally {
		        	span.end();
		        }
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
			Requirement<Embeddings.Requirement, Embeddings> requirement = ServiceCapabilityFactory.createRequirement(Embeddings.class);			
			Embeddings embeddings = capabilityLoader.loadOne(requirement, progressMonitor);
			assertNotNull(embeddings);
			assertEquals("text-embedding-ada-002", embeddings.getName());
			assertEquals("OpenAI", embeddings.getProvider());
			assertEquals(1536, embeddings.getDimensions());
			
			OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
			assertNotNull(openTelemetry);
	
	        Tracer tracer = openTelemetry.getTracer("test.ai");        
	        Span span = tracer
	        	.spanBuilder("Embeddings")
	        	.startSpan();
	        
        	List<List<Float>> vectors = embeddings
        		.generateAsync("Hello world!")
        		.contextWrite(reactor.util.context.Context.of(Context.class, Context.current().with(span)))
        		.doFinally(signal -> span.end())
        		.block();

    		for (List<Float> vector: vectors) {
    			System.out.println(vector.size());
    		}
		} finally {
			capabilityLoader.close(progressMonitor);
		}
	}
	
	@Test
	public void testOpenAIAsyncEmbeddingsPropagation() throws InterruptedException {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
		try {
			Requirement<Embeddings.Requirement, Embeddings> requirement = ServiceCapabilityFactory.createRequirement(Embeddings.class);			
			Embeddings embeddings = capabilityLoader.loadOne(requirement, progressMonitor);
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
		        Tracer tracer = openTelemetry.getTracer("test.ai");        
		        Span span = tracer
		        	.spanBuilder("Embeddings")
		        	.setParent(rootSpanContext)
		        	.startSpan();
		        
		        try (Scope scope = span.makeCurrent()) {
		        	List<List<Float>> vectors = embeddings
		        		.generateAsync("Hello world!")
		        		.contextWrite(reactor.util.context.Context.of(Context.class, Context.current().with(span)))
		        		.block();
		        	
		        	vectors.forEach(vector -> System.out.println(vector.size()));
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
	public void testChat() {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
		OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);

		List<Chat> chats = new ArrayList<>();		
		try {
			Requirement<Chat.Requirement, Chat> requirement = ServiceCapabilityFactory.createRequirement(Chat.class);			
			for (CapabilityProvider<Chat> chatProvider: capabilityLoader.<Chat>load(requirement, progressMonitor)) {
				chatProvider.getPublisher().subscribe(chats::add);
			}
			
	        Tracer tracer = openTelemetry.getTracer("test.ai");        
	        Span span = tracer
	        	.spanBuilder("Chat")
	        	.startSpan();
	        try (Scope scope = span.makeCurrent()) {			
				for (Chat chat: chats) {
					chat(chat, openTelemetry);				
				}
	        } finally {
	        	span.end();
	        }
		} finally {
			capabilityLoader.close(progressMonitor);
		}
	}
	
	private void chat(Chat chat, OpenTelemetry openTelemetry) {
		System.out.println("=== Chat ===");
		assertNotNull(chat);
		System.out.println("Name:\t" + chat.getName());
		System.out.println("Provider:\t" + chat.getProvider());
		System.out.println("Max input:\t" + chat.getMaxInputTokens());
		System.out.println("Max output:\t" + chat.getMaxOutputTokens());
        
    	List<ResponseMessage> responses = chat.chat(
    		Chat.Role.system.createMessage("You are a helpful assistant. You will talk like a pirate."),
    		Chat.Role.user.createMessage("Can you help me?"),
    		Chat.Role.system.createMessage("Of course, me hearty! What can I do for ye?"),
    		Chat.Role.user.createMessage("What's the best way to train a parrot?")
    	);
    	
    	for (ResponseMessage response: responses) {
    		System.out.println(response.getContent());
    	}
	}
	
	@Test
	public void testOllamaChat() {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new PrintStreamProgressMonitor();
		OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
		assertNotNull(openTelemetry);

		List<Chat> chats = new ArrayList<>();		
		try {
			Chat.Requirement cReq = new Chat.Requirement("Ollama", "llama3.2", null);
			Requirement<Chat.Requirement, Chat> requirement = ServiceCapabilityFactory.createRequirement(Chat.class, null, cReq);			
			for (CapabilityProvider<Chat> chatProvider: capabilityLoader.<Chat>load(requirement, progressMonitor)) {
				chatProvider.getPublisher().subscribe(chats::add);
			}
			
			for (Chat chat: chats) {
				chat(chat, openTelemetry);				
			}						
		} finally {
			capabilityLoader.close(progressMonitor);
		}
	}
	
//	@Test
//	public void testMonoError() {
//		String result = Mono.just("Hello")
//		.map(str -> {
//			if (1 > 0) {
//				throw new RuntimeException("uh");
//			}
//			return str + " World";
//		})
//		.onErrorMap(error -> {
//			error.printStackTrace();
//			return error;
//		})
//		.doFinally(signal -> System.out.println(signal))
//		.block();
//		System.out.println(result);		
//	}
	
}
