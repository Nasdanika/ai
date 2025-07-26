package org.nasdanika.ai;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

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
		
		/**
		 * Images encoded as base64 url
		 * @return
		 */
		List<String> getImages();
		
		/**
		 * Adds an image encoded as base64 data URL
		 * @param dataUrl
		 * @return this message
		 */
		Message addImage(String dataUrl);
		
		default Message addImage(File file) {
			try {
				return addImage(ImageIO.read(file));
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot read image from file '" + file.getAbsolutePath() + "': " + e, e);
			}
		}

		default Message addImage(InputStream inputStream) {
			try {
				return addImage(ImageIO.read(inputStream));
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot read image from input stream: " + e, e);
			}
		}

		default Message addImage(URL url) {
			try {
				return addImage(ImageIO.read(url));
			} catch (IOException e) {
				throw new IllegalArgumentException("Cannot read image from URL '" + url + "': " + e, e);
			}
		}

		default Message addImage(BufferedImage image) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try {
				try (baos) {
					ImageIO.write(image, "PNG", baos);												
				}
		        String base64Image = java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
			    return addImage("data:image/png;base64," + base64Image);
			} catch (Exception e) {
				throw new IllegalArgumentException("Cannot write image: " + e, e);
			}
			
		}
		
		/**
		 * Creates a message
		 * @param role
		 * @param content Message content. Can be null.
		 * @return
		 */
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
				
				private List<String> images = new ArrayList<>();

				@Override
				public List<String> getImages() {
					return Collections.unmodifiableList(images);
				}

				@Override
				public Message addImage(String dataUrl) {
					images.add(dataUrl);
					return this;
				}
				
			};
			
		}
		
	}
	
	interface ResponseMessage extends Message {
		
		String getRefusal();
		
		String getFinishReason();
		
		@Override
		default Message addImage(String dataUrl) {
			throw new UnsupportedOperationException();			
		}
		
		@Override
		default List<String> getImages() {
			return Collections.emptyList();
		}
		
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
