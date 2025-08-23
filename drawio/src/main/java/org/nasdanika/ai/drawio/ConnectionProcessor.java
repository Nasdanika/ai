package org.nasdanika.ai.drawio;

import org.nasdanika.ai.Section;
import org.nasdanika.drawio.Connection;
import org.nasdanika.graph.processor.ProcessorElement;
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

}
