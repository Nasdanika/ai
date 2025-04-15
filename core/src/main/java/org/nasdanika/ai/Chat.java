package org.nasdanika.ai;

import java.util.Arrays;
import java.util.List;

import reactor.core.publisher.Mono;

public interface Chat extends Model {
	
	/**
	 * Chat requirement.
	 * String attributes match any value if null.
	 */
	record Requirement(
		String provider,
		String model,
		String version) {}	
	
	interface Message {
		
		String getRole();
		
		String getContent();
		
		static Message create(String role, String content) {
			return new Message() {
				
				@Override
				public String getRole() {
					return role;
				}
				
				@Override
				public String getContent() {
					return content;
				}
				
			};
			
		}
		
	}
	
	interface ResponseMessage extends Message {
		
		String getRefusal();
		
		String getFinishReason();
		
	}
		
	enum Role {
		
		system,
		assistant,
		user,
		function,
		tool,
		developer;		
		
		public Message createMessage(String content) {
			return Message.create(name(), content);
		}
		
	}
	
	Mono<List<ResponseMessage>> chatAsync(List<Message> messages);
	
	default Mono<List<ResponseMessage>> chatAsync(Message... messages) {
		return chatAsync(Arrays.asList(messages));
	}		
	
	default List<ResponseMessage> chat(List<Message> messages) {
		return chatAsync(messages).block();
	}
		
	default List<ResponseMessage> chat(Message... messages) {
		return chat(Arrays.asList(messages));
	}	
	
	int getMaxOutputTokens();

}
