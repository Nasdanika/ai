package org.nasdanika.ai.drawio;

import java.util.function.Consumer;

import org.nasdanika.common.Section;
import org.nasdanika.drawio.Connection;
import org.nasdanika.graph.processor.ProcessorElement;
import org.nasdanika.graph.processor.RegistryEntry;
import org.nasdanika.graph.processor.SourceHandler;

public class ConnectionProcessor extends LayerElementProcessor<Connection> {

	public ConnectionProcessor(DrawioProcessorFactory factory) {
		super(factory);
	}
	
	
	@Override
	protected Section doCreateSection() {
		Section section = super.doCreateSection();
		
		// TODO - style, geometry (direction), start and end decorations
		
		return section;
	}
	
	// icon?
	

	@ProcessorElement
	@Override
	public void setElement(Connection element) {
		super.setElement(element);
	}
		
	@SourceHandler
	public ConnectionProcessor getSourceHandler() {
		return this;
	}
		
	@RegistryEntry("#element.target == #this")
	public NodeProcessor targetProcessor;	
		
	@Override
	protected Message createMessage(int depth) {
		return new Message(this, depth) {
			
			@Override
			void process(Consumer<Message> publisher) {
				if (targetProcessor != null) {
					publisher.accept(targetProcessor.createMessage(depth + 1));
				}
				if (linkTargetProcessor != null) {
					publisher.accept(linkTargetProcessor.createMessage(depth + 1));
				}
			}
			
		};
	}
	
}
