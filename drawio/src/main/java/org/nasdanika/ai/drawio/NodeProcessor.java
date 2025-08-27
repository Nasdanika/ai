package org.nasdanika.ai.drawio;

import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.nasdanika.common.Section;
import org.nasdanika.drawio.Connection;
import org.nasdanika.drawio.Element;
import org.nasdanika.drawio.Layer;
import org.nasdanika.drawio.ModelElement;
import org.nasdanika.drawio.Node;
import org.nasdanika.graph.processor.ChildProcessor;
import org.nasdanika.graph.processor.OutgoingEndpoint;
import org.nasdanika.graph.processor.ProcessorElement;
import org.nasdanika.graph.processor.ProcessorInfo;

import reactor.core.publisher.Mono;

public class NodeProcessor extends LayerElementProcessor<Node> {
		
	private static final String DATA_URI_PNG_PREFIX_NO_BASE_64 = "data:image/png,";
	private static final String DATA_URI_JPEG_PREFIX_NO_BASE_64 = "data:image/jpeg,";	
	
	public NodeProcessor(DrawioProcessorFactory factory) {
		super(factory);
	}
	
	@Override
	protected Section doCreateSection() {
		Section section = super.doCreateSection();
		
		// TODO - style, size, geometry
		
		return section;
	}
	
	@ChildProcessor(info = true)
	public void addChildInfo(ProcessorInfo<BaseProcessor<?>> childInfo) {
		childInfos.put((ModelElement) childInfo.getElement(), childInfo);
	}
	
	@OutgoingEndpoint
	public void addOutgoingEndpoints(Connection connection, ConnectionProcessor connectionProcessor) {
		outgoingEndpoints.put(connection, CompletableFuture.completedStage(connectionProcessor).toCompletableFuture());
	}

	@ProcessorElement
	@Override
	public void setElement(Node element) {
		super.setElement(element);
	}
	
	@Override
	public Mono<Section> createSectionAsync() {
		// TODO image description if image and image narrator (URI)
		return super.createSectionAsync();
	}
	
	@Override
	public int compareTo(BaseProcessor<?> o) {
		int thisDistance = distance(o);
		int oDistance = o.distance(this);
		if (thisDistance == -1) {
			if (oDistance != -1) {
				return 1;
			}
		} else {
			if (oDistance == -1) {
				return -1;
			}
			int cmp = thisDistance - oDistance;
			if (cmp != 0) {
				return cmp;
			}
		}
		
		if (o instanceof NodeProcessor && element.getModel().getPage() == ((NodeProcessor) o).element.getModel().getPage()) {
			return configuration.getPageNodesComparator().compare(element, ((NodeProcessor) o).element);
		}		
		
		return super.compareTo(o);
	}
		
	@Override
	protected Message createMessage(int depth) {
		return new Message(this, depth) {
			
			@Override
			void process(Consumer<Message> publisher) {
				for (ProcessorInfo<BaseProcessor<?>> ci: childInfos.values()) {
					publisher.accept(ci.getProcessor().createMessage(depth + 1));					
				}
				for (CompletableFuture<ConnectionProcessor> ci: outgoingEndpoints.values()) {
					publisher.accept(ci.join().createMessage(depth + 1));					
				}						
				if (linkTargetProcessor != null) {
					publisher.accept(linkTargetProcessor.createMessage(depth + 1));
				}
			}
			
		};
	}	
	
//		Map<String, String> style = element.getStyle();
//		String image = style.get("image");
//		if (!Util.isBlank(image)) {
//			// Drawio does not add ;base64 to the image URL, browsers don't understand. Fixing it here.
//			if (image.startsWith(DATA_URI_PNG_PREFIX_NO_BASE_64)) {
//				int insertIdx = DATA_URI_PNG_PREFIX_NO_BASE_64.length() - 1;
//				image = image.substring(0, insertIdx) + ";base64" + image.substring(insertIdx);
//			} else if (image.startsWith(DATA_URI_JPEG_PREFIX_NO_BASE_64)) {
//				int insertIdx = DATA_URI_JPEG_PREFIX_NO_BASE_64.length() - 1;
//				image = image.substring(0, insertIdx) + ";base64" + image.substring(insertIdx);
//			}
//		
//			// TODO - describe here
//			
//		}
	
}
