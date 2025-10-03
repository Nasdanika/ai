package org.nasdanika.ai.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.nasdanika.ai.Chat;
import org.nasdanika.ai.Chat.ResponseMessage;
import org.nasdanika.ai.SearchResult;
import org.nasdanika.ai.SimilaritySearch;
import org.nasdanika.ai.SimilaritySearch.EmbeddingsItem;
import org.nasdanika.ai.SimilaritySearch.IndexId;
import org.nasdanika.ai.TextFloatVectorEmbeddingModel;
import org.nasdanika.ai.cli.HnswIndexCommandBase;
import org.nasdanika.capability.CapabilityLoader;
import org.nasdanika.cli.ParentCommands;
import org.nasdanika.cli.RootCommand;
import org.nasdanika.common.MarkdownHelper;
import org.nasdanika.html.Button;
import org.nasdanika.html.HTMLPage;
import org.nasdanika.html.Tag;
import org.nasdanika.html.TextArea;
import org.nasdanika.html.alpinejs.AlpineJsFactory;
import org.nasdanika.html.bootstrap.BootstrapFactory;
import org.nasdanika.html.bootstrap.Breakpoint;
import org.nasdanika.html.bootstrap.Card;
import org.nasdanika.html.bootstrap.Color;
import org.nasdanika.html.bootstrap.Container;
import org.nasdanika.html.bootstrap.InputGroup;
import org.nasdanika.html.bootstrap.Size;
import org.nasdanika.http.AbstractHttpServerCommand;
import org.nasdanika.http.TelemetryFilter;

import com.github.jelmerk.hnswlib.core.hnsw.HnswIndex;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongGauge;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Tracer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import reactor.netty.http.server.HttpServerRoutes;

@Command(
		description = "Serves HTTP routes for AI chat",
		name = "chat-server")
@ParentCommands(RootCommand.class)
public class ChatServerCommand extends AbstractHttpServerCommand {
	
	protected OpenTelemetry openTelemetry;
	private TextFloatVectorEmbeddingModel embeddingModel;
	private Chat chat;
	
	protected String getInstrumentationScopeName() {
		return getClass().getName();
	}
	
	protected String getInstrumentationScopeVersion() {
		String[] version = spec.version();
		if (version == null) {
			return null;
		}
		return String.join(System.lineSeparator(), version);
	}	

	public ChatServerCommand(
			TextFloatVectorEmbeddingModel embeddingModel,
			Chat chat,
			OpenTelemetry openTelemetry,
			CapabilityLoader capabilityLoader) {
		super(capabilityLoader);
		this.embeddingModel = embeddingModel;
		this.chat = chat;
		this.openTelemetry = openTelemetry;		
	}	
	
	@Parameters(
		index =  "0",	
		arity = "1",
		description = "Index file")
	private File index;
	
	@Parameters(
		index =  "1",	
		arity = "1",
		description = "Text map")
	private File textMap;
		
	@Option(
			names = "--distance-threshold", 
			description = {
				"Distance theshold, results with the",
				"distance above the threshold are discarded.",
				"If there are no results below the threshold",
				"'No matches' is displayed in the chat",
				"Defaults to ${DEFAULT-VALUE}"
			},
			defaultValue = "0.25")
	private double distanceThreshold;
		
	@Option(
		names = "--number-of-items", 
		description = {
			"Number of semantic search items to return",
			"Defaults to ${DEFAULT-VALUE}"
		},
		defaultValue = "10")
	private int numberOfItems;
	
	@Option(
			names = "--exact", 
			description = {
				"If true (default) the index is exact",
				"Set to false if you have a large number",
				"of index entries to improve performance"
			},
			negatable = true,
			defaultValue = "true")
	private boolean exact;
	
	protected void buildRoutes(HttpServerRoutes routes) {	
		try (InputStream textMapInputStream = new FileInputStream(textMap)) {
			HnswIndex<IndexId, float[], EmbeddingsItem, Float> hnswIndex = HnswIndexCommandBase.loadIndex(index);			
			SimilaritySearch<List<Float>, Float> vectorSearch = SimilaritySearch.from(exact ? hnswIndex.asExactIndex() : hnswIndex);				
			SimilaritySearch<List<List<Float>>, Float> multiVectorSearch = SimilaritySearch.adapt(vectorSearch);	
			SimilaritySearch<String, Float> textSearch = SimilaritySearch.textFloatVectorEmbeddingSearch(multiVectorSearch, embeddingModel);
			
			Tracer tracer = openTelemetry.getTracer(getInstrumentationScopeName(), getInstrumentationScopeVersion());
			
			Meter meter = openTelemetry
				.getMeterProvider()
				.meterBuilder(getInstrumentationScopeName())
				.setInstrumentationVersion(getInstrumentationScopeVersion())
				.build();
						
			LongGauge durationGauge = meter.gaugeBuilder("duration").ofLongs().build();			
			
			TelemetryFilter telemetryFilter = new TelemetryFilter(
					tracer, 
					openTelemetry.getPropagators().getTextMapPropagator(), 
					(path, duration) ->  durationGauge.set(duration, Attributes.of(AttributeKey.stringKey("path"), path)),
					true);
			
			routes.get("/index.html", telemetryFilter.wrapStringHandler(this::home));
			JSONObject textMapObj = new JSONObject(new JSONTokener(textMapInputStream));
			Function<String, String> textProvider = uri -> textMapObj.getString(uri);
			routes.post("/chat", (request, response) -> chat(request, response, textSearch, textProvider, telemetryFilter));
		} catch (IOException e) {
			throw new CommandLine.ExecutionException(spec.commandLine(), "Failed to load vector index or text map", e);
		}
	}
	
	protected String home(HttpServerRequest request, HttpServerResponse response) {
		BootstrapFactory bootstrapFactory = BootstrapFactory.INSTANCE;
		AlpineJsFactory alpineJsFactory = AlpineJsFactory.INSTANCE;
		
		HTMLPage page = bootstrapFactory.bootstrapCdnHTMLPage();		
		alpineJsFactory.cdn(page);
		Container chatApp = bootstrapFactory.container();
		JSONObject appData = alpineJsFactory.from(chatApp.toHTMLElement()).data();
		JSONArray messagesArray = new JSONArray();
		appData
			.put("messages", messagesArray)
			.put("text", "");
		
		page.body(chatApp);
		
		// Chat message cards
		Card messageCard = bootstrapFactory.card();
		messageCard.margin().bottom(Breakpoint.DEFAULT, Size.S1);
		Tag messageCardHtmlElement = messageCard.toHTMLElement();
		alpineJsFactory
			.from(messageCardHtmlElement)
			.bind("class", "'border-' + message.style");
		messageCard.border(Color.DEFAULT);		
		Tag messageCardBody = messageCard.getBody().toHTMLElement();
		messageCardBody.content("Loading...");
		alpineJsFactory
			.from(messageCardBody)
			.html("message.content");				
		Tag messagesFor = alpineJsFactory._for("message in messages", messageCardHtmlElement);		
		chatApp.row().col().content(messagesFor);
		
		// Text area
		TextArea textArea = bootstrapFactory.getHTMLFactory().textArea();
		textArea
			.name("userInput")
			.placeholder("Ask me anything about TOGAF 10");
		InputGroup textAreaInputGroup = bootstrapFactory
			.inputGroup()
			.input(textArea)
			.prepend("Chat");
		alpineJsFactory.from(textArea).model("text");
		Button submitButton = bootstrapFactory.getHTMLFactory().button("Submit");
		
		String submitHandler = """
			messages.push({
				content: text,
				style: 'primary'
			});
			
			var responseMessage = Alpine.reactive({
				content: 'Processing...',
				style: 'muted'
			});
			messages.push(responseMessage);						
			
			fetch("chat", {
				method: 'POST',
				body: text
			}).then(response => {
				if (response.ok) {
					response.json().then(responseJson => {
						responseMessage.content = responseJson.content;
						responseMessage.style = responseJson.style;
					});
				} else {
					responseMessage.content = response.status + ": " + response.statusText;
					responseMessage.style = 'danger';
				}
			});
			text = '';
			""";
		
		alpineJsFactory
			.from(submitButton)
			.on("click", submitHandler)
			.bind("disabled", "!text");
		org.nasdanika.html.bootstrap.Button<Button> bootstrapSubmitButton = bootstrapFactory.button(submitButton, Color.PRIMARY, false);
		textAreaInputGroup.append(bootstrapSubmitButton);
		chatApp.row().col().content(textAreaInputGroup);
		return page.toString();
	}
	
	protected NettyOutbound chat(
			HttpServerRequest request, 
			HttpServerResponse response, 
			SimilaritySearch<String, Float> textSearch,		
			Function<String,String> textProvider,
			TelemetryFilter telemetryFilter) {
		Mono<String> requestString = request.receive().aggregate().asString();
		
		record Result(String text, List<SearchResult<Float>> results, List<ResponseMessage> chatResponses) {}
		
		Mono<String> responseString = requestString
			.flatMap(rs -> {
				Mono<List<SearchResult<Float>>> searchResults = textSearch.findAsync(rs, numberOfItems);
				return searchResults.map(sr -> new Result(rs, sr, null));
			})
			.flatMap(result -> {
				List<Chat.Message> messages = new ArrayList<>();
	    		messages.add(Chat.Role.system.createMessage(
    				"""	    				
    				You are a helpful assistant.
    				You will answer user question about TOGAF 10 standard leveraging provided documents
    				and provide references to the used documents.
    				Output your answer in markdown.	    				
    				"""));
	    		messages.add(Chat.Role.user.createMessage(result.text()));
	    		
	    		List<SearchResult<Float>> references = new ArrayList<>();
	    		
	    		Map<String, List<SearchResult<Float>>> groupedResults = org.nasdanika.common.Util.groupBy(result.results(), SearchResult::getUri);
				for (Entry<String, List<SearchResult<Float>>> sre: groupedResults.entrySet()) {
					boolean closeEnough = false;
					for (SearchResult<Float> sr: sre.getValue()) {
						if (sr.getDistance() <= distanceThreshold) {
							closeEnough = true;
							references.add(sr);
						}
					}
					if (closeEnough) {
						StringBuilder messageBuilder = new StringBuilder("Use this document with URL " + sre.getKey() + ":" + System.lineSeparator());
						String contents = textProvider.apply(sre.getKey()); // No chunking/selection in this case - entire page.
						messageBuilder.append(System.lineSeparator() + System.lineSeparator() + contents);					
						messages.add(Chat.Role.system.createMessage(messageBuilder.toString()));
					}
				}		
				
				if (references.isEmpty()) {
					return Mono.just(new Result(result.text(), null, null));
				}				
				
				return chat
						.chatAsync(messages)
						.map(chatResponses -> new Result(result.text(), references, chatResponses));
			})				
			.map(result -> {
				JSONObject jResult = new JSONObject();
				if (result.results() != null) {
					JSONObject references = new JSONObject();
					jResult.put("references", references);
					Map<String, List<SearchResult<Float>>> groupedResults = org.nasdanika.common.Util.groupBy(result.results(), SearchResult::getUri);
					for (Entry<String, List<SearchResult<Float>>> gre: groupedResults.entrySet()) {				
						JSONObject refObj = new JSONObject();
						references.put(gre.getKey(),refObj);
						for (SearchResult<Float> sr: gre.getValue()) {
							refObj.put(String.valueOf(sr.getIndex()), sr.getDistance());
						}
					}
				}
				
				StringBuilder contentBuilder = new StringBuilder();
				
				if (result.chatResponses() == null) {
					contentBuilder.append("No matches");
				} else {
					for (ResponseMessage responseMessage: result.chatResponses()) {
						contentBuilder
							.append(System.lineSeparator())
							.append(System.lineSeparator())
							.append(responseMessage.getContent())
							.append(System.lineSeparator())
							.append(System.lineSeparator());						
					}
					
					contentBuilder
						.append(System.lineSeparator())
						.append(System.lineSeparator())
						.append("## References:")
						.append(System.lineSeparator())
						.append(System.lineSeparator());
					
					for (SearchResult<Float> reference: result.results()) {
						contentBuilder
							.append(System.lineSeparator())
							.append("* ")
							.append(reference.getUri());
					}
					contentBuilder.append(System.lineSeparator());											
				}
				
				jResult.put("content", MarkdownHelper.INSTANCE.markdownToHtml(contentBuilder.toString()));
				jResult.put("style", true && result.chatResponses() == null ? "warning" : "info");
				
				return jResult.toString();
		});
		return response
				.header("Content-Type", "application/json")
				.sendString(telemetryFilter.filter(request, responseString));
	}
	
	@Override
	public Integer call() throws Exception {
		startServer(this::buildRoutes);
		return 0;
	}

}
