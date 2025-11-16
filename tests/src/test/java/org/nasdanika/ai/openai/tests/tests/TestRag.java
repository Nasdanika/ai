package org.nasdanika.ai.openai.tests.tests;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;
import org.nasdanika.ai.Chat;
import org.nasdanika.ai.Chat.Message;
import org.nasdanika.ai.Chat.ResponseMessage;
import org.nasdanika.ai.openai.OpenAIChat;
import org.nasdanika.capability.CapabilityLoader;
import org.nasdanika.capability.ServiceCapabilityFactory;
import org.nasdanika.capability.ServiceCapabilityFactory.Requirement;
import org.nasdanika.common.LoggerProgressMonitor;
import org.nasdanika.common.ProgressMonitor;
import org.nasdanika.http.SerpapiConnector;
import org.nasdanika.http.SerpapiConnector.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class TestRag {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TestRag.class);
	
	@Test
	public void testOpenAISemanticKernelDefinitions() {
        String apiKey = System.getenv("SERPER_KEY");
        
        SerpapiConnector serpApiConnector = new SerpapiConnector(apiKey) { 
        	
        	@Override
        	protected String getMainContentSelector(String link) {
        		if (link.startsWith("https://learn.microsoft.com/")) {
        			return super.getMainContentSelector(link) + " div.content";
        		}
        		return super.getMainContentSelector(link);
        	}
        	
        };
        
		CapabilityLoader capabilityLoader = new CapabilityLoader();
		ProgressMonitor progressMonitor = new LoggerProgressMonitor(LOGGER);
		OpenTelemetry openTelemetry = capabilityLoader.loadOne(ServiceCapabilityFactory.createRequirement(OpenTelemetry.class), progressMonitor);
		
		try {
			BiConsumer<Integer, Integer> usageConsumer = (promptTokens, completionTokens) -> {
				System.out.println("Usage: " + promptTokens + " / " + completionTokens);
			};
			OpenAIChat.Requirement cReq = new OpenAIChat.Requirement("gpt-4o", null, usageConsumer);
			Requirement<OpenAIChat.Requirement, Chat> requirement = ServiceCapabilityFactory.createRequirement(Chat.class, null, cReq);			
			Chat chat = capabilityLoader.<Chat>loadOne(requirement, progressMonitor);
			
	        Tracer tracer = openTelemetry.getTracer("test.ai");        
	        Span span = tracer
	        	.spanBuilder("Chat")
	        	.startSpan();
	        try (Scope scope = span.makeCurrent()) {
	    	    File docDir = new File("C:\\Users\\pavel\\git-models\\semantic-kernel\\ecore\\doc\\semantic-kernel\\eClassifiers");
	    	    org.nasdanika.common.Util.walk(null, (BiConsumer<File,String>) (file, path) -> generateDoc(serpApiConnector, chat, file, path), docDir.listFiles());        
	        } finally {
	        	span.end();
	        }
		} finally {
			capabilityLoader.close(progressMonitor);
		}       		
	}
			
	private static final String MD_EXTENSION = ".md";
	
	private static void generateDoc(SerpapiConnector serpApiConnector, Chat chat, File file, String path) {
		if (file.isFile() 
				&& file.getName().endsWith(MD_EXTENSION) 
				&& (file.length() < 50 || System.currentTimeMillis() - file.lastModified() > TimeUnit.MILLISECONDS.convert(2, TimeUnit.DAYS ))) {
			String[] pa = path.split("/");
			
			String term;
			if (pa.length == 1) {
				term = pa[0].substring(0, pa[0].length() - MD_EXTENSION.length());				
			} else {
				term = pa[0] + " " + pa[2].substring(0, pa[2].length() - MD_EXTENSION.length()) + " property";								
			}
	
			System.out.println("Generating definition for: " + term);
			
		    String query = "microsoft semantic kernel Java %s".formatted(term);
	        Flux<SearchResult> results = serpApiConnector.search(
	        		query, 
	        		10, 
	        		0, 
	        		(url, next) -> {
	        			if (url.startsWith("https://learn.microsoft.com/")) {
	        				return next;
	        			}
			        	return Mono.just("Skipping " + url);
			        });	        
	        List<SearchResult> resultList = results.collectList().block();
			EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
			Encoding encoding = registry.getEncoding(EncodingType.CL100K_BASE);
			
			int tokenBudget = 20000;
		
        	List<Message> messages = new ArrayList<>();
        	Message prompt = Chat.Role.system.createMessage("""
        			You are an expert in Agentic AI and Microsoft Semantic Kernel Java.
        			You will be provided a list of grounding information from Microsoft Learn 
        			and you will be asked a question about Microsoft Semantic Kernel Java terminology and components.
        			You need to provide definitions in Markdown.
        			
        			If there is no information, reply with 'NO_INFORMATION'. 
        			""");
        	messages.add(prompt);
        	
            for (SearchResult result: resultList) {
            	if (result.link().startsWith("https://learn.microsoft.com/")) {
	            	int resultTokens = encoding.encode(result.markdownMainContent()).size();
	            	if (resultTokens > 0) {
		            	StringBuilder resultMessageBuilder = new StringBuilder("[")
		            			.append(result.title())
		            			.append("](")
		            			.append(result.link())
		            			.append(")")
		            			.append(System.lineSeparator())
		            			.append(System.lineSeparator())
		            			.append("---")
		            			.append(System.lineSeparator())
		            			.append(System.lineSeparator())
		            			.append(result.markdownMainContent());
		            			            	
		            	System.out.println("\t" + result.title() + " (" + result.link() + ") " + result.mainContent().length() + " / " + resultTokens);
		            	tokenBudget -= resultTokens;
		            	if (tokenBudget < 0) {
		            		break;
		            	}
		            	
		            	Message resultMessage = Chat.Role.system.createMessage(resultMessageBuilder.toString());
		            	messages.add(resultMessage);
	            	}
            	}
            }
        	
		    String question = "Provide a summary with Java code snippets, if possible, of Microsoft Semantic Kernel Java %s".formatted(term);
        	messages.add(Chat.Role.user.createMessage(question));

        	List<? extends ResponseMessage> responses = chat.chatAsync(messages).block();

        	StringBuilder definitionBuilder = new StringBuilder();
        	for (ResponseMessage response: responses) {
        		definitionBuilder
        			.append(response.getContent())
        			.append(System.lineSeparator())
        			.append(System.lineSeparator());
        	}

	        try {	        	
	        	Files.writeString(file.toPath(), definitionBuilder.length() < 50 ? "" : definitionBuilder.toString());
	        	System.out.println("Definition length: " + definitionBuilder.length());
	        } catch (IOException e) {
	        	e.printStackTrace();
	        }
	        try {
				Thread.sleep(Duration.ofSeconds(65)); // Poor man's rate limiting.
			} catch (InterruptedException e) {
				e.printStackTrace();
			} 
		}
	}	
		
}
