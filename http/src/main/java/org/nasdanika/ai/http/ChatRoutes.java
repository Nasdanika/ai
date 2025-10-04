package org.nasdanika.ai.http;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.json.JSONObject;
import org.nasdanika.ai.Chat;
import org.nasdanika.ai.Chat.ResponseMessage;
import org.nasdanika.ai.SearchResult;
import org.nasdanika.common.MarkdownHelper;

import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import reactor.core.publisher.Mono;
import reactor.netty.NettyOutbound;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import io.netty.handler.codec.http.cookie.ServerCookieEncoder;

public class ChatRoutes extends AbstractTelemetryChatRoutes {
			
	protected Chat chat;
	
	/**
	 * @param chat
	 */
	public ChatRoutes(Chat chat) {
		super("chat");
		this.chat = chat;
	}
	
	protected String getCookie() {
		return "CHATID";
	}
	
	
	protected Cookie createChatCookie(String chatId) {
        DefaultCookie cookie = new DefaultCookie("SESSIONID", chatId);
        cookie.setPath(getCookiePath());
        cookie.setMaxAge(getCookieMaxAge()); 
		return cookie;
	}

	protected int getCookieMaxAge() {
		return 3600;
	}

	protected String getCookiePath() {
		return "/";
	}
	
//	SimilaritySearch<String, Float> textSearch,		
//	Function<String,String> textProvider,
//	TelemetryFilter telemetryFilter
	
	protected Mono<String> chat(String chatId, String question) {
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
		
		telemetryFilter.filter(request, responseString);
		
		return Mono.just("No clue: " + chatId);
	}
	
	public NettyOutbound chat(
			HttpServerRequest request, 
			HttpServerResponse response) {
		
		String chatId = request
				.cookies()
				.get(getCookie())
				.stream()
				.findFirst()
				.map(Cookie::value)
				.orElseGet(() -> {
					String newChatId = UUID.randomUUID().toString();
					 Cookie cookie = createChatCookie(newChatId);
					 String encodedCookie = ServerCookieEncoder.STRICT.encode(cookie);
			         response.addHeader("Set-Cookie", encodedCookie);					 
					return newChatId;
				});
		
		Mono<String> requestString = request.receive().aggregate().asString();		
		return response
				.header("Content-Type", "application/json")
				.sendString(requestString.flatMap(question -> chat(request, chatId, question)));
	}
	
}
