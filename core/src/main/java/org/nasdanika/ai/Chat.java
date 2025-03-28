package org.nasdanika.ai;

import java.util.Arrays;
import java.util.List;

public interface Chat extends Model {
	
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
	
	List<ResponseMessage> chat(List<Message> messages);
		
	default List<ResponseMessage> chat(Message... messages) {
		return chat(Arrays.asList(messages));
	}	
	
	int getMaxOutputTokens();

}
