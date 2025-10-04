package org.nasdanika.ai.http;

import org.json.JSONObject;
import org.nasdanika.html.HTMLPage;
import org.nasdanika.html.alpinejs.AlpineJsFactory;
import org.nasdanika.html.bootstrap.BootstrapFactory;
import org.nasdanika.html.bootstrap.Color;
import org.nasdanika.http.TelemetryFilter;

import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerRequest;

public abstract class AbstractTelemetryChatRoutes extends AbstractChatRoutes {
	
	protected TelemetryFilter telemetryFilter;
	
	protected AbstractTelemetryChatRoutes(
			BootstrapFactory bootstrapFactory, 
			AlpineJsFactory alpineJsFactory,
			TelemetryFilter telemetryFilter) {
		super(bootstrapFactory, alpineJsFactory);
		this.telemetryFilter = telemetryFilter;
	}

	protected AbstractTelemetryChatRoutes(TelemetryFilter telemetryFilter) {
		this.telemetryFilter = telemetryFilter;
	}
	
	@Override
	protected final Mono<Message> chat(HttpServerRequest request, String chatId, String question, JSONObject config) {
		if (telemetryFilter == null) {
			return chat(chatId, question, config);
		}
		return telemetryFilter.filter(request, chat(chatId, question, config));
	}
	
	protected final Mono<HTMLPage> buildPage(HttpServerRequest request) {
		if (telemetryFilter == null) {
			return super.buildPage(request);
		}
		return telemetryFilter.filter(request, super.buildPage(request));
	}
	
	protected Mono<Message> chat(String chatId, String question, JSONObject config) {
		return chatContent(chatId, question, config).map(c -> new Message(c, Color.INFO));
	}
	
	protected abstract Mono<String> chatContent(String chatId, String question, JSONObject config);
	
	/**
	 * Use chatContent(chatId,question,config)
	 */
	@Override	
	@Deprecated 
	protected final Mono<String> chatContent(HttpServerRequest request, String chatId, String question, JSONObject config) {
		throw new UnsupportedOperationException("Use chatContent(chatId,question,config)");
	}
	
}
