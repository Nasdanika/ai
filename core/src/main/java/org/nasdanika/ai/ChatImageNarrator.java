package org.nasdanika.ai;

import java.awt.image.BufferedImage;
import java.util.List;

import org.nasdanika.ai.Chat.ResponseMessage;

import reactor.core.publisher.Mono;

public class ChatImageNarrator implements ImageNarrator {
	
	private Chat chat;
	private String prompt;

	public ChatImageNarrator(Chat chat, String prompt) {
		this.chat = chat;
		this.prompt = prompt;
	}
	
	public ChatImageNarrator(Chat chat) {
		this(chat, "Describe this image");
	}

	@Override
	public Mono<String> generateAsync(BufferedImage input) {
    	Mono<List<? extends ResponseMessage>> responses = chat.chatAsync(
    		Chat.Role.user.createMessage(prompt).addImage(input)
    	);
        	
    	return responses.map(responseMessages -> {
        	StringBuilder resultBuilder = new StringBuilder();
        	for (ResponseMessage responseMessage: responseMessages) {
        		if (resultBuilder.length() > 0) {
        			resultBuilder.append(System.lineSeparator());
        		}
        		resultBuilder.append(responseMessage.getContent());
        	}
    		
    		return resultBuilder.toString();    		
    	});
	}
	
	@Override
	public String generate(BufferedImage input) {        
    	List<? extends ResponseMessage> responses = chat.chat(
    		Chat.Role.user.createMessage(prompt).addImage(input)
    	);
    	
    	StringBuilder resultBuilder = new StringBuilder();
    	for (ResponseMessage response: responses) {
    		if (resultBuilder.length() > 0) {
    			resultBuilder.append(System.lineSeparator());
    		}
    		resultBuilder.append(response.getContent());
    	}
		
		return resultBuilder.toString();
	}

}
