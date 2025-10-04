package org.nasdanika.ai.openai.tests.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.jupiter.api.Test;
import org.nasdanika.ai.CachingImageNarrator;
import org.nasdanika.ai.Chat;
import org.nasdanika.ai.Chat.ResponseMessage;
import org.nasdanika.ai.ChatImageNarrator;
import org.nasdanika.ai.EmbeddingGenerator;
import org.nasdanika.ai.ImageNarrator;
import org.nasdanika.ai.SearchResult;
import org.nasdanika.ai.SimilaritySearch;
import org.nasdanika.ai.TextFloatVectorChunkingEmbeddingModel;
import org.nasdanika.ai.TextFloatVectorEmbeddingModel;
import org.nasdanika.ai.TextFloatVectorEmbeddingResource;
import org.nasdanika.ai.TextFloatVectorEmbeddingResourceContents;
import org.nasdanika.ai.TextFloatVectorEncodingChunkingEmbeddingModel;
import org.nasdanika.capability.CapabilityLoader;
import org.nasdanika.capability.CapabilityProvider;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.capability.ServiceCapabilityFactory.Requirement;
import org.nasdanika.common.LoggerProgressMonitor;
import org.nasdanika.common.MarkdownHelper;
import org.nasdanika.common.ProgressMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jelmerk.hnswlib.core.DistanceFunctions;
import com.github.jelmerk.hnswlib.core.hnsw.HnswIndex;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TestAI {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TestAI.class);
	
	@Test
	public void testEmbeddings() throws Exception {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);				
		try {
			Requirement<EmbeddingGenerator.Requirement, TextFloatVectorEmbeddingModel> requirement = ServiceCapabilityFactory.createRequirement(TextFloatVectorEmbeddingModel.class);			
			Iterable<CapabilityProvider<TextFloatVectorEmbeddingModel>> embeddingsProviders = capabilityLoader.load(requirement, progressMonitor);
			List<TextFloatVectorEmbeddingModel	> allEmbeddings = new ArrayList<>();
			embeddingsProviders.forEach(ep -> allEmbeddings.addAll(ep.getPublisher().collect(Collectors.toList()).block()));
			for (TextFloatVectorEmbeddingModel embeddings: allEmbeddings) {				
				assertNotNull(embeddings);
				System.out.println("=== Embeddings ===");
				System.out.println("Name:\t" + embeddings.getName());
				System.out.println("Provider:\t" + embeddings.getProvider());
				System.out.println("Max input:\t" + embeddings.getMaxInputTokens());
				System.out.println("Dimensions:\t" + embeddings.getDimensions());
						
	        	for (List<Float> vector: embeddings.generate("Hello world!")) {		
        			System.out.println("\t\t" + vector.size());
	        	}
			}
		} finally {
			capabilityLoader.close(progressMonitor);
		}
	}
	
	@Test
	public void testEmbeddingsWithTelemetry() throws Exception {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);				
		try {
			OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
			assertNotNull(openTelemetry);			
			
			Requirement<EmbeddingGenerator.Requirement, TextFloatVectorEmbeddingModel> requirement = ServiceCapabilityFactory.createRequirement(TextFloatVectorEmbeddingModel.class);			
			Iterable<CapabilityProvider<TextFloatVectorEmbeddingModel>> embeddingsProviders = capabilityLoader.load(requirement, progressMonitor);
			List<TextFloatVectorEmbeddingModel	> allEmbeddings = new ArrayList<>();
			embeddingsProviders.forEach(ep -> allEmbeddings.addAll(ep.getPublisher().collect(Collectors.toList()).block()));
	        Tracer tracer = openTelemetry.getTracer("test.ai");        
	        Span span = tracer
	        	.spanBuilder("Embeddings")
	        	.startSpan();
	        
	        try (Scope scope = span.makeCurrent()) {
				for (TextFloatVectorEmbeddingModel embeddings: allEmbeddings) {				
					assertNotNull(embeddings);
					System.out.println("=== Embeddings ===");
					System.out.println("Name:\t" + embeddings.getName());
					System.out.println("Provider:\t" + embeddings.getProvider());
					System.out.println("Max input:\t" + embeddings.getMaxInputTokens());
					System.out.println("Dimensions:\t" + embeddings.getDimensions());
							
		        	for (List<Float> vector: embeddings.generate("Hello world!")) {		
	        			System.out.println("\t\t" + vector.size());
		        	}
				}
	        } finally {
	        	span.end();
	        }
		} finally {
			capabilityLoader.close(progressMonitor);
		}
	}
		
	@Test
	public void testEmbeddingsBatch() throws Exception {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);				
		try {
			OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
			assertNotNull(openTelemetry);			
			
			Requirement<EmbeddingGenerator.Requirement, TextFloatVectorEmbeddingModel> requirement = ServiceCapabilityFactory.createRequirement(TextFloatVectorEmbeddingModel.class);			
			Iterable<CapabilityProvider<TextFloatVectorEmbeddingModel>> embeddingsProviders = capabilityLoader.load(requirement, progressMonitor);
			List<TextFloatVectorEmbeddingModel> allEmbeddings = new ArrayList<>();
			embeddingsProviders.forEach(ep -> allEmbeddings.addAll(ep.getPublisher().collect(Collectors.toList()).block()));
	        Tracer tracer = openTelemetry.getTracer("test.ai");        
	        Span span = tracer
	        	.spanBuilder("Embeddings")
	        	.startSpan();
	        
	        try (Scope scope = span.makeCurrent()) {
				for (TextFloatVectorEmbeddingModel embeddings: allEmbeddings) {				
					assertNotNull(embeddings);
					System.out.println("=== Embeddings ===");
					System.out.println("Name:\t" + embeddings.getName());
					System.out.println("Provider:\t" + embeddings.getProvider());
					System.out.println("Max input:\t" + embeddings.getMaxInputTokens());
					System.out.println("Dimensions:\t" + embeddings.getDimensions());
										        
			        List<String> input = new ArrayList<>();
			        for (int i = 0; i < 77; ++i) {
			        	input.add("Hello " + i);
			        }
					
		        	for (Entry<String, List<List<Float>>> vectors: embeddings.generate(input).entrySet()) {		
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
		ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);				
		try {
			OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
			assertNotNull(openTelemetry);			
			
			EmbeddingGenerator.Requirement eReq = TextFloatVectorEmbeddingModel.createRequirement("Ollama", null, null);
			Requirement<EmbeddingGenerator.Requirement, TextFloatVectorEmbeddingModel> requirement = ServiceCapabilityFactory.createRequirement(TextFloatVectorEmbeddingModel.class, null, eReq);			
			Iterable<CapabilityProvider<TextFloatVectorEmbeddingModel>> embeddingsProviders = capabilityLoader.load(requirement, progressMonitor);
			List<TextFloatVectorEmbeddingModel> allEmbeddings = new ArrayList<>();
			embeddingsProviders.forEach(ep -> ep.getPublisher().subscribe(allEmbeddings::add));
			for (TextFloatVectorEmbeddingModel embeddings: allEmbeddings) {				
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
		ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);
		try {
			Requirement<EmbeddingGenerator.Requirement, TextFloatVectorEmbeddingModel> requirement = ServiceCapabilityFactory.createRequirement(TextFloatVectorEmbeddingModel.class);			
			TextFloatVectorEmbeddingModel embeddings = capabilityLoader.loadOne(requirement, progressMonitor);
			assertNotNull(embeddings);
			assertEquals("text-embedding-ada-002", embeddings.getName());
			assertEquals("OpenAI", embeddings.getProvider());
			assertEquals(1536, embeddings.getDimensions());
	        
        	List<List<Float>> vectors = embeddings.generateAsync("Hello world!").block();

    		for (List<Float> vector: vectors) {
    			System.out.println(vector.size());
    		}
		} finally {
			capabilityLoader.close(progressMonitor);
		}
	}
		
	@Test
	public void testOpenAIAsyncEmbeddingsWithTelemetry() throws InterruptedException {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);
		try {
			Requirement<EmbeddingGenerator.Requirement, TextFloatVectorEmbeddingModel> requirement = ServiceCapabilityFactory.createRequirement(TextFloatVectorEmbeddingModel.class);			
			TextFloatVectorEmbeddingModel embeddings = capabilityLoader.loadOne(requirement, progressMonitor);
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
	public void testOpenAIAsyncEmbeddingsBatch() throws InterruptedException {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);
		try {
			Requirement<EmbeddingGenerator.Requirement, TextFloatVectorEmbeddingModel> requirement = ServiceCapabilityFactory.createRequirement(TextFloatVectorEmbeddingModel.class);			
			TextFloatVectorEmbeddingModel embeddings = capabilityLoader.loadOne(requirement, progressMonitor);
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
	        
	        List<String> input = new ArrayList<>();
	        for (int i = 0; i < 77; ++i) {
	        	input.add("Hello " + i);
	        }
	        
	    	Map<String, List<List<Float>>> vectors = embeddings
	    		.generateAsync(input)
	    		.contextWrite(reactor.util.context.Context.of(Context.class, Context.current().with(span)))
	    		.doFinally(signal -> span.end())
	    		.block();
	
			for (Entry<String, List<List<Float>>> vector: vectors.entrySet()) {
				System.out.println(vector.getKey() + ": " +  vector.getValue().size());
			}
		} finally {
			capabilityLoader.close(progressMonitor);
		}
	}
	
	
	@Test
	public void testOpenAIAsyncEmbeddingsPropagation() throws InterruptedException {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);
		try {
			Requirement<EmbeddingGenerator.Requirement, TextFloatVectorEmbeddingModel> requirement = ServiceCapabilityFactory.createRequirement(TextFloatVectorEmbeddingModel.class);			
			TextFloatVectorEmbeddingModel embeddings = capabilityLoader.loadOne(requirement, progressMonitor);
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
		ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);
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
        
    	List<? extends ResponseMessage> responses = chat.chat(
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
	public void testOpenAIDescribeImage() {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);
		OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
	
		List<Chat> chats = new ArrayList<>();		
		try {
			Chat.Requirement cReq = new Chat.Requirement("OpenAI", "gpt-4o", null);
			Requirement<Chat.Requirement, Chat> requirement = ServiceCapabilityFactory.createRequirement(Chat.class, null, cReq);			
			for (CapabilityProvider<Chat> chatProvider: capabilityLoader.<Chat>load(requirement, progressMonitor)) {
				chatProvider.getPublisher().subscribe(chats::add);
			}
			
	        Tracer tracer = openTelemetry.getTracer("test.ai");        
	        Span span = tracer
	        	.spanBuilder("Chat")
	        	.startSpan();
	        try (Scope scope = span.makeCurrent()) {			
				for (Chat chat: chats) {
					describeImage(chat, openTelemetry);				
				}
	        } finally {
	        	span.end();
	        }
		} finally {
			capabilityLoader.close(progressMonitor);
		}
	}	
	
	@Test
	public void testOpenAINarrateImage() {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);
		OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
	
		List<Chat> chats = new ArrayList<>();		
		try {
			Chat.Requirement cReq = new Chat.Requirement("OpenAI", "gpt-4o", null);
			Requirement<Chat.Requirement, Chat> requirement = ServiceCapabilityFactory.createRequirement(Chat.class, null, cReq);			
			for (CapabilityProvider<Chat> chatProvider: capabilityLoader.<Chat>load(requirement, progressMonitor)) {
				chatProvider.getPublisher().subscribe(chats::add);
			}
			
	        Tracer tracer = openTelemetry.getTracer("test.ai");        
	        Span span = tracer
	        	.spanBuilder("Chat")
	        	.startSpan();
	        try (Scope scope = span.makeCurrent()) {			
				for (Chat chat: chats) {
					narrateImage(chat, openTelemetry);				
				}
	        } finally {
	        	span.end();
	        }
		} finally {
			capabilityLoader.close(progressMonitor);
		}
	}
		
	@Test
	public void testOpenAICachedNarrateImage() {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);
		OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
	
		List<Chat> chats = new ArrayList<>();		
		try {
			Chat.Requirement cReq = new Chat.Requirement("OpenAI", "gpt-4o", null);
			Requirement<Chat.Requirement, Chat> requirement = ServiceCapabilityFactory.createRequirement(Chat.class, null, cReq);			
			for (CapabilityProvider<Chat> chatProvider: capabilityLoader.<Chat>load(requirement, progressMonitor)) {
				chatProvider.getPublisher().subscribe(chats::add);
			}
			
	        Tracer tracer = openTelemetry.getTracer("test.ai");        
	        Span span = tracer
	        	.spanBuilder("Chat")
	        	.startSpan();
	        try (Scope scope = span.makeCurrent()) {			
				for (Chat chat: chats) {
					Map<String,String> cache = new HashMap<>();
					narrateImage(chat, openTelemetry, cache);				
					narrateImage(chat, openTelemetry, cache);
					System.out.println(cache);
				}
	        } finally {
	        	span.end();
	        }
		} finally {
			capabilityLoader.close(progressMonitor);
		}
	}		
	
	@Test
	public void testOllamaDescribeImage() {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);
		OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
	
		List<Chat> chats = new ArrayList<>();		
		try {
			Chat.Requirement cReq = new Chat.Requirement("Ollama", "llava", null);
			Requirement<Chat.Requirement, Chat> requirement = ServiceCapabilityFactory.createRequirement(Chat.class, null, cReq);			
			for (CapabilityProvider<Chat> chatProvider: capabilityLoader.<Chat>load(requirement, progressMonitor)) {
				chatProvider.getPublisher().subscribe(chats::add);
			}
			
	        Tracer tracer = openTelemetry.getTracer("test.ai");        
	        Span span = tracer
	        	.spanBuilder("Chat")
	        	.startSpan();
	        try (Scope scope = span.makeCurrent()) {			
				for (Chat chat: chats) {
					describeImage(chat, openTelemetry);				
				}
	        } finally {
	        	span.end();
	        }
		} finally {
			capabilityLoader.close(progressMonitor);
		}
	}	
	
	private void describeImage(Chat chat, OpenTelemetry openTelemetry) {
		System.out.println("=== Describe image ===");
		assertNotNull(chat);
		System.out.println("Name:\t" + chat.getName());
		System.out.println("Provider:\t" + chat.getProvider());
		System.out.println("Max input:\t" + chat.getMaxInputTokens());
		System.out.println("Max output:\t" + chat.getMaxOutputTokens());
        
    	List<? extends ResponseMessage> responses = chat.chat(
    		Chat.Role.user.createMessage("Describe this image").addImage(new File("llama.png"))
    	);
    	
    	for (ResponseMessage response: responses) {
    		System.out.println(response.getContent());
    	}
	}
		
	private void narrateImage(Chat chat, OpenTelemetry openTelemetry) {
		System.out.println("=== Narrate image ===");
		assertNotNull(chat);
		System.out.println("Name:\t" + chat.getName());
		System.out.println("Provider:\t" + chat.getProvider());
		System.out.println("Max input:\t" + chat.getMaxInputTokens());
		System.out.println("Max output:\t" + chat.getMaxOutputTokens());
        
		ChatImageNarrator chatImageNarrator = new ChatImageNarrator(chat);
		String narration = chatImageNarrator.asFileEmbeddingGenerator().generate(new File("llama.png"));
		System.out.println(narration);
	}
		
	private void narrateImage(Chat chat, OpenTelemetry openTelemetry, Map<String, String> cache) {
		System.out.println("=== Narrate image with cache ===");	    
		ChatImageNarrator chatImageNarrator = new ChatImageNarrator(chat);
		ImageNarrator cachingImageNarrator = new CachingImageNarrator(chatImageNarrator, cache);
		String narration = cachingImageNarrator.asFileEmbeddingGenerator().generateAsync(new File("llama.png")).block();
		System.out.println(narration);
	}	
	
	
	@Test
	public void testOllamaChat() {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);
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
		
	/**
	 * Generates OpenAI embeddings for site pages and adds them to search-documents-embeddings.json
	 * @throws InterruptedException
	 */
	@Test
	public void testGenerateSearchDocumentsJSONOpenAIAsyncEmbeddings() throws Exception {
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);
		try {
			EmbeddingGenerator.Requirement eReq = TextFloatVectorEmbeddingModel.createRequirement(
					"OpenAI", 
					"text-embedding-ada-002", 
					null);
			Requirement<EmbeddingGenerator.Requirement, TextFloatVectorEmbeddingModel> requirement = ServiceCapabilityFactory.createRequirement(TextFloatVectorEmbeddingModel.class, null, eReq);			
			TextFloatVectorEmbeddingModel embeddings = capabilityLoader.loadOne(requirement, progressMonitor);
			assertNotNull(embeddings);
			assertEquals("text-embedding-ada-002", embeddings.getName());
			assertEquals("OpenAI", embeddings.getProvider());
			assertEquals(1536, embeddings.getDimensions());
			
			OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
			assertNotNull(openTelemetry);
	
	        Tracer tracer = openTelemetry.getTracer("test.ai");        
	        Span span = tracer
	        	.spanBuilder("Search embeddings")
	        	.startSpan();
	        
	        int documents = 0;
	        int textLength = 0; 
	        AtomicInteger vectorCount = new AtomicInteger();
	        AtomicInteger tokenCount = new AtomicInteger();
	        
	        try (Scope scope = span.makeCurrent()) {	        
		        Collection<Mono<List<List<Float>>>> tasks = new ArrayList<>();
		        TextFloatVectorEmbeddingModel chunkingEmbeddings = new TextFloatVectorEncodingChunkingEmbeddingModel(
		        		embeddings, 
		        		1000, 
		        		20, 
		        		EncodingType.CL100K_BASE) {
		        	
		        	@Override
		        	protected IntArrayList encode(String input) {
		        		IntArrayList tokens = super.encode(input);
		        		tokenCount.addAndGet(tokens.size());
						return tokens;
		        	}
		        };
		        
				File input =  new File("../../nasdanika.github.io/docs/search-documents.json").getCanonicalFile();
				try (InputStream in = new FileInputStream(input)) {
					JSONObject jsonObject = new JSONObject(new JSONTokener(in));
					for (String path: jsonObject.keySet()) {		
						JSONObject data = jsonObject.getJSONObject(path);
						JSONArray ea = new JSONArray();
						data.put("embeddings", ea);
						String content = data.getString("content");
						textLength += content.length();
						++documents;
						Mono<List<List<Float>>> task = chunkingEmbeddings
							.generateAsync(content)
							.map(vectors -> {
								JSONObject jEmbeddings = new JSONObject();
								jEmbeddings.put("provider", embeddings.getProvider());
								jEmbeddings.put("model", embeddings.getName());
								String version = embeddings.getVersion();
								if (version != null) {
									jEmbeddings.put("version", embeddings.getVersion());
								}
								jEmbeddings.put("dimensions", embeddings.getDimensions());
								JSONArray jVectors = new JSONArray();
								jEmbeddings.put("vectors", jVectors);
								for (List<Float> vector: vectors) {
									jVectors.put(vector);
									vectorCount.incrementAndGet();
								}
								
								synchronized (jsonObject) {
									ea.put(jEmbeddings);
									System.out.print("." + vectors.size());
								}
								return vectors;
							})
							.contextWrite(reactor.util.context.Context.of(Context.class, Context.current().with(span)));
						
						tasks.add(task);
					}
					Mono.zip(tasks, Function.identity()).block();
					File output =  new File("../../nasdanika.github.io/docs/search-documents-embeddings.json");
					try (Writer writer = new FileWriter(output)) {
						jsonObject.write(writer);
					}					
				}					        
	        } finally {
	        	span.end();
	        }
	        System.out.println("Documents " + documents + ", length " + textLength + ", tokens " + tokenCount + ", vectors " + vectorCount.get());
		} finally {
			capabilityLoader.close(progressMonitor);
		}
	}
	
	@Test
	public void testRAG() throws Exception {
		// Creating a embeddings resource set from search-documents-embeddings.json
		Collection<TextFloatVectorEmbeddingResourceContents> resourceContents = new ArrayList<>(); 
		File input =  new File("test-data/search-documents-embeddings.json").getCanonicalFile();
		try (InputStream in = new FileInputStream(input)) {
			JSONObject jsonObject = new JSONObject(new JSONTokener(in));
			for (String path: jsonObject.keySet()) {		
				JSONObject data = jsonObject.getJSONObject(path);
				JSONArray ea = data.getJSONArray("embeddings");
				for (int i = 0; i < ea.length(); ++i) {
					JSONObject embeddings = ea.getJSONObject(i);				
					resourceContents.add(new TextFloatVectorEmbeddingResourceContents() {
						
						@Override
						public String getVersion() {							
							return embeddings.optString("version");
						}
						
						@Override
						public String getProvider() {
							return embeddings.getString("provider");
						}
						
						@Override
						public String getName() {
							return embeddings.getString("model");
						}
						
						@Override
						public String getUri() {
							return "https://docs.nasdanika.org/" + path;
						}
						
						@Override
						public List<List<Float>> getEmbeddings() {
							JSONArray vectors = embeddings.getJSONArray("vectors");
							List<List<Float>> ret = new ArrayList<>();
							for (int i = 0; i < vectors.length(); ++i) {
								JSONArray vector = vectors.getJSONArray(i);
								List<Float> v = new ArrayList<>();
								for (int j = 0; j < vector.length(); ++j) {
									v.add(vector.getFloat(j));
								}
								ret.add(v);
							}
							return ret;
						}
						
						@Override
						public int getDimensions() {
							return embeddings.getInt("dimensions");
						}
						
						@Override
						public String getContents() {
							return data.getString("content");
						}

						@Override
						public String getMimeType() {
							return "text/plain";
						}
					});
				}
			}
		}
				
		TextFloatVectorEmbeddingResource resource = new TextFloatVectorEmbeddingResource() {
			
			@Override
			public Flux<TextFloatVectorEmbeddingResourceContents> getContents() {
				return Flux.fromIterable(resourceContents);
			}

			@Override
			public String getMimeType() {
				return "text/plain";
			}
			
		};
		
		// Similarity search index				
		HnswIndex<SimilaritySearch.IndexId, float[], SimilaritySearch.EmbeddingsItem, Float> hnswIndex = HnswIndex
			.newBuilder(1536, DistanceFunctions.FLOAT_COSINE_DISTANCE, resourceContents.size())
			.withM(16)
			.withEf(200)
			.withEfConstruction(200)
			.build();
		
		Map<String, String> contentMap = new HashMap<>();
		
		resource.getContents().subscribe(er -> {
			List<List<Float>> vectors = er.getEmbeddings();
			for (int i = 0; i < vectors.size(); ++i) {
				List<Float> vector = vectors.get(i);
				float[] fVector = new float[vector.size()];
				for (int j = 0; j < fVector.length; ++j) {
					fVector[j] = vector.get(j);
				}
				hnswIndex.add(new SimilaritySearch.EmbeddingsItem(
						new SimilaritySearch.IndexId(er.getUri(), i), 
						fVector, 
						er.getDimensions()));				
			}
			contentMap.put(er.getUri(), er.getContents());
//			System.out.println(er.getUri() + " " + er.getEmbeddings().size() + " " + er.getContent().length());
		});
		
		hnswIndex.save(new File("test-data/hnsw-index.bin"));
		
		SimilaritySearch<List<Float>, Float> vectorSearch = SimilaritySearch.from(hnswIndex);				
		SimilaritySearch<List<List<Float>>, Float> multiVectorSearch = SimilaritySearch.adapt(vectorSearch);	
		
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);
		try {
			EmbeddingGenerator.Requirement eReq = TextFloatVectorEmbeddingModel.createRequirement("OpenAI", "text-embedding-ada-002", null);
			Requirement<EmbeddingGenerator.Requirement, TextFloatVectorEmbeddingModel> requirement = ServiceCapabilityFactory.createRequirement(TextFloatVectorEmbeddingModel.class, null, eReq);			
			TextFloatVectorEmbeddingModel embeddings = capabilityLoader.loadOne(requirement, progressMonitor);
			
			OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
			assertNotNull(openTelemetry);
	
	        Tracer tracer = openTelemetry.getTracer("test.ai");        
	        Span span = tracer
	        	.spanBuilder("Search embeddings")
	        	.startSpan();
	        
	        try (Scope scope = span.makeCurrent()) {	        
	        	TextFloatVectorChunkingEmbeddingModel<?> chunkingEmbeddings = new TextFloatVectorEncodingChunkingEmbeddingModel(
		        		embeddings, 
		        		1000, 
		        		20, 
		        		EncodingType.CL100K_BASE);
		        
				SimilaritySearch<String, Float> textSearch = SimilaritySearch.textFloatVectorEmbeddingSearch(multiVectorSearch, chunkingEmbeddings);
				
				String query = 
					"""
					I have a Drawio diagram. I want to generate a documentation web site from the diagram and publish it to GitHub pages.	
					""";
				
				List<SearchResult<Float>> searchResults = textSearch.find(query, 10);
				for (SearchResult<Float> searchResult: searchResults) {
					System.out.println("=== " + searchResult.getDistance() + " " + searchResult.getUri() + " " + searchResult.getIndex() + " ===");
//					System.out.println(contentMap.get(searchResult.getUri()));
				}		
				
				// Chat
				Chat.Requirement cReq = new Chat.Requirement("OpenAI", "gpt-4o", null);
				Requirement<Chat.Requirement, Chat> chatRequirement = ServiceCapabilityFactory.createRequirement(Chat.class, null, cReq);
				Chat chat = capabilityLoader.loadOne(chatRequirement, progressMonitor);
				System.out.println("=== Chat ===");
				
				List<Chat.Message> messages = new ArrayList<>();
	    		messages.add(Chat.Role.system.createMessage("You are a helpful assistant. You will answer user question leveraging provided documents and provide references to the used documents. Output your answer in markdown"));
	    		messages.add(Chat.Role.user.createMessage(query));
	    		
	    		Map<String, List<SearchResult<Float>>> groupedResults = org.nasdanika.common.Util.groupBy(searchResults, SearchResult::getUri);
				for (Entry<String, List<SearchResult<Float>>> sre: groupedResults.entrySet()) {
					StringBuilder messageBuilder = new StringBuilder("Use this document with URL " + sre.getKey() + ":" + System.lineSeparator());
					List<String> chunks = chunkingEmbeddings.chunk(contentMap.get(sre.getKey()));
					for (SearchResult<Float> chunkResult: sre.getValue()) {
						String chunk = chunks.get(chunkResult.getIndex());
						messageBuilder.append(System.lineSeparator() + System.lineSeparator() + chunk);
					}
					
					messages.add(Chat.Role.system.createMessage(messageBuilder.toString()));
				}		
				
		    	List<? extends ResponseMessage> responses = chat.chat(messages);		    			    	
		    	
		    	for (ResponseMessage response: responses) {
		    		System.out.println(MarkdownHelper.INSTANCE.markdownToHtml(response.getContent()));
		    	}				
	        } finally {
	        	span.end();
	        }
		} finally {
			capabilityLoader.close(progressMonitor);
		}		
	}	
	
}
