package org.nasdanika.ai;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleDescriptor.Version;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import reactor.core.publisher.Mono;

public interface Chat extends Model {
	
	public static final Chat ECHO = new Chat() {

		@Override
		public int getMaxInputTokens() {
			return Integer.MAX_VALUE;
		}

		@Override
		public String getProvider() {
			return "Nasdanika";
		}

		@Override
		public String getName() {
			return "Echo";
		}

		@Override
		public String getVersion() {
			Optional<Version> moduleVersion = getClass().getModule().getDescriptor().version();
			return moduleVersion == null ? new Date().toString() : moduleVersion.toString();
		}

		@Override
		public Mono<List<? extends ResponseMessage>> chatAsync(List<Message> messages) {
			List<? extends ResponseMessage> responseMessages = messages.stream().map(m ->  new ResponseMessage() {
				
				@Override
				public String getRole() {
					return Role.assistant.name();
				}
				
				@Override
				public String getContent() {
					return m.getContent();
				}
				
				@Override
				public String getRefusal() {
					return null;
				}
				
				@Override
				public String getFinishReason() {
					return "stop";
				}
			})
			.toList();
			Mono<List<? extends ResponseMessage>> mono = Mono.just(responseMessages);
			return mono.delayElement(Duration.ofSeconds(1));
		}

		@Override
		public int getMaxOutputTokens() {
			return Integer.MAX_VALUE;
		}
		
	};
	
	public static final String LOREM_IPSUM_TEXT = 
			"""
			Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec tempus ac nibh a convallis. 
			Phasellus tristique, ex ac maximus iaculis, nulla magna aliquam turpis, a vestibulum ligula metus varius lectus. 
			Duis rhoncus suscipit odio, vel porttitor metus egestas quis. Nunc quis tristique orci. 
			Phasellus tellus mi, pellentesque id aliquam a, eleifend nec nunc. Curabitur volutpat feugiat vestibulum. Maecenas sit amet dapibus velit.
			Fusce scelerisque, nisl a fermentum vulputate, nisi enim hendrerit orci, a vestibulum sem leo at lacus. Duis vel rhoncus odio.
			Nam facilisis est in ullamcorper consectetur. Donec aliquet velit quis dolor accumsan maximus. Aenean sodales mattis sem sed tincidunt.
			Aliquam tristique augue nec tristique lobortis. Vivamus id metus in justo dignissim viverra. 
			Pellentesque ipsum lectus, ultricies in vehicula efficitur, aliquam vel purus.
			Pellentesque lacus metus, vestibulum at convallis non, sodales sed ante.
			
			Donec tincidunt elit eros, sit amet blandit ex posuere sed. 
			Pellentesque pharetra magna lacus, rhoncus placerat risus faucibus id. Duis non euismod turpis. 
			Vestibulum interdum dictum velit sit amet sagittis. Proin velit eros, interdum a nibh sed, viverra luctus ex.
			Integer commodo diam id arcu varius pharetra. Proin porta justo lorem, quis sodales ex fermentum a. 
			Sed at consequat dolor. In quis consectetur leo, non aliquet mauris.
			
			Aenean laoreet dui a facilisis efficitur. Maecenas consectetur ligula non magna porta congue. Etiam a ornare lectus. 
			Nulla at ligula et tortor mattis bibendum id quis sapien. Aenean eget condimentum enim. 
			Duis ullamcorper malesuada sapien et egestas. In hac habitasse platea dictumst. Quisque sed fermentum tortor. In vehicula auctor felis. 
			Donec fringilla turpis eget tortor lobortis posuere quis ac erat. Morbi mattis elementum felis, sit amet scelerisque lectus finibus a. 
			Donec ac est odio. Sed sollicitudin, arcu sit amet pulvinar pulvinar, lorem nisl malesuada odio, nec dapibus metus dui porta urna. 
			In viverra rhoncus est, sed interdum elit faucibus placerat. Proin ut ligula venenatis, bibendum massa in, fermentum sapien. 
			Mauris tempor eros ligula, id placerat eros vulputate quis.
			
			Vestibulum quis dignissim urna. Pellentesque eros turpis, laoreet vel justo vel, tincidunt suscipit elit. Nullam eget cursus nulla. 
			Nulla pellentesque molestie sem, sed dictum lacus efficitur eu. Quisque orci lectus, egestas eget nunc quis, placerat blandit ligula. 
			Cras quis dictum sapien. Duis varius metus sapien, quis venenatis lectus dapibus ac. Sed eget sem ac leo elementum rhoncus. 
			Nunc dolor eros, dapibus a porta eget, vestibulum vel nunc. Aliquam erat volutpat. Nam imperdiet libero velit, vel ullamcorper nisi lacinia nec. 
			Suspendisse blandit dolor ut odio aliquam sodales.
			
			In nec augue pulvinar, semper felis fringilla, vehicula dui. 
			Praesent vulputate tellus ac ante varius, sit amet ultricies felis ultrices. Pellentesque egestas lectus non nibh viverra, vitae tristique orci ultrices. 
			Donec non sem imperdiet neque varius sodales ut ut odio. Sed vehicula pellentesque nulla, ut dignissim turpis blandit non. Donec fermentum pellentesque nisi in facilisis. 
			Aenean mollis purus vel ex porttitor, sed bibendum ante mollis. Fusce molestie justo turpis, luctus sollicitudin augue pulvinar quis. 
			Nunc id tincidunt erat. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. 
			Cras accumsan laoreet velit. Vestibulum sit amet nisl mauris.			
			""";
	
	
	public static final Chat LOREM_IPSUM = new Chat() {

		@Override
		public int getMaxInputTokens() {
			return Integer.MAX_VALUE;
		}

		@Override
		public String getProvider() {
			return "Nasdanika";
		}

		@Override
		public String getName() {
			return "Lorem Impsum";
		}

		@Override
		public String getVersion() {
			Optional<Version> moduleVersion = getClass().getModule().getDescriptor().version();
			return moduleVersion == null ? new Date().toString() : moduleVersion.toString();
		}

		@Override
		public Mono<List<? extends ResponseMessage>> chatAsync(List<Message> messages) {
			Mono<List<? extends ResponseMessage>> ret = Mono.just(List.of(new ResponseMessage() {
				
				@Override
				public String getRole() {
					return Role.assistant.name();
				}
				
				@Override
				public String getContent() {
					return LOREM_IPSUM_TEXT;
				}
				
				@Override
				public String getRefusal() {
					return null;
				}
				
				@Override
				public String getFinishReason() {
					return "stop";
				}
			}));
			
			return ret.delayElement(Duration.ofSeconds(1));
		}

		@Override
		public int getMaxOutputTokens() {
			return Integer.MAX_VALUE;
		}
		
	};
	
	
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
	
	Mono<List<? extends ResponseMessage>> chatAsync(List<Message> messages);
	
	default Mono<List<? extends ResponseMessage>> chatAsync(Message... messages) {
		return chatAsync(Arrays.asList(messages));
	}		
	
	default List<? extends ResponseMessage> chat(List<Message> messages) {
		return chatAsync(messages).block();
	}
		
	default List<? extends ResponseMessage> chat(Message... messages) {
		return chat(Arrays.asList(messages));
	}	
	
	int getMaxOutputTokens();

}
