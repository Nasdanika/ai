package org.nasdanika.ai.http;

import java.util.List;

import org.json.JSONObject;
import org.nasdanika.ai.Chat;
import org.nasdanika.html.alpinejs.AlpineJsFactory;
import org.nasdanika.html.bootstrap.BootstrapFactory;
import org.nasdanika.http.TelemetryFilter;

import reactor.core.publisher.Mono;

public abstract class AbstractAIChatRoutes extends AbstractTelemetryChatRoutes {
			
	protected Chat chat;
	
//	/**
//	 * @param chat
//	 */
//	public AbstractAIChatRoutes(Chat chat) {
//		this.chat = chat;
//	}
	
	protected AbstractAIChatRoutes(
			BootstrapFactory bootstrapFactory, 
			AlpineJsFactory alpineJsFactory,
			TelemetryFilter telemetryFilter,
			Chat chat) {
		super(bootstrapFactory, alpineJsFactory, telemetryFilter);
		this.chat = chat;
	}

	protected AbstractAIChatRoutes(TelemetryFilter telemetryFilter, Chat chat) {
		super(telemetryFilter);
		this.chat = chat;
	}

	protected abstract Mono<List<Chat.Message>> generateChatRequestMessages(String chatId, String question, JSONObject config);

	protected abstract Mono<String> generateResponseContent(String chatId, String question, List<? extends Chat.ResponseMessage> responses, JSONObject config);
	
	@Override
	protected final Mono<String> chatContent(String chatId, String question, JSONObject config) {
		return generateChatRequestMessages(chatId, question, config)
			.flatMap(chat::chatAsync)
			.flatMap(responses -> generateResponseContent(chatId, question, responses, config));
	}
	
}
